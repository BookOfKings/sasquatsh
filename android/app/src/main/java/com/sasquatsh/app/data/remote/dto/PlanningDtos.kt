package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlanningSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "title") val title: String,
    @Json(name = "status") val status: String,
    @Json(name = "responseDeadline") val responseDeadline: String,
    @Json(name = "createdBy") val createdBy: String?,
    @Json(name = "dates") val dates: List<PlanningDateDto>?,
    @Json(name = "gameSuggestions") val gameSuggestions: List<GameSuggestionDto>?,
    @Json(name = "invitees") val invitees: List<PlanningInviteeDto>?,
    @Json(name = "items") val items: List<PlanningItemDto>?,
)

@JsonClass(generateAdapter = true)
data class PlanningDateDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "startTime") val startTime: String?,
    @Json(name = "votes") val votes: List<DateVoteDto>?,
)

@JsonClass(generateAdapter = true)
data class DateVoteDto(
    @Json(name = "userId") val userId: String,
    @Json(name = "available") val available: Boolean,
    @Json(name = "displayName") val displayName: String?,
)

@JsonClass(generateAdapter = true)
data class GameSuggestionDto(
    @Json(name = "id") val id: String,
    @Json(name = "bggId") val bggId: Int?,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String?,
    @Json(name = "suggestedBy") val suggestedBy: String?,
    @Json(name = "votes") val votes: List<String>?,
)

@JsonClass(generateAdapter = true)
data class PlanningInviteeDto(
    @Json(name = "userId") val userId: String,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
    @Json(name = "status") val status: String?,
)

@JsonClass(generateAdapter = true)
data class PlanningItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "itemName") val itemName: String,
    @Json(name = "itemCategory") val itemCategory: String?,
    @Json(name = "claimedByUserId") val claimedByUserId: String?,
    @Json(name = "claimedByDisplayName") val claimedByDisplayName: String?,
)
