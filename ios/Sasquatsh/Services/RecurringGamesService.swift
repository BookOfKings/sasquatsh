import Foundation

protocol RecurringGamesServiceProtocol {
    func getRecurringGames(groupId: String) async throws -> [RecurringGame]
    func createRecurringGame(input: CreateRecurringGameInput) async throws -> RecurringGame
    func updateRecurringGame(id: String, input: UpdateRecurringGameInput) async throws -> RecurringGame
    func deleteRecurringGame(id: String, deleteFutureEvents: Bool) async throws
}

final class RecurringGamesService: RecurringGamesServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getRecurringGames(groupId: String) async throws -> [RecurringGame] {
        let queryItems = [URLQueryItem(name: "groupId", value: groupId)]
        return try await api.get("recurring-games", queryItems: queryItems, authenticated: true)
    }

    func createRecurringGame(input: CreateRecurringGameInput) async throws -> RecurringGame {
        return try await api.post("recurring-games", body: input, authenticated: true)
    }

    func updateRecurringGame(id: String, input: UpdateRecurringGameInput) async throws -> RecurringGame {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        return try await api.put("recurring-games", body: input, queryItems: queryItems, authenticated: true)
    }

    func deleteRecurringGame(id: String, deleteFutureEvents: Bool) async throws {
        var queryItems = [URLQueryItem(name: "id", value: id)]
        if deleteFutureEvents {
            queryItems.append(URLQueryItem(name: "deleteFutureEvents", value: "true"))
        }
        try await api.deleteVoid("recurring-games", queryItems: queryItems, authenticated: true)
    }
}
