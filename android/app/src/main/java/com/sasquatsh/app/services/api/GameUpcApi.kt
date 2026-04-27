package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface GameUpcApi {

    // GET game-upc?upc=...&search=...
    @GET("game-upc")
    suspend fun lookupUpc(
        @Query("upc") upc: String,
        @Query("search") search: String? = null
    ): Response<Any>

    // POST game-upc?upc=...&bggId=...
    @POST("game-upc")
    suspend fun voteMatch(
        @Query("upc") upc: String,
        @Query("bggId") bggId: Int
    ): Response<Any>
}
