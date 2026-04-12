package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.EventsApi
import com.sasquatsh.app.data.remote.dto.EventDetailDto
import com.sasquatsh.app.data.remote.dto.EventSummaryDto
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val eventsApi: EventsApi,
) {

    suspend fun browseEvents(filters: Map<String, String> = emptyMap()): ApiResult<List<EventSummaryDto>> {
        return safeApiCall { eventsApi.browseEvents(filters) }
    }

    suspend fun getHostedEvents(): ApiResult<List<EventSummaryDto>> {
        return safeApiCall { eventsApi.getHostedEvents() }
    }

    suspend fun getRegisteredEvents(): ApiResult<List<EventSummaryDto>> {
        return safeApiCall { eventsApi.getRegisteredEvents() }
    }

    suspend fun getEvent(eventId: String): ApiResult<EventDetailDto> {
        return safeApiCall { eventsApi.getEvent(eventId) }
    }

    suspend fun getGroupEvents(groupId: String): ApiResult<List<EventSummaryDto>> {
        return safeApiCall { eventsApi.getGroupEvents(groupId) }
    }

    suspend fun registerForEvent(eventId: String): ApiResult<Unit> {
        return safeApiCall { eventsApi.registerForEvent(eventId) }
    }

    suspend fun cancelRegistration(eventId: String): ApiResult<Unit> {
        return safeApiCall { eventsApi.cancelRegistration(eventId) }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.Success(Unit as T)
                }
            } else {
                ApiResult.Error(
                    message = response.errorBody()?.string() ?: "Unknown error",
                    code = response.code(),
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }
}
