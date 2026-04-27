package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface RaffleApi {

    // GET raffle
    @GET("raffle")
    suspend fun getActiveRaffle(): Response<Any>

    // GET raffle?id=...
    @GET("raffle")
    suspend fun getRaffle(
        @Query("id") id: String
    ): Response<Any>

    // POST raffle?action=mail-in
    @POST("raffle")
    suspend fun submitMailInEntry(
        @Query("action") action: String = "mail-in",
        @Body input: Any
    ): Response<Unit>
}
