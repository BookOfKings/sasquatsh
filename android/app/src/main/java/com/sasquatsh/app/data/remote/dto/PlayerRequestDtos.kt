package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerRequestDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "gameTitle") val gameTitle: String?,
    @Json(name = "eventDate") val eventDate: String?,
    @Json(name = "startTime") val startTime: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "state") val state: String?,
    @Json(name = "postalCode") val postalCode: String?,
    @Json(name = "maxPlayers") val maxPlayers: Int?,
    @Json(name = "currentPlayers") val currentPlayers: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "user") val user: UserSummaryDto?,
)
