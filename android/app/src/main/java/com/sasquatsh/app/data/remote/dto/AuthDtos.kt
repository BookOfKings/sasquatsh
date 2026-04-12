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
    @Json(name = "firebase_uid") val firebaseUid: String,
    @Json(name = "email") val email: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "subscription_tier") val subscriptionTier: String?,
    @Json(name = "is_admin") val isAdmin: Boolean?,
    @Json(name = "is_founding_member") val isFoundingMember: Boolean?,
)

@JsonClass(generateAdapter = true)
data class CheckUsernameResponse(
    @Json(name = "available") val available: Boolean,
)
