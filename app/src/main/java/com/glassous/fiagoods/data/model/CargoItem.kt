package com.glassous.fiagoods.data.model

import com.google.gson.annotations.SerializedName

data class CargoItem(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerializedName("group_names") val groupNames: List<String> = emptyList(),
    @SerializedName("categories") val categories: List<String> = emptyList(),
    @SerializedName("price") val price: Double,
    @SerializedName("link") val link: String,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("copy_count") val copyCount: Int = 0
)