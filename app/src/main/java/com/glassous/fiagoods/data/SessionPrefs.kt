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
    private const val KEY_OSS_ENDPOINT = "oss_endpoint"
    private const val KEY_OSS_BUCKET = "oss_bucket"
    private const val KEY_OSS_ACCESS_KEY_ID = "oss_access_key_id"
    private const val KEY_OSS_ACCESS_KEY_SECRET = "oss_access_key_secret"
    private const val KEY_OSS_PUBLIC_BASE_URL = "oss_public_base_url"

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

    fun getOssEndpoint(context: Context): String? = prefs(context).getString(KEY_OSS_ENDPOINT, null)
    fun getOssBucket(context: Context): String? = prefs(context).getString(KEY_OSS_BUCKET, null)
    fun getOssAccessKeyId(context: Context): String? = prefs(context).getString(KEY_OSS_ACCESS_KEY_ID, null)
    fun getOssAccessKeySecret(context: Context): String? = prefs(context).getString(KEY_OSS_ACCESS_KEY_SECRET, null)
    fun getOssPublicBaseUrl(context: Context): String? = prefs(context).getString(KEY_OSS_PUBLIC_BASE_URL, null)

    fun setOssConfig(
        context: Context,
        endpoint: String?,
        bucket: String?,
        accessKeyId: String?,
        accessKeySecret: String?,
        publicBaseUrl: String?
    ) {
        val editor = prefs(context).edit()
        if (endpoint != null) editor.putString(KEY_OSS_ENDPOINT, endpoint)
        if (bucket != null) editor.putString(KEY_OSS_BUCKET, bucket)
        if (accessKeyId != null) editor.putString(KEY_OSS_ACCESS_KEY_ID, accessKeyId)
        if (accessKeySecret != null) editor.putString(KEY_OSS_ACCESS_KEY_SECRET, accessKeySecret)
        if (publicBaseUrl != null) editor.putString(KEY_OSS_PUBLIC_BASE_URL, publicBaseUrl)
        editor.apply()
    }
}
