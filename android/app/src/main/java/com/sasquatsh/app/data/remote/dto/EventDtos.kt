package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserSummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
    @Json(name = "isFoundingMember") val isFoundingMember: Boolean?,
    @Json(name = "isAdmin") val isAdmin: Boolean?,
)

@JsonClass(generateAdapter = true)
data class EventSummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "gameTitle") val gameTitle: String?,
    @Json(name = "gameCategory") val gameCategory: String?,
    @Json(name = "gameSystem") val gameSystem: String?,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "durationMinutes") val durationMinutes: Int?,
    @Json(name = "city") val city: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "difficultyLevel") val difficultyLevel: String?,
    @Json(name = "maxPlayers") val maxPlayers: Int,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean?,
    @Json(name = "confirmedCount") val confirmedCount: Int?,
    @Json(name = "isPublic") val isPublic: Boolean?,
    @Json(name = "isCharityEvent") val isCharityEvent: Boolean?,
    @Json(name = "minAge") val minAge: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "primaryGameThumbnail") val primaryGameThumbnail: String?,
    @Json(name = "host") val host: UserSummaryDto?,
)

@JsonClass(generateAdapter = true)
data class EventDetailDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "gameTitle") val gameTitle: String?,
    @Json(name = "gameCategory") val gameCategory: String?,
    @Json(name = "gameSystem") val gameSystem: String?,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "durationMinutes") val durationMinutes: Int?,
    @Json(name = "city") val city: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "postalCode") val postalCode: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "difficultyLevel") val difficultyLevel: String?,
    @Json(name = "maxPlayers") val maxPlayers: Int,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean?,
    @Json(name = "isPublic") val isPublic: Boolean?,
    @Json(name = "isCharityEvent") val isCharityEvent: Boolean?,
    @Json(name = "minAge") val minAge: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "groupId") val groupId: String?,
    @Json(name = "isMultiTable") val isMultiTable: Boolean?,
    @Json(name = "host") val host: UserSummaryDto?,
    @Json(name = "registrations") val registrations: List<RegistrationDto>?,
    @Json(name = "items") val items: List<EventItemDto>?,
    @Json(name = "games") val games: List<EventGameDto>?,
)

@JsonClass(generateAdapter = true)
data class RegistrationDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "status") val status: String,
    @Json(name = "registeredAt") val registeredAt: String?,
    @Json(name = "user") val user: UserSummaryDto?,
)

@JsonClass(generateAdapter = true)
data class EventItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "itemName") val itemName: String,
    @Json(name = "itemCategory") val itemCategory: String?,
    @Json(name = "quantityNeeded") val quantityNeeded: Int?,
    @Json(name = "claimedByUserId") val claimedByUserId: String?,
    @Json(name = "claimedAt") val claimedAt: String?,
    @Json(name = "claimedBy") val claimedBy: UserSummaryDto?,
)

@JsonClass(generateAdapter = true)
data class EventGameDto(
    @Json(name = "id") val id: String,
    @Json(name = "bggId") val bggId: Int?,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String?,
    @Json(name = "minPlayers") val minPlayers: Int?,
    @Json(name = "maxPlayers") val maxPlayers: Int?,
    @Json(name = "playingTime") val playingTime: Int?,
    @Json(name = "isPrimary") val isPrimary: Boolean?,
    @Json(name = "isAlternative") val isAlternative: Boolean?,
)
