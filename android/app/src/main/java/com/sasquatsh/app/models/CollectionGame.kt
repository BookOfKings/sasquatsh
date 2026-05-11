package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CollectionGame(
    @Json(name = "id") val id: String = "",
    @Json(name = "bgg_id") val bggId: Int? = null,
    @Json(name = "game_name") val gameName: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "min_players") val minPlayers: Int? = null,
    @Json(name = "max_players") val maxPlayers: Int? = null,
    @Json(name = "year_published") val yearPublished: Int? = null,
    @Json(name = "bgg_rank") val bggRank: Int? = null,
    @Json(name = "average_rating") val averageRating: Double? = null,
    @Json(name = "playing_time") val playingTime: Int? = null
)

@JsonClass(generateAdapter = true)
data class CollectionResponse(
    @Json(name = "games") val games: List<CollectionGame>? = null,
    @Json(name = "added") val added: Int? = null
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
