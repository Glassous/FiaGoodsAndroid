package com.glassous.fiagoods.data

import com.glassous.fiagoods.BuildConfig
import com.glassous.fiagoods.data.model.CargoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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
                val type = object : TypeToken<List<CargoItem>>() {}.type
                gson.fromJson<List<CargoItem>>(body, type) ?: emptyList()
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
                val type = object : TypeToken<List<CargoItem>>() {}.type
                val list = gson.fromJson<List<CargoItem>>(body, type) ?: emptyList()
                list.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun fetchAppPassword(): String? {
        if (baseRest.isBlank() || apiKey.isBlank()) return null
        return try {
            val url = "$baseRest/app_secrets?key=eq.login_password&select=value"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val type = object : TypeToken<List<Map<String, String>>>() {}.type
                val list = gson.fromJson<List<Map<String, String>>>(body, type) ?: emptyList()
                list.firstOrNull()?.get("value")
            }
        } catch (e: Exception) {
            null
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
                val type = object : TypeToken<List<Map<String, String>>>() {}.type
                val list = gson.fromJson<List<Map<String, String>>>(body, type) ?: emptyList()
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
                val type = object : TypeToken<List<CargoItem>>() {}.type
                val list = gson.fromJson<List<CargoItem>>(resBody, type) ?: emptyList()
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
                val resBody = resp.body?.string() ?: return null
                val type = object : TypeToken<List<CargoItem>>() {}.type
                val list = gson.fromJson<List<CargoItem>>(resBody, type) ?: emptyList()
                list.firstOrNull()
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
                val type = object : TypeToken<List<Map<String, String>>>() {}.type
                val list = gson.fromJson<List<Map<String, String>>>(body, type) ?: emptyList()
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
