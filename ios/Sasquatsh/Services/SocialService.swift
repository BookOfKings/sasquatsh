import Foundation

protocol SocialServiceProtocol: Sendable {
    func getPlayerRequests(filters: PlayerRequestFilters) async throws -> [PlayerRequest]
    func getMyPlayerRequests() async throws -> [PlayerRequest]
    func createPlayerRequest(input: CreatePlayerRequestInput) async throws -> PlayerRequest
    func fillPlayerRequest(id: String) async throws -> PlayerRequest
    func cancelPlayerRequest(id: String) async throws -> PlayerRequest
    func deletePlayerRequest(id: String) async throws
    func getEventLocations() async throws -> [EventLocation]
    func getHotLocations() async throws -> [EventLocation]
    func createEventLocation(input: CreateEventLocationInput) async throws -> EventLocation
    func getGameInvitation(code: String) async throws -> GameInvitation
    func acceptGameInvitation(code: String) async throws -> AcceptGameInviteResponse
    func createGameInvitation(input: CreateGameInvitationInput) async throws -> GameInvitation
    func getEventInvitations(eventId: String) async throws -> [GameInvitation]
    func revokeGameInvitation(id: String) async throws
}

struct AcceptGameInviteResponse: Codable {
    let message: String
    let eventId: String
}

final class SocialService: SocialServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getPlayerRequests(filters: PlayerRequestFilters = PlayerRequestFilters()) async throws -> [PlayerRequest] {
        try await api.get("player-requests", queryItems: filters.queryItems, authenticated: true)
    }

    func getMyPlayerRequests() async throws -> [PlayerRequest] {
        try await api.get("player-requests", queryItems: [.init(name: "id", value: "mine")], authenticated: true)
    }

    func createPlayerRequest(input: CreatePlayerRequestInput) async throws -> PlayerRequest {
        try await api.post("player-requests", body: input)
    }

    func fillPlayerRequest(id: String) async throws -> PlayerRequest {
        try await api.post("player-requests", queryItems: [
            .init(name: "id", value: id),
            .init(name: "action", value: "fill")
        ])
    }

    func cancelPlayerRequest(id: String) async throws -> PlayerRequest {
        try await api.post("player-requests", queryItems: [
            .init(name: "id", value: id),
            .init(name: "action", value: "cancel")
        ])
    }

    func deletePlayerRequest(id: String) async throws {
        try await api.deleteVoid("player-requests", queryItems: [.init(name: "id", value: id)])
    }

    func getEventLocations() async throws -> [EventLocation] {
        try await api.get("event-locations", queryItems: [.init(name: "forEvent", value: "true")], authenticated: false)
    }

    func getHotLocations() async throws -> [EventLocation] {
        try await api.get("event-locations", queryItems: [.init(name: "hot", value: "true")], authenticated: false)
    }

    func createEventLocation(input: CreateEventLocationInput) async throws -> EventLocation {
        try await api.post("event-locations", body: input)
    }

    func getGameInvitation(code: String) async throws -> GameInvitation {
        try await api.get("invitations", queryItems: [.init(name: "code", value: code)], authenticated: true)
    }

    func acceptGameInvitation(code: String) async throws -> AcceptGameInviteResponse {
        try await api.post("invitations", queryItems: [
            .init(name: "code", value: code),
            .init(name: "action", value: "accept")
        ])
    }

    func createGameInvitation(input: CreateGameInvitationInput) async throws -> GameInvitation {
        try await api.post("invitations", body: input)
    }

    func getEventInvitations(eventId: String) async throws -> [GameInvitation] {
        try await api.get("invitations", queryItems: [.init(name: "eventId", value: eventId)], authenticated: true)
    }

    func revokeGameInvitation(id: String) async throws {
        try await api.deleteVoid("invitations", queryItems: [.init(name: "id", value: id)])
    }
}
