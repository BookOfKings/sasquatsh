package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface SessionsApi {

    // GET sessions?eventId=...
    @GET("sessions")
    suspend fun getSessions(
        @Query("eventId") eventId: String
    ): Response<Any>

    // POST sessions?action=register&sessionId=...
    @POST("sessions")
    suspend fun registerForSession(
        @Query("action") action: String = "register",
        @Query("sessionId") sessionId: String
    ): Response<Unit>

    // DELETE sessions?sessionId=...
    @DELETE("sessions")
    suspend fun cancelSessionRegistration(
        @Query("sessionId") sessionId: String
    ): Response<Unit>
}
