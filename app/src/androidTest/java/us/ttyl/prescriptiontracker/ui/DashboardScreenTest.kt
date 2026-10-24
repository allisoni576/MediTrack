package us.ttyl.prescriptiontracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus

class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersViewMedicationsButtonAndTodaysMedicationsCard() {
        val medications = listOf(
            MedicationEntity(id = 1, name = "Aspirin", dosage = 1, notes = "")
        )

        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = medications,
                doseEvents = emptyList(),
                onOpenMedication = {},
                onOpenMedicationList = {},
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = {},
                onOpenSettings = {},
                onLock = {},
            )
        }

        composeRule.onNodeWithText("View Medications").assertIsDisplayed()
        composeRule.onNodeWithText("📋 Today's Medications").assertIsDisplayed()
    }

    @Test
    fun rendersZeroCountsWhenNoDosesToday() {
        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = emptyList(),
                doseEvents = emptyList(),
                onOpenMedication = {},
                onOpenMedicationList = {},
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = {},
                onOpenSettings = {},
                onLock = {},
            )
        }

        composeRule.onNodeWithText("0 Taken · 0 Missed · 0 Pending").assertIsDisplayed()
    }

    @Test
    fun rendersTodayScopedCountsForMixedStatuses() {
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 2, notes = "")
        val now = System.currentTimeMillis()
        val doseEvents = listOf(
            DoseEventEntity(id = 1, medicationId = 1, scheduledAt = now, status = DoseStatus.TAKEN),
            DoseEventEntity(id = 2, medicationId = 1, scheduledAt = now, status = DoseStatus.MISSED),
            DoseEventEntity(id = 3, medicationId = 1, scheduledAt = now, status = DoseStatus.PENDING),
            DoseEventEntity(id = 4, medicationId = 1, scheduledAt = now, status = DoseStatus.PENDING),
        )

        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = listOf(medication),
                doseEvents = doseEvents,
                onOpenMedication = {},
                onOpenMedicationList = {},
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = {},
                onOpenSettings = {},
                onLock = {},
            )
        }

        // Multiple PENDING dose events for the SAME medication should only contribute once to
        // the Pending count, since counts reflect distinct medications, not raw dose-event rows.
        composeRule.onNodeWithText("1 Taken · 1 Missed · 1 Pending").assertIsDisplayed()
    }

    @Test
    fun countsAreDistinctPerMedicationAcrossMultipleMedications() {
        val medications = listOf(
            MedicationEntity(id = 1, name = "Aspirin", dosage = 1, notes = ""),
            MedicationEntity(id = 2, name = "Ibuprofen", dosage = 1, notes = ""),
        )
        val now = System.currentTimeMillis()
        val doseEvents = listOf(
            // Medication 1 missed twice today (e.g. two doses/day) — should count as 1 missed medication.
            DoseEventEntity(id = 1, medicationId = 1, scheduledAt = now, status = DoseStatus.MISSED),
            DoseEventEntity(id = 2, medicationId = 1, scheduledAt = now, status = DoseStatus.MISSED),
            // Medication 2 missed once today.
            DoseEventEntity(id = 3, medicationId = 2, scheduledAt = now, status = DoseStatus.MISSED),
        )

        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = medications,
                doseEvents = doseEvents,
                onOpenMedication = {},
                onOpenMedicationList = {},
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = {},
                onOpenSettings = {},
                onLock = {},
            )
        }

        composeRule.onNodeWithText("0 Taken · 2 Missed · 0 Pending").assertIsDisplayed()
    }

    @Test
    fun callsOpenMedicationOverviewAction() {
        var called = false

        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = emptyList(),
                doseEvents = emptyList(),
                onOpenMedication = {},
                onOpenMedicationList = {},
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = { called = true },
                onOpenSettings = {},
                onLock = {},
            )
        }

        composeRule.onNodeWithText("📋 Today's Medications").performClick()
        assertTrue(called)
    }

    @Test
    fun callsOpenMedicationListAction() {
        var called = false
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 1, notes = "")

        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = listOf(medication),
                doseEvents = emptyList(),
                onOpenMedication = {},
                onOpenMedicationList = { called = true },
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = {},
                onOpenSettings = {},
                onLock = {},
            )
        }

        composeRule.onNodeWithText("View Medications").performClick()
        assertTrue(called)
    }

    @Test
    fun callsLockAction() {
        var called = false

        composeRule.setContent {
            DashboardScreen(
                username = "Pat",
                medications = emptyList(),
                doseEvents = emptyList(),
                onOpenMedication = {},
                onOpenMedicationList = {},
                onOpenReminders = {},
                onOpenHistory = {},
                onOpenMedicationOverview = {},
                onOpenSettings = {},
                onLock = { called = true },
            )
        }

        composeRule.onNodeWithText("Lock").performClick()
        assertTrue(called)
    }
}

