package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.ScryfallCardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ScryfallApi {

    @GET("scryfall")
    suspend fun searchCards(
        @Query("action") action: String = "search",
        @Query("q") query: String,
    ): Response<List<ScryfallCardDto>>

    @GET("scryfall")
    suspend fun autocomplete(
        @Query("action") action: String = "autocomplete",
        @Query("q") query: String,
    ): Response<List<String>>

    @GET("scryfall")
    suspend fun getCard(
        @Query("action") action: String = "get",
        @Query("id") id: String,
    ): Response<ScryfallCardDto>
}
