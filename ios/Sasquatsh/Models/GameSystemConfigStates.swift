import Foundation

// MARK: - MTG Format Category Helper

enum MtgFormatCategory: String, CaseIterable, Identifiable {
    case constructed, commander, limited, casual

    var id: String { rawValue }
    var displayName: String { rawValue.capitalized }

    var formats: [(id: String, name: String)] {
        switch self {
        case .constructed: return [
            ("standard", "Standard"), ("modern", "Modern"), ("pioneer", "Pioneer"),
            ("legacy", "Legacy"), ("vintage", "Vintage"), ("pauper", "Pauper")
        ]
        case .commander: return [
            ("commander", "Commander"), ("oathbreaker", "Oathbreaker"), ("brawl", "Brawl")
        ]
        case .limited: return [
            ("draft", "Draft"), ("sealed", "Sealed"), ("cube", "Cube")
        ]
        case .casual: return [
            ("casual", "Casual"), ("custom", "Custom")
        ]
        }
    }

    static func category(for formatId: String?) -> MtgFormatCategory {
        guard let formatId else { return .casual }
        for cat in allCases {
            if cat.formats.contains(where: { $0.id == formatId }) { return cat }
        }
        return .casual
    }
}

// MARK: - MTG Config State

struct MtgConfigState {
    var formatId: String? = nil
    var customFormatName: String = ""
    var formatCategory: MtgFormatCategory = .commander
    var eventType: String = "casual"
    var playMode: String? = nil
    var matchStyle: String? = nil
    var roundsCount: Int? = nil
    var roundTimeMinutes: Int = 50
    var podsSize: Int = 4
    var topCut: Int? = nil
    var powerLevelRange: String? = nil
    var powerLevelMin: Int? = nil
    var powerLevelMax: Int? = nil
    var allowProxies: Bool = false
    var proxyLimit: Int? = nil
    var bannedCards: [String] = []
    var requireDeckRegistration: Bool = false
    var deckSubmissionDeadline: Date? = nil
    var houseRulesNotes: String = ""
    var packsPerPlayer: Int = 3
    var draftStyle: String = "standard"
    var cubeId: String = ""
    var hasPrizes: Bool = false
    var entryFee: String = ""
    var entryFeeCurrency: String = "USD"
    var prizeStructure: String = ""
    var allowSpectators: Bool = true

    var isCommanderFamily: Bool {
        ["commander", "oathbreaker", "brawl"].contains(formatId ?? "")
    }

    var isLimitedFormat: Bool {
        ["draft", "sealed", "cube"].contains(formatId ?? "")
    }

    var showPowerLevel: Bool {
        isCommanderFamily || formatId == "casual"
    }

    mutating func applyFormatDefaults() {
        formatCategory = MtgFormatCategory.category(for: formatId)
        guard let formatId else { return }
        switch formatId {
        case "commander", "oathbreaker", "brawl":
            eventType = "pods"
            playMode = "assigned_pods"
            roundTimeMinutes = 90
            matchStyle = nil
            powerLevelRange = "mid"
        case "standard", "modern", "pioneer", "legacy", "vintage", "pauper":
            eventType = "swiss"
            matchStyle = "bo3"
            roundTimeMinutes = 50
            playMode = "tournament_pairings"
            powerLevelRange = nil
        case "draft", "sealed", "cube":
            eventType = "swiss"
            matchStyle = "bo3"
            roundTimeMinutes = 50
            playMode = "tournament_pairings"
            powerLevelRange = nil
        default:
            break
        }
    }

    func toInput() -> MtgEventConfigInput {
        let fee = Double(entryFee)
        let deadline = deckSubmissionDeadline?.apiDateString
        return MtgEventConfigInput(
            formatId: formatId,
            customFormatName: formatId == "custom" ? customFormatName : nil,
            eventType: eventType,
            roundsCount: roundsCount,
            roundTimeMinutes: roundTimeMinutes,
            podsSize: eventType == "pods" ? podsSize : nil,
            allowProxies: allowProxies,
            proxyLimit: allowProxies ? proxyLimit : nil,
            powerLevelMin: powerLevelRange == "custom" ? powerLevelMin : nil,
            powerLevelMax: powerLevelRange == "custom" ? powerLevelMax : nil,
            powerLevelRange: showPowerLevel ? powerLevelRange : nil,
            bannedCards: bannedCards.isEmpty ? nil : bannedCards,
            packsPerPlayer: isLimitedFormat ? packsPerPlayer : nil,
            draftStyle: isLimitedFormat ? draftStyle : nil,
            cubeId: formatId == "cube" && !cubeId.isEmpty ? cubeId : nil,
            hasPrizes: hasPrizes,
            prizeStructure: hasPrizes ? prizeStructure : nil,
            entryFee: fee,
            entryFeeCurrency: entryFeeCurrency,
            requireDeckRegistration: requireDeckRegistration,
            deckSubmissionDeadline: requireDeckRegistration ? deadline : nil,
            allowSpectators: allowSpectators,
            matchStyle: matchStyle,
            topCut: topCut,
            playMode: playMode,
            houseRulesNotes: houseRulesNotes.isEmpty ? nil : houseRulesNotes
        )
    }

    init() {}

    init(from config: MtgEventConfig) {
        formatId = config.formatId
        customFormatName = config.customFormatName ?? ""
        formatCategory = MtgFormatCategory.category(for: config.formatId)
        eventType = config.eventType ?? "casual"
        playMode = config.playMode
        matchStyle = config.matchStyle
        roundsCount = config.roundsCount
        roundTimeMinutes = config.roundTimeMinutes ?? 50
        podsSize = config.podsSize ?? 4
        topCut = config.topCut
        powerLevelRange = config.powerLevelRange
        powerLevelMin = config.powerLevelMin
        powerLevelMax = config.powerLevelMax
        allowProxies = config.allowProxies ?? false
        proxyLimit = config.proxyLimit
        bannedCards = config.bannedCards ?? []
        requireDeckRegistration = config.requireDeckRegistration ?? false
        houseRulesNotes = config.houseRulesNotes ?? ""
        packsPerPlayer = config.packsPerPlayer ?? 3
        draftStyle = config.draftStyle ?? "standard"
        cubeId = config.cubeId ?? ""
        hasPrizes = config.hasPrizes ?? false
        entryFee = config.entryFee.map { String(format: "%.2f", $0) } ?? ""
        entryFeeCurrency = config.entryFeeCurrency ?? "USD"
        prizeStructure = config.prizeStructure ?? ""
        allowSpectators = config.allowSpectators ?? true
    }
}

// MARK: - Pokemon Config State

struct PokemonConfigState {
    var formatId: String? = nil
    var customFormatName: String = ""
    var eventType: String = "casual"
    var tournamentStyle: String? = nil
    var roundsCount: Int? = nil
    var roundTimeMinutes: Int = 50
    var bestOf: Int = 3
    var topCut: Int? = nil
    var requireDeckRegistration: Bool = false
    var deckSubmissionDeadline: Date? = nil
    var allowDeckChanges: Bool = true
    var enforceFormatLegality: Bool = true
    var allowProxies: Bool = false
    var proxyLimit: Int? = nil
    var houseRulesNotes: String = ""
    var hasPrizes: Bool = false
    var entryFee: String = ""
    var entryFeeCurrency: String = "USD"
    var prizeStructure: String = ""
    var usePlayPoints: Bool = false
    var organizerConfirmedOfficialLocation: Bool = false
    var providesBasicEnergy: Bool = false
    var providesDamageCounters: Bool = false
    var sleevesRecommended: Bool = true
    var providesBuildBattleKits: Bool = false
    var hasJuniorDivision: Bool = false
    var hasSeniorDivision: Bool = false
    var hasMastersDivision: Bool = true
    var allowSpectators: Bool = true

    mutating func applyFormatDefaults() {
        guard let formatId else { return }
        switch formatId {
        case "standard", "expanded":
            eventType = "league_challenge"
            tournamentStyle = "swiss"
            bestOf = 3
            roundTimeMinutes = 50
            enforceFormatLegality = true
        case "theme":
            eventType = "casual"
            tournamentStyle = nil
            enforceFormatLegality = true
        case "unlimited":
            eventType = "casual"
            enforceFormatLegality = false
        default:
            break
        }
    }

    mutating func applyEventTypeDefaults() {
        switch eventType {
        case "prerelease":
            providesBasicEnergy = true
            providesBuildBattleKits = true
        case "draft":
            providesBasicEnergy = true
        default:
            break
        }
    }

    func toInput() -> PokemonEventConfigInput {
        let fee = Double(entryFee)
        let deadline = deckSubmissionDeadline?.apiDateString
        return PokemonEventConfigInput(
            formatId: formatId,
            customFormatName: formatId == nil ? customFormatName : nil,
            eventType: eventType,
            tournamentStyle: tournamentStyle,
            roundsCount: roundsCount,
            roundTimeMinutes: roundTimeMinutes,
            bestOf: bestOf,
            topCut: topCut,
            requireDeckRegistration: requireDeckRegistration,
            deckSubmissionDeadline: requireDeckRegistration ? deadline : nil,
            allowDeckChanges: allowDeckChanges,
            enforceFormatLegality: enforceFormatLegality,
            allowProxies: allowProxies,
            proxyLimit: allowProxies ? proxyLimit : nil,
            houseRulesNotes: houseRulesNotes.isEmpty ? nil : houseRulesNotes,
            hasPrizes: hasPrizes,
            prizeStructure: hasPrizes ? prizeStructure : nil,
            entryFee: fee,
            entryFeeCurrency: entryFeeCurrency,
            usePlayPoints: usePlayPoints,
            organizerConfirmedOfficialLocation: organizerConfirmedOfficialLocation,
            providesBasicEnergy: providesBasicEnergy,
            providesDamageCounters: providesDamageCounters,
            sleevesRecommended: sleevesRecommended,
            providesBuildBattleKits: providesBuildBattleKits,
            hasJuniorDivision: hasJuniorDivision,
            hasSeniorDivision: hasSeniorDivision,
            hasMastersDivision: hasMastersDivision,
            allowSpectators: allowSpectators
        )
    }

    init() {}

    init(from config: PokemonEventConfig) {
        formatId = config.formatId
        customFormatName = config.customFormatName ?? ""
        eventType = config.eventType ?? "casual"
        tournamentStyle = config.tournamentStyle
        roundsCount = config.roundsCount
        roundTimeMinutes = config.roundTimeMinutes ?? 50
        bestOf = config.bestOf ?? 3
        topCut = config.topCut
        requireDeckRegistration = config.requireDeckRegistration ?? false
        allowDeckChanges = config.allowDeckChanges ?? true
        enforceFormatLegality = config.enforceFormatLegality ?? true
        allowProxies = config.allowProxies ?? false
        proxyLimit = config.proxyLimit
        houseRulesNotes = config.houseRulesNotes ?? ""
        hasPrizes = config.hasPrizes ?? false
        entryFee = config.entryFee.map { String(format: "%.2f", $0) } ?? ""
        entryFeeCurrency = config.entryFeeCurrency ?? "USD"
        prizeStructure = config.prizeStructure ?? ""
        usePlayPoints = config.usePlayPoints ?? false
        organizerConfirmedOfficialLocation = config.organizerConfirmedOfficialLocation ?? false
        providesBasicEnergy = config.providesBasicEnergy ?? false
        providesDamageCounters = config.providesDamageCounters ?? false
        sleevesRecommended = config.sleevesRecommended ?? true
        providesBuildBattleKits = config.providesBuildBattleKits ?? false
        hasJuniorDivision = config.hasJuniorDivision ?? false
        hasSeniorDivision = config.hasSeniorDivision ?? false
        hasMastersDivision = config.hasMastersDivision ?? true
        allowSpectators = config.allowSpectators ?? true
    }
}

// MARK: - Yu-Gi-Oh Config State

struct YugiohConfigState {
    var formatId: String? = "advanced"
    var customFormatName: String = ""
    var eventType: String? = nil
    var tournamentStyle: String? = nil
    var roundsCount: Int? = nil
    var roundTimeMinutes: Int = 40
    var bestOf: Int = 3
    var topCut: Int? = nil
    var allowProxies: Bool = false
    var proxyLimit: Int? = nil
    var requireDeckRegistration: Bool = false
    var deckSubmissionDeadline: Date? = nil
    var allowSideDeck: Bool = true
    var enforceFormatLegality: Bool = true
    var houseRulesNotes: String = ""
    var hasPrizes: Bool = false
    var entryFee: String = ""
    var entryFeeCurrency: String = "USD"
    var prizeStructure: String = ""
    var isOfficialEvent: Bool = false
    var awardsOtsPoints: Bool = false
    var allowSpectators: Bool = true

    mutating func applyFormatDefaults() {
        guard let formatId else { return }
        switch formatId {
        case "advanced", "traditional", "speed_duel":
            enforceFormatLegality = true
        case "casual", "time_wizard":
            enforceFormatLegality = false
        default:
            break
        }
    }

    mutating func applyEventTypeDefaults() {
        guard let eventType else { return }
        let officialTypes = ["ots", "regional", "ycs", "nationals", "worlds"]
        if officialTypes.contains(eventType) {
            isOfficialEvent = true
            requireDeckRegistration = true
            allowProxies = false
        } else {
            isOfficialEvent = false
            awardsOtsPoints = false
        }
    }

    func toInput() -> YugiohEventConfigInput {
        let fee = Double(entryFee)
        let deadline = deckSubmissionDeadline?.apiDateString
        return YugiohEventConfigInput(
            formatId: formatId,
            customFormatName: formatId == nil ? customFormatName : nil,
            eventType: eventType,
            tournamentStyle: tournamentStyle,
            roundsCount: roundsCount,
            roundTimeMinutes: roundTimeMinutes,
            bestOf: bestOf,
            topCut: topCut,
            allowProxies: allowProxies,
            proxyLimit: allowProxies ? proxyLimit : nil,
            requireDeckRegistration: requireDeckRegistration,
            deckSubmissionDeadline: requireDeckRegistration ? deadline : nil,
            allowSideDeck: allowSideDeck,
            enforceFormatLegality: enforceFormatLegality,
            houseRulesNotes: houseRulesNotes.isEmpty ? nil : houseRulesNotes,
            hasPrizes: hasPrizes,
            prizeStructure: hasPrizes ? prizeStructure : nil,
            entryFee: fee,
            entryFeeCurrency: entryFeeCurrency,
            isOfficialEvent: isOfficialEvent,
            awardsOtsPoints: awardsOtsPoints,
            allowSpectators: allowSpectators
        )
    }

    init() {}

    init(from config: YugiohEventConfig) {
        formatId = config.formatId
        customFormatName = config.customFormatName ?? ""
        eventType = config.eventType
        tournamentStyle = config.tournamentStyle
        roundsCount = config.roundsCount
        roundTimeMinutes = config.roundTimeMinutes ?? 40
        bestOf = config.bestOf ?? 3
        topCut = config.topCut
        allowProxies = config.allowProxies ?? false
        proxyLimit = config.proxyLimit
        requireDeckRegistration = config.requireDeckRegistration ?? false
        allowSideDeck = config.allowSideDeck ?? true
        enforceFormatLegality = config.enforceFormatLegality ?? true
        houseRulesNotes = config.houseRulesNotes ?? ""
        hasPrizes = config.hasPrizes ?? false
        entryFee = config.entryFee.map { String(format: "%.2f", $0) } ?? ""
        entryFeeCurrency = config.entryFeeCurrency ?? "USD"
        prizeStructure = config.prizeStructure ?? ""
        isOfficialEvent = config.isOfficialEvent ?? false
        awardsOtsPoints = config.awardsOtsPoints ?? false
        allowSpectators = config.allowSpectators ?? true
    }
}

// MARK: - Warhammer 40K Config State

struct Warhammer40kConfigState {
    var gameType: String? = nil
    var pointsLimit: Int = 2000
    var playerMode: String = "1v1"
    var missionPack: String = ""
    var missionNotes: String = ""
    var missionSelection: String? = nil
    var secondaryObjectives: String? = nil
    var battleReadyRequired: Bool = false
    var wysiwygRequired: Bool = false
    var forgeWorldAllowed: Bool = true
    var legendsAllowed: Bool = true
    var armyRulesNotes: String = ""
    var allowProxies: Bool = true
    var proxyNotes: String = ""
    var terrainType: String = "standard"
    var tableSize: String = "44x60"
    var timeLimitMinutes: Int? = nil
    var eventType: String? = nil
    var tournamentStyle: String? = nil
    var roundsCount: Int? = nil
    var roundTimeMinutes: Int = 120
    var includeTopCut: Bool = false
    var scoringType: String? = nil
    var requireArmyList: Bool = false
    var armyListDeadline: Date? = nil
    var armyListNotes: String = ""
    var startingSupplyLimit: Int = 1000
    var startingCrusadePoints: Int = 5
    var crusadeProgressionNotes: String = ""
    var hasPrizes: Bool = false
    var entryFee: String = ""
    var entryFeeCurrency: String = "USD"
    var prizeStructure: String = ""
    var allowSpectators: Bool = true

    var showCrusadeSettings: Bool {
        gameType == "crusade" && eventType == "campaign"
    }

    var showArmyListSubmission: Bool {
        eventType != nil && eventType != "casual"
    }

    var autoTableSize: String {
        switch pointsLimit {
        case ...999: return "44x30"
        default: return "44x60"
        }
    }

    mutating func applyPointsDefaults() {
        tableSize = autoTableSize
    }

    func toInput() -> Warhammer40kEventConfigInput {
        let fee = Double(entryFee)
        let deadline = armyListDeadline?.apiDateString
        return Warhammer40kEventConfigInput(
            gameType: gameType,
            pointsLimit: pointsLimit,
            playerMode: playerMode,
            missionPack: missionPack.isEmpty ? nil : missionPack,
            missionNotes: missionNotes.isEmpty ? nil : missionNotes,
            missionSelection: missionSelection,
            preSelectedMissions: nil,
            secondaryObjectives: secondaryObjectives,
            battleReadyRequired: battleReadyRequired,
            wysiwygRequired: wysiwygRequired,
            forgeWorldAllowed: forgeWorldAllowed,
            legendsAllowed: legendsAllowed,
            armyRulesNotes: armyRulesNotes.isEmpty ? nil : armyRulesNotes,
            requireArmyList: requireArmyList,
            armyListDeadline: requireArmyList ? deadline : nil,
            armyListNotes: armyListNotes.isEmpty ? nil : armyListNotes,
            terrainType: terrainType,
            tableSize: tableSize,
            timeLimitMinutes: timeLimitMinutes,
            eventType: eventType,
            tournamentStyle: tournamentStyle,
            roundsCount: roundsCount,
            roundTimeMinutes: roundTimeMinutes,
            includeTopCut: includeTopCut,
            scoringType: scoringType,
            startingSupplyLimit: showCrusadeSettings ? startingSupplyLimit : nil,
            startingCrusadePoints: showCrusadeSettings ? startingCrusadePoints : nil,
            crusadeProgressionNotes: showCrusadeSettings && !crusadeProgressionNotes.isEmpty ? crusadeProgressionNotes : nil,
            hasPrizes: hasPrizes,
            prizeStructure: hasPrizes ? prizeStructure : nil,
            entryFee: fee,
            entryFeeCurrency: entryFeeCurrency,
            allowSpectators: allowSpectators,
            allowProxies: allowProxies,
            proxyNotes: allowProxies && !proxyNotes.isEmpty ? proxyNotes : nil
        )
    }

    init() {}

    init(from config: Warhammer40kEventConfig) {
        gameType = config.gameType
        pointsLimit = config.pointsLimit ?? 2000
        playerMode = config.playerMode ?? "1v1"
        missionPack = config.missionPack ?? ""
        missionNotes = config.missionNotes ?? ""
        missionSelection = config.missionSelection
        secondaryObjectives = config.secondaryObjectives
        battleReadyRequired = config.battleReadyRequired ?? false
        wysiwygRequired = config.wysiwygRequired ?? false
        forgeWorldAllowed = config.forgeWorldAllowed ?? true
        legendsAllowed = config.legendsAllowed ?? true
        armyRulesNotes = config.armyRulesNotes ?? ""
        allowProxies = config.allowProxies ?? true
        proxyNotes = config.proxyNotes ?? ""
        terrainType = config.terrainType ?? "standard"
        tableSize = config.tableSize ?? "44x60"
        timeLimitMinutes = config.timeLimitMinutes
        eventType = config.eventType
        tournamentStyle = config.tournamentStyle
        roundsCount = config.roundsCount
        roundTimeMinutes = config.roundTimeMinutes ?? 120
        includeTopCut = config.includeTopCut ?? false
        scoringType = config.scoringType
        requireArmyList = config.requireArmyList ?? false
        armyListNotes = config.armyListNotes ?? ""
        startingSupplyLimit = config.startingSupplyLimit ?? 1000
        startingCrusadePoints = config.startingCrusadePoints ?? 5
        crusadeProgressionNotes = config.crusadeProgressionNotes ?? ""
        hasPrizes = config.hasPrizes ?? false
        entryFee = config.entryFee.map { String(format: "%.2f", $0) } ?? ""
        entryFeeCurrency = config.entryFeeCurrency ?? "USD"
        prizeStructure = config.prizeStructure ?? ""
        allowSpectators = config.allowSpectators ?? true
    }
}
