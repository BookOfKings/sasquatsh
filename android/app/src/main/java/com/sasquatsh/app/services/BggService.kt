package com.sasquatsh.app.services

import com.sasquatsh.app.models.BggCacheListResponse
import com.sasquatsh.app.models.BggGame
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.services.api.BggApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BggService @Inject constructor(
    private val bggApi: BggApi,
    private val moshi: Moshi
) {
    private val searchListType = Types.newParameterizedType(List::class.java, BggSearchResult::class.java)

    suspend fun searchGames(query: String): List<BggSearchResult> {
        val response = bggApi.searchGames(query)
        if (!response.isSuccessful) throw Exception("Failed to search games")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<BggSearchResult>>(searchListType).fromJson(json) ?: emptyList()
    }

    suspend fun getGameDetails(bggId: Int): BggGame {
        val response = bggApi.getGameDetails(bggId)
        if (!response.isSuccessful) throw Exception("Failed to load game details")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(BggGame::class.java).fromJson(json)
            ?: throw Exception("Failed to parse game details")
    }

    suspend fun listCachedGames(page: Int = 1, limit: Int = 100): BggCacheListResponse {
        val response = bggApi.listCachedGames(page = page, limit = limit)
        if (!response.isSuccessful) throw Exception("Failed to load cached games")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(BggCacheListResponse::class.java).fromJson(json)
            ?: BggCacheListResponse(games = emptyList(), total = 0, page = page, limit = limit, totalPages = 0)
    }
}
