package us.ttyl.prescriptiontracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import us.ttyl.prescriptiontracker.data.local.MedicationEntity

class MedicationListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersMedicationCards() {
        val medications = listOf(
            MedicationEntity(id = 1, name = "Aspirin", dosage = 1, notes = "")
        )

        composeRule.setContent {
            MedicationListScreen(
                medications = medications,
                onOpenMedication = {},
                onAddMedication = {},
                onDeleteMedication = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("Aspirin").assertIsDisplayed()
    }

    @Test
    fun callsOpenMedicationDetails() {
        var openedMedication: MedicationEntity? = null
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 1, notes = "")

        composeRule.setContent {
            MedicationListScreen(
                medications = listOf(medication),
                onOpenMedication = { openedMedication = it },
                onAddMedication = {},
                onDeleteMedication = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("View Details").performClick()
        assertTrue(openedMedication?.id == 1L)
    }

    @Test
    fun callsAddMedicationAction() {
        var called = false

        composeRule.setContent {
            MedicationListScreen(
                medications = emptyList(),
                onOpenMedication = {},
                onAddMedication = { called = true },
                onDeleteMedication = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("+").performClick()
        assertTrue(called)
    }

    @Test
    fun callsBackAction() {
        var called = false

        composeRule.setContent {
            MedicationListScreen(
                medications = emptyList(),
                onOpenMedication = {},
                onAddMedication = {},
                onDeleteMedication = {},
                onBack = { called = true },
            )
        }

        composeRule.onNodeWithText("< Back").performClick()
        assertTrue(called)
    }

    @Test
    fun callsDeleteMedicationAction() {
        var deletedMedication: MedicationEntity? = null
        val medication = MedicationEntity(id = 1, name = "Aspirin", dosage = 1, notes = "")

        composeRule.setContent {
            MedicationListScreen(
                medications = listOf(medication),
                onOpenMedication = {},
                onAddMedication = {},
                onDeleteMedication = { deletedMedication = it },
                onBack = {},
            )
        }

        composeRule.onNodeWithText("-").performClick()
        assertTrue(deletedMedication?.id == 1L)
    }
}
