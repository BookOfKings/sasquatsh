package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.MtgDeckApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MtgDeckService @Inject constructor(
    private val mtgDeckApi: MtgDeckApi,
    private val moshi: Moshi
) {
    private val deckListType = Types.newParameterizedType(List::class.java, MtgDeck::class.java)

    suspend fun getMyDecks(): List<MtgDeck> {
        val response = mtgDeckApi.getMyDecks()
        if (!response.isSuccessful) throw Exception("Failed to load decks")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<MtgDeck>>(deckListType).fromJson(json) ?: emptyList()
    }

    suspend fun createDeck(input: CreateDeckInput): MtgDeck {
        val body = moshi.adapter(CreateDeckInput::class.java).toJsonValue(input) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.createDeck(body)
        if (!response.isSuccessful) throw Exception("Failed to create deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }

    suspend fun updateDeck(id: String, input: UpdateDeckInput): MtgDeck {
        val body = moshi.adapter(UpdateDeckInput::class.java).toJsonValue(input) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.updateDeck(id, body)
        if (!response.isSuccessful) throw Exception("Failed to update deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }

    suspend fun deleteDeck(id: String) {
        val response = mtgDeckApi.deleteDeck(id)
        if (!response.isSuccessful) throw Exception("Failed to delete deck")
    }

    suspend fun addCard(deckId: String, card: DeckCardInput) {
        val body = moshi.adapter(DeckCardInput::class.java).toJsonValue(card) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.addCard(deckId, body)
        if (!response.isSuccessful) throw Exception("Failed to add card")
    }

    suspend fun importDeck(input: ImportDeckInput): MtgDeck {
        val body = moshi.adapter(ImportDeckInput::class.java).toJsonValue(input) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.importDeck(input = body)
        if (!response.isSuccessful) throw Exception("Failed to import deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }
}
