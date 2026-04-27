package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface MtgDeckApi {

    // GET mtg-decks
    @GET("mtg-decks")
    suspend fun getMyDecks(): Response<List<Any>>

    // GET mtg-decks?id=...
    @GET("mtg-decks")
    suspend fun getDeck(
        @Query("id") id: String
    ): Response<Any>

    // POST mtg-decks
    @POST("mtg-decks")
    suspend fun createDeck(
        @Body input: Any
    ): Response<Any>

    // PUT mtg-decks?id=...
    @PUT("mtg-decks")
    suspend fun updateDeck(
        @Query("id") id: String,
        @Body input: Any
    ): Response<Any>

    // DELETE mtg-decks?id=...
    @DELETE("mtg-decks")
    suspend fun deleteDeck(
        @Query("id") id: String
    ): Response<Unit>

    // POST mtg-decks?id=... (add card to deck)
    @POST("mtg-decks")
    suspend fun addCard(
        @Query("id") deckId: String,
        @Body card: Any
    ): Response<Unit>

    // DELETE mtg-decks?id=...&cardId=...
    @DELETE("mtg-decks")
    suspend fun removeCard(
        @Query("id") deckId: String,
        @Query("cardId") cardId: String
    ): Response<Unit>

    // PUT mtg-decks?id=...&cardId=...
    @PUT("mtg-decks")
    suspend fun updateCard(
        @Query("id") deckId: String,
        @Query("cardId") cardId: String,
        @Body body: Any
    ): Response<Unit>

    // POST mtg-decks?action=import
    @POST("mtg-decks")
    suspend fun importDeck(
        @Query("action") action: String = "import",
        @Body input: Any
    ): Response<Any>
}
