package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun SetupScreen(
    onSetupComplete: (username: String, password: String, biometricEnabled: Boolean) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Text("Set Up PrescriptionTracker")
    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Display name (optional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("Confirm password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Enable biometric unlock")
        Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it })
    }

    if (error.isNotEmpty()) {
        Text(error)
    }

    Button(
        onClick = {
            when {
                password.length < 6 -> error = "Password must be at least 6 characters"
                password != confirmPassword -> error = "Passwords do not match"
                else -> {
                    error = ""
                    onSetupComplete(username.trim(), password, biometricEnabled)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Set Up App")
    }
}

