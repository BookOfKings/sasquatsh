package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.MtgDeckDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface MtgDeckApi {

    @GET("mtg-decks")
    suspend fun getMyDecks(): Response<List<MtgDeckDto>>

    @GET("mtg-decks")
    suspend fun getDeck(@Query("id") deckId: String): Response<MtgDeckDto>

    @POST("mtg-decks")
    suspend fun createDeck(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<MtgDeckDto>

    @PUT("mtg-decks")
    suspend fun updateDeck(
        @Query("id") deckId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<MtgDeckDto>

    @DELETE("mtg-decks")
    suspend fun deleteDeck(@Query("id") deckId: String): Response<Unit>
}
