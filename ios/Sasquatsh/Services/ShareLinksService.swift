import Foundation

protocol ShareLinksServiceProtocol: Sendable {
    func preview(code: String) async throws -> ShareLinkPreview
    func accept(code: String) async throws -> ShareLinkAcceptResponse
    func create(input: CreateShareLinkInput) async throws -> ShareLinkCreated
}

struct ShareLinkPreview: Codable {
    let linkType: String
    let group: ShareLinkGroup?
    let target: ShareLinkTarget?
    let invitedBy: ShareLinkInviter?
}

struct ShareLinkGroup: Codable {
    let id: String
    let name: String
    let slug: String
    let logoUrl: String?
}

struct ShareLinkTarget: Codable {
    let type: String
    let id: String
    let title: String
}

struct ShareLinkInviter: Codable {
    let displayName: String?
    let avatarUrl: String?
}

struct ShareLinkAcceptResponse: Codable {
    let alreadyUsed: Bool?
    let groupSlug: String?
    let target: ShareLinkTarget?
}

struct CreateShareLinkInput: Encodable {
    let groupId: String
    let linkType: String
    var planningSessionId: String?
    var eventId: String?
    var maxUses: Int?
    var expiresInDays: Int?
}

struct ShareLinkCreated: Decodable {
    let id: String
    let inviteCode: String?
    let linkType: String?
    let url: String?

    // snake_case fallback
    enum CodingKeys: String, CodingKey {
        case id, linkType, url
        case inviteCode = "inviteCode"
        case invite_code, link_type
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        inviteCode = (try? c.decode(String.self, forKey: .inviteCode)) ?? (try? c.decode(String.self, forKey: .invite_code))
        linkType = (try? c.decode(String.self, forKey: .linkType)) ?? (try? c.decode(String.self, forKey: .link_type))
        url = try? c.decode(String.self, forKey: .url)
    }
}

struct ShareLinksService: ShareLinksServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func preview(code: String) async throws -> ShareLinkPreview {
        try await api.get("share-links", queryItems: [
            .init(name: "code", value: code)
        ], authenticated: false)
    }

    func accept(code: String) async throws -> ShareLinkAcceptResponse {
        try await api.post("share-links", queryItems: [
            .init(name: "code", value: code),
            .init(name: "action", value: "accept")
        ])
    }

    func create(input: CreateShareLinkInput) async throws -> ShareLinkCreated {
        try await api.post("share-links", body: input, queryItems: [
            .init(name: "action", value: "create")
        ])
    }
}
