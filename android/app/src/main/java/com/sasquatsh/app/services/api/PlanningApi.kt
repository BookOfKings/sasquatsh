package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface PlanningApi {

    // GET planning?groupId=...
    @GET("planning")
    suspend fun getGroupSessions(
        @Query("groupId") groupId: String
    ): Response<List<Any>>

    // GET planning?mine=true
    @GET("planning")
    suspend fun getMySessions(
        @Query("mine") mine: String = "true"
    ): Response<List<Any>>

    // GET planning?id=...
    @GET("planning")
    suspend fun getSession(
        @Query("id") id: String
    ): Response<Any>

    // POST planning
    @POST("planning")
    suspend fun createSession(
        @Body input: Any
    ): Response<Any>

    // POST planning?id=...&action=respond
    @POST("planning")
    suspend fun submitResponse(
        @Query("id") sessionId: String,
        @Query("action") action: String = "respond",
        @Body input: Any
    ): Response<Unit>

    // POST planning?id=...&action=suggest-game
    @POST("planning")
    suspend fun suggestGame(
        @Query("id") sessionId: String,
        @Query("action") action: String = "suggest-game",
        @Body input: Any
    ): Response<Any>

    // POST planning?id=...&action=vote-game&suggestionId=...
    @POST("planning")
    suspend fun voteForGame(
        @Query("id") sessionId: String,
        @Query("action") action: String = "vote-game",
        @Query("suggestionId") suggestionId: String
    ): Response<Unit>

    // POST planning?id=...&action=unvote-game&suggestionId=...
    @POST("planning")
    suspend fun unvoteGame(
        @Query("id") sessionId: String,
        @Query("action") action: String = "unvote-game",
        @Query("suggestionId") suggestionId: String
    ): Response<Unit>

    // POST planning?id=...&action=remove-suggestion&suggestionId=...
    @POST("planning")
    suspend fun removeSuggestion(
        @Query("id") sessionId: String,
        @Query("action") action: String = "remove-suggestion",
        @Query("suggestionId") suggestionId: String
    ): Response<Unit>

    // PUT planning?id=...&action=finalize
    @PUT("planning")
    suspend fun finalizeSession(
        @Query("id") sessionId: String,
        @Query("action") action: String = "finalize",
        @Body input: Any
    ): Response<Any>

    // PUT planning?id=...&action=cancel
    @PUT("planning")
    suspend fun cancelSession(
        @Query("id") sessionId: String,
        @Query("action") action: String = "cancel"
    ): Response<Unit>

    // DELETE planning?id=...
    @DELETE("planning")
    suspend fun deleteSession(
        @Query("id") sessionId: String
    ): Response<Unit>

    // POST planning?id=...&action=add-item
    @POST("planning")
    suspend fun addItem(
        @Query("id") sessionId: String,
        @Query("action") action: String = "add-item",
        @Body input: Any
    ): Response<Any>

    // POST planning?id=...&action=claim-item&itemId=...
    @POST("planning")
    suspend fun claimItem(
        @Query("id") sessionId: String,
        @Query("action") action: String = "claim-item",
        @Query("itemId") itemId: String
    ): Response<Unit>

    // POST planning?id=...&action=unclaim-item&itemId=...
    @POST("planning")
    suspend fun unclaimItem(
        @Query("id") sessionId: String,
        @Query("action") action: String = "unclaim-item",
        @Query("itemId") itemId: String
    ): Response<Unit>

    // POST planning?id=...&action=remove-item&itemId=...
    @POST("planning")
    suspend fun removeItem(
        @Query("id") sessionId: String,
        @Query("action") action: String = "remove-item",
        @Query("itemId") itemId: String
    ): Response<Unit>

    // POST planning?id=...&action=add-invitees
    @POST("planning")
    suspend fun addInvitees(
        @Query("id") sessionId: String,
        @Query("action") action: String = "add-invitees",
        @Body body: Map<String, List<String>>
    ): Response<Unit>

    // POST planning?id=...&action=update-settings
    @POST("planning")
    suspend fun updateSettings(
        @Query("id") sessionId: String,
        @Query("action") action: String = "update-settings",
        @Body body: Any
    ): Response<Any>

    // POST planning?id=...&action=schedule-sessions
    @POST("planning")
    suspend fun scheduleSessions(
        @Query("id") sessionId: String,
        @Query("action") action: String = "schedule-sessions",
        @Body body: Any
    ): Response<Any>
}
