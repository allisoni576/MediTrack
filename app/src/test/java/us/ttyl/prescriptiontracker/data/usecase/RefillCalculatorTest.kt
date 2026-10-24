package us.ttyl.prescriptiontracker.data.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.reminder.FrequencyType
import java.util.Calendar

class RefillCalculatorTest {

    private fun calendarFor(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            set(year, month, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun dailyMedicationAddsDaysOfMedicationToStartDate() {
        // 30 total, 1 unit/dose, 1x/day => 30 days of medication.
        val start = calendarFor(2026, Calendar.JANUARY, 1)
        val expected = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 30) }

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 30,
            dosage = 1,
            timesPerDay = 1,
            frequencyType = FrequencyType.DAILY,
            daysOfWeekMask = 0,
            startDate = start.timeInMillis,
        )

        assertEquals(expected.timeInMillis, refillDate)
    }

    @Test
    fun dailyMedicationAccountsForMultipleDosesPerDay() {
        // 30 total, 1 unit/dose, 3x/day => consumed 3/day => floor(30/3) = 10 days.
        val start = calendarFor(2026, Calendar.JANUARY, 1)
        val expected = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 10) }

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 30,
            dosage = 1,
            timesPerDay = 3,
            frequencyType = FrequencyType.DAILY,
            daysOfWeekMask = 0,
            startDate = start.timeInMillis,
        )

        assertEquals(expected.timeInMillis, refillDate)
    }

    @Test
    fun dailyMedicationFloorsUnevenDivision() {
        // 100 total, 30/day => floor(100/30) = 3 days, not 3.33.
        val start = calendarFor(2026, Calendar.JANUARY, 1)
        val expected = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 3) }

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 100,
            dosage = 30,
            timesPerDay = 1,
            frequencyType = FrequencyType.DAILY,
            daysOfWeekMask = 0,
            startDate = start.timeInMillis,
        )

        assertEquals(expected.timeInMillis, refillDate)
    }

    @Test
    fun weeklyMedicationEvaluatesStartDateAsDayZero() {
        // Start date itself is a Monday, Monday is the only chosen day, and there's exactly
        // 1 day of medication (totalQuantity == dosage). The refill date should be the start
        // date itself, proving day 0 (the start date) is evaluated rather than skipped.
        val monday = calendarFor(2026, Calendar.JANUARY, 5) // 2026-01-05 is a Monday
        assertEquals(Calendar.MONDAY, monday.get(Calendar.DAY_OF_WEEK))
        val mondayBit = 1 shl (Calendar.MONDAY - 1)

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 1,
            dosage = 1,
            timesPerDay = 1,
            frequencyType = FrequencyType.WEEKLY,
            daysOfWeekMask = mondayBit,
            startDate = monday.timeInMillis,
        )

        assertEquals(monday.timeInMillis, refillDate)
    }

    @Test
    fun weeklyMedicationAdvancesAcrossMultipleChosenDays() {
        // Start on Monday; dosing days are Monday and Wednesday; totalQuantity covers 3 doses.
        // Day 0 (Mon) -> 1 consumed, next chosen day Wed -> 2 consumed, next chosen day
        // following Mon -> 3 consumed -> that day is the refill date.
        val monday = calendarFor(2026, Calendar.JANUARY, 5) // Monday
        val mondayBit = 1 shl (Calendar.MONDAY - 1)
        val wednesdayBit = 1 shl (Calendar.WEDNESDAY - 1)
        val expected = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) } // following Monday

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 3,
            dosage = 1,
            timesPerDay = 1,
            frequencyType = FrequencyType.WEEKLY,
            daysOfWeekMask = mondayBit or wednesdayBit,
            startDate = monday.timeInMillis,
        )

        assertEquals(expected.timeInMillis, refillDate)
    }

    @Test
    fun weeklyMedicationWithNoDaysSelectedReturnsNull() {
        val start = calendarFor(2026, Calendar.JANUARY, 5)

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 10,
            dosage = 1,
            timesPerDay = 1,
            frequencyType = FrequencyType.WEEKLY,
            daysOfWeekMask = 0,
            startDate = start.timeInMillis,
        )

        assertNull(refillDate)
    }

    @Test
    fun zeroConsumptionReturnsNull() {
        val start = calendarFor(2026, Calendar.JANUARY, 5)

        val refillDate = RefillCalculator.calculateRefillDate(
            totalQuantity = 0,
            dosage = 1,
            timesPerDay = 1,
            frequencyType = FrequencyType.DAILY,
            daysOfWeekMask = 0,
            startDate = start.timeInMillis,
        )

        assertNull(refillDate)
    }
}

