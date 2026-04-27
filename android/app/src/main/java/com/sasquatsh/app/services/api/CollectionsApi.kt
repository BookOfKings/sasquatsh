package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface CollectionsApi {

    // GET collections
    @GET("collections")
    suspend fun getMyCollection(): Response<Any>

    // GET collections?userId=...
    @GET("collections")
    suspend fun getUserCollection(
        @Query("userId") userId: String
    ): Response<Any>

    // GET collections?action=top-games
    @GET("collections")
    suspend fun getTopGames(
        @Query("action") action: String = "top-games"
    ): Response<Any>

    // POST collections (body: { games: [...] })
    @POST("collections")
    suspend fun addGame(
        @Body body: Any
    ): Response<Any>

    // DELETE collections?bggId=...
    @DELETE("collections")
    suspend fun removeGame(
        @Query("bggId") bggId: Int
    ): Response<Unit>
}
