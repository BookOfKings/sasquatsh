import Foundation

protocol GroupsServiceProtocol: Sendable {
    func getPublicGroups(filter: GroupSearchFilter) async throws -> [GroupSummary]
    func getMyGroups() async throws -> [GroupSummary]
    func getGroup(id: String) async throws -> GameGroup
    func getGroupBySlug(_ slug: String) async throws -> GameGroup
    func createGroup(input: CreateGroupInput) async throws -> GameGroup
    func updateGroup(id: String, input: UpdateGroupInput) async throws -> GameGroup
    func deleteGroup(id: String) async throws
    func joinGroup(id: String) async throws
    func leaveGroup(id: String) async throws
    func requestToJoin(groupId: String, message: String?) async throws
    func getMembers(groupId: String) async throws -> [GroupMember]
    func removeMember(groupId: String, userId: String) async throws
    func changeRole(groupId: String, userId: String, role: MemberRole) async throws
    func transferOwnership(groupId: String, newOwnerId: String) async throws
    func getJoinRequests(groupId: String) async throws -> [JoinRequest]
    func approveRequest(groupId: String, userId: String) async throws
    func rejectRequest(groupId: String, userId: String) async throws
    func createInvitation(groupId: String, input: CreateInvitationInput?) async throws -> GroupInvitation
    func getInvitations(groupId: String) async throws -> [GroupInvitation]
    func revokeInvitation(groupId: String, inviteId: String) async throws
    func getMyPendingInvitations() async throws -> [PendingGroupInvitation]
    func respondToInvitation(invitationId: String, accept: Bool) async throws
    func previewInvite(code: String) async throws -> InvitationPreview
    func acceptInvite(code: String) async throws -> AcceptInviteResponse
}

struct AcceptInviteResponse: Codable {
    let message: String
    let groupId: String
    let groupName: String
}

final class GroupsService: GroupsServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getPublicGroups(filter: GroupSearchFilter = GroupSearchFilter()) async throws -> [GroupSummary] {
        try await api.get("groups", queryItems: filter.queryItems, authenticated: true)
    }

    func getMyGroups() async throws -> [GroupSummary] {
        try await api.get("groups", queryItems: [.init(name: "mine", value: "true")], authenticated: true)
    }

    func getGroup(id: String) async throws -> GameGroup {
        try await api.get("groups", queryItems: [.init(name: "id", value: id)], authenticated: true)
    }

    func getGroupBySlug(_ slug: String) async throws -> GameGroup {
        try await api.get("groups", queryItems: [.init(name: "slug", value: slug)], authenticated: true)
    }

    func createGroup(input: CreateGroupInput) async throws -> GameGroup {
        try await api.post("groups", body: input)
    }

    func updateGroup(id: String, input: UpdateGroupInput) async throws -> GameGroup {
        try await api.put("groups", body: input, queryItems: [.init(name: "id", value: id)])
    }

    func deleteGroup(id: String) async throws {
        try await api.deleteVoid("groups", queryItems: [.init(name: "id", value: id)])
    }

    func joinGroup(id: String) async throws {
        try await api.postVoid("groups", queryItems: [
            .init(name: "id", value: id),
            .init(name: "action", value: "join")
        ])
    }

    func leaveGroup(id: String) async throws {
        try await api.deleteVoid("groups", queryItems: [
            .init(name: "id", value: id),
            .init(name: "action", value: "leave")
        ])
    }

    func requestToJoin(groupId: String, message: String?) async throws {
        let body: [String: String]? = message.map { ["message": $0] }
        try await api.postVoid("groups", body: body, queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "request")
        ])
    }

    func getMembers(groupId: String) async throws -> [GroupMember] {
        try await api.get("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "include", value: "members")
        ], authenticated: true)
    }

    func removeMember(groupId: String, userId: String) async throws {
        try await api.deleteVoid("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "remove"),
            .init(name: "userId", value: userId)
        ])
    }

    func changeRole(groupId: String, userId: String, role: MemberRole) async throws {
        let body = ["role": role.rawValue]
        try await api.putVoid("groups", body: body, queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "role"),
            .init(name: "userId", value: userId)
        ])
    }

    func transferOwnership(groupId: String, newOwnerId: String) async throws {
        try await api.putVoid("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "transfer"),
            .init(name: "userId", value: newOwnerId)
        ])
    }

    func getJoinRequests(groupId: String) async throws -> [JoinRequest] {
        try await api.get("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "include", value: "requests")
        ], authenticated: true)
    }

    func approveRequest(groupId: String, userId: String) async throws {
        try await api.postVoid("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "approve"),
            .init(name: "userId", value: userId)
        ])
    }

    func rejectRequest(groupId: String, userId: String) async throws {
        try await api.postVoid("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "reject"),
            .init(name: "userId", value: userId)
        ])
    }

    func createInvitation(groupId: String, input: CreateInvitationInput?) async throws -> GroupInvitation {
        try await api.post("groups", body: input, queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "invite")
        ])
    }

    func getInvitations(groupId: String) async throws -> [GroupInvitation] {
        try await api.get("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "include", value: "invitations")
        ], authenticated: true)
    }

    func revokeInvitation(groupId: String, inviteId: String) async throws {
        try await api.deleteVoid("groups", queryItems: [
            .init(name: "id", value: groupId),
            .init(name: "action", value: "revoke-invite"),
            .init(name: "inviteId", value: inviteId)
        ])
    }

    func getMyPendingInvitations() async throws -> [PendingGroupInvitation] {
        try await api.get("groups", queryItems: [
            .init(name: "action", value: "my-invitations")
        ], authenticated: true)
    }

    func respondToInvitation(invitationId: String, accept: Bool) async throws {
        struct Response: Decodable { let message: String }
        let _: Response = try await api.post("groups", body: ["response": accept ? "accept" : "decline"], queryItems: [
            .init(name: "action", value: "respond-invite"),
            .init(name: "inviteId", value: invitationId)
        ])
    }

    func previewInvite(code: String) async throws -> InvitationPreview {
        try await api.get("groups", queryItems: [
            .init(name: "action", value: "preview-invite"),
            .init(name: "code", value: code)
        ])
    }

    func acceptInvite(code: String) async throws -> AcceptInviteResponse {
        try await api.post("groups", queryItems: [
            .init(name: "action", value: "accept-invite"),
            .init(name: "code", value: code)
        ])
    }
}
