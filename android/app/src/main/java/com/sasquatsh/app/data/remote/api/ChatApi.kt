package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.ChatMessageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ChatApi {

    @GET("chat")
    suspend fun getMessages(
        @Query("contextType") contextType: String,
        @Query("contextId") contextId: String,
    ): Response<List<ChatMessageDto>>

    @POST("chat")
    suspend fun sendMessage(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
    ): Response<ChatMessageDto>
}
