package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthSyncRequest(
    @Json(name = "email") val email: String,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "auth_provider") val authProvider: String = "email",
)

@JsonClass(generateAdapter = true)
data class AuthSyncResponse(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
    @Json(name = "subscriptionTier") val subscriptionTier: String?,
    @Json(name = "subscriptionOverrideTier") val subscriptionOverrideTier: String?,
    @Json(name = "isAdmin") val isAdmin: Boolean?,
    @Json(name = "isFoundingMember") val isFoundingMember: Boolean?,
    @Json(name = "authProvider") val authProvider: String?,
)

@JsonClass(generateAdapter = true)
data class CheckUsernameResponse(
    @Json(name = "available") val available: Boolean,
)
