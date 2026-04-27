package com.sasquatsh.app.services

import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.services.api.CollectionsApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionsService @Inject constructor(
    private val collectionsApi: CollectionsApi,
    private val moshi: Moshi
) {
    private val gameListType = Types.newParameterizedType(List::class.java, CollectionGame::class.java)

    suspend fun getMyCollection(): List<CollectionGame> {
        val response = collectionsApi.getMyCollection()
        if (!response.isSuccessful) throw Exception("Failed to load collection")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<CollectionGame>>(gameListType).fromJson(json) ?: emptyList()
    }

    suspend fun getUserCollection(userId: String): List<CollectionGame> {
        val response = collectionsApi.getUserCollection(userId)
        if (!response.isSuccessful) throw Exception("Failed to load user collection")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<CollectionGame>>(gameListType).fromJson(json) ?: emptyList()
    }

    suspend fun getTopGames(): List<CollectionGame> {
        val response = collectionsApi.getTopGames()
        if (!response.isSuccessful) throw Exception("Failed to load top games")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<CollectionGame>>(gameListType).fromJson(json) ?: emptyList()
    }

    suspend fun addGame(input: AddCollectionGameInput): List<CollectionGame> {
        val body = moshi.adapter(AddCollectionGameInput::class.java).toJsonValue(input)
            ?: throw Exception("Serialization failed")
        val response = collectionsApi.addGame(body)
        if (!response.isSuccessful) throw Exception("Failed to add game")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<CollectionGame>>(gameListType).fromJson(json) ?: emptyList()
    }

    suspend fun removeGame(bggId: Int) {
        val response = collectionsApi.removeGame(bggId)
        if (!response.isSuccessful) throw Exception("Failed to remove game")
    }
}
