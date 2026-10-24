package us.ttyl.prescriptiontracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Text("Settings")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Notification sound")
        Switch(checked = soundEnabled, onCheckedChange = onSoundChanged)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Notification vibration")
        Switch(checked = vibrationEnabled, onCheckedChange = onVibrationChanged)
    }
    if (biometricAvailable) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Biometric unlock")
            Switch(checked = biometricEnabled, onCheckedChange = onBiometricChanged)
        }
    } else {
        Text("Biometric unlock is unavailable on this device")
    }

    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Back")
    }
}

