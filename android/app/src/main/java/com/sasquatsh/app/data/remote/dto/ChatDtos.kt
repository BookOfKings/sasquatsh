package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "context_type") val contextType: String,
    @Json(name = "context_id") val contextId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
)
