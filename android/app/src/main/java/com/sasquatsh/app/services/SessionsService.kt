package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.SessionsApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionsService @Inject constructor(
    private val sessionsApi: SessionsApi,
    private val moshi: Moshi
) {

    suspend fun getEventSessions(eventId: String): EventSessionsResponse {
        val response = sessionsApi.getSessions(eventId)
        if (!response.isSuccessful) throw Exception("Failed to load sessions")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(EventSessionsResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse sessions")
    }

    suspend fun registerForSession(sessionId: String) {
        val response = sessionsApi.registerForSession(sessionId = sessionId)
        if (!response.isSuccessful) throw Exception("Failed to register for session")
    }

    suspend fun unregisterFromSession(sessionId: String) {
        val response = sessionsApi.cancelSessionRegistration(sessionId = sessionId)
        if (!response.isSuccessful) throw Exception("Failed to unregister from session")
    }
}
