package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.PlayerRequestsApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialService @Inject constructor(
    private val playerRequestsApi: PlayerRequestsApi,
    private val moshi: Moshi
) {

    private val listType = Types.newParameterizedType(List::class.java, PlayerRequest::class.java)

    suspend fun getPlayerRequests(filters: PlayerRequestFilters = PlayerRequestFilters()): List<PlayerRequest> {
        val response = playerRequestsApi.getPlayerRequests(
            eventId = filters.eventId,
            city = filters.city,
            state = filters.state,
            eventLocationId = filters.eventLocationId
        )
        if (!response.isSuccessful) throw Exception("Failed to load player requests")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<PlayerRequest>>(listType).fromJson(json) ?: emptyList()
    }

    suspend fun getMyPlayerRequests(): List<PlayerRequest> {
        val response = playerRequestsApi.getMyPlayerRequests()
        if (!response.isSuccessful) throw Exception("Failed to load my player requests")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<PlayerRequest>>(listType).fromJson(json) ?: emptyList()
    }

    suspend fun createPlayerRequest(input: CreatePlayerRequestInput): PlayerRequest {
        val response = playerRequestsApi.createPlayerRequest(input)
        if (!response.isSuccessful) throw Exception("Failed to create player request")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PlayerRequest::class.java).fromJson(json)
            ?: throw Exception("Failed to parse player request")
    }

    suspend fun fillPlayerRequest(id: String): PlayerRequest {
        val response = playerRequestsApi.fillPlayerRequest(id)
        if (!response.isSuccessful) throw Exception("Failed to fill player request")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PlayerRequest::class.java).fromJson(json)
            ?: throw Exception("Failed to parse player request")
    }

    suspend fun cancelPlayerRequest(id: String): PlayerRequest {
        val response = playerRequestsApi.cancelPlayerRequest(id)
        if (!response.isSuccessful) throw Exception("Failed to cancel player request")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PlayerRequest::class.java).fromJson(json)
            ?: throw Exception("Failed to parse player request")
    }

    suspend fun deletePlayerRequest(id: String) {
        val response = playerRequestsApi.deletePlayerRequest(id)
        if (!response.isSuccessful) throw Exception("Failed to delete player request")
    }
}
