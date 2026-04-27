package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "subscriptionTier") val subscriptionTier: SubscriptionTier? = null,
    @Json(name = "subscriptionExpiresAt") val subscriptionExpiresAt: String? = null,
    @Json(name = "subscriptionOverrideTier") val subscriptionOverrideTier: SubscriptionTier? = null,
    @Json(name = "isAdmin") val isAdmin: Boolean = false,
    @Json(name = "blockedUserIds") val blockedUserIds: List<String> = emptyList(),
    @Json(name = "createdAt") val createdAt: String? = null
) {
    val effectiveTier: SubscriptionTier
        get() = subscriptionOverrideTier ?: subscriptionTier ?: SubscriptionTier.FREE
}

@JsonClass(generateAdapter = true)
data class UserSummary(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "isFoundingMember") val isFoundingMember: Boolean? = null,
    @Json(name = "isAdmin") val isAdmin: Boolean? = null,
    @Json(name = "subscriptionTier") val subscriptionTier: SubscriptionTier? = null,
    @Json(name = "subscriptionOverrideTier") val subscriptionOverrideTier: SubscriptionTier? = null
) {
    val effectiveTier: SubscriptionTier
        get() = subscriptionOverrideTier ?: subscriptionTier ?: SubscriptionTier.FREE
}
