package com.glassous.fiagoods.data.model

import com.google.gson.annotations.SerializedName

data class CargoItem(
    val id: String,
    val description: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerializedName("group_names") val groupNames: List<String> = emptyList(),
    @SerializedName("categories") val categories: List<String> = emptyList(),
    val price: Double,
    val link: String,
    @SerializedName("is_favorite") val isFavorite: Boolean = false
)
