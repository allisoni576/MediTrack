package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp

@Composable
fun UnlockScreen(
    username: String,
    biometricEnabled: Boolean,
    onUnlock: (password: String, onResult: (Boolean) -> Unit) -> Unit,
    onResetData: () -> Unit,
    onUnlockSuccess: () -> Unit,
    onBiometricUnlock: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Text("Unlock PrescriptionTracker",  style = MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp)
    if (username.isNotBlank()) {
        Text("Welcome back, $username",  style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp)
    }

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    if (error.isNotEmpty()) {
        Text(error)
    }

    Button(
        onClick = {
            onUnlock(password) { valid ->
                if (valid) {
                    error = ""
                    onUnlockSuccess()
                } else {
                    error = "Incorrect password"
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Unlock")
    }

    if (biometricEnabled) {
        Button(
            onClick = onBiometricUnlock,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Unlock with biometrics")
        }
    }

    Button(
        onClick = onResetData,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Reset App Data")
    }
}

