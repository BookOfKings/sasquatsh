package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShelfScanQuota(
    @Json(name = "used") val used: Int = 0,
    @Json(name = "limit") val limit: Any? = null, // Int or "unlimited"
    @Json(name = "remaining") val remaining: Int = 0
) {
    val isUnlimited: Boolean
        get() = limit is String && (limit as String) == "unlimited"

    val limitDisplay: String
        get() = when {
            isUnlimited -> "Unlimited scans"
            else -> "$remaining scan${if (remaining == 1) "" else "s"} remaining this month"
        }
}

@JsonClass(generateAdapter = true)
data class ShelfScanResult(
    @Json(name = "games") val games: List<ShelfScanGame> = emptyList(),
    @Json(name = "rawText") val rawText: String? = null,
    @Json(name = "totalDetected") val totalDetected: Int? = null,
    @Json(name = "matched") val matched: Int? = null
)

@JsonClass(generateAdapter = true)
data class ShelfScanGame(
    @Json(name = "detectedTitle") val detectedTitle: String = "",
    @Json(name = "bggId") val bggId: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "yearPublished") val yearPublished: Int? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "minPlayers") val minPlayers: Int? = null,
    @Json(name = "maxPlayers") val maxPlayers: Int? = null,
    @Json(name = "playingTime") val playingTime: Int? = null,
    @Json(name = "confidence") val confidence: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null
) {
    val id: String get() = detectedTitle + (bggId ?: 0).toString()
}

@JsonClass(generateAdapter = true)
data class ShelfScanRequest(
    @Json(name = "image") val image: String
)
