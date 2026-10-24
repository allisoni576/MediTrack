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

class UnlockScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsErrorOnInvalidPassword() {
        composeRule.setContent {
            UnlockScreen(
                username = "Pat",
                biometricEnabled = false,
                onUnlock = { _, onResult -> onResult(false) },
                onResetData = {},
                onUnlockSuccess = {},
                onBiometricUnlock = {},
            )
        }

        composeRule.onNodeWithText("Password").performTextInput("wrong")
        composeRule.onNodeWithText("Unlock").performClick()

        composeRule.onNodeWithText("Incorrect password").assertIsDisplayed()
    }

    @Test
    fun invokesSuccessCallbackOnValidPassword() {
        var successCount = 0

        composeRule.setContent {
            UnlockScreen(
                username = "Pat",
                biometricEnabled = false,
                onUnlock = { _, onResult -> onResult(true) },
                onResetData = {},
                onUnlockSuccess = { successCount++ },
                onBiometricUnlock = {},
            )
        }

        composeRule.onNodeWithText("Password").performTextInput("123456")
        composeRule.onNodeWithText("Unlock").performClick()

        assertEquals(1, successCount)
    }

    @Test
    fun exposesBiometricAndResetActions() {
        var resetCalled = false
        var biometricCalled = false

        composeRule.setContent {
            UnlockScreen(
                username = "Pat",
                biometricEnabled = true,
                onUnlock = { _, onResult -> onResult(false) },
                onResetData = { resetCalled = true },
                onUnlockSuccess = {},
                onBiometricUnlock = { biometricCalled = true },
            )
        }

        composeRule.onNodeWithText("Unlock with biometrics").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Reset App Data").performClick()

        assertTrue(biometricCalled)
        assertTrue(resetCalled)
    }

    @Test
    fun showsWelcomeTextWhenUsernamePresent() {
        composeRule.setContent {
            UnlockScreen(
                username = "Pat",
                biometricEnabled = false,
                onUnlock = { _, onResult -> onResult(false) },
                onResetData = {},
                onUnlockSuccess = {},
                onBiometricUnlock = {},
            )
        }

        composeRule.onNodeWithText("Welcome back, Pat").assertIsDisplayed()
    }
}

