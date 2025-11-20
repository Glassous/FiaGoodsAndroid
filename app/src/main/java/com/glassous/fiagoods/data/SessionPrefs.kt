package com.glassous.fiagoods.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionPrefs {
    private const val FILE = "fiagoods_secure_prefs"
    private const val KEY_VERIFIED = "verified"
    private const val KEY_PASSWORD = "password"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_CARD_DENSITY = "card_density"

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isVerified(context: Context): Boolean = prefs(context).getBoolean(KEY_VERIFIED, false)
    fun getPassword(context: Context): String? = prefs(context).getString(KEY_PASSWORD, null)
    fun setVerified(context: Context, password: String) {
        prefs(context).edit().putBoolean(KEY_VERIFIED, true).putString(KEY_PASSWORD, password).apply()
    }
    fun clearVerified(context: Context) {
        prefs(context).edit().putBoolean(KEY_VERIFIED, false).remove(KEY_PASSWORD).apply()
    }

    fun getThemeMode(context: Context): String = prefs(context).getString(KEY_THEME_MODE, "system") ?: "system"
    fun setThemeMode(context: Context, mode: String) {
        val value = when (mode) {
            "system", "light", "dark" -> mode
            else -> "system"
        }
        prefs(context).edit().putString(KEY_THEME_MODE, value).apply()
    }

    fun getCardDensity(context: Context): Int = prefs(context).getInt(KEY_CARD_DENSITY, 3)
    fun setCardDensity(context: Context, density: Int) {
        val v = density.coerceIn(0, 10)
        prefs(context).edit().putInt(KEY_CARD_DENSITY, v).apply()
    }
}
