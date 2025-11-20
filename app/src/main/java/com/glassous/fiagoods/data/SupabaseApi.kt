package com.glassous.fiagoods.data

import com.glassous.fiagoods.BuildConfig
import com.glassous.fiagoods.data.model.CargoItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request

class SupabaseApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseRest = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1"
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY

    fun fetchCargoItems(): List<CargoItem> {
        if (baseRest.isBlank() || apiKey.isBlank()) return emptyList()
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
            return gson.fromJson<List<CargoItem>>(body, type) ?: emptyList()
        }
    }

    fun fetchCargoItem(id: String): CargoItem? {
        if (baseRest.isBlank() || apiKey.isBlank()) return null
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
            return list.firstOrNull()
        }
    }

    fun fetchAppPassword(): String? {
        if (baseRest.isBlank() || apiKey.isBlank()) return null
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
            val value = list.firstOrNull()?.get("value")
            return value
        }
    }
}