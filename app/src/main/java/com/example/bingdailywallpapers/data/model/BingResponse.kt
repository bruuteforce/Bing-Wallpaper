package com.example.bingdailywallpapers.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class BingResponse(
    @Json(name = "images") val images: List<BingImage>?,
    @Json(name = "tooltips") val tooltips: Tooltips?
)

@Serializable
@JsonClass(generateAdapter = true)
data class BingImage(
    @Json(name = "startdate") val startdate: String?,
    @Json(name = "fullstartdate") val fullstartdate: String?,
    @Json(name = "enddate") val enddate: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "urlbase") val urlbase: String?,
    @Json(name = "copyright") val copyright: String?,
    @Json(name = "copyrightlink") val copyrightlink: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "quiz") val quiz: String?,
    @Json(name = "wp") val wp: Boolean?,
    @Json(name = "hsh") val hsh: String?
) {
    val fullImageUrl: String
        get() = if (url?.startsWith("http") == true) url else "https://www.bing.com${url ?: ""}"
}

fun BingImage.isFavoritedIn(favorites: List<BingImage>): Boolean {
    val targetId = fullImageUrl.ifEmpty { urlbase ?: url ?: hsh ?: "" }
    if (targetId.isEmpty()) return false
    return favorites.any { fav ->
        val favId = fav.fullImageUrl.ifEmpty { fav.urlbase ?: fav.url ?: fav.hsh ?: "" }
        favId == targetId
    }
}

@Serializable
@JsonClass(generateAdapter = true)
data class Tooltips(
    @Json(name = "loading") val loading: String?,
    @Json(name = "previous") val previous: String?,
    @Json(name = "next") val next: String?,
    @Json(name = "walle") val walle: String?,
    @Json(name = "walls") val walls: String?
)

