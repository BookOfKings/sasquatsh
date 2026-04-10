import Foundation

struct ScryfallCard: Codable, Identifiable {
    let scryfallId: String
    let oracleId: String?
    let name: String
    let manaCost: String?
    let cmc: Double?
    let typeLine: String?
    let oracleText: String?
    let power: String?
    let toughness: String?
    let loyalty: String?
    let colors: [String]?
    let colorIdentity: [String]?
    let keywords: [String]?
    let legalities: [String: String]?
    let setCode: String?
    let rarity: String?
    let imageUris: ScryfallImageUris?
    let prices: [String: String?]?
    let isDoubleFaced: Bool?
    let cardFaces: [ScryfallCardFace]?

    var id: String { scryfallId }

    var smallImageUrl: String? { imageUris?.small ?? cardFaces?.first?.imageUris?.small }
    var normalImageUrl: String? { imageUris?.normal ?? cardFaces?.first?.imageUris?.normal }
    var largeImageUrl: String? { imageUris?.large ?? cardFaces?.first?.imageUris?.large }

    var isCreature: Bool { typeLine?.contains("Creature") == true }
    var isLand: Bool { typeLine?.contains("Land") == true }
    var isInstant: Bool { typeLine?.contains("Instant") == true }
    var isSorcery: Bool { typeLine?.contains("Sorcery") == true }
    var isEnchantment: Bool { typeLine?.contains("Enchantment") == true }
    var isArtifact: Bool { typeLine?.contains("Artifact") == true }
    var isPlaneswalker: Bool { typeLine?.contains("Planeswalker") == true }

    var typeCategory: String {
        if isCreature { return "Creatures" }
        if isInstant { return "Instants" }
        if isSorcery { return "Sorceries" }
        if isEnchantment { return "Enchantments" }
        if isArtifact { return "Artifacts" }
        if isPlaneswalker { return "Planeswalkers" }
        if isLand { return "Lands" }
        return "Other"
    }
}

struct ScryfallImageUris: Codable {
    let small: String?
    let normal: String?
    let large: String?
    let artCrop: String?
    let png: String?
}

struct ScryfallCardFace: Codable {
    let name: String?
    let manaCost: String?
    let typeLine: String?
    let oracleText: String?
    let imageUris: ScryfallImageUris?
}

struct MtgDeck: Codable, Identifiable {
    let id: String
    let ownerUserId: String?
    let name: String
    let formatId: String?
    let commanderScryfallId: String?
    let partnerCommanderScryfallId: String?
    let description: String?
    let powerLevel: Int?
    let isPublic: Bool?
    let cards: [MtgDeckCard]?
    let cardCount: Int?
    let commander: ScryfallCard?
    let partnerCommander: ScryfallCard?
}

struct MtgDeckCard: Codable, Identifiable {
    let id: String
    let deckId: String?
    let scryfallId: String
    let quantity: Int
    let board: String  // main, sideboard, maybeboard, commander
    let card: ScryfallCard?
}

struct CreateDeckInput: Codable {
    var name: String
    var formatId: String?
    var description: String?
    var powerLevel: Int?
    var isPublic: Bool?
}

struct UpdateDeckInput: Codable {
    var name: String?
    var formatId: String?
    var description: String?
    var powerLevel: Int?
    var isPublic: Bool?
    var commanderScryfallId: String?
    var partnerCommanderScryfallId: String?
}

struct DeckCardInput: Codable {
    var scryfallId: String
    var quantity: Int
    var board: String
}

struct ImportDeckInput: Codable {
    var name: String?
    var formatId: String?
    var deckList: String?
    var url: String?
}

struct DeckResponse: Codable {
    let deck: MtgDeck?
}

struct DecksResponse: Codable {
    let decks: [MtgDeck]?
}
