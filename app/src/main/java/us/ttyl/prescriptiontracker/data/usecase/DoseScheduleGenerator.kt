package us.ttyl.prescriptiontracker.data.usecase

import us.ttyl.prescriptiontracker.data.local.MedicationDao
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderType
import java.util.Calendar

/**
 * Generates upcoming dose events + reminder instances from a medication's
 * configured schedule times, and keeps a rolling window topped up.
 */
class DoseScheduleGenerator(
    private val reminderDao: ReminderDao,
    private val medicationDao: MedicationDao? = null,
) {
    companion object {
        const val ROLLING_WINDOW_DAYS = 7
    }

    /** Creates schedule rows for the given times (HH:mm, 24h) and seeds the rolling window. */
    suspend fun createSchedule(medicationId: Long, timesOfDay: List<String>) {
        timesOfDay.forEach { time ->
            val scheduleId = reminderDao.insertSchedule(
                us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity(
                    medicationId = medicationId,
                    timeOfDay = time,
                )
            )
            generateDoseEventsForSchedule(medicationId, scheduleId, time, ROLLING_WINDOW_DAYS)
        }
    }

    /** Called periodically to ensure every active schedule has future dose events queued. */
    suspend fun extendSchedule(medicationId: Long) {
        val schedules = reminderDao.getSchedulesForMedication(medicationId)
        schedules.forEach { schedule ->
            generateDoseEventsForSchedule(medicationId, schedule.id, schedule.timeOfDay, ROLLING_WINDOW_DAYS)
        }
    }

    private suspend fun generateDoseEventsForSchedule(
        medicationId: Long,
        scheduleId: Long,
        timeOfDay: String,
        daysAhead: Int,
    ) {
        val (hour, minute) = parseTime(timeOfDay) ?: return
        val medication = medicationDao?.getById(medicationId)
        val now = System.currentTimeMillis()
        val latest = reminderDao.latestDoseEventForSchedule(scheduleId, now - 24L * 60 * 60 * 1000)
        val startDay = 0

        for (dayOffset in startDay until daysAhead) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val scheduledAt = cal.timeInMillis
            val isPast = scheduledAt < now - 60_000L
            var triggerAt = scheduledAt
            if (isPast) {
                // Normally a past occurrence is skipped (we wait for the next future occurrence).
                // However, if this schedule has no dose events yet at all (brand new schedule,
                // e.g. a medication just added with a start date/time already in the past), allow
                // this very first occurrence to fire immediately instead of silently waiting until
                // the same time tomorrow. This makes it possible to create medications with a
                // start time in the past and have the reminder fire right away, which is useful
                // for manual/automated testing. [scheduledAt] always reflects the true intended
                // dose time (so history/overview screens show the correct time), while
                // [triggerAt] controls when the reminder notification actually fires.
                if (latest != null || dayOffset != startDay) continue
                triggerAt = now
            }
            if (latest != null && scheduledAt <= latest.scheduledAt) continue
            if (medication != null) {
                if (scheduledAt < medication.startDate) continue
                val end = medication.endDate
                if (end != null && scheduledAt > end) continue
                if (medication.frequencyType == FrequencyType.WEEKLY) {
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 .. SATURDAY=7
                    val bit = 1 shl (dayOfWeek - 1)
                    if (medication.daysOfWeekMask and bit == 0) continue
                }
            }

            val doseEventId = reminderDao.insertDoseEvent(
                DoseEventEntity(
                    medicationId = medicationId,
                    scheduleId = scheduleId,
                    scheduledAt = scheduledAt,
                )
            )
            reminderDao.insertReminderInstance(
                ReminderInstanceEntity(
                    type = ReminderType.DOSE,
                    relatedEntityId = doseEventId,
                    triggerAt = triggerAt,
                    state = ReminderState.SCHEDULED,
                )
            )
        }
    }

    private fun parseTime(timeOfDay: String): Pair<Int, Int>? {
        val parts = timeOfDay.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
