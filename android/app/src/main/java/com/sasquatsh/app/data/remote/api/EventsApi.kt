package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.EventDetailDto
import com.sasquatsh.app.data.remote.dto.EventSummaryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface EventsApi {

    @GET("events")
    suspend fun getEvent(@Query("id") eventId: String): Response<EventDetailDto>

    @GET("events")
    suspend fun browseEvents(@QueryMap filters: Map<String, String>): Response<List<EventSummaryDto>>

    @GET("events?type=hosted")
    suspend fun getHostedEvents(): Response<List<EventSummaryDto>>

    @GET("events?type=registered")
    suspend fun getRegisteredEvents(): Response<List<EventSummaryDto>>

    @GET("events?type=group")
    suspend fun getGroupEvents(@Query("groupId") groupId: String): Response<List<EventSummaryDto>>

    @POST("events")
    suspend fun createEvent(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<EventDetailDto>

    @PUT("events")
    suspend fun updateEvent(
        @Query("id") eventId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<EventDetailDto>

    @DELETE("events")
    suspend fun deleteEvent(@Query("id") eventId: String): Response<Unit>

    @POST("events")
    suspend fun registerForEvent(
        @Query("id") eventId: String,
        @Query("action") action: String = "register",
    ): Response<Unit>

    @DELETE("events")
    suspend fun cancelRegistration(
        @Query("id") eventId: String,
        @Query("action") action: String = "unregister",
    ): Response<Unit>
}
