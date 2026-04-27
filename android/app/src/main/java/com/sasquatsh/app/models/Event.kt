package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Event(
    @Json(name = "id") val id: String,
    @Json(name = "hostUserId") val hostUserId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "gameCategory") val gameCategory: String? = null,
    @Json(name = "gameSystem") val gameSystem: GameSystem? = null,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "setupMinutes") val setupMinutes: Int? = null,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "venueHall") val venueHall: String? = null,
    @Json(name = "venueRoom") val venueRoom: String? = null,
    @Json(name = "venueTable") val venueTable: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean? = null,
    @Json(name = "difficultyLevel") val difficultyLevel: String? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "confirmedCount") val confirmedCount: Int = 0,
    @Json(name = "isPublic") val isPublic: Boolean = false,
    @Json(name = "isCharityEvent") val isCharityEvent: Boolean = false,
    @Json(name = "isMultiTable") val isMultiTable: Boolean? = null,
    @Json(name = "minAge") val minAge: Int? = null,
    @Json(name = "status") val status: String,
    @Json(name = "host") val host: UserSummary? = null,
    @Json(name = "registrations") val registrations: List<EventRegistration>? = null,
    @Json(name = "items") val items: List<EventItem>? = null,
    @Json(name = "games") val games: List<EventGameSummary>? = null,
    @Json(name = "mtgConfig") val mtgConfig: MtgEventConfig? = null,
    @Json(name = "pokemonConfig") val pokemonConfig: PokemonEventConfig? = null,
    @Json(name = "yugiohConfig") val yugiohConfig: YugiohEventConfig? = null,
    @Json(name = "warhammer40kConfig") val warhammer40kConfig: Warhammer40kEventConfig? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class EventSummary(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "gameCategory") val gameCategory: String? = null,
    @Json(name = "gameSystem") val gameSystem: GameSystem? = null,
    @Json(name = "primaryGameThumbnail") val primaryGameThumbnail: String? = null,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "difficultyLevel") val difficultyLevel: String? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "confirmedCount") val confirmedCount: Int = 0,
    @Json(name = "isPublic") val isPublic: Boolean = false,
    @Json(name = "isCharityEvent") val isCharityEvent: Boolean = false,
    @Json(name = "minAge") val minAge: Int? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "host") val host: UserSummary? = null
)

@JsonClass(generateAdapter = true)
data class EventRegistration(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "status") val status: String,
    @Json(name = "user") val user: UserSummary? = null,
    @Json(name = "registeredAt") val registeredAt: String
)

@JsonClass(generateAdapter = true)
data class EventItem(
    @Json(name = "id") val id: String,
    @Json(name = "itemName") val itemName: String,
    @Json(name = "itemCategory") val itemCategory: String,
    @Json(name = "quantityNeeded") val quantityNeeded: Int = 0,
    @Json(name = "claimedByUserId") val claimedByUserId: String? = null,
    @Json(name = "claimedByName") val claimedByName: String? = null,
    @Json(name = "claimedAt") val claimedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class EventGameSummary(
    @Json(name = "id") val id: String,
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "gameName") val gameName: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "isPrimary") val isPrimary: Boolean = false,
    @Json(name = "isAlternative") val isAlternative: Boolean = false
)

data class CreateEventInput(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "gameCategory") val gameCategory: String? = null,
    @Json(name = "gameSystem") val gameSystem: String? = null,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "setupMinutes") val setupMinutes: Int? = null,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "venueHall") val venueHall: String? = null,
    @Json(name = "venueRoom") val venueRoom: String? = null,
    @Json(name = "venueTable") val venueTable: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean? = null,
    @Json(name = "difficultyLevel") val difficultyLevel: String? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null,
    @Json(name = "isCharityEvent") val isCharityEvent: Boolean? = null,
    @Json(name = "minAge") val minAge: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "groupId") val groupId: String? = null,
    @Json(name = "mtgConfig") val mtgConfig: MtgEventConfigInput? = null,
    @Json(name = "pokemonConfig") val pokemonConfig: PokemonEventConfigInput? = null,
    @Json(name = "yugiohConfig") val yugiohConfig: YugiohEventConfigInput? = null,
    @Json(name = "warhammer40kConfig") val warhammer40kConfig: Warhammer40kEventConfigInput? = null
)

data class UpdateEventInput(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "gameTitle") val gameTitle: String? = null,
    @Json(name = "gameCategory") val gameCategory: String? = null,
    @Json(name = "gameSystem") val gameSystem: String? = null,
    @Json(name = "eventDate") val eventDate: String,
    @Json(name = "startTime") val startTime: String,
    @Json(name = "durationMinutes") val durationMinutes: Int,
    @Json(name = "setupMinutes") val setupMinutes: Int,
    @Json(name = "addressLine1") val addressLine1: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "postalCode") val postalCode: String? = null,
    @Json(name = "locationDetails") val locationDetails: String? = null,
    @Json(name = "eventLocationId") val eventLocationId: String? = null,
    @Json(name = "venueHall") val venueHall: String? = null,
    @Json(name = "venueRoom") val venueRoom: String? = null,
    @Json(name = "venueTable") val venueTable: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "hostIsPlaying") val hostIsPlaying: Boolean? = null,
    @Json(name = "difficultyLevel") val difficultyLevel: String? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int,
    @Json(name = "isPublic") val isPublic: Boolean,
    @Json(name = "isCharityEvent") val isCharityEvent: Boolean,
    @Json(name = "minAge") val minAge: Int? = null,
    @Json(name = "status") val status: String,
    @Json(name = "mtgConfig") val mtgConfig: MtgEventConfigInput? = null,
    @Json(name = "pokemonConfig") val pokemonConfig: PokemonEventConfigInput? = null,
    @Json(name = "yugiohConfig") val yugiohConfig: YugiohEventConfigInput? = null,
    @Json(name = "warhammer40kConfig") val warhammer40kConfig: Warhammer40kEventConfigInput? = null
)

data class CreateEventItemInput(
    @Json(name = "itemName") val itemName: String,
    @Json(name = "itemCategory") val itemCategory: String? = null,
    @Json(name = "quantityNeeded") val quantityNeeded: Int? = null
)

data class EventSearchFilter(
    val city: String? = null,
    val state: String? = null,
    val search: String? = null,
    val gameCategory: GameCategory? = null,
    val gameSystem: GameSystem? = null,
    val difficulty: DifficultyLevel? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val nearbyZip: String? = null,
    val radiusMiles: Int? = null
) {
    fun toQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        city?.let { map["city"] = it }
        state?.let { map["state"] = it }
        search?.takeIf { it.isNotEmpty() }?.let { map["search"] = it }
        gameCategory?.let { map["gameCategory"] = it.value }
        gameSystem?.let { map["gameSystem"] = it.value }
        difficulty?.let { map["difficulty"] = it.value }
        dateFrom?.let { map["dateFrom"] = it }
        dateTo?.let { map["dateTo"] = it }
        nearbyZip?.let { map["nearbyZip"] = it }
        radiusMiles?.let { map["radiusMiles"] = it.toString() }
        return map
    }
}
