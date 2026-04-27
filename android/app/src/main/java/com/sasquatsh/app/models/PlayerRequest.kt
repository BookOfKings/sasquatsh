package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerRequest(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "eventId") val eventId: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "playerCountNeeded") val playerCountNeeded: Int = 0,
    @Json(name = "status") val status: String,
    @Json(name = "isActive") val isActive: Boolean = false,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "expiresAt") val expiresAt: String,
    @Json(name = "event") val event: PlayerRequestEvent? = null,
    @Json(name = "host") val host: UserSummary? = null
)

@JsonClass(generateAdapter = true)
data class PlayerRequestEvent(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null
)

data class CreatePlayerRequestInput(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "playerCountNeeded") val playerCountNeeded: Int? = null
)

data class UpdatePlayerRequestInput(
    @Json(name = "description") val description: String? = null,
    @Json(name = "playerCountNeeded") val playerCountNeeded: Int? = null
)

data class PlayerRequestFilters(
    val eventId: String? = null,
    val city: String? = null,
    val state: String? = null,
    val eventLocationId: String? = null
) {
    fun toQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        eventId?.let { map["eventId"] = it }
        city?.let { map["city"] = it }
        state?.let { map["state"] = it }
        eventLocationId?.let { map["eventLocationId"] = it }
        return map
    }
}

@JsonClass(generateAdapter = true)
data class EventLocation(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "city") val city: String,
    @Json(name = "state") val state: String,
    @Json(name = "venue") val venue: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "isPermanent") val isPermanent: Boolean? = null,
    @Json(name = "recurringDays") val recurringDays: List<Int>? = null,
    @Json(name = "status") val status: EventLocationStatus,
    @Json(name = "eventCount") val eventCount: Int? = null,
    @Json(name = "userCount") val userCount: Int? = null,
    @Json(name = "createdByUserId") val createdByUserId: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "createdBy") val createdBy: UserSummary? = null
)

data class CreateEventLocationInput(
    @Json(name = "name") val name: String,
    @Json(name = "city") val city: String,
    @Json(name = "state") val state: String,
    @Json(name = "venue") val venue: String? = null,
    @Json(name = "isPermanent") val isPermanent: Boolean? = null,
    @Json(name = "recurringDays") val recurringDays: List<Int>? = null,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "endDate") val endDate: String? = null
)

@JsonClass(generateAdapter = true)
data class GameInvitation(
    @Json(name = "id") val id: String,
    @Json(name = "eventId") val eventId: String,
    @Json(name = "inviteCode") val inviteCode: String,
    @Json(name = "invitedByUserId") val invitedByUserId: String,
    @Json(name = "invitedEmail") val invitedEmail: String? = null,
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "acceptedByUserId") val acceptedByUserId: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "acceptedAt") val acceptedAt: String? = null,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "event") val event: GameInvitationEvent? = null
)

@JsonClass(generateAdapter = true)
data class GameInvitationEvent(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int,
    @Json(name = "host") val host: UserSummary? = null
)

data class CreateGameInvitationInput(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "expiresInDays") val expiresInDays: Int? = null
)
