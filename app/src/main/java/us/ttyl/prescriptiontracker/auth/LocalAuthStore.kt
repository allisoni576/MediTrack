package us.ttyl.prescriptiontracker.auth

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

data class AuthState(
    val isConfigured: Boolean,
    val username: String,
    val biometricEnabled: Boolean,
    val lastUnlockAt: Long,
    val failedAttempts: Int = 0,
    val lockoutUntil: Long = 0L,
)

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

class LocalAuthStore(private val context: Context) {
    private object Keys {
        val username = stringPreferencesKey("username")
        val passwordSalt = stringPreferencesKey("password_salt")
        val passwordHash = stringPreferencesKey("password_hash")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val lastUnlockAt = longPreferencesKey("last_unlock_at")
        val failedAttempts = androidx.datastore.preferences.core.intPreferencesKey("failed_attempts")
        val lockoutUntil = longPreferencesKey("lockout_until")
    }

    val authState: Flow<AuthState> = context.authDataStore.data.map { prefs ->
        AuthState(
            isConfigured = prefs[Keys.passwordHash] != null && prefs[Keys.passwordSalt] != null,
            username = prefs[Keys.username].orEmpty(),
            biometricEnabled = prefs[Keys.biometricEnabled] ?: false,
            lastUnlockAt = prefs[Keys.lastUnlockAt] ?: 0L,
            failedAttempts = prefs[Keys.failedAttempts] ?: 0,
            lockoutUntil = prefs[Keys.lockoutUntil] ?: 0L,
        )
    }

    suspend fun setup(username: String, password: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        context.authDataStore.edit { prefs ->
            prefs[Keys.username] = username.trim()
            prefs[Keys.passwordSalt] = base64(salt)
            prefs[Keys.passwordHash] = base64(hash)
            prefs[Keys.biometricEnabled] = false
            prefs[Keys.lastUnlockAt] = System.currentTimeMillis()
            prefs[Keys.failedAttempts] = 0
            prefs[Keys.lockoutUntil] = 0L
        }
    }

    /** Returns remaining lockout milliseconds, or 0 if not locked out. */
    suspend fun remainingLockoutMillis(): Long {
        val prefs = context.authDataStore.data.first()
        val until = prefs[Keys.lockoutUntil] ?: 0L
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    suspend fun validatePassword(password: String): Boolean {
        if (remainingLockoutMillis() > 0) return false

        val prefs = context.authDataStore.data.first()
        val saltB64 = prefs[Keys.passwordSalt] ?: return false
        val hashB64 = prefs[Keys.passwordHash] ?: return false
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val actual = hashPassword(password, Base64.decode(saltB64, Base64.NO_WRAP))
        val isValid = MessageDigest.isEqual(expected, actual)
        if (isValid) {
            context.authDataStore.edit {
                it[Keys.lastUnlockAt] = System.currentTimeMillis()
                it[Keys.failedAttempts] = 0
                it[Keys.lockoutUntil] = 0L
            }
        } else {
            registerFailedAttempt()
        }
        return isValid
    }

    private suspend fun registerFailedAttempt() {
        context.authDataStore.edit { prefs ->
            val attempts = (prefs[Keys.failedAttempts] ?: 0) + 1
            prefs[Keys.failedAttempts] = attempts
            if (attempts >= 5) {
                val backoffSeconds = 30L * (1 shl minOf(attempts - 5, 5))
                prefs[Keys.lockoutUntil] = System.currentTimeMillis() + backoffSeconds * 1000
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.authDataStore.edit { it[Keys.biometricEnabled] = enabled }
    }

    suspend fun recordUnlock() {
        context.authDataStore.edit { it[Keys.lastUnlockAt] = System.currentTimeMillis() }
    }

    suspend fun resetAll() {
        context.authDataStore.edit { it.clear() }
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }

    private fun base64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
