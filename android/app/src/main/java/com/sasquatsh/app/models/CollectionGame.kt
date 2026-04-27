package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CollectionGame(
    @Json(name = "id") val id: String,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "gameName") val gameName: String = "",
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "yearPublished") val yearPublished: Int? = null,
    @Json(name = "bggRank") val bggRank: Int? = null,
    @Json(name = "averageRating") val averageRating: Double? = null,
    @Json(name = "playingTime") val playingTime: Int? = null
)

data class AddCollectionGameInput(
    @Json(name = "bggId") val bggId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "yearPublished") val yearPublished: Int? = null,
    @Json(name = "bggRank") val bggRank: Int? = null,
    @Json(name = "averageRating") val averageRating: Double? = null
)
