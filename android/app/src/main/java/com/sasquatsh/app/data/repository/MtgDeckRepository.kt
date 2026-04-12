package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.MtgDeckApi
import com.sasquatsh.app.data.remote.api.ScryfallApi
import com.sasquatsh.app.data.remote.dto.MtgDeckDto
import com.sasquatsh.app.data.remote.dto.ScryfallCardDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MtgDeckRepository @Inject constructor(
    private val mtgDeckApi: MtgDeckApi,
    private val scryfallApi: ScryfallApi,
) {
    suspend fun getMyDecks(): ApiResult<List<MtgDeckDto>> {
        return try {
            val response = mtgDeckApi.getMyDecks()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load decks: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load decks")
        }
    }

    suspend fun getDeck(deckId: String): ApiResult<MtgDeckDto> {
        return try {
            val response = mtgDeckApi.getDeck(deckId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to load deck: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load deck")
        }
    }

    suspend fun createDeck(name: String, formatId: String?, description: String?): ApiResult<MtgDeckDto> {
        return try {
            val body = mutableMapOf<String, Any?>("name" to name)
            if (formatId != null) body["formatId"] = formatId
            if (description != null) body["description"] = description
            val response = mtgDeckApi.createDeck(body)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to create deck: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to create deck")
        }
    }

    suspend fun updateDeck(deckId: String, updates: Map<String, Any?>): ApiResult<MtgDeckDto> {
        return try {
            val response = mtgDeckApi.updateDeck(deckId, updates)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to update deck: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to update deck")
        }
    }

    suspend fun deleteDeck(deckId: String): ApiResult<Unit> {
        return try {
            val response = mtgDeckApi.deleteDeck(deckId)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to delete deck: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to delete deck")
        }
    }

    suspend fun searchCards(query: String): ApiResult<List<ScryfallCardDto>> {
        return try {
            val response = scryfallApi.searchCards(query = query)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Card search failed: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Card search failed")
        }
    }

    suspend fun autocomplete(query: String): ApiResult<List<String>> {
        return try {
            val response = scryfallApi.autocomplete(query = query)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Autocomplete failed", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Autocomplete failed")
        }
    }
}
