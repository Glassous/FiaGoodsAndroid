package com.glassous.fiagoods.data.model

import com.google.gson.annotations.SerializedName

data class CargoItem(
    val id: String,
    val name: String,
    val category: String,
    val stock: Int?,
    val price: Double?,
    val sold: Int,
    val brief: String,
    val description: String,
    val specs: String,
    @SerializedName("image_urls") val imageUrls: List<String> = emptyList()
)