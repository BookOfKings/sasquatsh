import Foundation

protocol PlanningServiceProtocol: Sendable {
    func getGroupSessions(groupId: String) async throws -> [PlanningSession]
    func getMySessions() async throws -> [PlanningSession]
    func getSession(id: String) async throws -> PlanningSession
    func createSession(input: CreatePlanningSessionInput) async throws -> PlanningSession
    func submitResponse(sessionId: String, input: PlanningResponseInput) async throws
    func suggestGame(sessionId: String, input: SuggestGameInput) async throws -> GameSuggestion
    func voteForGame(sessionId: String, suggestionId: String) async throws
    func unvoteGame(sessionId: String, suggestionId: String) async throws
    func removeSuggestion(sessionId: String, suggestionId: String) async throws
    func finalizeSession(sessionId: String, selectedDateId: String?, selectedGameId: String?) async throws -> FinalizeResponse
    func cancelSession(sessionId: String) async throws
    func deleteSession(sessionId: String) async throws
    func addItem(sessionId: String, input: AddPlanningItemInput) async throws -> PlanningItem
    func claimItem(sessionId: String, itemId: String) async throws
    func unclaimItem(sessionId: String, itemId: String) async throws
    func removeItem(sessionId: String, itemId: String) async throws
    func addInvitees(sessionId: String, userIds: [String]) async throws
    func updateSettings(sessionId: String, tableCount: Int?) async throws
    func scheduleSessions(sessionId: String, schedule: [ScheduleEntry]) async throws
}

struct FinalizeResponse: Codable {
    let eventId: String
    let message: String
}

struct ScheduleEntry: Codable {
    let suggestionId: String
    let tableNumber: Int
    let slotIndex: Int
}

final class PlanningService: PlanningServiceProtocol {
    private let api: APIClient

    init(api: APIClient) {
        self.api = api
    }

    func getGroupSessions(groupId: String) async throws -> [PlanningSession] {
        try await api.get("planning", queryItems: [.init(name: "groupId", value: groupId)], authenticated: true)
    }

    func getMySessions() async throws -> [PlanningSession] {
        try await api.get("planning", queryItems: [.init(name: "mine", value: "true")], authenticated: true)
    }

    func getSession(id: String) async throws -> PlanningSession {
        try await api.get("planning", queryItems: [.init(name: "id", value: id)], authenticated: true)
    }

    func createSession(input: CreatePlanningSessionInput) async throws -> PlanningSession {
        try await api.post("planning", body: input)
    }

    func submitResponse(sessionId: String, input: PlanningResponseInput) async throws {
        try await api.postVoid("planning", body: input, queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "respond")
        ])
    }

    func suggestGame(sessionId: String, input: SuggestGameInput) async throws -> GameSuggestion {
        try await api.post("planning", body: input, queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "suggest-game")
        ])
    }

    func voteForGame(sessionId: String, suggestionId: String) async throws {
        try await api.postVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "vote-game"),
            .init(name: "suggestionId", value: suggestionId)
        ])
    }

    func unvoteGame(sessionId: String, suggestionId: String) async throws {
        try await api.postVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "unvote-game"),
            .init(name: "suggestionId", value: suggestionId)
        ])
    }

    func removeSuggestion(sessionId: String, suggestionId: String) async throws {
        try await api.postVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "remove-suggestion"),
            .init(name: "suggestionId", value: suggestionId)
        ])
    }

    func finalizeSession(sessionId: String, selectedDateId: String?, selectedGameId: String?) async throws -> FinalizeResponse {
        struct FinalizeInput: Codable {
            let selectedDateId: String?
            let selectedGameId: String?
        }
        return try await api.put("planning", body: FinalizeInput(selectedDateId: selectedDateId, selectedGameId: selectedGameId), queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "finalize")
        ])
    }

    func cancelSession(sessionId: String) async throws {
        try await api.putVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "cancel")
        ])
    }

    func deleteSession(sessionId: String) async throws {
        try await api.deleteVoid("planning", queryItems: [.init(name: "id", value: sessionId)])
    }

    func addItem(sessionId: String, input: AddPlanningItemInput) async throws -> PlanningItem {
        try await api.post("planning", body: input, queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "add-item")
        ])
    }

    func claimItem(sessionId: String, itemId: String) async throws {
        try await api.postVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "claim-item"),
            .init(name: "itemId", value: itemId)
        ])
    }

    func unclaimItem(sessionId: String, itemId: String) async throws {
        try await api.postVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "unclaim-item"),
            .init(name: "itemId", value: itemId)
        ])
    }

    func removeItem(sessionId: String, itemId: String) async throws {
        try await api.postVoid("planning", queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "remove-item"),
            .init(name: "itemId", value: itemId)
        ])
    }

    func addInvitees(sessionId: String, userIds: [String]) async throws {
        try await api.postVoid("planning", body: ["userIds": userIds], queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "add-invitees")
        ])
    }

    func updateSettings(sessionId: String, tableCount: Int?) async throws {
        struct Body: Encodable { let tableCount: Int? }
        let _: UpdateSettingsResponse = try await api.post("planning", body: Body(tableCount: tableCount), queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "update-settings")
        ])
    }

    func scheduleSessions(sessionId: String, schedule: [ScheduleEntry]) async throws {
        struct Body: Encodable { let schedule: [ScheduleEntry] }
        let _: ScheduleResponse = try await api.post("planning", body: Body(schedule: schedule), queryItems: [
            .init(name: "id", value: sessionId),
            .init(name: "action", value: "schedule-sessions")
        ])
    }
}

private struct UpdateSettingsResponse: Decodable {
    let message: String
    let tableCount: Int?
}

private struct ScheduleResponse: Decodable {
    let message: String
}
