package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date
import java.util.UUID

@JsonClass(generateAdapter = true)
data class RoundCounterState(
    @Json(name = "id") val id: String,
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "roundNumber") var roundNumber: Int,
    @Json(name = "turnNumber") var turnNumber: Int? = null,
    @Json(name = "phaseKey") var phaseKey: String? = null,
    @Json(name = "activePlayerId") var activePlayerId: String? = null,
    @Json(name = "minimumRound") val minimumRound: Int,
    @Json(name = "startingRound") val startingRound: Int,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String
) {
    companion object {
        fun new(startingRound: Int = 1, minimumRound: Int = 1): RoundCounterState {
            val now = Date().toString()
            return RoundCounterState(
                id = UUID.randomUUID().toString(),
                sessionId = UUID.randomUUID().toString(),
                roundNumber = startingRound,
                minimumRound = minimumRound,
                startingRound = startingRound,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class RoundCounterEvent(
    @Json(name = "id") val id: String,
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "roundNumber") val roundNumber: Int,
    @Json(name = "turnNumber") val turnNumber: Int? = null,
    @Json(name = "phaseKey") val phaseKey: String? = null,
    @Json(name = "eventType") val eventType: String,
    @Json(name = "createdAt") val createdAt: String
) {
    companion object {
        fun log(state: RoundCounterState, eventType: String): RoundCounterEvent {
            return RoundCounterEvent(
                id = UUID.randomUUID().toString(),
                sessionId = state.sessionId,
                roundNumber = state.roundNumber,
                turnNumber = state.turnNumber,
                phaseKey = state.phaseKey,
                eventType = eventType,
                createdAt = Date().toString()
            )
        }
    }
}
