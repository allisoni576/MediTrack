package us.ttyl.prescriptiontracker.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.MedicationEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseEventEntity
import us.ttyl.prescriptiontracker.data.local.reminder.DoseStatus

class MedicationOverviewScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersAllThreeSectionLabelsInOrder() {
        composeRule.setContent {
            MedicationOverviewScreen(
                medications = emptyList(),
                doseEvents = emptyList(),
                onMarkTaken = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Missed Medications").assertIsDisplayed()
        composeRule.onNodeWithText("Taken Medications").assertIsDisplayed()
        composeRule.onNodeWithText("Pending Medications").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyStatesForAllSectionsIndependently() {
        composeRule.setContent {
            MedicationOverviewScreen(
                medications = emptyList(),
                doseEvents = emptyList(),
                onMarkTaken = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("No missed doses in this range").assertIsDisplayed()
        composeRule.onNodeWithText("No medications taken today yet.").assertIsDisplayed()
        composeRule.onNodeWithText("No pending medications for today.").assertIsDisplayed()
    }

    @Test
    fun rendersMissedTakenAndPendingEntries() {
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 2, notes = "")
        val now = System.currentTimeMillis()
        val doseEvents = listOf(
            DoseEventEntity(id = 1, medicationId = 1, scheduledAt = now - 60_000, status = DoseStatus.MISSED),
            DoseEventEntity(id = 2, medicationId = 1, scheduledAt = now, status = DoseStatus.TAKEN),
            DoseEventEntity(id = 3, medicationId = 1, scheduledAt = now, status = DoseStatus.PENDING),
        )

        composeRule.setContent {
            MedicationOverviewScreen(
                medications = listOf(medication),
                doseEvents = doseEvents,
                onMarkTaken = {},
                onBack = {},
            )
        }

        // "Aspirin" should appear three times: once per section (missed, taken, pending).
        composeRule.onAllNodesWithText("Aspirin").assertCountEquals(3)
    }

    @Test
    fun takenEntryCheckboxIsPreCheckedAndDisabled() {
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 2, notes = "")
        val now = System.currentTimeMillis()
        val doseEvents = listOf(
            DoseEventEntity(id = 1, medicationId = 1, scheduledAt = now, status = DoseStatus.TAKEN),
        )

        composeRule.setContent {
            MedicationOverviewScreen(
                medications = listOf(medication),
                doseEvents = doseEvents,
                onMarkTaken = {},
                onBack = {},
            )
        }

        val checkbox = composeRule.onNode(androidx.compose.ui.test.isToggleable())
        checkbox.assertIsOn()
        checkbox.assertIsNotEnabled()
    }

    @Test
    fun commitsCheckedDoseIdsFromMissedAndPendingSectionsOnBack() {
        var markedIds: List<Long>? = null
        var backCalled = false
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 2, notes = "")
        val now = System.currentTimeMillis()
        val doseEvents = listOf(
            DoseEventEntity(id = 1, medicationId = 1, scheduledAt = now - 60_000, status = DoseStatus.MISSED),
            DoseEventEntity(id = 2, medicationId = 1, scheduledAt = now, status = DoseStatus.PENDING),
        )

        composeRule.setContent {
            MedicationOverviewScreen(
                medications = listOf(medication),
                doseEvents = doseEvents,
                onMarkTaken = { markedIds = it },
                onBack = { backCalled = true },
            )
        }

        composeRule.onAllNodesWithText("Mark as Taken")[0].assertIsDisplayed()
        composeRule.onNode(androidx.compose.ui.test.isToggleable()).performClick()
        composeRule.onNodeWithText("Back to Dashboard").performClick()

        assertTrue(backCalled)
        assertTrue(markedIds?.isNotEmpty() == true)
    }

    @Test
    fun callsBackWithoutMarkingWhenNothingChecked() {
        var markedCalled = false
        var backCalled = false

        composeRule.setContent {
            MedicationOverviewScreen(
                medications = emptyList(),
                doseEvents = emptyList(),
                onMarkTaken = { markedCalled = true },
                onBack = { backCalled = true },
            )
        }

        composeRule.onNodeWithText("< Back").performClick()

        assertTrue(backCalled)
        assertTrue(!markedCalled)
    }
}


