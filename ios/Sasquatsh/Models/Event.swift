import Foundation

struct Event: Codable, Identifiable {
    let id: String
    let hostUserId: String
    var title: String
    var description: String?
    var gameTitle: String?
    var gameCategory: String?
    var eventDate: String
    var startTime: String?
    var durationMinutes: Int?
    var setupMinutes: Int?
    var addressLine1: String?
    var city: String?
    var state: String?
    var postalCode: String?
    var locationDetails: String?
    var eventLocationId: String?
    var venueHall: String?
    var venueRoom: String?
    var venueTable: String?
    var timezone: String?
    var hostIsPlaying: Bool?
    var difficultyLevel: String?
    var maxPlayers: Int?
    var confirmedCount: Int
    var isPublic: Bool
    var isCharityEvent: Bool
    var minAge: Int?
    var status: String
    let host: UserSummary?
    var registrations: [EventRegistration]?
    var items: [EventItem]?
    var games: [EventGameSummary]?
    let createdAt: String?
}

struct EventSummary: Codable, Identifiable {
    let id: String
    let title: String
    let gameTitle: String?
    let gameCategory: String?
    let eventDate: String
    let startTime: String?
    let durationMinutes: Int?
    let city: String?
    let state: String?
    let difficultyLevel: String?
    let maxPlayers: Int?
    let confirmedCount: Int
    let isPublic: Bool
    let isCharityEvent: Bool
    let minAge: Int?
    let eventLocationId: String?
    let timezone: String?
    let status: String
    let host: UserSummary?
}

struct EventRegistration: Codable, Identifiable {
    let id: String
    let userId: String
    let status: String
    let user: UserSummary?
    let registeredAt: String
}

struct EventItem: Codable, Identifiable {
    let id: String
    var itemName: String
    var itemCategory: String
    var quantityNeeded: Int
    var claimedByUserId: String?
    var claimedByName: String?
    var claimedAt: String?
}

struct EventGameSummary: Codable, Identifiable {
    let id: String
    let bggId: Int?
    let gameName: String
    let thumbnailUrl: String?
    let minPlayers: Int?
    let maxPlayers: Int?
    let playingTime: Int?
    let isPrimary: Bool
    let isAlternative: Bool
}

struct CreateEventInput: Codable {
    var title: String
    var description: String?
    var gameTitle: String?
    var gameCategory: String?
    var eventDate: String
    var startTime: String
    var durationMinutes: Int?
    var setupMinutes: Int?
    var addressLine1: String?
    var city: String?
    var state: String?
    var postalCode: String?
    var locationDetails: String?
    var eventLocationId: String?
    var venueHall: String?
    var venueRoom: String?
    var venueTable: String?
    var timezone: String?
    var hostIsPlaying: Bool?
    var difficultyLevel: String?
    var maxPlayers: Int?
    var isPublic: Bool?
    var isCharityEvent: Bool?
    var minAge: Int?
    var status: String?
    var groupId: String?
}

struct UpdateEventInput: Codable {
    var title: String
    var description: String?
    var gameTitle: String?
    var gameCategory: String?
    var eventDate: String
    var startTime: String
    var durationMinutes: Int
    var setupMinutes: Int
    var addressLine1: String?
    var city: String?
    var state: String?
    var postalCode: String?
    var locationDetails: String?
    var eventLocationId: String?
    var venueHall: String?
    var venueRoom: String?
    var venueTable: String?
    var timezone: String?
    var hostIsPlaying: Bool?
    var difficultyLevel: String?
    var maxPlayers: Int
    var isPublic: Bool
    var isCharityEvent: Bool
    var minAge: Int?
    var status: String
}

struct CreateEventItemInput: Codable {
    var itemName: String
    var itemCategory: String?
    var quantityNeeded: Int?
}

struct EventSearchFilter {
    var city: String?
    var state: String?
    var search: String?
    var gameCategory: GameCategory?
    var difficulty: DifficultyLevel?
    var dateFrom: String?
    var dateTo: String?
    var nearbyZip: String?
    var radiusMiles: Int?

    var queryItems: [URLQueryItem] {
        var items: [URLQueryItem] = []
        if let city { items.append(.init(name: "city", value: city)) }
        if let state { items.append(.init(name: "state", value: state)) }
        if let search, !search.isEmpty { items.append(.init(name: "search", value: search)) }
        if let gameCategory { items.append(.init(name: "gameCategory", value: gameCategory.rawValue)) }
        if let difficulty { items.append(.init(name: "difficulty", value: difficulty.rawValue)) }
        if let dateFrom { items.append(.init(name: "dateFrom", value: dateFrom)) }
        if let dateTo { items.append(.init(name: "dateTo", value: dateTo)) }
        if let nearbyZip { items.append(.init(name: "nearbyZip", value: nearbyZip)) }
        if let radiusMiles { items.append(.init(name: "radiusMiles", value: String(radiusMiles))) }
        return items
    }
}
