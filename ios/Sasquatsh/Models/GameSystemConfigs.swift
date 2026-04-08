import Foundation

// MARK: - MTG Event Configuration

struct MtgEventConfig: Codable {
    let eventId: String?
    let formatId: String?
    let customFormatName: String?
    let eventType: String?
    let roundsCount: Int?
    let roundTimeMinutes: Int?
    let podsSize: Int?
    let allowProxies: Bool?
    let proxyLimit: Int?
    let powerLevelMin: Int?
    let powerLevelMax: Int?
    let powerLevelRange: String?
    let bannedCards: [String]?
    let packsPerPlayer: Int?
    let draftStyle: String?
    let cubeId: String?
    let hasPrizes: Bool?
    let prizeStructure: String?
    let entryFee: Double?
    let entryFeeCurrency: String?
    let requireDeckRegistration: Bool?
    let deckSubmissionDeadline: String?
    let allowSpectators: Bool?
    let matchStyle: String?
    let topCut: Int?
    let playMode: String?
    let houseRulesNotes: String?

    var formatDisplayName: String {
        guard let formatId else { return "Custom" }
        let names: [String: String] = [
            "commander": "Commander",
            "standard": "Standard",
            "modern": "Modern",
            "pioneer": "Pioneer",
            "legacy": "Legacy",
            "vintage": "Vintage",
            "pauper": "Pauper",
            "draft": "Draft",
            "sealed": "Sealed",
            "cube": "Cube",
            "oathbreaker": "Oathbreaker",
            "brawl": "Brawl",
            "casual": "Casual",
            "custom": customFormatName ?? "Custom"
        ]
        return names[formatId] ?? formatId.capitalized
    }

    var eventTypeDisplayName: String {
        guard let eventType else { return "" }
        let names: [String: String] = [
            "casual": "Casual",
            "swiss": "Swiss",
            "single_elim": "Single Elimination",
            "double_elim": "Double Elimination",
            "round_robin": "Round Robin",
            "pods": "Pods"
        ]
        return names[eventType] ?? eventType.capitalized
    }

    var powerLevelDisplayName: String? {
        guard let powerLevelRange else { return nil }
        let names: [String: String] = [
            "casual": "Casual",
            "mid": "Mid",
            "high": "High",
            "cedh": "cEDH",
            "custom": "Custom"
        ]
        return names[powerLevelRange] ?? powerLevelRange.capitalized
    }
}

// MARK: - Pokemon TCG Event Configuration

struct PokemonEventConfig: Codable {
    let eventId: String?
    let formatId: String?
    let customFormatName: String?
    let eventType: String?
    let tournamentStyle: String?
    let roundsCount: Int?
    let roundTimeMinutes: Int?
    let bestOf: Int?
    let topCut: Int?
    let requireDeckRegistration: Bool?
    let deckSubmissionDeadline: String?
    let allowDeckChanges: Bool?
    let enforceFormatLegality: Bool?
    let entryFee: Double?
    let entryFeeCurrency: String?
    let hasPrizes: Bool?
    let prizeStructure: String?
    let allowProxies: Bool?
    let proxyLimit: Int?
    let usePlayPoints: Bool?
    let houseRulesNotes: String?
    let allowSpectators: Bool?
    let providesBasicEnergy: Bool?
    let providesDamageCounters: Bool?
    let sleevesRecommended: Bool?
    let providesBuildBattleKits: Bool?
    let organizerConfirmedOfficialLocation: Bool?
    let ageDivisions: [String]?
    let hasJuniorDivision: Bool?
    let hasSeniorDivision: Bool?
    let hasMastersDivision: Bool?

    var formatDisplayName: String {
        guard let formatId else { return "Custom" }
        let names: [String: String] = [
            "standard": "Standard",
            "expanded": "Expanded",
            "unlimited": "Unlimited",
            "theme": "Theme"
        ]
        return names[formatId] ?? customFormatName ?? formatId.capitalized
    }

    var eventTypeDisplayName: String {
        guard let eventType else { return "" }
        let names: [String: String] = [
            "casual": "Casual",
            "league": "League",
            "league_cup": "League Cup",
            "league_challenge": "League Challenge",
            "regional": "Regional",
            "international": "International",
            "worlds": "Worlds",
            "prerelease": "Prerelease",
            "draft": "Draft"
        ]
        return names[eventType] ?? eventType.capitalized
    }
}

// MARK: - Yu-Gi-Oh! Event Configuration

struct YugiohEventConfig: Codable {
    let eventId: String?
    let formatId: String?
    let customFormatName: String?
    let eventType: String?
    let tournamentStyle: String?
    let roundsCount: Int?
    let roundTimeMinutes: Int?
    let bestOf: Int?
    let topCut: Int?
    let allowProxies: Bool?
    let proxyLimit: Int?
    let requireDeckRegistration: Bool?
    let deckSubmissionDeadline: String?
    let allowSideDeck: Bool?
    let enforceFormatLegality: Bool?
    let houseRulesNotes: String?
    let hasPrizes: Bool?
    let prizeStructure: String?
    let entryFee: Double?
    let entryFeeCurrency: String?
    let isOfficialEvent: Bool?
    let awardsOtsPoints: Bool?
    let allowSpectators: Bool?

    var formatDisplayName: String {
        guard let formatId else { return "Custom" }
        let names: [String: String] = [
            "advanced": "Advanced",
            "traditional": "Traditional",
            "speed_duel": "Speed Duel",
            "time_wizard": "Time Wizard",
            "casual": "Casual"
        ]
        return names[formatId] ?? customFormatName ?? formatId.capitalized
    }

    var eventTypeDisplayName: String {
        guard let eventType else { return "" }
        let names: [String: String] = [
            "casual": "Casual",
            "locals": "Locals",
            "ots": "OTS Championship",
            "regional": "Regional",
            "ycs": "YCS",
            "nationals": "Nationals",
            "worlds": "Worlds"
        ]
        return names[eventType] ?? eventType.capitalized
    }
}

// MARK: - Warhammer 40K Event Configuration

struct Warhammer40kEventConfig: Codable {
    let eventId: String?
    let gameType: String?
    let pointsLimit: Int?
    let playerMode: String?
    let missionPack: String?
    let missionNotes: String?
    let missionSelection: String?
    let preSelectedMissions: [String]?
    let secondaryObjectives: String?
    let battleReadyRequired: Bool?
    let wysiwygRequired: Bool?
    let forgeWorldAllowed: Bool?
    let legendsAllowed: Bool?
    let armyRulesNotes: String?
    let requireArmyList: Bool?
    let armyListDeadline: String?
    let armyListNotes: String?
    let terrainType: String?
    let tableSize: String?
    let timeLimitMinutes: Int?
    let eventType: String?
    let tournamentStyle: String?
    let roundsCount: Int?
    let roundTimeMinutes: Int?
    let includeTopCut: Bool?
    let scoringType: String?
    let startingSupplyLimit: Int?
    let startingCrusadePoints: Int?
    let crusadeProgressionNotes: String?
    let hasPrizes: Bool?
    let prizeStructure: String?
    let entryFee: Double?
    let entryFeeCurrency: String?
    let allowSpectators: Bool?
    let allowProxies: Bool?
    let proxyNotes: String?

    var gameTypeDisplayName: String {
        guard let gameType else { return "" }
        let names: [String: String] = [
            "matched": "Matched Play",
            "narrative": "Narrative",
            "crusade": "Crusade",
            "open": "Open Play"
        ]
        return names[gameType] ?? gameType.capitalized
    }

    var eventTypeDisplayName: String {
        guard let eventType else { return "" }
        let names: [String: String] = [
            "casual": "Casual",
            "tournament": "Tournament",
            "campaign": "Campaign",
            "league": "League"
        ]
        return names[eventType] ?? eventType.capitalized
    }

    var playerModeDisplayName: String? {
        guard let playerMode else { return nil }
        let names: [String: String] = [
            "1v1": "1v1",
            "2v2": "2v2",
            "group": "Group"
        ]
        return names[playerMode] ?? playerMode
    }
}
