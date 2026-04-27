package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.PlanningApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

data class FinalizeResult(
    val message: String,
    val eventId: String
)

@Singleton
class PlanningService @Inject constructor(
    private val planningApi: PlanningApi,
    private val moshi: Moshi
) {

    private val sessionListType = Types.newParameterizedType(List::class.java, PlanningSession::class.java)

    suspend fun getGroupSessions(groupId: String): List<PlanningSession> {
        val response = planningApi.getGroupSessions(groupId)
        if (!response.isSuccessful) throw Exception("Failed to load planning sessions")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<PlanningSession>>(sessionListType).fromJson(json) ?: emptyList()
    }

    suspend fun getMySessions(): List<PlanningSession> {
        val response = planningApi.getMySessions()
        if (!response.isSuccessful) throw Exception("Failed to load my sessions")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<PlanningSession>>(sessionListType).fromJson(json) ?: emptyList()
    }

    suspend fun getSession(id: String): PlanningSession {
        val response = planningApi.getSession(id)
        if (!response.isSuccessful) throw Exception("Failed to load session")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PlanningSession::class.java).fromJson(json)
            ?: throw Exception("Failed to parse session")
    }

    suspend fun createSession(input: CreatePlanningSessionInput): PlanningSession {
        val response = planningApi.createSession(input)
        if (!response.isSuccessful) throw Exception("Failed to create session")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PlanningSession::class.java).fromJson(json)
            ?: throw Exception("Failed to parse session")
    }

    suspend fun submitResponse(sessionId: String, input: PlanningResponseInput) {
        val response = planningApi.submitResponse(sessionId = sessionId, input = input)
        if (!response.isSuccessful) throw Exception("Failed to submit response")
    }

    suspend fun suggestGame(sessionId: String, input: SuggestGameInput): GameSuggestion {
        val response = planningApi.suggestGame(sessionId = sessionId, input = input)
        if (!response.isSuccessful) throw Exception("Failed to suggest game")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(GameSuggestion::class.java).fromJson(json)
            ?: throw Exception("Failed to parse suggestion")
    }

    suspend fun voteForGame(sessionId: String, suggestionId: String) {
        val response = planningApi.voteForGame(sessionId = sessionId, suggestionId = suggestionId)
        if (!response.isSuccessful) throw Exception("Failed to vote for game")
    }

    suspend fun unvoteGame(sessionId: String, suggestionId: String) {
        val response = planningApi.unvoteGame(sessionId = sessionId, suggestionId = suggestionId)
        if (!response.isSuccessful) throw Exception("Failed to remove vote")
    }

    suspend fun removeSuggestion(sessionId: String, suggestionId: String) {
        val response = planningApi.removeSuggestion(sessionId = sessionId, suggestionId = suggestionId)
        if (!response.isSuccessful) throw Exception("Failed to remove suggestion")
    }

    suspend fun finalizeSession(sessionId: String, selectedDateId: String?, selectedGameId: String?): FinalizeResult {
        val input = mutableMapOf<String, Any>()
        selectedDateId?.let { input["selectedDateId"] = it }
        selectedGameId?.let { input["selectedGameId"] = it }
        val response = planningApi.finalizeSession(sessionId = sessionId, input = input)
        if (!response.isSuccessful) throw Exception("Failed to finalize session")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return FinalizeResult(
            message = map?.get("message") as? String ?: "Session finalized",
            eventId = map?.get("eventId") as? String ?: ""
        )
    }

    suspend fun cancelSession(sessionId: String) {
        val response = planningApi.cancelSession(sessionId = sessionId)
        if (!response.isSuccessful) throw Exception("Failed to cancel session")
    }

    suspend fun addItem(sessionId: String, input: AddPlanningItemInput): PlanningItem {
        val response = planningApi.addItem(sessionId = sessionId, input = input)
        if (!response.isSuccessful) throw Exception("Failed to add item")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PlanningItem::class.java).fromJson(json)
            ?: throw Exception("Failed to parse item")
    }

    suspend fun claimItem(sessionId: String, itemId: String) {
        val response = planningApi.claimItem(sessionId = sessionId, itemId = itemId)
        if (!response.isSuccessful) throw Exception("Failed to claim item")
    }

    suspend fun unclaimItem(sessionId: String, itemId: String) {
        val response = planningApi.unclaimItem(sessionId = sessionId, itemId = itemId)
        if (!response.isSuccessful) throw Exception("Failed to unclaim item")
    }

    suspend fun removeItem(sessionId: String, itemId: String) {
        val response = planningApi.removeItem(sessionId = sessionId, itemId = itemId)
        if (!response.isSuccessful) throw Exception("Failed to remove item")
    }

    suspend fun scheduleSessions(sessionId: String, schedule: List<ScheduleEntry>) {
        val body = mapOf("schedule" to schedule)
        val response = planningApi.scheduleSessions(sessionId = sessionId, body = body)
        if (!response.isSuccessful) throw Exception("Failed to schedule sessions")
    }

    suspend fun updateSettings(sessionId: String, tableCount: Int?) {
        val body = mutableMapOf<String, Any?>()
        body["tableCount"] = tableCount
        val response = planningApi.updateSettings(sessionId = sessionId, body = body)
        if (!response.isSuccessful) throw Exception("Failed to update settings")
    }
}
