package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface ShareLinksApi {

    // GET share-links?code=...
    @GET("share-links")
    suspend fun preview(
        @Query("code") code: String
    ): Response<Any>

    // POST share-links?code=...&action=accept
    @POST("share-links")
    suspend fun accept(
        @Query("code") code: String,
        @Query("action") action: String = "accept"
    ): Response<Any>

    // POST share-links?action=create
    @POST("share-links")
    suspend fun create(
        @Query("action") action: String = "create",
        @Body input: Any
    ): Response<Any>
}
