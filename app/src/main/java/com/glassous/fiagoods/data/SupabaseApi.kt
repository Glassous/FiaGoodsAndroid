package com.glassous.fiagoods.data

import com.glassous.fiagoods.BuildConfig
import com.glassous.fiagoods.data.model.CargoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.Type

class SupabaseApi {
    private val client = OkHttpClient.Builder()
        .callTimeout(java.time.Duration.ofSeconds(20))
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .readTimeout(java.time.Duration.ofSeconds(20))
        .writeTimeout(java.time.Duration.ofSeconds(20))
        .build()
    private val gson = Gson()
    private val baseRest = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1"
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY

    companion object {
        // 【核心修改】：使用 getParameterized 动态构建类型，彻底解决 R8 混淆导致的泛型丢失问题
        // 这里的 List 使用 Java 的 List，但在 Kotlin 中可以直接写 List::class.java
        val TYPE_LIST_CARGO: Type = TypeToken.getParameterized(List::class.java, CargoItem::class.java).type

        // 内部使用的 Map List 类型
        private val TYPE_LIST_MAP_STRING: Type = TypeToken.getParameterized(List::class.java,
            TypeToken.getParameterized(Map::class.java, String::class.java, String::class.java).type
        ).type
    }

    fun fetchCargoItems(): List<CargoItem> {
        if (baseRest.isBlank() || apiKey.isBlank()) return emptyList()
        return try {
            val url = "$baseRest/cargo_items?select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                gson.fromJson<List<CargoItem>>(body, TYPE_LIST_CARGO) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun fetchCargoItem(id: String): CargoItem? {
        if (baseRest.isBlank() || apiKey.isBlank()) return null
        return try {
            val url = "$baseRest/cargo_items?id=eq.$id&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val list = gson.fromJson<List<CargoItem>>(body, TYPE_LIST_CARGO) ?: emptyList()
                list.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    @Throws(Exception::class)
    fun fetchAppPassword(): String {
        if (baseRest.contains("null") || baseRest.length < 10) throw Exception("Config Error: URL=$baseRest")
        if (apiKey.isBlank()) throw Exception("Config Error: API Key is blank")

        val url = "$baseRest/app_secrets?key=eq.login_password&select=value"
        val req = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""

                if (!resp.isSuccessful) {
                    throw Exception("HTTP ${resp.code}: $body")
                }
                if (body.isBlank()) {
                    throw Exception("Response body is empty")
                }

                try {
                    val list = gson.fromJson<List<Map<String, String>>>(body, TYPE_LIST_MAP_STRING)

                    if (list == null) throw Exception("Gson returned null. Body: $body")
                    if (list.isEmpty()) throw Exception("List is empty. Body: $body")

                    val item = list.firstOrNull() ?: throw Exception("First item null")
                    return item["value"] ?: throw Exception("Key 'value' not found in: $item")

                } catch (e: Exception) {
                    throw Exception("Parse Error: ${e.message} | Body snippet: ${body.take(100)}")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun fetchOssConfig(): Map<String, String> {
        if (baseRest.isBlank() || apiKey.isBlank()) return emptyMap()
        return try {
            val url = "$baseRest/app_secrets?or=(key.eq.oss_endpoint,key.eq.oss_bucket,key.eq.oss_access_key_id,key.eq.oss_access_key_secret,key.eq.oss_public_base_url)&select=key,value"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyMap()
                val body = resp.body?.string() ?: return emptyMap()
                val list = gson.fromJson<List<Map<String, String>>>(body, TYPE_LIST_MAP_STRING) ?: emptyList()
                list.associate { (it["key"] ?: "") to (it["value"] ?: "") }.filterKeys { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun createCargoItem(item: CargoItem): CargoItem? {
        if (baseRest.isBlank() || apiKey.isBlank()) return null
        return try {
            val url = "$baseRest/cargo_items"
            val json = gson.toJson(item)
            val body: RequestBody = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val resBody = resp.body?.string() ?: return null
                val list = gson.fromJson<List<CargoItem>>(resBody, TYPE_LIST_CARGO) ?: emptyList()
                list.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun updateCargoItem(id: String, patch: Map<String, Any?>): CargoItem? {
        if (baseRest.isBlank() || apiKey.isBlank()) return null
        return try {
            val url = "$baseRest/cargo_items?id=eq.$id"
            val json = gson.toJson(patch)
            val body: RequestBody = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=representation")
                .method("PATCH", body)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val resBody = resp.body?.string()
                if (resBody.isNullOrBlank()) {
                    return fetchCargoItem(id)
                }
                val list = gson.fromJson<List<CargoItem>>(resBody, TYPE_LIST_CARGO) ?: emptyList()
                if (list.isNotEmpty()) list.first() else fetchCargoItem(id)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun deleteCargoItem(id: String): Boolean {
        if (baseRest.isBlank() || apiKey.isBlank()) return false
        return try {
            val url = "$baseRest/cargo_items?id=eq.$id"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=minimal")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    fun fetchFavoriteItemIds(): List<String> {
        if (baseRest.isBlank() || apiKey.isBlank()) return emptyList()
        return try {
            val url = "$baseRest/favorite_items?select=item_id"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val list = gson.fromJson<List<Map<String, String>>>(body, TYPE_LIST_MAP_STRING) ?: emptyList()
                list.mapNotNull { it["item_id"] }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFavorite(itemId: String): Boolean {
        if (baseRest.isBlank() || apiKey.isBlank()) return false
        return try {
            val url = "$baseRest/favorite_items"
            val json = gson.toJson(mapOf("item_id" to itemId))
            val body: RequestBody = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build()
            client.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    fun removeFavorite(itemId: String): Boolean {
        if (baseRest.isBlank() || apiKey.isBlank()) return false
        return try {
            val url = "$baseRest/favorite_items?item_id=eq.$itemId"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .addHeader("Prefer", "return=minimal")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}