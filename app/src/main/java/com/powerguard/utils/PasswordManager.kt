package com.powerguard.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.powerguard.PowerGuardApp

class PasswordManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setPassword(password: String) {
        encryptedPrefs.edit().putString(PowerGuardApp.KEY_PASSWORD, password).apply()
    }

    fun getPassword(): String? {
        return encryptedPrefs.getString(PowerGuardApp.KEY_PASSWORD, null)
    }

    fun verifyPassword(password: String): Boolean {
        val storedPassword = getPassword()
        return storedPassword != null && storedPassword == password
    }

    fun hasPassword(): Boolean {
        return getPassword() != null
    }

    fun isProtectionEnabled(): Boolean {
        return encryptedPrefs.getBoolean(PowerGuardApp.KEY_PROTECTION_ENABLED, false)
    }

    fun setProtectionEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(PowerGuardApp.KEY_PROTECTION_ENABLED, enabled).apply()
    }

    fun isShizukuEnabled(): Boolean {
        return encryptedPrefs.getBoolean(PowerGuardApp.KEY_SHIZUKU_ENABLED, false)
    }

    fun setShizukuEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(PowerGuardApp.KEY_SHIZUKU_ENABLED, enabled).apply()
    }
}
