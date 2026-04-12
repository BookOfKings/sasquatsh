package com.sasquatsh.app.domain.model

import com.squareup.moshi.Json

enum class GameSystem(val value: String) {
    @Json(name = "board_game") BOARD_GAME("board_game"),
    @Json(name = "mtg") MTG("mtg"),
    @Json(name = "pokemon_tcg") POKEMON_TCG("pokemon_tcg"),
    @Json(name = "yugioh") YUGIOH("yugioh"),
    @Json(name = "warhammer40k") WARHAMMER_40K("warhammer40k");

    val displayName: String get() = when (this) {
        BOARD_GAME -> "Board Game"
        MTG -> "Magic: The Gathering"
        POKEMON_TCG -> "Pokemon TCG"
        YUGIOH -> "Yu-Gi-Oh!"
        WARHAMMER_40K -> "Warhammer 40k"
    }

    companion object {
        fun fromValue(value: String?): GameSystem =
            entries.find { it.value == value } ?: BOARD_GAME
    }
}

enum class SubscriptionTier(val value: String) {
    FREE("free"),
    BASIC("basic"),
    PRO("pro"),
    PREMIUM("premium");

    companion object {
        fun fromValue(value: String?): SubscriptionTier =
            entries.find { it.value == value } ?: FREE
    }
}

enum class GroupType(val value: String) {
    GEOGRAPHIC("geographic"),
    INTEREST("interest"),
    BOTH("both");

    companion object {
        fun fromValue(value: String?): GroupType =
            entries.find { it.value == value } ?: BOTH
    }
}

enum class JoinPolicy(val value: String) {
    OPEN("open"),
    REQUEST("request"),
    INVITE_ONLY("invite_only");

    companion object {
        fun fromValue(value: String?): JoinPolicy =
            entries.find { it.value == value } ?: OPEN
    }
}

enum class MemberRole(val value: String) {
    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member");

    companion object {
        fun fromValue(value: String?): MemberRole =
            entries.find { it.value == value } ?: MEMBER
    }
}
