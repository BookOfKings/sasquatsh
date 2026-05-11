package com.sasquatsh.app.services

import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.services.api.CollectionsApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionsService @Inject constructor(
    private val collectionsApi: CollectionsApi,
    private val moshi: Moshi
) {

    @Suppress("UNCHECKED_CAST")
    private fun parseGamesFromBody(body: Any?, useNameField: Boolean = false): List<CollectionGame> {
        val map = body as? Map<String, Any?> ?: return emptyList()
        val gamesList = map["games"] as? List<Map<String, Any?>> ?: return emptyList()
        return gamesList.mapNotNull { g ->
            try {
                val name = if (useNameField) {
                    (g["name"] as? String) ?: (g["game_name"] as? String) ?: ""
                } else {
                    (g["game_name"] as? String) ?: (g["name"] as? String) ?: ""
                }
                CollectionGame(
                    id = (g["id"] as? String) ?: (g["bgg_id"]?.toString()) ?: "",
                    bggId = (g["bgg_id"] as? Number)?.toInt(),
                    gameName = name,
                    thumbnailUrl = g["thumbnail_url"] as? String,
                    minPlayers = (g["min_players"] as? Number)?.toInt(),
                    maxPlayers = (g["max_players"] as? Number)?.toInt(),
                    yearPublished = (g["year_published"] as? Number)?.toInt(),
                    bggRank = (g["bgg_rank"] as? Number)?.toInt(),
                    averageRating = (g["average_rating"] as? Number)?.toDouble(),
                    playingTime = (g["playing_time"] as? Number)?.toDouble()?.toInt()
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun getMyCollection(): List<CollectionGame> {
        val response = collectionsApi.getMyCollection()
        if (!response.isSuccessful) throw Exception("Failed to load collection")
        return parseGamesFromBody(response.body())
    }

    suspend fun getUserCollection(userId: String): List<CollectionGame> {
        val response = collectionsApi.getUserCollection(userId)
        if (!response.isSuccessful) throw Exception("Failed to load user collection")
        return parseGamesFromBody(response.body())
    }

    suspend fun getTopGames(): List<CollectionGame> {
        val response = collectionsApi.getTopGames()
        if (!response.isSuccessful) throw Exception("Failed to load top games")
        return parseGamesFromBody(response.body(), useNameField = true)
    }

    suspend fun addGame(input: AddCollectionGameInput): List<CollectionGame> {
        val gameJson = moshi.adapter(AddCollectionGameInput::class.java).toJsonValue(input)
            ?: throw Exception("Serialization failed")
        val body = mapOf("games" to listOf(gameJson))
        val response = collectionsApi.addGame(body)
        if (!response.isSuccessful) throw Exception("Failed to add game")
        return parseGamesFromBody(response.body())
    }

    suspend fun removeGame(bggId: Int) {
        val response = collectionsApi.removeGame(bggId)
        if (!response.isSuccessful) throw Exception("Failed to remove game")
    }
}
