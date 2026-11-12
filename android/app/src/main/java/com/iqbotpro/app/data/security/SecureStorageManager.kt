package com.iqbotpro.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure credential storage for IQ BOT PRO.
 * Credentials are encrypted using Android Keystore.
 */
class SecureStorageManager(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Legacy Bridge settings - retained during migration
    fun saveBridgeHost(host: String) {
        prefs.edit().putString(KEY_BRIDGE_HOST, host.trim()).apply()
    }

    fun getBridgeHost(): String {
        return prefs.getString(KEY_BRIDGE_HOST, "ws://127.0.0.1:8765")
            ?: "ws://127.0.0.1:8765"
    }

    // PRACTICE account
    fun savePracticeAccountId(id: String) {
        prefs.edit().putString(KEY_ACCOUNT_ID, id.trim()).apply()
    }

    fun getPracticeAccountId(): String {
        return prefs.getString(KEY_ACCOUNT_ID, "") ?: ""
    }

    fun savePracticePassword(password: String) {
        prefs.edit().putString(KEY_ENCRYPTED_PASSWORD, password).apply()
    }

    fun getPracticePassword(): String {
        return prefs.getString(KEY_ENCRYPTED_PASSWORD, "") ?: ""
    }

    fun hasPracticePassword(): Boolean {
        return getPracticePassword().isNotEmpty()
    }

    // REAL account - stored separately
    fun saveRealAccountId(id: String) {
        prefs.edit().putString(KEY_REAL_ACCOUNT_ID, id.trim()).apply()
    }

    fun getRealAccountId(): String {
        return prefs.getString(KEY_REAL_ACCOUNT_ID, "") ?: ""
    }

    fun saveRealPassword(password: String) {
        prefs.edit().putString(KEY_REAL_PASSWORD, password).apply()
    }

    fun getRealPassword(): String {
        return prefs.getString(KEY_REAL_PASSWORD, "") ?: ""
    }

    fun hasRealPassword(): Boolean {
        return getRealPassword().isNotEmpty()
    }

    // Compatibility with the previous Bridge implementation
    fun hasSavedPassword(): Boolean = hasPracticePassword()

    fun getDecryptedPasswordForBridge(): String = getPracticePassword()

    // Clear credentials
    fun clearPracticeCredentials() {
        prefs.edit()
            .remove(KEY_ACCOUNT_ID)
            .remove(KEY_ENCRYPTED_PASSWORD)
            .apply()
    }

    fun clearRealCredentials() {
        prefs.edit()
            .remove(KEY_REAL_ACCOUNT_ID)
            .remove(KEY_REAL_PASSWORD)
            .apply()
    }

    fun clearCredentials() {
        clearPracticeCredentials()
        clearRealCredentials()
    }

    // Language
    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    companion object {
        private const val SECURE_PREFS_FILE = "iq_bot_pro_secure_vault"

        private const val KEY_BRIDGE_HOST = "bridge_host_url"

        // Existing Practice keys - do not rename
        private const val KEY_ACCOUNT_ID = "practice_account_id"
        private const val KEY_ENCRYPTED_PASSWORD = "enc_practice_password"

        // New Real keys
        private const val KEY_REAL_ACCOUNT_ID = "real_account_id"
        private const val KEY_REAL_PASSWORD = "enc_real_password"

        private const val KEY_LANGUAGE = "app_language"

        const val ACCOUNT_MODE_PRACTICE = "PRACTICE"
        const val ACCOUNT_MODE_REAL = "REAL"
    }
}
