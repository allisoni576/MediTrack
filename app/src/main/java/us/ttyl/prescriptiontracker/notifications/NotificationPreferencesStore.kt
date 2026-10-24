package us.ttyl.prescriptiontracker.notifications

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class NotificationPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

private val Context.notificationPrefsDataStore by preferencesDataStore(name = "notification_prefs")

class NotificationPreferencesStore(private val context: Context) {
    private object Keys {
        val sound = booleanPreferencesKey("sound_enabled")
        val vibration = booleanPreferencesKey("vibration_enabled")
        val inactivityTimeoutMs = longPreferencesKey("inactivity_timeout_ms")
    }

    val preferencesFlow: Flow<NotificationPreferences> = context.notificationPrefsDataStore.data.map { prefs ->
        NotificationPreferences(
            soundEnabled = prefs[Keys.sound] ?: true,
            vibrationEnabled = prefs[Keys.vibration] ?: true,
        )
    }

    suspend fun preferences(): NotificationPreferences = preferencesFlow.first()

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.notificationPrefsDataStore.edit { it[Keys.sound] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.notificationPrefsDataStore.edit { it[Keys.vibration] = enabled }
    }
}

