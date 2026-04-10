import Foundation

protocol SessionsServiceProtocol {
    func getSessions(eventId: String) async throws -> EventSessionsResponse
    func registerForSession(sessionId: String) async throws
    func cancelSessionRegistration(sessionId: String) async throws
}

final class SessionsService: SessionsServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getSessions(eventId: String) async throws -> EventSessionsResponse {
        let queryItems = [URLQueryItem(name: "eventId", value: eventId)]
        return try await api.get("sessions", queryItems: queryItems, authenticated: true)
    }

    func registerForSession(sessionId: String) async throws {
        let queryItems = [
            URLQueryItem(name: "action", value: "register"),
            URLQueryItem(name: "sessionId", value: sessionId)
        ]
        try await api.postVoid("sessions", queryItems: queryItems, authenticated: true)
    }

    func cancelSessionRegistration(sessionId: String) async throws {
        let queryItems = [URLQueryItem(name: "sessionId", value: sessionId)]
        try await api.deleteVoid("sessions", queryItems: queryItems, authenticated: true)
    }
}
