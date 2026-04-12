package com.sasquatsh.app.domain.model

data class User(
    val id: String,
    val firebaseUid: String,
    val email: String,
    val displayName: String?,
    val username: String?,
    val avatarUrl: String?,
    val subscriptionTier: SubscriptionTier,
    val isAdmin: Boolean,
    val isFoundingMember: Boolean,
)

data class UserSummary(
    val id: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isFoundingMember: Boolean,
    val isAdmin: Boolean,
)
