package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventTable(
    @Json(name = "id") val id: String,
    @Json(name = "tableNumber") val tableNumber: Int,
    @Json(name = "tableName") val tableName: String? = null
)

@JsonClass(generateAdapter = true)
data class SessionRegistration(
    @Json(name = "userId") val userId: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "isHostReserved") val isHostReserved: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class GameSession(
    @Json(name = "id") val id: String,
    @Json(name = "tableId") val tableId: String,
    @Json(name = "tableNumber") val tableNumber: Int,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "slotIndex") val slotIndex: Int,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int,
    @Json(name = "status") val status: String,
    @Json(name = "registeredCount") val registeredCount: Int = 0,
    @Json(name = "isFull") val isFull: Boolean = false,
    @Json(name = "isUserRegistered") val isUserRegistered: Boolean = false,
    @Json(name = "registrations") val registrations: List<SessionRegistration>? = null
)

@JsonClass(generateAdapter = true)
data class EventSessionsResponse(
    @Json(name = "tables") val tables: List<EventTable>,
    @Json(name = "sessions") val sessions: List<GameSession>
)
