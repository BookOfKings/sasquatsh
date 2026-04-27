package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface ScryfallApi {

    // GET scryfall?search=...
    @GET("scryfall")
    suspend fun searchCards(
        @Query("search") query: String
    ): Response<Any>

    // GET scryfall?autocomplete=...
    @GET("scryfall")
    suspend fun autocomplete(
        @Query("autocomplete") query: String
    ): Response<List<String>>

    // GET scryfall?id=...
    @GET("scryfall")
    suspend fun getCard(
        @Query("id") id: String
    ): Response<Any>
}
