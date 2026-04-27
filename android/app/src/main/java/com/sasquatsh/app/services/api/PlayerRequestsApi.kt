package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface PlayerRequestsApi {

    // GET player-requests (+ filter params)
    @GET("player-requests")
    suspend fun getPlayerRequests(
        @Query("eventId") eventId: String? = null,
        @Query("city") city: String? = null,
        @Query("state") state: String? = null,
        @Query("eventLocationId") eventLocationId: String? = null
    ): Response<List<Any>>

    // GET player-requests?id=mine
    @GET("player-requests")
    suspend fun getMyPlayerRequests(
        @Query("id") id: String = "mine"
    ): Response<List<Any>>

    // POST player-requests
    @POST("player-requests")
    suspend fun createPlayerRequest(
        @Body input: Any
    ): Response<Any>

    // POST player-requests?id=...&action=fill
    @POST("player-requests")
    suspend fun fillPlayerRequest(
        @Query("id") id: String,
        @Query("action") action: String = "fill"
    ): Response<Any>

    // POST player-requests?id=...&action=cancel
    @POST("player-requests")
    suspend fun cancelPlayerRequest(
        @Query("id") id: String,
        @Query("action") action: String = "cancel"
    ): Response<Any>

    // DELETE player-requests?id=...
    @DELETE("player-requests")
    suspend fun deletePlayerRequest(
        @Query("id") id: String
    ): Response<Unit>
}
