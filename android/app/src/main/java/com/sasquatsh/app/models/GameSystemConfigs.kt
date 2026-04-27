package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─── Response models (received from backend) ───

@JsonClass(generateAdapter = true)
data class MtgEventConfig(
    @Json(name = "eventId") val eventId: String? = null,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "customFormatName") val customFormatName: String? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "podsSize") val podsSize: Int? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyLimit") val proxyLimit: Int? = null,
    @Json(name = "powerLevelMin") val powerLevelMin: Int? = null,
    @Json(name = "powerLevelMax") val powerLevelMax: Int? = null,
    @Json(name = "powerLevelRange") val powerLevelRange: String? = null,
    @Json(name = "bannedCards") val bannedCards: List<String>? = null,
    @Json(name = "packsPerPlayer") val packsPerPlayer: Int? = null,
    @Json(name = "draftStyle") val draftStyle: String? = null,
    @Json(name = "cubeId") val cubeId: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "requireDeckRegistration") val requireDeckRegistration: Boolean? = null,
    @Json(name = "deckSubmissionDeadline") val deckSubmissionDeadline: String? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null,
    @Json(name = "matchStyle") val matchStyle: String? = null,
    @Json(name = "topCut") val topCut: Int? = null,
    @Json(name = "playMode") val playMode: String? = null,
    @Json(name = "houseRulesNotes") val houseRulesNotes: String? = null
) {
    val formatDisplayName: String
        get() {
            val fid = formatId ?: return "Custom"
            val names = mapOf(
                "commander" to "Commander",
                "standard" to "Standard",
                "modern" to "Modern",
                "pioneer" to "Pioneer",
                "legacy" to "Legacy",
                "vintage" to "Vintage",
                "pauper" to "Pauper",
                "draft" to "Draft",
                "sealed" to "Sealed",
                "cube" to "Cube",
                "oathbreaker" to "Oathbreaker",
                "brawl" to "Brawl",
                "casual" to "Casual",
                "custom" to (customFormatName ?: "Custom")
            )
            return names[fid] ?: fid.replaceFirstChar { it.uppercase() }
        }

    val eventTypeDisplayName: String
        get() {
            val et = eventType ?: return ""
            val names = mapOf(
                "casual" to "Casual",
                "swiss" to "Swiss",
                "single_elim" to "Single Elimination",
                "double_elim" to "Double Elimination",
                "round_robin" to "Round Robin",
                "pods" to "Pods"
            )
            return names[et] ?: et.replaceFirstChar { it.uppercase() }
        }

    val powerLevelDisplayName: String?
        get() {
            val plr = powerLevelRange ?: return null
            val names = mapOf(
                "casual" to "Casual",
                "mid" to "Mid",
                "high" to "High",
                "cedh" to "cEDH",
                "custom" to "Custom"
            )
            return names[plr] ?: plr.replaceFirstChar { it.uppercase() }
        }
}

@JsonClass(generateAdapter = true)
data class PokemonEventConfig(
    @Json(name = "eventId") val eventId: String? = null,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "customFormatName") val customFormatName: String? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "tournamentStyle") val tournamentStyle: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "bestOf") val bestOf: Int? = null,
    @Json(name = "topCut") val topCut: Int? = null,
    @Json(name = "requireDeckRegistration") val requireDeckRegistration: Boolean? = null,
    @Json(name = "deckSubmissionDeadline") val deckSubmissionDeadline: String? = null,
    @Json(name = "allowDeckChanges") val allowDeckChanges: Boolean? = null,
    @Json(name = "enforceFormatLegality") val enforceFormatLegality: Boolean? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyLimit") val proxyLimit: Int? = null,
    @Json(name = "usePlayPoints") val usePlayPoints: Boolean? = null,
    @Json(name = "houseRulesNotes") val houseRulesNotes: String? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null,
    @Json(name = "providesBasicEnergy") val providesBasicEnergy: Boolean? = null,
    @Json(name = "providesDamageCounters") val providesDamageCounters: Boolean? = null,
    @Json(name = "sleevesRecommended") val sleevesRecommended: Boolean? = null,
    @Json(name = "providesBuildBattleKits") val providesBuildBattleKits: Boolean? = null,
    @Json(name = "organizerConfirmedOfficialLocation") val organizerConfirmedOfficialLocation: Boolean? = null,
    @Json(name = "ageDivisions") val ageDivisions: List<String>? = null,
    @Json(name = "hasJuniorDivision") val hasJuniorDivision: Boolean? = null,
    @Json(name = "hasSeniorDivision") val hasSeniorDivision: Boolean? = null,
    @Json(name = "hasMastersDivision") val hasMastersDivision: Boolean? = null
) {
    val formatDisplayName: String
        get() {
            val fid = formatId ?: return "Custom"
            val names = mapOf(
                "standard" to "Standard",
                "expanded" to "Expanded",
                "unlimited" to "Unlimited",
                "theme" to "Theme"
            )
            return names[fid] ?: customFormatName ?: fid.replaceFirstChar { it.uppercase() }
        }

    val eventTypeDisplayName: String
        get() {
            val et = eventType ?: return ""
            val names = mapOf(
                "casual" to "Casual",
                "league" to "League",
                "league_cup" to "League Cup",
                "league_challenge" to "League Challenge",
                "regional" to "Regional",
                "international" to "International",
                "worlds" to "Worlds",
                "prerelease" to "Prerelease",
                "draft" to "Draft"
            )
            return names[et] ?: et.replaceFirstChar { it.uppercase() }
        }
}

@JsonClass(generateAdapter = true)
data class YugiohEventConfig(
    @Json(name = "eventId") val eventId: String? = null,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "customFormatName") val customFormatName: String? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "tournamentStyle") val tournamentStyle: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "bestOf") val bestOf: Int? = null,
    @Json(name = "topCut") val topCut: Int? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyLimit") val proxyLimit: Int? = null,
    @Json(name = "requireDeckRegistration") val requireDeckRegistration: Boolean? = null,
    @Json(name = "deckSubmissionDeadline") val deckSubmissionDeadline: String? = null,
    @Json(name = "allowSideDeck") val allowSideDeck: Boolean? = null,
    @Json(name = "enforceFormatLegality") val enforceFormatLegality: Boolean? = null,
    @Json(name = "houseRulesNotes") val houseRulesNotes: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "isOfficialEvent") val isOfficialEvent: Boolean? = null,
    @Json(name = "awardsOtsPoints") val awardsOtsPoints: Boolean? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null
) {
    val formatDisplayName: String
        get() {
            val fid = formatId ?: return "Custom"
            val names = mapOf(
                "advanced" to "Advanced",
                "traditional" to "Traditional",
                "speed_duel" to "Speed Duel",
                "time_wizard" to "Time Wizard",
                "casual" to "Casual"
            )
            return names[fid] ?: customFormatName ?: fid.replaceFirstChar { it.uppercase() }
        }

    val eventTypeDisplayName: String
        get() {
            val et = eventType ?: return ""
            val names = mapOf(
                "casual" to "Casual",
                "locals" to "Locals",
                "ots" to "OTS Championship",
                "regional" to "Regional",
                "ycs" to "YCS",
                "nationals" to "Nationals",
                "worlds" to "Worlds"
            )
            return names[et] ?: et.replaceFirstChar { it.uppercase() }
        }
}

@JsonClass(generateAdapter = true)
data class Warhammer40kEventConfig(
    @Json(name = "eventId") val eventId: String? = null,
    @Json(name = "gameType") val gameType: String? = null,
    @Json(name = "pointsLimit") val pointsLimit: Int? = null,
    @Json(name = "playerMode") val playerMode: String? = null,
    @Json(name = "missionPack") val missionPack: String? = null,
    @Json(name = "missionNotes") val missionNotes: String? = null,
    @Json(name = "missionSelection") val missionSelection: String? = null,
    @Json(name = "preSelectedMissions") val preSelectedMissions: List<String>? = null,
    @Json(name = "secondaryObjectives") val secondaryObjectives: String? = null,
    @Json(name = "battleReadyRequired") val battleReadyRequired: Boolean? = null,
    @Json(name = "wysiwygRequired") val wysiwygRequired: Boolean? = null,
    @Json(name = "forgeWorldAllowed") val forgeWorldAllowed: Boolean? = null,
    @Json(name = "legendsAllowed") val legendsAllowed: Boolean? = null,
    @Json(name = "armyRulesNotes") val armyRulesNotes: String? = null,
    @Json(name = "requireArmyList") val requireArmyList: Boolean? = null,
    @Json(name = "armyListDeadline") val armyListDeadline: String? = null,
    @Json(name = "armyListNotes") val armyListNotes: String? = null,
    @Json(name = "terrainType") val terrainType: String? = null,
    @Json(name = "tableSize") val tableSize: String? = null,
    @Json(name = "timeLimitMinutes") val timeLimitMinutes: Int? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "tournamentStyle") val tournamentStyle: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "includeTopCut") val includeTopCut: Boolean? = null,
    @Json(name = "scoringType") val scoringType: String? = null,
    @Json(name = "startingSupplyLimit") val startingSupplyLimit: Int? = null,
    @Json(name = "startingCrusadePoints") val startingCrusadePoints: Int? = null,
    @Json(name = "crusadeProgressionNotes") val crusadeProgressionNotes: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyNotes") val proxyNotes: String? = null
) {
    val gameTypeDisplayName: String
        get() {
            val gt = gameType ?: return ""
            val names = mapOf(
                "matched" to "Matched Play",
                "narrative" to "Narrative",
                "crusade" to "Crusade",
                "open" to "Open Play"
            )
            return names[gt] ?: gt.replaceFirstChar { it.uppercase() }
        }

    val eventTypeDisplayName: String
        get() {
            val et = eventType ?: return ""
            val names = mapOf(
                "casual" to "Casual",
                "tournament" to "Tournament",
                "campaign" to "Campaign",
                "league" to "League"
            )
            return names[et] ?: et.replaceFirstChar { it.uppercase() }
        }

    val playerModeDisplayName: String?
        get() {
            val pm = playerMode ?: return null
            val names = mapOf(
                "1v1" to "1v1",
                "2v2" to "2v2",
                "group" to "Group"
            )
            return names[pm] ?: pm
        }
}

// ─── Input models (sent to backend) ───

data class MtgEventConfigInput(
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "customFormatName") val customFormatName: String? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "podsSize") val podsSize: Int? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyLimit") val proxyLimit: Int? = null,
    @Json(name = "powerLevelMin") val powerLevelMin: Int? = null,
    @Json(name = "powerLevelMax") val powerLevelMax: Int? = null,
    @Json(name = "powerLevelRange") val powerLevelRange: String? = null,
    @Json(name = "bannedCards") val bannedCards: List<String>? = null,
    @Json(name = "packsPerPlayer") val packsPerPlayer: Int? = null,
    @Json(name = "draftStyle") val draftStyle: String? = null,
    @Json(name = "cubeId") val cubeId: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "requireDeckRegistration") val requireDeckRegistration: Boolean? = null,
    @Json(name = "deckSubmissionDeadline") val deckSubmissionDeadline: String? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null,
    @Json(name = "matchStyle") val matchStyle: String? = null,
    @Json(name = "topCut") val topCut: Int? = null,
    @Json(name = "playMode") val playMode: String? = null,
    @Json(name = "houseRulesNotes") val houseRulesNotes: String? = null
)

data class PokemonEventConfigInput(
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "customFormatName") val customFormatName: String? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "tournamentStyle") val tournamentStyle: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "bestOf") val bestOf: Int? = null,
    @Json(name = "topCut") val topCut: Int? = null,
    @Json(name = "requireDeckRegistration") val requireDeckRegistration: Boolean? = null,
    @Json(name = "deckSubmissionDeadline") val deckSubmissionDeadline: String? = null,
    @Json(name = "allowDeckChanges") val allowDeckChanges: Boolean? = null,
    @Json(name = "enforceFormatLegality") val enforceFormatLegality: Boolean? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyLimit") val proxyLimit: Int? = null,
    @Json(name = "houseRulesNotes") val houseRulesNotes: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "usePlayPoints") val usePlayPoints: Boolean? = null,
    @Json(name = "organizerConfirmedOfficialLocation") val organizerConfirmedOfficialLocation: Boolean? = null,
    @Json(name = "providesBasicEnergy") val providesBasicEnergy: Boolean? = null,
    @Json(name = "providesDamageCounters") val providesDamageCounters: Boolean? = null,
    @Json(name = "sleevesRecommended") val sleevesRecommended: Boolean? = null,
    @Json(name = "providesBuildBattleKits") val providesBuildBattleKits: Boolean? = null,
    @Json(name = "hasJuniorDivision") val hasJuniorDivision: Boolean? = null,
    @Json(name = "hasSeniorDivision") val hasSeniorDivision: Boolean? = null,
    @Json(name = "hasMastersDivision") val hasMastersDivision: Boolean? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null
)

data class YugiohEventConfigInput(
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "customFormatName") val customFormatName: String? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "tournamentStyle") val tournamentStyle: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "bestOf") val bestOf: Int? = null,
    @Json(name = "topCut") val topCut: Int? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyLimit") val proxyLimit: Int? = null,
    @Json(name = "requireDeckRegistration") val requireDeckRegistration: Boolean? = null,
    @Json(name = "deckSubmissionDeadline") val deckSubmissionDeadline: String? = null,
    @Json(name = "allowSideDeck") val allowSideDeck: Boolean? = null,
    @Json(name = "enforceFormatLegality") val enforceFormatLegality: Boolean? = null,
    @Json(name = "houseRulesNotes") val houseRulesNotes: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "isOfficialEvent") val isOfficialEvent: Boolean? = null,
    @Json(name = "awardsOtsPoints") val awardsOtsPoints: Boolean? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null
)

// ─── State models (used in ViewModel UI state) ───

data class MtgConfigState(
    val formatId: String? = null,
    val customFormatName: String? = null,
    val eventType: String? = null,
    val roundsCount: Int? = null,
    val roundTimeMinutes: Int? = null,
    val podsSize: Int? = null,
    val allowProxies: Boolean? = null,
    val proxyLimit: Int? = null,
    val powerLevelRange: String? = null,
    val powerLevelMin: Int? = null,
    val powerLevelMax: Int? = null,
    val bannedCards: List<String>? = null,
    val packsPerPlayer: Int? = null,
    val draftStyle: String? = null,
    val cubeId: String? = null,
    val hasPrizes: Boolean? = null,
    val prizeStructure: String? = null,
    val entryFee: Double? = null,
    val entryFeeCurrency: String? = null,
    val requireDeckRegistration: Boolean? = null,
    val deckSubmissionDeadline: String? = null,
    val allowSpectators: Boolean? = null,
    val matchStyle: String? = null,
    val topCut: Int? = null,
    val playMode: String? = null,
    val houseRulesNotes: String? = null
) {
    fun toInput() = MtgEventConfigInput(
        formatId = formatId, customFormatName = customFormatName,
        eventType = eventType, roundsCount = roundsCount,
        roundTimeMinutes = roundTimeMinutes, podsSize = podsSize,
        allowProxies = allowProxies, proxyLimit = proxyLimit,
        powerLevelRange = powerLevelRange, powerLevelMin = powerLevelMin,
        powerLevelMax = powerLevelMax, bannedCards = bannedCards,
        packsPerPlayer = packsPerPlayer, draftStyle = draftStyle,
        cubeId = cubeId, hasPrizes = hasPrizes,
        prizeStructure = prizeStructure, entryFee = entryFee,
        entryFeeCurrency = entryFeeCurrency,
        requireDeckRegistration = requireDeckRegistration,
        deckSubmissionDeadline = deckSubmissionDeadline,
        allowSpectators = allowSpectators, matchStyle = matchStyle,
        topCut = topCut, playMode = playMode,
        houseRulesNotes = houseRulesNotes
    )

    companion object {
        fun fromConfig(config: MtgEventConfig) = MtgConfigState(
            formatId = config.formatId, customFormatName = config.customFormatName,
            eventType = config.eventType, roundsCount = config.roundsCount,
            roundTimeMinutes = config.roundTimeMinutes, podsSize = config.podsSize,
            allowProxies = config.allowProxies, proxyLimit = config.proxyLimit,
            powerLevelRange = config.powerLevelRange, powerLevelMin = config.powerLevelMin,
            powerLevelMax = config.powerLevelMax, bannedCards = config.bannedCards,
            packsPerPlayer = config.packsPerPlayer, draftStyle = config.draftStyle,
            cubeId = config.cubeId, hasPrizes = config.hasPrizes,
            prizeStructure = config.prizeStructure, entryFee = config.entryFee,
            entryFeeCurrency = config.entryFeeCurrency,
            requireDeckRegistration = config.requireDeckRegistration,
            deckSubmissionDeadline = config.deckSubmissionDeadline,
            allowSpectators = config.allowSpectators, matchStyle = config.matchStyle,
            topCut = config.topCut, playMode = config.playMode,
            houseRulesNotes = config.houseRulesNotes
        )
    }
}

data class PokemonConfigState(
    val formatId: String? = null,
    val customFormatName: String? = null,
    val eventType: String? = null,
    val tournamentStyle: String? = null,
    val roundsCount: Int? = null,
    val roundTimeMinutes: Int? = null,
    val bestOf: Int? = null,
    val topCut: Int? = null,
    val requireDeckRegistration: Boolean? = null,
    val deckSubmissionDeadline: String? = null,
    val allowDeckChanges: Boolean? = null,
    val enforceFormatLegality: Boolean? = null,
    val allowProxies: Boolean? = null,
    val proxyLimit: Int? = null,
    val houseRulesNotes: String? = null,
    val hasPrizes: Boolean? = null,
    val prizeStructure: String? = null,
    val entryFee: Double? = null,
    val entryFeeCurrency: String? = null,
    val usePlayPoints: Boolean? = null,
    val organizerConfirmedOfficialLocation: Boolean? = null,
    val providesBasicEnergy: Boolean? = null,
    val providesDamageCounters: Boolean? = null,
    val sleevesRecommended: Boolean? = null,
    val providesBuildBattleKits: Boolean? = null,
    val hasJuniorDivision: Boolean? = null,
    val hasSeniorDivision: Boolean? = null,
    val hasMastersDivision: Boolean? = null,
    val allowSpectators: Boolean? = null
) {
    fun toInput() = PokemonEventConfigInput(
        formatId = formatId, customFormatName = customFormatName,
        eventType = eventType, tournamentStyle = tournamentStyle,
        roundsCount = roundsCount, roundTimeMinutes = roundTimeMinutes,
        bestOf = bestOf, topCut = topCut,
        requireDeckRegistration = requireDeckRegistration,
        deckSubmissionDeadline = deckSubmissionDeadline,
        allowDeckChanges = allowDeckChanges,
        enforceFormatLegality = enforceFormatLegality,
        allowProxies = allowProxies, proxyLimit = proxyLimit,
        houseRulesNotes = houseRulesNotes, hasPrizes = hasPrizes,
        prizeStructure = prizeStructure, entryFee = entryFee,
        entryFeeCurrency = entryFeeCurrency, usePlayPoints = usePlayPoints,
        organizerConfirmedOfficialLocation = organizerConfirmedOfficialLocation,
        providesBasicEnergy = providesBasicEnergy,
        providesDamageCounters = providesDamageCounters,
        sleevesRecommended = sleevesRecommended,
        providesBuildBattleKits = providesBuildBattleKits,
        hasJuniorDivision = hasJuniorDivision,
        hasSeniorDivision = hasSeniorDivision,
        hasMastersDivision = hasMastersDivision,
        allowSpectators = allowSpectators
    )

    companion object {
        fun fromConfig(config: PokemonEventConfig) = PokemonConfigState(
            formatId = config.formatId, customFormatName = config.customFormatName,
            eventType = config.eventType, tournamentStyle = config.tournamentStyle,
            roundsCount = config.roundsCount, roundTimeMinutes = config.roundTimeMinutes,
            bestOf = config.bestOf, topCut = config.topCut,
            requireDeckRegistration = config.requireDeckRegistration,
            deckSubmissionDeadline = config.deckSubmissionDeadline,
            allowDeckChanges = config.allowDeckChanges,
            enforceFormatLegality = config.enforceFormatLegality,
            allowProxies = config.allowProxies, proxyLimit = config.proxyLimit,
            houseRulesNotes = config.houseRulesNotes, hasPrizes = config.hasPrizes,
            prizeStructure = config.prizeStructure, entryFee = config.entryFee,
            entryFeeCurrency = config.entryFeeCurrency, usePlayPoints = config.usePlayPoints,
            organizerConfirmedOfficialLocation = config.organizerConfirmedOfficialLocation,
            providesBasicEnergy = config.providesBasicEnergy,
            providesDamageCounters = config.providesDamageCounters,
            sleevesRecommended = config.sleevesRecommended,
            providesBuildBattleKits = config.providesBuildBattleKits,
            hasJuniorDivision = config.hasJuniorDivision,
            hasSeniorDivision = config.hasSeniorDivision,
            hasMastersDivision = config.hasMastersDivision,
            allowSpectators = config.allowSpectators
        )
    }
}

data class YugiohConfigState(
    val formatId: String? = null,
    val customFormatName: String? = null,
    val eventType: String? = null,
    val tournamentStyle: String? = null,
    val roundsCount: Int? = null,
    val roundTimeMinutes: Int? = null,
    val bestOf: Int? = null,
    val topCut: Int? = null,
    val allowProxies: Boolean? = null,
    val proxyLimit: Int? = null,
    val requireDeckRegistration: Boolean? = null,
    val deckSubmissionDeadline: String? = null,
    val allowSideDeck: Boolean? = null,
    val enforceFormatLegality: Boolean? = null,
    val houseRulesNotes: String? = null,
    val hasPrizes: Boolean? = null,
    val prizeStructure: String? = null,
    val entryFee: Double? = null,
    val entryFeeCurrency: String? = null,
    val isOfficialEvent: Boolean? = null,
    val awardsOtsPoints: Boolean? = null,
    val allowSpectators: Boolean? = null
) {
    fun toInput() = YugiohEventConfigInput(
        formatId = formatId, customFormatName = customFormatName,
        eventType = eventType, tournamentStyle = tournamentStyle,
        roundsCount = roundsCount, roundTimeMinutes = roundTimeMinutes,
        bestOf = bestOf, topCut = topCut,
        allowProxies = allowProxies, proxyLimit = proxyLimit,
        requireDeckRegistration = requireDeckRegistration,
        deckSubmissionDeadline = deckSubmissionDeadline,
        allowSideDeck = allowSideDeck,
        enforceFormatLegality = enforceFormatLegality,
        houseRulesNotes = houseRulesNotes, hasPrizes = hasPrizes,
        prizeStructure = prizeStructure, entryFee = entryFee,
        entryFeeCurrency = entryFeeCurrency,
        isOfficialEvent = isOfficialEvent,
        awardsOtsPoints = awardsOtsPoints,
        allowSpectators = allowSpectators
    )

    companion object {
        fun fromConfig(config: YugiohEventConfig) = YugiohConfigState(
            formatId = config.formatId, customFormatName = config.customFormatName,
            eventType = config.eventType, tournamentStyle = config.tournamentStyle,
            roundsCount = config.roundsCount, roundTimeMinutes = config.roundTimeMinutes,
            bestOf = config.bestOf, topCut = config.topCut,
            allowProxies = config.allowProxies, proxyLimit = config.proxyLimit,
            requireDeckRegistration = config.requireDeckRegistration,
            deckSubmissionDeadline = config.deckSubmissionDeadline,
            allowSideDeck = config.allowSideDeck,
            enforceFormatLegality = config.enforceFormatLegality,
            houseRulesNotes = config.houseRulesNotes, hasPrizes = config.hasPrizes,
            prizeStructure = config.prizeStructure, entryFee = config.entryFee,
            entryFeeCurrency = config.entryFeeCurrency,
            isOfficialEvent = config.isOfficialEvent,
            awardsOtsPoints = config.awardsOtsPoints,
            allowSpectators = config.allowSpectators
        )
    }
}

data class Warhammer40kConfigState(
    val gameType: String? = null,
    val pointsLimit: Int? = null,
    val playerMode: String? = null,
    val missionPack: String? = null,
    val missionNotes: String? = null,
    val missionSelection: String? = null,
    val preSelectedMissions: List<String>? = null,
    val secondaryObjectives: String? = null,
    val battleReadyRequired: Boolean? = null,
    val wysiwygRequired: Boolean? = null,
    val forgeWorldAllowed: Boolean? = null,
    val legendsAllowed: Boolean? = null,
    val armyRulesNotes: String? = null,
    val requireArmyList: Boolean? = null,
    val armyListDeadline: String? = null,
    val armyListNotes: String? = null,
    val terrainType: String? = null,
    val tableSize: String? = null,
    val timeLimitMinutes: Int? = null,
    val eventType: String? = null,
    val tournamentStyle: String? = null,
    val roundsCount: Int? = null,
    val roundTimeMinutes: Int? = null,
    val includeTopCut: Boolean? = null,
    val scoringType: String? = null,
    val startingSupplyLimit: Int? = null,
    val startingCrusadePoints: Int? = null,
    val crusadeProgressionNotes: String? = null,
    val hasPrizes: Boolean? = null,
    val prizeStructure: String? = null,
    val entryFee: Double? = null,
    val entryFeeCurrency: String? = null,
    val allowSpectators: Boolean? = null,
    val allowProxies: Boolean? = null,
    val proxyNotes: String? = null
) {
    fun toInput() = Warhammer40kEventConfigInput(
        gameType = gameType, pointsLimit = pointsLimit,
        playerMode = playerMode, missionPack = missionPack,
        missionNotes = missionNotes, missionSelection = missionSelection,
        preSelectedMissions = preSelectedMissions,
        secondaryObjectives = secondaryObjectives,
        battleReadyRequired = battleReadyRequired,
        wysiwygRequired = wysiwygRequired,
        forgeWorldAllowed = forgeWorldAllowed,
        legendsAllowed = legendsAllowed,
        armyRulesNotes = armyRulesNotes,
        requireArmyList = requireArmyList,
        armyListDeadline = armyListDeadline,
        armyListNotes = armyListNotes,
        terrainType = terrainType, tableSize = tableSize,
        timeLimitMinutes = timeLimitMinutes,
        eventType = eventType, tournamentStyle = tournamentStyle,
        roundsCount = roundsCount, roundTimeMinutes = roundTimeMinutes,
        includeTopCut = includeTopCut, scoringType = scoringType,
        startingSupplyLimit = startingSupplyLimit,
        startingCrusadePoints = startingCrusadePoints,
        crusadeProgressionNotes = crusadeProgressionNotes,
        hasPrizes = hasPrizes, prizeStructure = prizeStructure,
        entryFee = entryFee, entryFeeCurrency = entryFeeCurrency,
        allowSpectators = allowSpectators,
        allowProxies = allowProxies, proxyNotes = proxyNotes
    )

    companion object {
        fun fromConfig(config: Warhammer40kEventConfig) = Warhammer40kConfigState(
            gameType = config.gameType, pointsLimit = config.pointsLimit,
            playerMode = config.playerMode, missionPack = config.missionPack,
            missionNotes = config.missionNotes, missionSelection = config.missionSelection,
            preSelectedMissions = config.preSelectedMissions,
            secondaryObjectives = config.secondaryObjectives,
            battleReadyRequired = config.battleReadyRequired,
            wysiwygRequired = config.wysiwygRequired,
            forgeWorldAllowed = config.forgeWorldAllowed,
            legendsAllowed = config.legendsAllowed,
            armyRulesNotes = config.armyRulesNotes,
            requireArmyList = config.requireArmyList,
            armyListDeadline = config.armyListDeadline,
            armyListNotes = config.armyListNotes,
            terrainType = config.terrainType, tableSize = config.tableSize,
            timeLimitMinutes = config.timeLimitMinutes,
            eventType = config.eventType, tournamentStyle = config.tournamentStyle,
            roundsCount = config.roundsCount, roundTimeMinutes = config.roundTimeMinutes,
            includeTopCut = config.includeTopCut, scoringType = config.scoringType,
            startingSupplyLimit = config.startingSupplyLimit,
            startingCrusadePoints = config.startingCrusadePoints,
            crusadeProgressionNotes = config.crusadeProgressionNotes,
            hasPrizes = config.hasPrizes, prizeStructure = config.prizeStructure,
            entryFee = config.entryFee, entryFeeCurrency = config.entryFeeCurrency,
            allowSpectators = config.allowSpectators,
            allowProxies = config.allowProxies, proxyNotes = config.proxyNotes
        )
    }
}

data class Warhammer40kEventConfigInput(
    @Json(name = "gameType") val gameType: String? = null,
    @Json(name = "pointsLimit") val pointsLimit: Int? = null,
    @Json(name = "playerMode") val playerMode: String? = null,
    @Json(name = "missionPack") val missionPack: String? = null,
    @Json(name = "missionNotes") val missionNotes: String? = null,
    @Json(name = "missionSelection") val missionSelection: String? = null,
    @Json(name = "preSelectedMissions") val preSelectedMissions: List<String>? = null,
    @Json(name = "secondaryObjectives") val secondaryObjectives: String? = null,
    @Json(name = "battleReadyRequired") val battleReadyRequired: Boolean? = null,
    @Json(name = "wysiwygRequired") val wysiwygRequired: Boolean? = null,
    @Json(name = "forgeWorldAllowed") val forgeWorldAllowed: Boolean? = null,
    @Json(name = "legendsAllowed") val legendsAllowed: Boolean? = null,
    @Json(name = "armyRulesNotes") val armyRulesNotes: String? = null,
    @Json(name = "requireArmyList") val requireArmyList: Boolean? = null,
    @Json(name = "armyListDeadline") val armyListDeadline: String? = null,
    @Json(name = "armyListNotes") val armyListNotes: String? = null,
    @Json(name = "terrainType") val terrainType: String? = null,
    @Json(name = "tableSize") val tableSize: String? = null,
    @Json(name = "timeLimitMinutes") val timeLimitMinutes: Int? = null,
    @Json(name = "eventType") val eventType: String? = null,
    @Json(name = "tournamentStyle") val tournamentStyle: String? = null,
    @Json(name = "roundsCount") val roundsCount: Int? = null,
    @Json(name = "roundTimeMinutes") val roundTimeMinutes: Int? = null,
    @Json(name = "includeTopCut") val includeTopCut: Boolean? = null,
    @Json(name = "scoringType") val scoringType: String? = null,
    @Json(name = "startingSupplyLimit") val startingSupplyLimit: Int? = null,
    @Json(name = "startingCrusadePoints") val startingCrusadePoints: Int? = null,
    @Json(name = "crusadeProgressionNotes") val crusadeProgressionNotes: String? = null,
    @Json(name = "hasPrizes") val hasPrizes: Boolean? = null,
    @Json(name = "prizeStructure") val prizeStructure: String? = null,
    @Json(name = "entryFee") val entryFee: Double? = null,
    @Json(name = "entryFeeCurrency") val entryFeeCurrency: String? = null,
    @Json(name = "allowSpectators") val allowSpectators: Boolean? = null,
    @Json(name = "allowProxies") val allowProxies: Boolean? = null,
    @Json(name = "proxyNotes") val proxyNotes: String? = null
)
