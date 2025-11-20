package com.glassous.fiagoods.data.model

import com.google.gson.annotations.SerializedName

data class CargoItem(
    val id: String,
    val description: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerializedName("group_name") val groupName: String,
    val category: String,
    val price: Double,
    val link: String
)
