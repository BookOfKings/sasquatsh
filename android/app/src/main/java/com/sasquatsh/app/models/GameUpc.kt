package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpcLookupResult(
    @Json(name = "upc") val upc: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "searched_for") val searchedFor: String? = null,
    @Json(name = "bgg_info_status") val bggInfoStatus: String? = null,
    @Json(name = "bgg_info") val bggInfo: List<UpcBggInfo>? = null
)

@JsonClass(generateAdapter = true)
data class UpcBggInfo(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "published") val published: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "confidence") val confidence: Double? = null
)
