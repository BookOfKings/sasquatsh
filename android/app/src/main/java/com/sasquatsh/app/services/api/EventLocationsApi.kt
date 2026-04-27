package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface EventLocationsApi {

    // GET event-locations?forEvent=true
    @GET("event-locations")
    suspend fun getEventLocations(
        @Query("forEvent") forEvent: String = "true"
    ): Response<List<Any>>

    // GET event-locations?hot=true
    @GET("event-locations")
    suspend fun getHotLocations(
        @Query("hot") hot: String = "true"
    ): Response<List<Any>>

    // POST event-locations
    @POST("event-locations")
    suspend fun createEventLocation(
        @Body input: Any
    ): Response<Any>
}
