package com.sasquatsh.app.models

import com.squareup.moshi.Json

enum class GameSystem(val value: String) {
    @Json(name = "board_game") BOARD_GAME("board_game"),
    @Json(name = "mtg") MTG("mtg"),
    @Json(name = "pokemon_tcg") POKEMON_TCG("pokemon_tcg"),
    @Json(name = "yugioh") YUGIOH("yugioh"),
    @Json(name = "warhammer40k") WARHAMMER_40K("warhammer40k");

    val displayName: String
        get() = when (this) {
            BOARD_GAME -> "Board Game"
            MTG -> "Magic: The Gathering"
            POKEMON_TCG -> "Pokémon TCG"
            YUGIOH -> "Yu-Gi-Oh!"
            WARHAMMER_40K -> "Warhammer 40K"
        }

    val shortName: String
        get() = when (this) {
            BOARD_GAME -> "Board Game"
            MTG -> "MTG"
            POKEMON_TCG -> "Pokémon"
            YUGIOH -> "Yu-Gi-Oh!"
            WARHAMMER_40K -> "40K"
        }

    val iconResName: String
        get() = when (this) {
            BOARD_GAME -> "ic_dice"
            MTG -> "ic_cards"
            POKEMON_TCG -> "ic_bolt_circle"
            YUGIOH -> "ic_star_circle"
            WARHAMMER_40K -> "ic_shield"
        }

    val logoAssetName: String
        get() = when (this) {
            BOARD_GAME -> "Logo"
            MTG -> "mtg-logo"
            POKEMON_TCG -> "pokemon-logo"
            YUGIOH -> "yugioh-logo"
            WARHAMMER_40K -> "warhammer40k-logo"
        }

    companion object {
        fun fromValue(value: String): GameSystem? =
            entries.find { it.value == value }
    }
}

enum class SubscriptionTier(val value: String) {
    @Json(name = "free") FREE("free"),
    @Json(name = "basic") BASIC("basic"),
    @Json(name = "pro") PRO("pro"),
    @Json(name = "premium") PREMIUM("premium");

    val displayName: String
        get() = when (this) {
            FREE -> "Free"
            BASIC -> "Basic"
            PRO -> "Pro"
            PREMIUM -> "Premium"
        }

    val priceLabel: String
        get() = when (this) {
            FREE -> "$0"
            BASIC -> "$4.99/mo"
            PRO -> "$7.99/mo"
            PREMIUM -> "Custom"
        }

    val rank: Int
        get() = when (this) {
            FREE -> 0
            BASIC -> 1
            PRO -> 2
            PREMIUM -> 3
        }

    companion object {
        fun fromValue(value: String): SubscriptionTier? =
            entries.find { it.value == value }
    }
}

enum class GameCategory(val value: String) {
    @Json(name = "strategy") STRATEGY("strategy"),
    @Json(name = "party") PARTY("party"),
    @Json(name = "cooperative") COOPERATIVE("cooperative"),
    @Json(name = "deckbuilding") DECKBUILDING("deckbuilding"),
    @Json(name = "workerplacement") WORKERPLACEMENT("workerplacement"),
    @Json(name = "areacontrol") AREACONTROL("areacontrol"),
    @Json(name = "dice") DICE("dice"),
    @Json(name = "trivia") TRIVIA("trivia"),
    @Json(name = "roleplaying") ROLEPLAYING("roleplaying"),
    @Json(name = "miniatures") MINIATURES("miniatures"),
    @Json(name = "card") CARD("card"),
    @Json(name = "family") FAMILY("family"),
    @Json(name = "abstract") ABSTRACT("abstract"),
    @Json(name = "other") OTHER("other");

    val displayName: String
        get() = when (this) {
            DECKBUILDING -> "Deck Building"
            WORKERPLACEMENT -> "Worker Placement"
            AREACONTROL -> "Area Control"
            ROLEPLAYING -> "Roleplaying"
            else -> value.replaceFirstChar { it.uppercase() }
        }

    companion object {
        fun fromValue(value: String): GameCategory? =
            entries.find { it.value == value }
    }
}

enum class DifficultyLevel(val value: String) {
    @Json(name = "beginner") BEGINNER("beginner"),
    @Json(name = "intermediate") INTERMEDIATE("intermediate"),
    @Json(name = "advanced") ADVANCED("advanced");

    val displayName: String
        get() = value.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromValue(value: String): DifficultyLevel? =
            entries.find { it.value == value }
    }
}

enum class EventStatus(val value: String) {
    @Json(name = "draft") DRAFT("draft"),
    @Json(name = "published") PUBLISHED("published"),
    @Json(name = "cancelled") CANCELLED("cancelled"),
    @Json(name = "completed") COMPLETED("completed");

    val displayName: String
        get() = value.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromValue(value: String): EventStatus? =
            entries.find { it.value == value }
    }
}

enum class ItemCategory(val value: String) {
    @Json(name = "food") FOOD("food"),
    @Json(name = "drinks") DRINKS("drinks"),
    @Json(name = "supplies") SUPPLIES("supplies"),
    @Json(name = "other") OTHER("other");

    val displayName: String
        get() = value.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromValue(value: String): ItemCategory? =
            entries.find { it.value == value }
    }
}

enum class GroupType(val value: String) {
    @Json(name = "geographic") GEOGRAPHIC("geographic"),
    @Json(name = "interest") INTEREST("interest"),
    @Json(name = "both") BOTH("both");

    val displayName: String
        get() = value.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromValue(value: String): GroupType? =
            entries.find { it.value == value }
    }
}

enum class MemberRole(val value: String) {
    @Json(name = "owner") OWNER("owner"),
    @Json(name = "admin") ADMIN("admin"),
    @Json(name = "member") MEMBER("member");

    val displayName: String
        get() = value.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromValue(value: String): MemberRole? =
            entries.find { it.value == value }
    }
}

enum class JoinPolicy(val value: String) {
    @Json(name = "open") OPEN("open"),
    @Json(name = "request") REQUEST("request"),
    @Json(name = "invite_only") INVITE_ONLY("invite_only");

    val displayName: String
        get() = when (this) {
            OPEN -> "Open"
            REQUEST -> "Request to Join"
            INVITE_ONLY -> "Invite Only"
        }

    companion object {
        fun fromValue(value: String): JoinPolicy? =
            entries.find { it.value == value }
    }
}

enum class PlanningStatus(val value: String) {
    @Json(name = "open") OPEN("open"),
    @Json(name = "finalized") FINALIZED("finalized"),
    @Json(name = "cancelled") CANCELLED("cancelled");

    val displayName: String
        get() = value.replaceFirstChar { it.uppercase() }

    companion object {
        fun fromValue(value: String): PlanningStatus? =
            entries.find { it.value == value }
    }
}

enum class JoinRequestStatus(val value: String) {
    @Json(name = "pending") PENDING("pending"),
    @Json(name = "approved") APPROVED("approved"),
    @Json(name = "rejected") REJECTED("rejected");

    companion object {
        fun fromValue(value: String): JoinRequestStatus? =
            entries.find { it.value == value }
    }
}

enum class SubscriptionStatus(val value: String) {
    @Json(name = "active") ACTIVE("active"),
    @Json(name = "past_due") PAST_DUE("past_due"),
    @Json(name = "canceled") CANCELED("canceled"),
    @Json(name = "incomplete") INCOMPLETE("incomplete");

    companion object {
        fun fromValue(value: String): SubscriptionStatus? =
            entries.find { it.value == value }
    }
}

enum class InvoiceStatus(val value: String) {
    @Json(name = "paid") PAID("paid"),
    @Json(name = "open") OPEN("open"),
    @Json(name = "draft") DRAFT("draft"),
    @Json(name = "void") VOID("void"),
    @Json(name = "uncollectible") UNCOLLECTIBLE("uncollectible");

    companion object {
        fun fromValue(value: String): InvoiceStatus? =
            entries.find { it.value == value }
    }
}

enum class EventLocationStatus(val value: String) {
    @Json(name = "pending") PENDING("pending"),
    @Json(name = "approved") APPROVED("approved"),
    @Json(name = "rejected") REJECTED("rejected");

    companion object {
        fun fromValue(value: String): EventLocationStatus? =
            entries.find { it.value == value }
    }
}

enum class AppTimezone(val value: String) {
    EASTERN("America/New_York"),
    CENTRAL("America/Chicago"),
    MOUNTAIN("America/Denver"),
    ARIZONA("America/Phoenix"),
    PACIFIC("America/Los_Angeles"),
    ALASKA("America/Anchorage"),
    HAWAII("Pacific/Honolulu"),
    UK("Europe/London"),
    CENTRAL_EUROPE("Europe/Paris"),
    JAPAN("Asia/Tokyo"),
    SYDNEY("Australia/Sydney");

    val displayName: String
        get() = when (this) {
            EASTERN -> "Eastern (ET)"
            CENTRAL -> "Central (CT)"
            MOUNTAIN -> "Mountain (MT)"
            ARIZONA -> "Arizona (MST)"
            PACIFIC -> "Pacific (PT)"
            ALASKA -> "Alaska (AKT)"
            HAWAII -> "Hawaii (HST)"
            UK -> "UK (GMT/BST)"
            CENTRAL_EUROPE -> "Central Europe (CET)"
            JAPAN -> "Japan (JST)"
            SYDNEY -> "Sydney (AEST)"
        }

    companion object {
        fun fromValue(value: String): AppTimezone? =
            entries.find { it.value == value }
    }
}
