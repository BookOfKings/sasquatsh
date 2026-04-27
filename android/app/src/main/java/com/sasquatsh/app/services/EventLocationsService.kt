package com.sasquatsh.app.services

import com.sasquatsh.app.models.CreateEventLocationInput
import com.sasquatsh.app.models.EventLocation
import com.sasquatsh.app.services.api.EventLocationsApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventLocationsService @Inject constructor(
    private val eventLocationsApi: EventLocationsApi,
    private val moshi: Moshi
) {
    private val listType = Types.newParameterizedType(List::class.java, EventLocation::class.java)

    suspend fun getEventLocations(): List<EventLocation> {
        val response = eventLocationsApi.getEventLocations()
        if (!response.isSuccessful) throw Exception("Failed to load event locations")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<EventLocation>>(listType).fromJson(json) ?: emptyList()
    }

    suspend fun getHotLocations(): List<EventLocation> {
        val response = eventLocationsApi.getHotLocations()
        if (!response.isSuccessful) throw Exception("Failed to load hot locations")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<EventLocation>>(listType).fromJson(json) ?: emptyList()
    }

    suspend fun createEventLocation(input: CreateEventLocationInput): EventLocation {
        val body = moshi.adapter(CreateEventLocationInput::class.java).toJsonValue(input)
            ?: throw Exception("Serialization failed")
        val response = eventLocationsApi.createEventLocation(body)
        if (!response.isSuccessful) throw Exception("Failed to create event location")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(EventLocation::class.java).fromJson(json)
            ?: throw Exception("Failed to parse event location")
    }
}
