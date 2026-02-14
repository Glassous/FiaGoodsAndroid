package com.glassous.fiagoods.data

import com.glassous.fiagoods.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.annotation.Keep

@Keep
data class AdItem(
    val imageName: String,
    val text: String
)

class AdApi {
    private val client = OkHttpClient.Builder()
        .callTimeout(java.time.Duration.ofSeconds(10))
        .connectTimeout(java.time.Duration.ofSeconds(5))
        .readTimeout(java.time.Duration.ofSeconds(10))
        .build()
    private val gson = Gson()

    private val baseAdsUrl: String by lazy {
        val versionUrl = BuildConfig.APP_VERSION_JSON_URL.trim()
        val lastSlash = versionUrl.lastIndexOf('/')
        if (lastSlash != -1) {
            versionUrl.substring(0, lastSlash + 1)
        } else {
            ""
        }
    }

    suspend fun fetchAds(): List<AdItem> {
        if (baseAdsUrl.isBlank()) return emptyList()

        val adsUrl = "${baseAdsUrl}ads.json"

        return withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(adsUrl).get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList<AdItem>()
                    val body = resp.body?.string() ?: return@use emptyList<AdItem>()
                    try {
                        val list = gson.fromJson(body, Array<AdItem>::class.java)
                        list.toList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun buildImageUrl(imageName: String): String {
        // "images directly placed in R2 object storage adsimage folder"
        // Use the same base URL as ads.json (derived from version.json URL)
        return "${baseAdsUrl}adsimage/$imageName"
    }
}
