package com.drawtaxi.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecureCredentialsManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        "drawtaxi_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var ovhSmtpUsername: String
        get() = securePrefs.getString(KEY_OVH_USERNAME, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_OVH_USERNAME, value).apply()

    var ovhSmtpPassword: String
        get() = securePrefs.getString(KEY_OVH_PASSWORD, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_OVH_PASSWORD, value).apply()

    fun hasCredentials(): Boolean {
        return ovhSmtpUsername.isNotBlank() && ovhSmtpPassword.isNotBlank()
    }

    fun clearCredentials() {
        securePrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_OVH_USERNAME = "ovh_smtp_username"
        private const val KEY_OVH_PASSWORD = "ovh_smtp_password"
    }
}
