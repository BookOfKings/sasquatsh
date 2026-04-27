package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

    // GET chat?contextType=...&contextId=...&limit=...&before=...
    @GET("chat")
    suspend fun getMessages(
        @Query("contextType") contextType: String,
        @Query("contextId") contextId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<Any>

    // POST chat?contextType=...&contextId=...
    @POST("chat")
    suspend fun sendMessage(
        @Query("contextType") contextType: String,
        @Query("contextId") contextId: String,
        @Body input: Any
    ): Response<Any>

    // DELETE chat?contextType=...&contextId=...&messageId=...
    @DELETE("chat")
    suspend fun deleteMessage(
        @Query("contextType") contextType: String,
        @Query("contextId") contextId: String,
        @Query("messageId") messageId: String
    ): Response<Unit>

    // POST chat?contextType=...&contextId=...&messageId=...&action=report
    @POST("chat")
    suspend fun reportMessage(
        @Query("contextType") contextType: String,
        @Query("contextId") contextId: String,
        @Query("messageId") messageId: String,
        @Query("action") action: String = "report",
        @Body input: Any
    ): Response<Any>
}
