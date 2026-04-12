package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BggGameDto(
    @Json(name = "bgg_id") val bggId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "year_published") val yearPublished: Int?,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "min_players") val minPlayers: Int?,
    @Json(name = "max_players") val maxPlayers: Int?,
    @Json(name = "playing_time") val playingTime: Int?,
    @Json(name = "min_playing_time") val minPlayingTime: Int?,
    @Json(name = "max_playing_time") val maxPlayingTime: Int?,
    @Json(name = "description") val description: String?,
    @Json(name = "average_rating") val averageRating: Double?,
    @Json(name = "categories") val categories: List<String>?,
    @Json(name = "mechanics") val mechanics: List<String>?,
)
