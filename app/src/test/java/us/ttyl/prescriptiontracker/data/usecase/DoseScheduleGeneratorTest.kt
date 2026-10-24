package us.ttyl.prescriptiontracker.data.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import us.ttyl.prescriptiontracker.data.local.reminder.MedicationScheduleEntity
import us.ttyl.prescriptiontracker.data.local.reminder.RefillReminderEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderDao
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderInstanceEntity
import us.ttyl.prescriptiontracker.data.local.reminder.ReminderState

class DoseScheduleGeneratorTest {
    @Test
    fun createScheduleGeneratesDoseEventsForRollingWindow() = runBlocking {
        val dao = InMemoryReminderDao()
        val generator = DoseScheduleGenerator(dao)

        generator.createSchedule(medicationId = 1L, timesOfDay = listOf("08:00"))

        assertEquals(1, dao.schedules.size)
        // One dose event should be generated per day in the rolling window (today may be skipped if time has passed).
        assertTrue(dao.doseEvents.isNotEmpty())
        assertTrue(dao.doseEvents.size <= DoseScheduleGenerator.ROLLING_WINDOW_DAYS)
        assertEquals(dao.doseEvents.size, dao.reminderInstances.size)
    }

    @Test
    fun extendScheduleDoesNotDuplicateExistingDoseEvents() = runBlocking {
        val dao = InMemoryReminderDao()
        val generator = DoseScheduleGenerator(dao)

        generator.createSchedule(medicationId = 1L, timesOfDay = listOf("08:00"))
        val firstRunCount = dao.doseEvents.size

        generator.extendSchedule(medicationId = 1L)

        assertEquals(firstRunCount, dao.doseEvents.size)
    }

    @Test
    fun invalidTimeFormatIsIgnored() = runBlocking {
        val dao = InMemoryReminderDao()
        val generator = DoseScheduleGenerator(dao)

        generator.createSchedule(medicationId = 1L, timesOfDay = listOf("not-a-time"))

        assertEquals(1, dao.schedules.size)
        assertTrue(dao.doseEvents.isEmpty())
    }

    @Test
    fun pastTimeOfDayFiresImmediatelyForBrandNewSchedule() = runBlocking {
        val dao = InMemoryReminderDao()
        val generator = DoseScheduleGenerator(dao)
        val now = System.currentTimeMillis()
        val fiveMinutesAgo = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            add(java.util.Calendar.MINUTE, -5)
        }
        val pastTime = String.format(
            "%02d:%02d",
            fiveMinutesAgo.get(java.util.Calendar.HOUR_OF_DAY),
            fiveMinutesAgo.get(java.util.Calendar.MINUTE),
        )

        generator.createSchedule(medicationId = 1L, timesOfDay = listOf(pastTime))

        // A past time-of-day with no prior dose events should still generate one dose event,
        // and it should fire immediately (via its reminder instance's triggerAt) instead of being
        // skipped until the same time tomorrow. The dose event's own scheduledAt, however, should
        // continue to reflect the true intended dose time (five minutes ago), not "now", so that
        // history/overview screens show the medication's actual scheduled time.
        assertTrue(dao.doseEvents.isNotEmpty())
        val immediateEvent = dao.doseEvents.minByOrNull { it.scheduledAt }
        assertTrue(immediateEvent != null && immediateEvent.scheduledAt <= now)
        assertTrue(immediateEvent != null && immediateEvent.scheduledAt >= fiveMinutesAgo.timeInMillis - 60_000L)

        val immediateReminder = dao.reminderInstances.minByOrNull { it.triggerAt }
        assertTrue(immediateReminder != null && immediateReminder.triggerAt >= now - 1_000L)
        assertTrue(immediateReminder != null && immediateReminder.triggerAt <= now + 60_000L)
    }

    @Test
    fun extendScheduleDoesNotRefireAlreadyGeneratedPastOccurrence() = runBlocking {
        val dao = InMemoryReminderDao()
        val generator = DoseScheduleGenerator(dao)
        val now = System.currentTimeMillis()
        val fiveMinutesAgo = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            add(java.util.Calendar.MINUTE, -5)
        }
        val pastTime = String.format(
            "%02d:%02d",
            fiveMinutesAgo.get(java.util.Calendar.HOUR_OF_DAY),
            fiveMinutesAgo.get(java.util.Calendar.MINUTE),
        )

        generator.createSchedule(medicationId = 1L, timesOfDay = listOf(pastTime))
        val firstRunCount = dao.doseEvents.size

        generator.extendSchedule(medicationId = 1L)

        assertEquals(firstRunCount, dao.doseEvents.size)
    }
}

private class InMemoryReminderDao : ReminderDao {
    val schedules = mutableListOf<MedicationScheduleEntity>()
    val doseEvents = mutableListOf<DoseEventEntity>()
    val reminderInstances = mutableListOf<ReminderInstanceEntity>()
    private var nextScheduleId = 1L
    private var nextDoseEventId = 1L
    private var nextReminderId = 1L

    override suspend fun insertSchedule(schedule: MedicationScheduleEntity): Long {
        val id = nextScheduleId++
        schedules.add(schedule.copy(id = id))
        return id
    }

    override suspend fun insertDoseEvent(event: DoseEventEntity): Long {
        val id = nextDoseEventId++
        doseEvents.add(event.copy(id = id))
        return id
    }

    override suspend fun insertRefillReminder(refill: RefillReminderEntity): Long = 1L

    override suspend fun insertReminderInstance(instance: ReminderInstanceEntity): Long {
        val id = nextReminderId++
        reminderInstances.add(instance.copy(id = id))
        return id
    }

    override fun observeByState(state: ReminderState) = throw UnsupportedOperationException()

    override fun observeAll() = throw UnsupportedOperationException()

    override suspend fun updateReminderState(id: Long, newState: ReminderState) {}

    override suspend fun dueReminders(atMillis: Long, state: ReminderState): List<ReminderInstanceEntity> = emptyList()

    override suspend fun dueScheduledReminders(atMillis: Long): List<ReminderInstanceEntity> = emptyList()

    override suspend fun getAllScheduledReminders(): List<ReminderInstanceEntity> =
        reminderInstances.filter { it.state == ReminderState.SCHEDULED }

    override suspend fun getById(id: Long): ReminderInstanceEntity? = reminderInstances.find { it.id == id }

    override suspend fun rescheduleReminder(id: Long, triggerAt: Long, newState: ReminderState) {}

    override suspend fun getSchedulesForMedication(medicationId: Long): List<MedicationScheduleEntity> =
        schedules.filter { it.medicationId == medicationId }

    override suspend fun deleteSchedulesForMedication(medicationId: Long) {
        schedules.removeAll { it.medicationId == medicationId }
    }

    override suspend fun latestDoseEventForSchedule(scheduleId: Long, from: Long): DoseEventEntity? =
        doseEvents.filter { it.scheduleId == scheduleId && it.scheduledAt >= from }
            .maxByOrNull { it.scheduledAt }

    override fun observeDoseEventsForMedication(medicationId: Long) = throw UnsupportedOperationException()

    override fun observeAllDoseEvents() = throw UnsupportedOperationException()

    override suspend fun getDoseEventsInRange(from: Long, to: Long): List<DoseEventEntity> = emptyList()

    override suspend fun updateDoseEventStatus(id: Long, status: DoseStatus, takenAt: Long?) {}

    override suspend fun markOverdueDosesMissed(cutoff: Long) {}

    override suspend fun getDoseEventById(id: Long): DoseEventEntity? = doseEvents.find { it.id == id }

    override suspend fun getRefillReminderForMedication(medicationId: Long): RefillReminderEntity? = null


    override suspend fun getRefillReminderById(id: Long): RefillReminderEntity? = null

    override suspend fun deleteRefillRemindersForMedication(medicationId: Long) {}

    override suspend fun updateReminderNote(id: Long, note: String) {}

    override suspend fun getPendingDoseEventIds(medicationId: Long): List<Long> = emptyList()

    override suspend fun deletePendingDoseEventsForMedication(medicationId: Long) {}

    override suspend fun deleteDoseReminderInstances(doseEventIds: List<Long>) {}

    override suspend fun deleteRefillReminderInstances(refillIds: List<Long>) {}
}

