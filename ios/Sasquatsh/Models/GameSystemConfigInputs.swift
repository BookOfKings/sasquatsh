import Foundation

// MARK: - MTG Event Config Input

struct MtgEventConfigInput: Codable {
    var formatId: String?
    var customFormatName: String?
    var eventType: String?
    var roundsCount: Int?
    var roundTimeMinutes: Int?
    var podsSize: Int?
    var allowProxies: Bool?
    var proxyLimit: Int?
    var powerLevelMin: Int?
    var powerLevelMax: Int?
    var powerLevelRange: String?
    var bannedCards: [String]?
    var packsPerPlayer: Int?
    var draftStyle: String?
    var cubeId: String?
    var hasPrizes: Bool?
    var prizeStructure: String?
    var entryFee: Double?
    var entryFeeCurrency: String?
    var requireDeckRegistration: Bool?
    var deckSubmissionDeadline: String?
    var allowSpectators: Bool?
    var matchStyle: String?
    var topCut: Int?
    var playMode: String?
    var houseRulesNotes: String?
}

// MARK: - Pokemon Event Config Input

struct PokemonEventConfigInput: Codable {
    var formatId: String?
    var customFormatName: String?
    var eventType: String?
    var tournamentStyle: String?
    var roundsCount: Int?
    var roundTimeMinutes: Int?
    var bestOf: Int?
    var topCut: Int?
    var requireDeckRegistration: Bool?
    var deckSubmissionDeadline: String?
    var allowDeckChanges: Bool?
    var enforceFormatLegality: Bool?
    var allowProxies: Bool?
    var proxyLimit: Int?
    var houseRulesNotes: String?
    var hasPrizes: Bool?
    var prizeStructure: String?
    var entryFee: Double?
    var entryFeeCurrency: String?
    var usePlayPoints: Bool?
    var organizerConfirmedOfficialLocation: Bool?
    var providesBasicEnergy: Bool?
    var providesDamageCounters: Bool?
    var sleevesRecommended: Bool?
    var providesBuildBattleKits: Bool?
    var hasJuniorDivision: Bool?
    var hasSeniorDivision: Bool?
    var hasMastersDivision: Bool?
    var allowSpectators: Bool?
}

// MARK: - Yu-Gi-Oh Event Config Input

struct YugiohEventConfigInput: Codable {
    var formatId: String?
    var customFormatName: String?
    var eventType: String?
    var tournamentStyle: String?
    var roundsCount: Int?
    var roundTimeMinutes: Int?
    var bestOf: Int?
    var topCut: Int?
    var allowProxies: Bool?
    var proxyLimit: Int?
    var requireDeckRegistration: Bool?
    var deckSubmissionDeadline: String?
    var allowSideDeck: Bool?
    var enforceFormatLegality: Bool?
    var houseRulesNotes: String?
    var hasPrizes: Bool?
    var prizeStructure: String?
    var entryFee: Double?
    var entryFeeCurrency: String?
    var isOfficialEvent: Bool?
    var awardsOtsPoints: Bool?
    var allowSpectators: Bool?
}

// MARK: - Warhammer 40K Event Config Input

struct Warhammer40kEventConfigInput: Codable {
    var gameType: String?
    var pointsLimit: Int?
    var playerMode: String?
    var missionPack: String?
    var missionNotes: String?
    var missionSelection: String?
    var preSelectedMissions: [String]?
    var secondaryObjectives: String?
    var battleReadyRequired: Bool?
    var wysiwygRequired: Bool?
    var forgeWorldAllowed: Bool?
    var legendsAllowed: Bool?
    var armyRulesNotes: String?
    var requireArmyList: Bool?
    var armyListDeadline: String?
    var armyListNotes: String?
    var terrainType: String?
    var tableSize: String?
    var timeLimitMinutes: Int?
    var eventType: String?
    var tournamentStyle: String?
    var roundsCount: Int?
    var roundTimeMinutes: Int?
    var includeTopCut: Bool?
    var scoringType: String?
    var startingSupplyLimit: Int?
    var startingCrusadePoints: Int?
    var crusadeProgressionNotes: String?
    var hasPrizes: Bool?
    var prizeStructure: String?
    var entryFee: Double?
    var entryFeeCurrency: String?
    var allowSpectators: Bool?
    var allowProxies: Bool?
    var proxyNotes: String?
}
