package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Badge(
    @Json(name = "id") val id: Int,
    @Json(name = "slug") val slug: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "iconSvg") val iconSvg: String? = null,
    @Json(name = "category") val category: String,
    @Json(name = "tier") val tier: String,
    @Json(name = "requirementType") val requirementType: String = "",
    @Json(name = "requirementCount") val requirementCount: Int = 0,
    @Json(name = "sortOrder") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class UserBadge(
    @Json(name = "id") val id: String,
    @Json(name = "badgeId") val badgeId: Int,
    @Json(name = "earnedAt") val earnedAt: String = "",
    @Json(name = "isPinned") val isPinned: Boolean = false,
    @Json(name = "badge") val badge: Badge
)

@JsonClass(generateAdapter = true)
data class BadgesResponse(
    @Json(name = "badges") val badges: List<UserBadge>,
    @Json(name = "newlyEarned") val newlyEarned: Int? = null
)

@JsonClass(generateAdapter = true)
data class AllBadgesResponse(
    @Json(name = "badges") val badges: List<Badge>
)

@JsonClass(generateAdapter = true)
data class PinResponse(
    @Json(name = "pinned") val pinned: Boolean
)
