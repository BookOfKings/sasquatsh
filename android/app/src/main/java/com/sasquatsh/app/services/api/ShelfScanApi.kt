package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface ShelfScanApi {

    // GET shelf-scan (get remaining scans quota)
    @GET("shelf-scan")
    suspend fun getRemainingScans(): Response<Any>

    // POST shelf-scan (send image for AI analysis)
    @POST("shelf-scan")
    suspend fun scanImage(
        @Body body: Any
    ): Response<Any>
}
