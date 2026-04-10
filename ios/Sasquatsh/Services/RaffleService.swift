import Foundation
import os.log

private let logger = Logger(subsystem: "com.sasquatsh.ios", category: "Raffle")

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
            let response: RaffleResponse = try await api.get("raffle", authenticated: true)
            if let raffle = response.raffle {
                logger.error("DEBUG Raffle loaded: \(raffle.prizeName) banner=\(raffle.bannerImageUrl ?? "nil") prize=\(raffle.prizeImageUrl ?? "nil")")
            }
            return response.raffle
        } catch {
            logger.error("Error loading active raffle: \(error.localizedDescription)")
            return nil
        }
    }

    func getRaffle(id: String) async throws -> Raffle {
        let queryItems = [URLQueryItem(name: "id", value: id)]
        let response: RaffleResponse = try await api.get("raffle", queryItems: queryItems, authenticated: true)
        return response.raffle!
    }

    func submitMailInEntry(raffleId: String, name: String, address: String) async throws {
        let queryItems = [URLQueryItem(name: "action", value: "mail-in")]
        let input = MailInEntryInput(raffleId: raffleId, name: name, address: address)
        try await api.postVoid("raffle", body: input, queryItems: queryItems, authenticated: true)
    }
}
