import Foundation

protocol FirstPlayerServiceProtocol {
    func getRandomStatement() async throws -> FirstPlayerStatement
}

struct FirstPlayerStatement: Codable {
    let id: Int
    let statement: String
}

struct FirstPlayerStatementResponse: Codable {
    let statement: FirstPlayerStatement
}

struct FirstPlayerService: FirstPlayerServiceProtocol {
    private let client: APIClient

    init(client: APIClient) {
        self.client = client
    }

    func getRandomStatement() async throws -> FirstPlayerStatement {
        let response: FirstPlayerStatementResponse = try await client.get("first-player/random")
        return response.statement
    }
}
