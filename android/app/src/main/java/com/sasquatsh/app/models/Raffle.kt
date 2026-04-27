package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Raffle(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "prizeName") val prizeName: String,
    @Json(name = "prizeDescription") val prizeDescription: String? = null,
    @Json(name = "prizeImageUrl") val prizeImageUrl: String? = null,
    @Json(name = "prizeBggId") val prizeBggId: Int? = null,
    @Json(name = "prizeValueCents") val prizeValueCents: Int? = null,
    @Json(name = "startDate") val startDate: String,
    @Json(name = "endDate") val endDate: String,
    @Json(name = "termsConditions") val termsConditions: String? = null,
    @Json(name = "mailInInstructions") val mailInInstructions: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "winnerUserId") val winnerUserId: String? = null,
    @Json(name = "winnerSelectedAt") val winnerSelectedAt: String? = null,
    @Json(name = "winnerNotifiedAt") val winnerNotifiedAt: String? = null,
    @Json(name = "winnerClaimedAt") val winnerClaimedAt: String? = null,
    @Json(name = "bannerImageUrl") val bannerImageUrl: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "stats") val stats: RaffleStats? = null,
    @Json(name = "userEntries") val userEntries: List<RaffleEntry>? = null,
    @Json(name = "userTotalEntries") val userTotalEntries: Int? = null,
    @Json(name = "winner") val winner: RaffleUser? = null
) {
    val isActive: Boolean get() = status == "active"
    val isEnded: Boolean get() = status == "ended"
    val hasWinner: Boolean get() = winnerUserId != null

    val prizeValueFormatted: String?
        get() {
            val cents = prizeValueCents ?: return null
            if (cents <= 0) return null
            val dollars = cents / 100.0
            return String.format("$%.2f", dollars)
        }
}

@JsonClass(generateAdapter = true)
data class RaffleStats(
    @Json(name = "totalEntries") val totalEntries: Int? = null,
    @Json(name = "uniqueParticipants") val uniqueParticipants: Int? = null,
    @Json(name = "entries") val entries: Int? = null,
    @Json(name = "users") val users: Int? = null
) {
    val displayTotalEntries: Int get() = totalEntries ?: entries ?: 0
    val displayParticipants: Int get() = uniqueParticipants ?: users ?: 0
}

@JsonClass(generateAdapter = true)
data class RaffleUser(
    @Json(name = "id") val id: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RaffleEntry(
    @Json(name = "id") val id: String,
    @Json(name = "raffleId") val raffleId: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "entryType") val entryType: String,
    @Json(name = "sourceId") val sourceId: String? = null,
    @Json(name = "entryCount") val entryCount: Int,
    @Json(name = "createdAt") val createdAt: String
)

enum class RaffleEntryType(val value: String) {
    @Json(name = "host_event") HOST_EVENT("host_event"),
    @Json(name = "plan_session") PLAN_SESSION("plan_session"),
    @Json(name = "attend_event") ATTEND_EVENT("attend_event"),
    @Json(name = "mail_in") MAIL_IN("mail_in");

    val displayName: String
        get() = when (this) {
            HOST_EVENT -> "Host Event"
            PLAN_SESSION -> "Plan Session"
            ATTEND_EVENT -> "Attend Event"
            MAIL_IN -> "Mail-In"
        }

    val iconResName: String
        get() = when (this) {
            HOST_EVENT -> "ic_star"
            PLAN_SESSION -> "ic_calendar_plus"
            ATTEND_EVENT -> "ic_person_clock"
            MAIL_IN -> "ic_envelope"
        }

    val description: String
        get() = when (this) {
            HOST_EVENT -> "Host a game event (2x for paid)"
            PLAN_SESSION -> "Create a planning session (2x for paid)"
            ATTEND_EVENT -> "Attend a game event"
            MAIL_IN -> "No-purchase-necessary entry"
        }

    companion object {
        fun fromValue(value: String): RaffleEntryType? =
            entries.find { it.value == value }
    }
}

@JsonClass(generateAdapter = true)
data class RaffleResponse(
    @Json(name = "raffle") val raffle: Raffle? = null
)

data class MailInEntryInput(
    @Json(name = "raffleId") val raffleId: String,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String
)
