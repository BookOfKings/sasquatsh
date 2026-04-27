package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface BggApi {

    // GET bgg?search=...
    @GET("bgg")
    suspend fun searchGames(
        @Query("search") query: String
    ): Response<List<Any>>

    // GET bgg?id=...
    @GET("bgg")
    suspend fun getGameDetails(
        @Query("id") bggId: Int
    ): Response<Any>

    // GET bgg-cache?action=list&page=...&limit=...
    @GET("bgg-cache")
    suspend fun listCachedGames(
        @Query("action") action: String = "list",
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<Any>
}
