package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "bio") val bio: String?,
    @Json(name = "birth_year") val birthYear: Int?,
    @Json(name = "home_city") val homeCity: String?,
    @Json(name = "home_state") val homeState: String?,
    @Json(name = "home_zip") val homeZip: String?,
    @Json(name = "max_travel_miles") val maxTravelMiles: Int?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "favorite_games") val favoriteGames: List<String>?,
    @Json(name = "preferred_game_types") val preferredGameTypes: List<String>?,
    @Json(name = "subscription_tier") val subscriptionTier: String?,
    @Json(name = "subscription_override_tier") val subscriptionOverrideTier: String?,
    @Json(name = "is_founding_member") val isFoundingMember: Boolean?,
    @Json(name = "is_admin") val isAdmin: Boolean?,
    @Json(name = "blocked_user_ids") val blockedUserIds: List<String>?,
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "birth_year") val birthYear: Int? = null,
    @Json(name = "home_city") val homeCity: String? = null,
    @Json(name = "home_state") val homeState: String? = null,
    @Json(name = "home_zip") val homeZip: String? = null,
    @Json(name = "max_travel_miles") val maxTravelMiles: Int? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "favorite_games") val favoriteGames: List<String>? = null,
    @Json(name = "preferred_game_types") val preferredGameTypes: List<String>? = null,
)
