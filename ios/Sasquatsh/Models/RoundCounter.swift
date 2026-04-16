import Foundation

struct RoundCounterState: Codable, Identifiable {
    var id: UUID
    var sessionId: UUID
    var roundNumber: Int
    var turnNumber: Int?
    var phaseKey: String?
    var activePlayerId: UUID?
    var minimumRound: Int
    var startingRound: Int
    var createdAt: Date
    var updatedAt: Date

    static func new(startingRound: Int = 1, minimumRound: Int = 1) -> RoundCounterState {
        let now = Date()
        return RoundCounterState(
            id: UUID(),
            sessionId: UUID(),
            roundNumber: startingRound,
            minimumRound: minimumRound,
            startingRound: startingRound,
            createdAt: now,
            updatedAt: now
        )
    }
}

struct RoundCounterEvent: Codable, Identifiable {
    let id: UUID
    let sessionId: UUID
    let roundNumber: Int
    let turnNumber: Int?
    let phaseKey: String?
    let eventType: String
    let createdAt: Date

    static func log(state: RoundCounterState, eventType: String) -> RoundCounterEvent {
        RoundCounterEvent(
            id: UUID(),
            sessionId: state.sessionId,
            roundNumber: state.roundNumber,
            turnNumber: state.turnNumber,
            phaseKey: state.phaseKey,
            eventType: eventType,
            createdAt: Date()
        )
    }
}
