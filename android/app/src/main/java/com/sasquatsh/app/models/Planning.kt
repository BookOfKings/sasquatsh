package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlanningSession(
    @Json(name = "id") val id: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "createdByUserId") val createdByUserId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "responseDeadline") val responseDeadline: String,
    @Json(name = "status") val status: PlanningStatus,
    @Json(name = "finalizedDate") val finalizedDate: String? = null,
    @Json(name = "finalizedGameId") val finalizedGameId: String? = null,
    @Json(name = "createdEventId") val createdEventId: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "inviteeCount") val inviteeCount: Int? = null,
    @Json(name = "group") val group: PlanningGroupInfo? = null,
    @Json(name = "createdBy") val createdBy: UserSummary? = null,
    @Json(name = "invitees") val invitees: List<PlanningInvitee>? = null,
    @Json(name = "dates") val dates: List<PlanningDate>? = null,
    @Json(name = "gameSuggestions") val gameSuggestions: List<GameSuggestion>? = null,
    @Json(name = "items") val items: List<PlanningItem>? = null,
    @Json(name = "openToGroup") val openToGroup: Boolean? = null,
    @Json(name = "maxParticipants") val maxParticipants: Int? = null,
    @Json(name = "maxGames") val maxGames: Int? = null,
    @Json(name = "tableCount") val tableCount: Int? = null,
    @Json(name = "scheduledSessions") val scheduledSessions: Any? = null,
    @Json(name = "hostSessionPreferences") val hostSessionPreferences: Any? = null
)

@JsonClass(generateAdapter = true)
data class PlanningGroupInfo(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String
)

@JsonClass(generateAdapter = true)
data class PlanningInvitee(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "hasResponded") val hasResponded: Boolean = false,
    @Json(name = "respondedAt") val respondedAt: String? = null,
    @Json(name = "cannotAttendAny") val cannotAttendAny: Boolean = false,
    @Json(name = "acceptedAt") val acceptedAt: String? = null,
    @Json(name = "hasSlot") val hasSlot: Boolean? = null,
    @Json(name = "user") val user: UserSummary? = null
)

@JsonClass(generateAdapter = true)
data class PlanningDate(
    @Json(name = "id") val id: String,
    @Json(name = "proposedDate") val proposedDate: String,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "availableCount") val availableCount: Int? = null,
    @Json(name = "votes") val votes: List<DateVote>? = null
)

@JsonClass(generateAdapter = true)
data class DateVote(
    @Json(name = "userId") val userId: String,
    @Json(name = "isAvailable") val isAvailable: Boolean,
    @Json(name = "user") val user: DateVoteUser? = null
)

@JsonClass(generateAdapter = true)
data class DateVoteUser(
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GameSuggestionUser(
    @Json(name = "id") val id: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "isFoundingMember") val isFoundingMember: Boolean? = null,
    @Json(name = "isAdmin") val isAdmin: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class GameSuggestion(
    @Json(name = "id") val id: String,
    @Json(name = "suggestedByUserId") val suggestedByUserId: String,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "voteCount") val voteCount: Int = 0,
    @Json(name = "hasVoted") val hasVoted: Boolean = false,
    @Json(name = "suggestedBy") val suggestedBy: GameSuggestionUser? = null
)

@JsonClass(generateAdapter = true)
data class PlanningItem(
    @Json(name = "id") val id: String,
    @Json(name = "itemName") val itemName: String,
    @Json(name = "itemCategory") val itemCategory: ItemCategory,
    @Json(name = "quantityNeeded") val quantityNeeded: Int? = null,
    @Json(name = "claimedByUserId") val claimedByUserId: String? = null,
    @Json(name = "claimedAt") val claimedAt: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "addedByUserId") val addedByUserId: String? = null,
    @Json(name = "claimedBy") val claimedBy: UserSummary? = null
)

data class CreatePlanningSessionInput(
    @Json(name = "groupId") val groupId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "responseDeadline") val responseDeadline: String,
    @Json(name = "inviteeUserIds") val inviteeUserIds: List<String>,
    @Json(name = "proposedDates") val proposedDates: List<ProposedDateInput>,
    @Json(name = "openToGroup") val openToGroup: Boolean? = null,
    @Json(name = "maxParticipants") val maxParticipants: Int? = null,
    @Json(name = "tableCount") val tableCount: Int? = null
)

data class ProposedDateInput(
    @Json(name = "date") val date: String,
    @Json(name = "startTime") val startTime: String? = null
)

data class PlanningResponseInput(
    @Json(name = "cannotAttendAny") val cannotAttendAny: Boolean,
    @Json(name = "dateAvailability") val dateAvailability: List<DateAvailabilityInput>
)

data class DateAvailabilityInput(
    @Json(name = "dateId") val dateId: String,
    @Json(name = "isAvailable") val isAvailable: Boolean
)

data class AddPlanningItemInput(
    @Json(name = "itemName") val itemName: String,
    @Json(name = "itemCategory") val itemCategory: ItemCategory,
    @Json(name = "quantityNeeded") val quantityNeeded: Int? = null
)

data class SuggestGameInput(
    @Json(name = "gameName") val gameName: String,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null
)

data class ScheduleEntry(
    @Json(name = "suggestionId") val suggestionId: String,
    @Json(name = "tableNumber") val tableNumber: Int,
    @Json(name = "slotIndex") val slotIndex: Int = 0
)
