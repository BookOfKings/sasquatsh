package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GameGroup(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "logoUrl") val logoUrl: String? = null,
    @Json(name = "coverImageUrl") val coverImageUrl: String? = null,
    @Json(name = "groupType") val groupType: GroupType,
    @Json(name = "locationCity") val locationCity: String? = null,
    @Json(name = "locationState") val locationState: String? = null,
    @Json(name = "locationRadiusMiles") val locationRadiusMiles: Int? = null,
    @Json(name = "joinPolicy") val joinPolicy: JoinPolicy,
    @Json(name = "createdByUserId") val createdByUserId: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String,
    @Json(name = "memberCount") val memberCount: Int? = null,
    @Json(name = "creator") val creator: UserSummary? = null
)

@JsonClass(generateAdapter = true)
data class GroupSummary(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "logoUrl") val logoUrl: String? = null,
    @Json(name = "groupType") val groupType: GroupType,
    @Json(name = "locationCity") val locationCity: String? = null,
    @Json(name = "locationState") val locationState: String? = null,
    @Json(name = "joinPolicy") val joinPolicy: JoinPolicy,
    @Json(name = "memberCount") val memberCount: Int = 0,
    @Json(name = "userRole") val userRole: MemberRole? = null
)

@JsonClass(generateAdapter = true)
data class GroupMember(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "role") val role: MemberRole,
    @Json(name = "joinedAt") val joinedAt: String
)

@JsonClass(generateAdapter = true)
data class JoinRequest(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: JoinRequestStatus,
    @Json(name = "createdAt") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class GroupInvitation(
    @Json(name = "id") val id: String,
    @Json(name = "inviteCode") val inviteCode: String,
    @Json(name = "invitedByDisplayName") val invitedByDisplayName: String? = null,
    @Json(name = "invitedEmail") val invitedEmail: String? = null,
    @Json(name = "maxUses") val maxUses: Int? = null,
    @Json(name = "usesCount") val usesCount: Int = 0,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "createdAt") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class RecurringGame(
    @Json(name = "id") val id: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "frequency") val frequency: String? = null,
    @Json(name = "dayOfWeek") val dayOfWeek: Int,
    @Json(name = "monthlyWeek") val monthlyWeek: Int? = null,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "durationMinutes") val durationMinutes: Int,
    @Json(name = "maxPlayers") val maxPlayers: Int,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "gameSystem") val gameSystem: String? = null,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "nextOccurrenceDate") val nextOccurrenceDate: String? = null,
    @Json(name = "lastGeneratedDate") val lastGeneratedDate: String? = null,
    @Json(name = "hostUserId") val hostUserId: String? = null,
    @Json(name = "createdByUserId") val createdByUserId: String? = null,
    @Json(name = "createdAt") val createdAt: String
) {
    val frequencyDisplayName: String
        get() = when (frequency) {
            "weekly" -> "Weekly"
            "biweekly" -> "Every 2 Weeks"
            "monthly" -> "Monthly"
            else -> "Weekly"
        }

    val dayOfWeekName: String
        get() {
            val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            return if (dayOfWeek in 0..6) days[dayOfWeek] else ""
        }

    val monthlyWeekName: String?
        get() {
            if (frequency != "monthly" || monthlyWeek == null) return null
            return when (monthlyWeek) {
                1 -> "1st"
                2 -> "2nd"
                3 -> "3rd"
                4 -> "4th"
                -1 -> "Last"
                else -> null
            }
        }

    val scheduleDescription: String
        get() = when (frequency) {
            "monthly" -> {
                val weekName = monthlyWeekName
                if (weekName != null) "$weekName $dayOfWeekName at $startTime"
                else "$dayOfWeekName at $startTime (monthly)"
            }
            "biweekly" -> "Every other $dayOfWeekName at $startTime"
            else -> "Every $dayOfWeekName at $startTime"
        }
}

data class CreateGroupInput(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "groupType") val groupType: GroupType,
    @Json(name = "locationCity") val locationCity: String? = null,
    @Json(name = "locationState") val locationState: String? = null,
    @Json(name = "locationRadiusMiles") val locationRadiusMiles: Int? = null,
    @Json(name = "joinPolicy") val joinPolicy: JoinPolicy? = null
)

data class UpdateGroupInput(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "groupType") val groupType: GroupType? = null,
    @Json(name = "locationCity") val locationCity: String? = null,
    @Json(name = "locationState") val locationState: String? = null,
    @Json(name = "locationRadiusMiles") val locationRadiusMiles: Int? = null,
    @Json(name = "joinPolicy") val joinPolicy: JoinPolicy? = null,
    @Json(name = "logoUrl") val logoUrl: String? = null
)

data class GroupSearchFilter(
    val search: String? = null,
    val groupType: GroupType? = null,
    val city: String? = null,
    val state: String? = null
) {
    fun toQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        search?.takeIf { it.isNotEmpty() }?.let { map["search"] = it }
        groupType?.let { map["type"] = it.value }
        city?.let { map["city"] = it }
        state?.let { map["state"] = it }
        return map
    }
}

data class CreateInvitationInput(
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "maxUses") val maxUses: Int? = null,
    @Json(name = "expiresInDays") val expiresInDays: Int? = null
)

@JsonClass(generateAdapter = true)
data class PendingGroupInvitation(
    @Json(name = "id") val id: String,
    @Json(name = "inviteCode") val inviteCode: String,
    @Json(name = "status") val status: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "invitedBy") val invitedBy: UserSummary? = null,
    @Json(name = "group") val group: InvitationGroupInfo? = null
)

@JsonClass(generateAdapter = true)
data class InvitationPreview(
    @Json(name = "inviteCode") val inviteCode: String,
    @Json(name = "invitedEmail") val invitedEmail: String? = null,
    @Json(name = "group") val group: InvitationGroupInfo,
    @Json(name = "invitedBy") val invitedBy: UserSummary,
    @Json(name = "expiresAt") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class InvitationGroupInfo(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "logoUrl") val logoUrl: String? = null,
    @Json(name = "groupType") val groupType: GroupType,
    @Json(name = "locationCity") val locationCity: String? = null,
    @Json(name = "locationState") val locationState: String? = null,
    @Json(name = "joinPolicy") val joinPolicy: JoinPolicy
)

@JsonClass(generateAdapter = true)
data class GroupMembership(
    @Json(name = "id") val id: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "role") val role: MemberRole,
    @Json(name = "joinedAt") val joinedAt: String,
    @Json(name = "user") val user: UserSummary? = null
)

data class CreateRecurringGameInput(
    @Json(name = "groupId") val groupId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "frequency") val frequency: String,
    @Json(name = "dayOfWeek") val dayOfWeek: Int,
    @Json(name = "monthlyWeek") val monthlyWeek: Int? = null,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "gameSystem") val gameSystem: String? = null,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null
)

data class UpdateRecurringGameInput(
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "frequency") val frequency: String? = null,
    @Json(name = "dayOfWeek") val dayOfWeek: Int? = null,
    @Json(name = "monthlyWeek") val monthlyWeek: Int? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "gameSystem") val gameSystem: String? = null,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null,
    @Json(name = "isActive") val isActive: Boolean? = null
)
