package us.ttyl.prescriptiontracker.data.usecase

import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import java.util.Calendar

data class AdherenceSummary(
    val totalDoses: Int,
    val takenDoses: Int,
    val missedDoses: Int,
    val pendingDoses: Int,
) {
    /** Percentage (0-100) of resolved doses (taken+missed) that were taken on time. */
    val adherencePercent: Int
        get() {
            val resolved = takenDoses + missedDoses
            if (resolved == 0) return 100
            return ((takenDoses.toDouble() / resolved) * 100).toInt()
        }
}

object AdherenceCalculator {
    fun calculate(events: List<DoseEventEntity>): AdherenceSummary {
        var taken = 0
        var missed = 0
        var pending = 0
        events.forEach { event ->
            when (event.status) {
                DoseStatus.TAKEN -> taken++
                DoseStatus.MISSED -> missed++
                DoseStatus.PENDING, DoseStatus.SNOOZED -> pending++
            }
        }
        return AdherenceSummary(
            totalDoses = events.size,
            takenDoses = taken,
            missedDoses = missed,
            pendingDoses = pending,
        )
    }

    /** Scopes the adherence summary to doses scheduled for the current calendar day only. */
    fun calculateForToday(events: List<DoseEventEntity>, nowMillis: Long = System.currentTimeMillis()): AdherenceSummary {
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24L * 60 * 60 * 1000
        val todayEvents = events.filter { it.scheduledAt in startOfDay until endOfDay }
        return calculate(todayEvents)
    }
}

