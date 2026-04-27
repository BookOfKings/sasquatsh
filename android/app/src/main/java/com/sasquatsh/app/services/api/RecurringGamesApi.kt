package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface RecurringGamesApi {

    // GET recurring-games?groupId=...
    @GET("recurring-games")
    suspend fun getRecurringGames(
        @Query("groupId") groupId: String
    ): Response<List<Any>>

    // POST recurring-games
    @POST("recurring-games")
    suspend fun createRecurringGame(
        @Body input: Any
    ): Response<Any>

    // PUT recurring-games?id=...
    @PUT("recurring-games")
    suspend fun updateRecurringGame(
        @Query("id") id: String,
        @Body input: Any
    ): Response<Any>

    // DELETE recurring-games?id=...&deleteFutureEvents=...
    @DELETE("recurring-games")
    suspend fun deleteRecurringGame(
        @Query("id") id: String,
        @Query("deleteFutureEvents") deleteFutureEvents: String? = null
    ): Response<Unit>
}
