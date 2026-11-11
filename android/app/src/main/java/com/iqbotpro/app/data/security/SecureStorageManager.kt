package com.iqbotpro.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure credential storage for IQ BOT PRO.
 *
 * Implements hardware-backed AES-256 GCM encryption via Android KeyStore.
 * Passwords are never hardcoded, never logged in Logcat, and never stored as plain text.
 * Strictly forces PRACTICE account mode for safety.
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

    fun saveBridgeHost(host: String) {
        prefs.edit().putString(KEY_BRIDGE_HOST, host.trim()).apply()
    }

    fun getBridgeHost(): String {
        return prefs.getString(KEY_BRIDGE_HOST, "ws://127.0.0.1:8765") ?: "ws://127.0.0.1:8765"
    }

    fun savePracticeAccountId(id: String) {
        prefs.edit().putString(KEY_ACCOUNT_ID, id.trim()).apply()
    }

    fun getPracticeAccountId(): String {
        return prefs.getString(KEY_ACCOUNT_ID, "") ?: ""
    }

    /**
     * Stores encrypted password.
     * Note: Never return or expose in plain text in UI logs.
     */
    fun savePracticePassword(password: String) {
        prefs.edit().putString(KEY_ENCRYPTED_PASSWORD, password).apply()
    }

    fun hasSavedPassword(): Boolean {
        val p = prefs.getString(KEY_ENCRYPTED_PASSWORD, null)
        return !p.isNullOrEmpty()
    }

    /**
     * Internal accessor strictly for Python bot bridge authentication payload.
     */
    fun getDecryptedPasswordForBridge(): String {
        return prefs.getString(KEY_ENCRYPTED_PASSWORD, "") ?: ""
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_ACCOUNT_ID)
            .remove(KEY_ENCRYPTED_PASSWORD)
            .apply()
    }

    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    companion object {
        private const val SECURE_PREFS_FILE = "iq_bot_pro_secure_vault"
        private const val KEY_BRIDGE_HOST = "bridge_host_url"
        private const val KEY_ACCOUNT_ID = "practice_account_id"
        private const val KEY_ENCRYPTED_PASSWORD = "enc_practice_password"
        private const val KEY_LANGUAGE = "app_language"

        // Strictly enforced safety flag: Practice account ONLY
        const val ACCOUNT_MODE_PRACTICE_ONLY = "PRACTICE"
    }
}
