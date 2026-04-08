import Foundation

protocol RaffleServiceProtocol {
    func getActiveRaffle() async throws -> Raffle?
    func getRaffle(id: String) async throws -> Raffle
    func submitMailInEntry(raffleId: String, name: String, address: String) async throws
}

final class RaffleService: RaffleServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getActiveRaffle() async throws -> Raffle? {
        do {
            let raffle: Raffle = try await api.get("raffle", authenticated: true)
            return raffle
        } catch {
            return nil
        }
    }

    func getRaffle(id: String) async throws -> Raffle {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        return try await api.get("raffle", queryItems: queryItems, authenticated: true)
    }

    func submitMailInEntry(raffleId: String, name: String, address: String) async throws {
        let queryItems = [URLQueryItem(name: "action", value: "mail-in")]
        let input = MailInEntryInput(raffleId: raffleId, name: name, address: address)
        try await api.postVoid("raffle", body: input, queryItems: queryItems, authenticated: true)
    }
}
