package us.ttyl.prescriptiontracker.data.usecase

import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import java.util.Calendar

/**
 * Computes a one-time estimated refill date for a medication from its supply, dosage, and
 * schedule. The result is calculated once (at create/edit time) from [totalQuantity]; it is
 * not user-editable and does not recur.
 *
 * Algorithm:
 * 1. `daysOfMedication = totalQuantity / (dosage * timesPerDay)` (floor division — this
 *    ensures the reminder never fires after supply would already be exhausted).
 * 2. If [frequencyType] is DAILY: `refillDate = startDate + daysOfMedication days`.
 * 3. If [frequencyType] is WEEKLY: walk forward day-by-day starting at (and including)
 *    `startDate`, decrementing `daysOfMedication` each time the current day's weekday is one
 *    of the chosen dosing days ([daysOfWeekMask]); the day on which it reaches zero is the
 *    refill date. `startDate` itself (day 0) is evaluated, since the first dose is taken then.
 */
object RefillCalculator {

    /**
     * Returns the estimated epoch-millis refill date, or null if it cannot be calculated
     * (e.g. no supply consumed per day, or a WEEKLY schedule with no days selected).
     */
    fun calculateRefillDate(
        totalQuantity: Int,
        dosage: Int,
        timesPerDay: Int,
        frequencyType: FrequencyType,
        daysOfWeekMask: Int,
        startDate: Long,
    ): Long? {
        val dosesPerDay = timesPerDay.coerceAtLeast(1)
        val consumedPerDay = dosage * dosesPerDay
        if (consumedPerDay <= 0 || totalQuantity <= 0) return null

        // Floor division: the reminder should never fire later than the day supply truly runs out.
        val daysOfMedication = totalQuantity / consumedPerDay

        return when (frequencyType) {
            FrequencyType.DAILY -> Calendar.getInstance().apply {
                timeInMillis = startDate
                add(Calendar.DAY_OF_YEAR, daysOfMedication)
            }.timeInMillis

            FrequencyType.WEEKLY -> {
                if (daysOfWeekMask == 0) return null
                var remaining = daysOfMedication
                val cal = Calendar.getInstance().apply { timeInMillis = startDate }
                // Day 0 (startDate itself) is evaluated before any advancement, since the first
                // dose is taken on the start date if it falls on a chosen dosing day.
                while (remaining != 0) {
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 .. SATURDAY=7
                    val bit = 1 shl (dayOfWeek - 1)
                    if (daysOfWeekMask and bit != 0) {
                        remaining -= 1
                        if (remaining == 0) break
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
        }
    }
}

