package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.ChatApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatService @Inject constructor(
    private val chatApi: ChatApi,
    private val moshi: Moshi
) {

    suspend fun getMessages(
        contextType: String,
        contextId: String,
        limit: Int,
        before: String?
    ): List<ChatMessage> {
        val response = chatApi.getMessages(contextType, contextId, limit, before)
        if (!response.isSuccessful) throw Exception("Failed to load messages")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val wrapper = moshi.adapter(ChatMessagesResponse::class.java).fromJson(json)
        return wrapper?.messages ?: emptyList()
    }

    suspend fun sendMessage(
        contextType: String,
        contextId: String,
        content: String
    ): ChatMessage {
        val input = SendMessageInput(content = content)
        val response = chatApi.sendMessage(contextType, contextId, input)
        if (!response.isSuccessful) throw Exception("Failed to send message")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val wrapper = moshi.adapter(SendMessageResponse::class.java).fromJson(json)
        return wrapper?.message ?: throw Exception("Failed to parse message")
    }

    suspend fun deleteMessage(
        contextType: String,
        contextId: String,
        messageId: String
    ) {
        val response = chatApi.deleteMessage(contextType, contextId, messageId)
        if (!response.isSuccessful) throw Exception("Failed to delete message")
    }

    suspend fun reportMessage(
        contextType: String,
        contextId: String,
        messageId: String,
        reason: String,
        details: String?
    ) {
        val input = ReportMessageInput(reason = reason, details = details)
        val response = chatApi.reportMessage(contextType, contextId, messageId, input = input)
        if (!response.isSuccessful) throw Exception("Failed to report message")
    }
}
