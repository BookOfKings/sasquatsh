package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "id") val id: String,
    @Json(name = "firebaseUid") val firebaseUid: String,
    @Json(name = "email") val email: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "birthYear") val birthYear: Int? = null,
    @Json(name = "maxTravelMiles") val maxTravelMiles: Int? = null,
    @Json(name = "homeCity") val homeCity: String? = null,
    @Json(name = "homeState") val homeState: String? = null,
    @Json(name = "homePostalCode") val homePostalCode: String? = null,
    @Json(name = "activeCity") val activeCity: String? = null,
    @Json(name = "activeState") val activeState: String? = null,
    @Json(name = "activeLocationExpiresAt") val activeLocationExpiresAt: String? = null,
    @Json(name = "activeEventLocationId") val activeEventLocationId: String? = null,
    @Json(name = "activeLocationHall") val activeLocationHall: String? = null,
    @Json(name = "activeLocationRoom") val activeLocationRoom: String? = null,
    @Json(name = "activeLocationTable") val activeLocationTable: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "favoriteGames") val favoriteGames: List<String>? = null,
    @Json(name = "preferredGameTypes") val preferredGameTypes: List<String>? = null,
    @Json(name = "collectionVisibility") val collectionVisibility: String? = null,
    @Json(name = "isAdmin") val isAdmin: Boolean = false,
    @Json(name = "subscriptionTier") val subscriptionTier: SubscriptionTier? = null,
    @Json(name = "subscriptionExpiresAt") val subscriptionExpiresAt: String? = null,
    @Json(name = "blockedUserIds") val blockedUserIds: List<String> = emptyList(),
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String,
    @Json(name = "groups") val groups: List<UserGroupMembership>? = null,
    @Json(name = "upcomingEvents") val upcomingEvents: List<UserUpcomingEvent>? = null,
    @Json(name = "stats") val stats: UserStats? = null
)

@JsonClass(generateAdapter = true)
data class UserGroupMembership(
    @Json(name = "role") val role: String,
    @Json(name = "joinedAt") val joinedAt: String,
    @Json(name = "group") val group: UserGroupInfo? = null
) {
    val id: String get() = group?.id ?: ""
}

@JsonClass(generateAdapter = true)
data class UserGroupInfo(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "logoUrl") val logoUrl: String? = null,
    @Json(name = "groupType") val groupType: String
)

@JsonClass(generateAdapter = true)
data class UserUpcomingEvent(
    @Json(name = "status") val status: String,
    @Json(name = "event") val event: UserEventInfo? = null
) {
    val id: String get() = event?.id ?: ""
}

@JsonClass(generateAdapter = true)
data class UserEventInfo(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null
)

@JsonClass(generateAdapter = true)
data class UserStats(
    @Json(name = "hostedCount") val hostedCount: Int = 0,
    @Json(name = "attendedCount") val attendedCount: Int = 0,
    @Json(name = "groupCount") val groupCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class PublicProfile(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "homeCity") val homeCity: String? = null,
    @Json(name = "homeState") val homeState: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "favoriteGames") val favoriteGames: List<String>? = null,
    @Json(name = "preferredGameTypes") val preferredGameTypes: List<String>? = null,
    @Json(name = "createdAt") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class BlockedUser(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null
)

data class UpdateProfileInput(
    @Json(name = "username") val username: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "birthYear") val birthYear: Int? = null,
    @Json(name = "maxTravelMiles") val maxTravelMiles: Int? = null,
    @Json(name = "homeCity") val homeCity: String? = null,
    @Json(name = "homeState") val homeState: String? = null,
    @Json(name = "homePostalCode") val homePostalCode: String? = null,
    @Json(name = "activeCity") val activeCity: String? = null,
    @Json(name = "activeState") val activeState: String? = null,
    @Json(name = "activeLocationExpiresAt") val activeLocationExpiresAt: String? = null,
    @Json(name = "activeEventLocationId") val activeEventLocationId: String? = null,
    @Json(name = "activeLocationHall") val activeLocationHall: String? = null,
    @Json(name = "activeLocationRoom") val activeLocationRoom: String? = null,
    @Json(name = "activeLocationTable") val activeLocationTable: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "favoriteGames") val favoriteGames: List<String>? = null,
    @Json(name = "preferredGameTypes") val preferredGameTypes: List<String>? = null,
    @Json(name = "collectionVisibility") val collectionVisibility: String? = null
)

@JsonClass(generateAdapter = true)
data class AvatarUploadResponse(
    @Json(name = "message") val message: String,
    @Json(name = "avatarUrl") val avatarUrl: String,
    @Json(name = "user") val user: UserProfile
)

@JsonClass(generateAdapter = true)
data class AvatarDeleteResponse(
    @Json(name = "message") val message: String,
    @Json(name = "user") val user: UserProfile
)

@JsonClass(generateAdapter = true)
data class UsernameCheckResponse(
    @Json(name = "available") val available: Boolean,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockActionResponse(
    @Json(name = "message") val message: String,
    @Json(name = "blockedUserIds") val blockedUserIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UserSearchResult(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null
)
