package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.RecurringGamesApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringGamesService @Inject constructor(
    private val recurringGamesApi: RecurringGamesApi,
    private val moshi: Moshi
) {

    private val recurringGameListType = Types.newParameterizedType(List::class.java, RecurringGame::class.java)

    suspend fun getGroupRecurringGames(groupId: String): List<RecurringGame> {
        val response = recurringGamesApi.getRecurringGames(groupId)
        if (!response.isSuccessful) throw Exception("Failed to load recurring games")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<RecurringGame>>(recurringGameListType).fromJson(json) ?: emptyList()
    }

    suspend fun createRecurringGame(input: CreateRecurringGameInput): RecurringGame {
        val response = recurringGamesApi.createRecurringGame(input)
        if (!response.isSuccessful) throw Exception("Failed to create recurring game")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(RecurringGame::class.java).fromJson(json)
            ?: throw Exception("Failed to parse recurring game")
    }

    suspend fun updateRecurringGame(id: String, input: UpdateRecurringGameInput): RecurringGame {
        val response = recurringGamesApi.updateRecurringGame(id, input)
        if (!response.isSuccessful) throw Exception("Failed to update recurring game")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(RecurringGame::class.java).fromJson(json)
            ?: throw Exception("Failed to parse recurring game")
    }

    suspend fun deleteRecurringGame(id: String, deleteFutureEvents: Boolean = false) {
        val response = recurringGamesApi.deleteRecurringGame(
            id = id,
            deleteFutureEvents = if (deleteFutureEvents) "true" else null
        )
        if (!response.isSuccessful) throw Exception("Failed to delete recurring game")
    }
}
