package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BggSearchResult(
    @Json(name = "bggId") val bggId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "yearPublished") val yearPublished: Int? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null
) {
    val id: Int get() = bggId
}

@JsonClass(generateAdapter = true)
data class BggGame(
    @Json(name = "bggId") val bggId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "yearPublished") val yearPublished: Int? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "minPlaytime") val minPlaytime: Int? = null,
    @Json(name = "maxPlaytime") val maxPlaytime: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "categories") val categories: List<String> = emptyList(),
    @Json(name = "mechanics") val mechanics: List<String> = emptyList()
) {
    val id: Int get() = bggId
}

@JsonClass(generateAdapter = true)
data class BggCachedGame(
    @Json(name = "bggId") val bggId: Int,
    @Json(name = "name") val name: String,
    @Json(name = "yearPublished") val yearPublished: Int? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "bggRank") val bggRank: Int? = null
) {
    val id: Int get() = bggId
}

@JsonClass(generateAdapter = true)
data class BggCacheListResponse(
    @Json(name = "games") val games: List<BggCachedGame>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "totalPages") val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class EventGame(
    @Json(name = "id") val id: String,
    @Json(name = "eventId") val eventId: String,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "isPrimary") val isPrimary: Boolean = false,
    @Json(name = "isAlternative") val isAlternative: Boolean = false,
    @Json(name = "addedByUserId") val addedByUserId: String? = null,
    @Json(name = "createdAt") val createdAt: String
)

data class AddEventGameInput(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "isPrimary") val isPrimary: Boolean? = null,
    @Json(name = "isAlternative") val isAlternative: Boolean? = null
)
