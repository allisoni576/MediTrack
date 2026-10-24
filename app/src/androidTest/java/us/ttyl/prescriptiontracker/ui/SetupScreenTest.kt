package us.ttyl.prescriptiontracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsValidationWhenPasswordTooShort() {
        composeRule.setContent {
            SetupScreen(onSetupComplete = { _, _, _ -> })
        }

        composeRule.onNodeWithText("Password").performTextInput("123")
        composeRule.onNodeWithText("Confirm password").performTextInput("123")
        composeRule.onNodeWithText("Set Up App").performClick()

        composeRule.onNodeWithText("Password must be at least 6 characters").assertIsDisplayed()
    }

    @Test
    fun submitsWhenInputsAreValid() {
        var capturedUsername = ""
        var capturedPassword = ""
        var capturedBiometric = false

        composeRule.setContent {
            SetupScreen(
                onSetupComplete = { username, password, biometricEnabled ->
                    capturedUsername = username
                    capturedPassword = password
                    capturedBiometric = biometricEnabled
                }
            )
        }

        composeRule.onNodeWithText("Display name (optional)").performTextInput("Pat")
        composeRule.onNodeWithText("Password").performTextInput("123456")
        composeRule.onNodeWithText("Confirm password").performTextInput("123456")
        composeRule.onNodeWithText("Enable biometric unlock").performClick()
        composeRule.onNodeWithText("Set Up App").performClick()

        assertEquals("Pat", capturedUsername)
        assertEquals("123456", capturedPassword)
        assertTrue(capturedBiometric)
    }

    @Test
    fun showsValidationWhenPasswordsDoNotMatch() {
        var called = false

        composeRule.setContent {
            SetupScreen(onSetupComplete = { _, _, _ -> called = true })
        }

        composeRule.onNodeWithText("Password").performTextInput("123456")
        composeRule.onNodeWithText("Confirm password").performTextInput("654321")
        composeRule.onNodeWithText("Set Up App").performClick()

        composeRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
        assertFalse(called)
    }
}

