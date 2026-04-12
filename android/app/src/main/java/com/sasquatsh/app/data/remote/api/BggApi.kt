package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.BggGameDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BggApi {

    @GET("bgg-cache")
    suspend fun searchGames(
        @Query("action") action: String = "search",
        @Query("q") query: String,
    ): Response<List<BggGameDto>>

    @GET("bgg-cache")
    suspend fun getPopularGames(
        @Query("action") action: String = "popular",
    ): Response<List<BggGameDto>>

    @GET("bgg-cache")
    suspend fun getGame(
        @Query("action") action: String = "get",
        @Query("bggId") bggId: Int,
    ): Response<BggGameDto>
}
