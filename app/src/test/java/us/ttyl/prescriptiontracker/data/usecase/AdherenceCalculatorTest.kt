package us.ttyl.prescriptiontracker.data.usecase

import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AdherenceCalculatorTest {
    @Test
    fun emptyListReturnsFullAdherence() {
        val summary = AdherenceCalculator.calculate(emptyList())
        assertEquals(100, summary.adherencePercent)
        assertEquals(0, summary.totalDoses)
    }

    @Test
    fun allTakenIsHundredPercent() {
        val events = listOf(
            doseEvent(status = DoseStatus.TAKEN),
            doseEvent(status = DoseStatus.TAKEN),
        )
        val summary = AdherenceCalculator.calculate(events)
        assertEquals(100, summary.adherencePercent)
        assertEquals(2, summary.takenDoses)
    }

    @Test
    fun halfMissedIsFiftyPercent() {
        val events = listOf(
            doseEvent(status = DoseStatus.TAKEN),
            doseEvent(status = DoseStatus.MISSED),
        )
        val summary = AdherenceCalculator.calculate(events)
        assertEquals(50, summary.adherencePercent)
        assertEquals(1, summary.takenDoses)
        assertEquals(1, summary.missedDoses)
    }

    @Test
    fun pendingDosesExcludedFromPercentCalculation() {
        val events = listOf(
            doseEvent(status = DoseStatus.TAKEN),
            doseEvent(status = DoseStatus.PENDING),
            doseEvent(status = DoseStatus.SNOOZED),
        )
        val summary = AdherenceCalculator.calculate(events)
        assertEquals(100, summary.adherencePercent)
        assertEquals(2, summary.pendingDoses)
    }

    private fun doseEvent(status: DoseStatus) = DoseEventEntity(
        id = 0,
        medicationId = 1,
        scheduledAt = System.currentTimeMillis(),
        status = status,
    )
}

