import Foundation

protocol EventsServiceProtocol: Sendable {
    func getPublicEvents(filter: EventSearchFilter) async throws -> [EventSummary]
    func getRegisteredEvents() async throws -> [EventSummary]
    func getHostedEvents() async throws -> [EventSummary]
    func getEvent(id: String) async throws -> Event
    func createEvent(input: CreateEventInput) async throws -> Event
    func updateEvent(id: String, input: UpdateEventInput) async throws -> Event
    func deleteEvent(id: String) async throws
    func registerForEvent(eventId: String) async throws
    func cancelRegistration(eventId: String) async throws
    func addItem(eventId: String, input: CreateEventItemInput) async throws -> EventItem
    func claimItem(itemId: String) async throws
    func unclaimItem(itemId: String) async throws
    func addGame(input: AddEventGameInput) async throws -> EventGame
    func removeGame(gameId: String) async throws
}

final class EventsService: EventsServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getPublicEvents(filter: EventSearchFilter = EventSearchFilter()) async throws -> [EventSummary] {
        var items = filter.queryItems
        items.append(.init(name: "type", value: "browse"))
        return try await api.get("events", queryItems: items, authenticated: true)
    }

    func getRegisteredEvents() async throws -> [EventSummary] {
        try await api.get("events", queryItems: [.init(name: "type", value: "registered")], authenticated: true)
    }

    func getHostedEvents() async throws -> [EventSummary] {
        try await api.get("events", queryItems: [.init(name: "type", value: "hosted")], authenticated: true)
    }

    func getEvent(id: String) async throws -> Event {
        try await api.get("events", queryItems: [.init(name: "id", value: id)], authenticated: true)
    }

    func createEvent(input: CreateEventInput) async throws -> Event {
        try await api.post("events", body: input)
    }

    func updateEvent(id: String, input: UpdateEventInput) async throws -> Event {
        try await api.put("events", body: input, queryItems: [.init(name: "id", value: id)])
    }

    func deleteEvent(id: String) async throws {
        try await api.deleteVoid("events", queryItems: [.init(name: "id", value: id)])
    }

    func registerForEvent(eventId: String) async throws {
        let body = ["eventId": eventId]
        try await api.postVoid("registrations", body: body)
    }

    func cancelRegistration(eventId: String) async throws {
        try await api.deleteVoid("registrations", queryItems: [.init(name: "eventId", value: eventId)])
    }

    func addItem(eventId: String, input: CreateEventItemInput) async throws -> EventItem {
        struct ItemRequest: Codable {
            let eventId: String
            let itemName: String
            let itemCategory: String?
            let quantityNeeded: Int?
        }
        let body = ItemRequest(eventId: eventId, itemName: input.itemName, itemCategory: input.itemCategory, quantityNeeded: input.quantityNeeded)
        return try await api.post("items", body: body)
    }

    func claimItem(itemId: String) async throws {
        try await api.putVoid("items", queryItems: [
            .init(name: "id", value: itemId),
            .init(name: "action", value: "claim")
        ])
    }

    func unclaimItem(itemId: String) async throws {
        try await api.putVoid("items", queryItems: [
            .init(name: "id", value: itemId),
            .init(name: "action", value: "unclaim")
        ])
    }

    func addGame(input: AddEventGameInput) async throws -> EventGame {
        try await api.post("event-games", body: input)
    }

    func removeGame(gameId: String) async throws {
        try await api.deleteVoid("event-games", queryItems: [.init(name: "id", value: gameId)])
    }
}
