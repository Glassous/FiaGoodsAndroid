package com.glassous.fiagoods.util

import com.glassous.fiagoods.BuildConfig

fun buildOssThumbnailUrl(url: String, widthPx: Int): String {
    if (!url.startsWith(BuildConfig.OSS_PUBLIC_BASE_URL)) return url
    val sep = if (url.contains("?")) "&" else "?"
    return "$url${sep}x-oss-process=image/resize,w_${widthPx}/quality,q_75/format,jpg"
}
