package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.EventsApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsService @Inject constructor(
    private val eventsApi: EventsApi,
    private val moshi: Moshi
) {

    private val summaryListType = Types.newParameterizedType(List::class.java, EventSummary::class.java)

    suspend fun getHostedEvents(): List<EventSummary> {
        val response = eventsApi.getHostedEvents()
        if (!response.isSuccessful) throw Exception("Failed to load hosted events")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<EventSummary>>(summaryListType).fromJson(json) ?: emptyList()
    }

    suspend fun getRegisteredEvents(): List<EventSummary> {
        val response = eventsApi.getRegisteredEvents()
        if (!response.isSuccessful) throw Exception("Failed to load registered events")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<EventSummary>>(summaryListType).fromJson(json) ?: emptyList()
    }

    suspend fun getPublicEvents(filter: EventSearchFilter): List<EventSummary> {
        val response = eventsApi.getPublicEvents(
            city = filter.city,
            state = filter.state,
            search = filter.search,
            gameCategory = filter.gameCategory?.value,
            gameSystem = filter.gameSystem?.value,
            difficulty = filter.difficulty?.value,
            dateFrom = filter.dateFrom,
            dateTo = filter.dateTo,
            nearbyZip = filter.nearbyZip,
            radiusMiles = filter.radiusMiles
        )
        if (!response.isSuccessful) throw Exception("Failed to load events")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<EventSummary>>(summaryListType).fromJson(json) ?: emptyList()
    }

    suspend fun getGroupEvents(groupId: String): List<EventSummary> {
        val response = eventsApi.getGroupEvents(groupId = groupId)
        if (!response.isSuccessful) throw Exception("Failed to load group events")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<EventSummary>>(summaryListType).fromJson(json) ?: emptyList()
    }

    suspend fun getEvent(id: String): Event {
        val response = eventsApi.getEvent(id)
        if (!response.isSuccessful) throw Exception("Failed to load event")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(Event::class.java).fromJson(json)
            ?: throw Exception("Failed to parse event")
    }

    suspend fun createEvent(input: CreateEventInput): Event {
        val response = eventsApi.createEvent(input)
        if (!response.isSuccessful) throw Exception("Failed to create event")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(Event::class.java).fromJson(json)
            ?: throw Exception("Failed to parse event")
    }

    suspend fun updateEvent(id: String, input: UpdateEventInput): Event {
        val response = eventsApi.updateEvent(id, input)
        if (!response.isSuccessful) throw Exception("Failed to update event")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(Event::class.java).fromJson(json)
            ?: throw Exception("Failed to parse event")
    }

    suspend fun deleteEvent(id: String) {
        val response = eventsApi.deleteEvent(id)
        if (!response.isSuccessful) throw Exception("Failed to delete event")
    }

    suspend fun registerForEvent(eventId: String) {
        val body = mapOf("eventId" to eventId)
        val response = eventsApi.registerForEvent(body)
        if (!response.isSuccessful) throw Exception("Failed to register for event")
    }

    suspend fun cancelRegistration(eventId: String) {
        val response = eventsApi.cancelRegistration(eventId)
        if (!response.isSuccessful) throw Exception("Failed to cancel registration")
    }

    suspend fun addItem(eventId: String, input: CreateEventItemInput) {
        val body = mapOf(
            "eventId" to eventId,
            "itemName" to input.itemName,
            "itemCategory" to (input.itemCategory ?: "other")
        )
        val response = eventsApi.addItem(body)
        if (!response.isSuccessful) throw Exception("Failed to add item")
    }

    suspend fun claimItem(itemId: String) {
        val response = eventsApi.claimItem(itemId)
        if (!response.isSuccessful) throw Exception("Failed to claim item")
    }

    suspend fun unclaimItem(itemId: String) {
        val response = eventsApi.unclaimItem(itemId)
        if (!response.isSuccessful) throw Exception("Failed to unclaim item")
    }

    suspend fun addGame(input: AddEventGameInput) {
        val response = eventsApi.addGame(input)
        if (!response.isSuccessful) throw Exception("Failed to add game")
    }

    suspend fun removeGame(gameId: String) {
        val response = eventsApi.removeGame(gameId)
        if (!response.isSuccessful) throw Exception("Failed to remove game")
    }
}
