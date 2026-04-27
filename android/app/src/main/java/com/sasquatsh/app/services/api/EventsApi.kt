package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface EventsApi {

    // GET events?type=browse (+ filter params)
    @GET("events")
    suspend fun getPublicEvents(
        @Query("type") type: String = "browse",
        @Query("city") city: String? = null,
        @Query("state") state: String? = null,
        @Query("search") search: String? = null,
        @Query("gameCategory") gameCategory: String? = null,
        @Query("gameSystem") gameSystem: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("nearbyZip") nearbyZip: String? = null,
        @Query("radiusMiles") radiusMiles: Int? = null
    ): Response<List<Any>>

    // GET events?type=registered
    @GET("events")
    suspend fun getRegisteredEvents(
        @Query("type") type: String = "registered"
    ): Response<List<Any>>

    // GET events?type=hosted
    @GET("events")
    suspend fun getHostedEvents(
        @Query("type") type: String = "hosted"
    ): Response<List<Any>>

    // GET events?type=group&groupId=...
    @GET("events")
    suspend fun getGroupEvents(
        @Query("type") type: String = "group",
        @Query("groupId") groupId: String
    ): Response<List<Any>>

    // GET events?id=...
    @GET("events")
    suspend fun getEvent(
        @Query("id") id: String
    ): Response<Any>

    // POST events
    @POST("events")
    suspend fun createEvent(
        @Body input: Any
    ): Response<Any>

    // PUT events?id=...
    @PUT("events")
    suspend fun updateEvent(
        @Query("id") id: String,
        @Body input: Any
    ): Response<Any>

    // DELETE events?id=...
    @DELETE("events")
    suspend fun deleteEvent(
        @Query("id") id: String
    ): Response<Unit>

    // POST registrations (body: { eventId })
    @POST("registrations")
    suspend fun registerForEvent(
        @Body body: Map<String, String>
    ): Response<Unit>

    // DELETE registrations?eventId=...
    @DELETE("registrations")
    suspend fun cancelRegistration(
        @Query("eventId") eventId: String
    ): Response<Unit>

    // POST items (body: { eventId, itemName, itemCategory?, quantityNeeded? })
    @POST("items")
    suspend fun addItem(
        @Body body: Any
    ): Response<Any>

    // PUT items?id=...&action=claim
    @PUT("items")
    suspend fun claimItem(
        @Query("id") itemId: String,
        @Query("action") action: String = "claim"
    ): Response<Unit>

    // PUT items?id=...&action=unclaim
    @PUT("items")
    suspend fun unclaimItem(
        @Query("id") itemId: String,
        @Query("action") action: String = "unclaim"
    ): Response<Unit>

    // POST event-games
    @POST("event-games")
    suspend fun addGame(
        @Body input: Any
    ): Response<Any>

    // DELETE event-games?id=...
    @DELETE("event-games")
    suspend fun removeGame(
        @Query("id") gameId: String
    ): Response<Unit>
}
