package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatUser(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "isFoundingMember") val isFoundingMember: Boolean? = null,
    @Json(name = "isAdmin") val isAdmin: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "id") val id: String,
    @Json(name = "contextType") val contextType: String,
    @Json(name = "contextId") val contextId: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "content") val content: String,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "user") val user: ChatUser? = null
)

data class SendMessageInput(
    @Json(name = "content") val content: String
)

enum class ChatReportReason(val value: String) {
    @Json(name = "harassment") HARASSMENT("harassment"),
    @Json(name = "spam") SPAM("spam"),
    @Json(name = "hate_speech") HATE_SPEECH("hate_speech"),
    @Json(name = "inappropriate") INAPPROPRIATE("inappropriate"),
    @Json(name = "threats") THREATS("threats"),
    @Json(name = "other") OTHER("other");

    val displayName: String
        get() = when (this) {
            HARASSMENT -> "Harassment"
            SPAM -> "Spam"
            HATE_SPEECH -> "Hate Speech"
            INAPPROPRIATE -> "Inappropriate"
            THREATS -> "Threats"
            OTHER -> "Other"
        }

    companion object {
        fun fromValue(value: String): ChatReportReason? =
            entries.find { it.value == value }
    }
}

data class ReportMessageInput(
    @Json(name = "reason") val reason: String,
    @Json(name = "details") val details: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessagesResponse(
    @Json(name = "messages") val messages: List<ChatMessage>
)

@JsonClass(generateAdapter = true)
data class SendMessageResponse(
    @Json(name = "message") val message: ChatMessage
)

@JsonClass(generateAdapter = true)
data class ReportMessageResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "reportId") val reportId: String? = null
)
