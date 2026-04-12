package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.ChatApi
import com.sasquatsh.app.data.remote.dto.ChatMessageDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
) {

    suspend fun getMessages(
        contextType: String,
        contextId: String,
    ): ApiResult<List<ChatMessageDto>> {
        return try {
            val response = chatApi.getMessages(contextType, contextId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load messages: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load messages")
        }
    }

    suspend fun sendMessage(
        contextType: String,
        contextId: String,
        content: String,
    ): ApiResult<ChatMessageDto> {
        return try {
            val body = mapOf(
                "contextType" to contextType,
                "contextId" to contextId,
                "content" to content,
            )
            val response = chatApi.sendMessage(body)
            if (response.isSuccessful) {
                val msg = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(msg)
            } else {
                ApiResult.Error("Failed to send message: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to send message")
        }
    }
}
