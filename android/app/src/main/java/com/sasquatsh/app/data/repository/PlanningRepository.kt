package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.PlanningApi
import com.sasquatsh.app.data.remote.dto.PlanningSessionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanningRepository @Inject constructor(
    private val planningApi: PlanningApi,
) {

    suspend fun getGroupSessions(groupId: String): ApiResult<List<PlanningSessionDto>> {
        return try {
            val response = planningApi.getGroupPlanningSessions(groupId)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load sessions: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load sessions")
        }
    }

    suspend fun getSession(id: String): ApiResult<PlanningSessionDto> {
        return try {
            val response = planningApi.getPlanningSession(id)
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Failed to load session: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load session")
        }
    }

    suspend fun createSession(
        body: Map<String, Any?>,
    ): ApiResult<PlanningSessionDto> {
        return try {
            val response = planningApi.createPlanningSession(body)
            if (response.isSuccessful) {
                val data = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(data)
            } else {
                ApiResult.Error("Failed to create session: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to create session")
        }
    }

    suspend fun voteDates(
        sessionId: String,
        votes: List<Map<String, Any>>,
    ): ApiResult<PlanningSessionDto> {
        return try {
            val body = mapOf<String, Any?>("dateVotes" to votes)
            val response = planningApi.respondToPlanningSession(sessionId, "vote-dates", body)
            if (response.isSuccessful) {
                val data = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(data)
            } else {
                ApiResult.Error("Failed to vote on dates: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to vote on dates")
        }
    }

    suspend fun suggestGame(
        sessionId: String,
        game: Map<String, Any?>,
    ): ApiResult<PlanningSessionDto> {
        return try {
            val response = planningApi.respondToPlanningSession(sessionId, "suggest-game", game)
            if (response.isSuccessful) {
                val data = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(data)
            } else {
                ApiResult.Error("Failed to suggest game: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to suggest game")
        }
    }

    suspend fun voteGame(
        sessionId: String,
        gameId: String,
    ): ApiResult<PlanningSessionDto> {
        return try {
            val body = mapOf<String, Any?>("gameSuggestionId" to gameId)
            val response = planningApi.respondToPlanningSession(sessionId, "vote-game", body)
            if (response.isSuccessful) {
                val data = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(data)
            } else {
                ApiResult.Error("Failed to vote on game: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to vote on game")
        }
    }
}
