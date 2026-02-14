package com.glassous.fiagoods.data

import com.glassous.fiagoods.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateApi {
    private val client = OkHttpClient.Builder()
        .callTimeout(java.time.Duration.ofSeconds(20))
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .readTimeout(java.time.Duration.ofSeconds(20))
        .writeTimeout(java.time.Duration.ofSeconds(20))
        .build()
    private val gson = Gson()

    data class VersionInfo(
        val versionCode: Int,
        val versionName: String,
        val apkFileName: String,
        val releaseNotes: List<String>?,
        val publishedAt: String?,
        @SerializedName("artTitleVersion")
        val artTitleVersion: Int? = null
    )

    data class FetchResult(
        val info: VersionInfo?,
        val error: String?,
        val httpCode: Int?,
        val bodyPreview: String?,
        val url: String
    )

    suspend fun fetchLatestVersionInfoVerbose(): FetchResult {
        val url = BuildConfig.APP_VERSION_JSON_URL.trim()
        if (url.isBlank()) return FetchResult(null, "APP_VERSION_JSON_URL is blank", null, null, "")
        return withContext(Dispatchers.IO) {
            try {
                // Add timestamp to prevent caching
                val urlWithTimestamp = if (url.contains("?")) "$url&t=${System.currentTimeMillis()}" else "$url?t=${System.currentTimeMillis()}"
                val req = Request.Builder().url(urlWithTimestamp).get().build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()
                    val preview = body?.let { if (it.length > 512) it.substring(0, 512) else it }
                    if (!resp.isSuccessful) return@withContext FetchResult(null, "HTTP ${resp.code} ${resp.message}", resp.code, preview, url)
                    if (body == null) return@withContext FetchResult(null, "Empty body", resp.code, null, url)
                    try {
                        val info = gson.fromJson(body, VersionInfo::class.java)
                        FetchResult(info, null, resp.code, preview, url)
                    } catch (e: Exception) {
                        FetchResult(null, e.toString(), resp.code, preview, url)
                    }
                }
            } catch (e: Exception) {
                FetchResult(null, e.toString(), null, null, url)
            }
        }
    }

    fun buildApkDownloadUrl(info: VersionInfo): String {
        val base = BuildConfig.APP_DOWNLOAD_BASE_URL.trimEnd('/')
        val name = info.apkFileName.trimStart('/')
        return "$base/$name"
    }
}
