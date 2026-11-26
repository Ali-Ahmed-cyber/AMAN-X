package com.amanx.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amanx.app.data.security.CryptoManager
import com.amanx.app.domain.model.AppLanguage
import com.amanx.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.amanXDataStore: DataStore<Preferences> by preferencesDataStore(name = "amanx_preferences")

data class UserPreferencesState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.AR,
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val compactDensity: Boolean = false,
    val fontScaleMultiplier: Float = 1.0f,
    val onboardingCompleted: Boolean = false,
    val pinConfigured: Boolean = false,
    val pinDeferred: Boolean = false,
    val biometricEnabled: Boolean = false,
    val alarmVibrationEnabled: Boolean = true,
    val alarmSoundEnabled: Boolean = true,
    val smartAlertDeduplication: Boolean = true,
    val lastBackupAt: Long = 0L,
    val failedPinAttempts: Int = 0,
    val lockoutUntilEpochMs: Long = 0L
)

class AmanXPreferencesDataStore(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val COMPACT_DENSITY = booleanPreferencesKey("compact_density")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PIN_CONFIGURED = booleanPreferencesKey("pin_configured")
        val PIN_DEFERRED = booleanPreferencesKey("pin_deferred")
        val PIN_SALT_ENC = stringPreferencesKey("pin_salt_enc")
        val PIN_HASH_ENC = stringPreferencesKey("pin_hash_enc")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val ALARM_VIBRATION = booleanPreferencesKey("alarm_vibration")
        val ALARM_SOUND = booleanPreferencesKey("alarm_sound")
        val SMART_DEDUP = booleanPreferencesKey("smart_dedup")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.amanXDataStore.data.map { prefs ->
        UserPreferencesState(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.AR,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
            highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
            compactDensity = prefs[Keys.COMPACT_DENSITY] ?: false,
            fontScaleMultiplier = prefs[Keys.FONT_SCALE]?.toFloatOrNull() ?: 1.0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            pinConfigured = prefs[Keys.PIN_CONFIGURED] ?: false,
            pinDeferred = prefs[Keys.PIN_DEFERRED] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            alarmVibrationEnabled = prefs[Keys.ALARM_VIBRATION] ?: true,
            alarmSoundEnabled = prefs[Keys.ALARM_SOUND] ?: true,
            smartAlertDeduplication = prefs[Keys.SMART_DEDUP] ?: true,
            lastBackupAt = prefs[Keys.LAST_BACKUP_AT] ?: 0L,
            failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
            lockoutUntilEpochMs = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.amanXDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.amanXDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.amanXDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.amanXDataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }

    suspend fun setCompactDensity(enabled: Boolean) {
        context.amanXDataStore.edit { it[Keys.COMPACT_DENSITY] = enabled }
    }

    suspend fun setFontScaleMultiplier(scale: Float) {
        context.amanXDataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.35f).toString() }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.amanXDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun deferPinSetup() {
        context.amanXDataStore.edit {
            it[Keys.PIN_DEFERRED] = true
            it[Keys.PIN_CONFIGURED] = false
        }
    }

    suspend fun configurePin(pinChars: CharArray, enableBiometric: Boolean) {
        val digest = cryptoManager.hashPin(pinChars)
        val encSalt = cryptoManager.encryptString(digest.saltBase64)
        val encHash = cryptoManager.encryptString(digest.hashBase64)
        context.amanXDataStore.edit {
            it[Keys.PIN_SALT_ENC] = encSalt
            it[Keys.PIN_HASH_ENC] = encHash
            it[Keys.PIN_CONFIGURED] = true
            it[Keys.PIN_DEFERRED] = false
            it[Keys.BIOMETRIC_ENABLED] = enableBiometric
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(pinChars: CharArray): Boolean {
        val prefs = context.amanXDataStore.data.first()
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        if (lockoutUntil > now) return false

        val encSalt = prefs[Keys.PIN_SALT_ENC] ?: return false
        val encHash = prefs[Keys.PIN_HASH_ENC] ?: return false
        val salt = runCatching { cryptoManager.decryptString(encSalt) }.getOrNull() ?: return false
        val expectedHash = runCatching { cryptoManager.decryptString(encHash) }.getOrNull() ?: return false

        val ok = cryptoManager.verifyPin(pinChars, salt, expectedHash)
        context.amanXDataStore.edit {
            if (ok) {
                it[Keys.FAILED_PIN_ATTEMPTS] = 0
                it[Keys.LOCKOUT_UNTIL] = 0L
            } else {
                val failures = (it[Keys.FAILED_PIN_ATTEMPTS] ?: 0) + 1
                it[Keys.FAILED_PIN_ATTEMPTS] = failures
                val lockoutSeconds = when {
                    failures >= 8 -> 900L
                    failures >= 5 -> 120L
                    failures >= 3 -> 30L
                    else -> 0L
                }
                if (lockoutSeconds > 0L) {
                    it[Keys.LOCKOUT_UNTIL] = now + lockoutSeconds * 1000L
                }
            }
        }
        return ok
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.amanXDataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAlarmOptions(vibration: Boolean, sound: Boolean) {
        context.amanXDataStore.edit {
            it[Keys.ALARM_VIBRATION] = vibration
            it[Keys.ALARM_SOUND] = sound
        }
    }

    suspend fun setSmartDeduplication(enabled: Boolean) {
        context.amanXDataStore.edit { it[Keys.SMART_DEDUP] = enabled }
    }

    suspend fun recordBackupTimestamp(timestamp: Long) {
        context.amanXDataStore.edit { it[Keys.LAST_BACKUP_AT] = timestamp }
    }

    suspend fun resetAppearanceDefaults() {
        context.amanXDataStore.edit {
            it[Keys.THEME_MODE] = ThemeMode.DARK.name
            it[Keys.REDUCE_MOTION] = false
            it[Keys.HIGH_CONTRAST] = false
            it[Keys.COMPACT_DENSITY] = false
            it[Keys.FONT_SCALE] = "1.0"
        }
    }

    suspend fun clearAllForWipe() {
        context.amanXDataStore.edit { prefs ->
            val lang = prefs[Keys.LANGUAGE] ?: AppLanguage.AR.name
            prefs.clear()
            prefs[Keys.LANGUAGE] = lang
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }
}
