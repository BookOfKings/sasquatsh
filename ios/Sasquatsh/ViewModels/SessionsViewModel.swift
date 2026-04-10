import SwiftUI

@Observable
@MainActor
final class SessionsViewModel {
    var tables: [EventTable] = []
    var sessions: [GameSession] = []
    var isLoading = false
    var error: String?
    var isRegistering = false

    private var eventId = ""
    private var services: ServiceContainer?

    func configure(services: ServiceContainer, eventId: String) {
        self.services = services
        self.eventId = eventId
    }

    func loadSessions() async {
        guard let services, !eventId.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            let response = try await services.sessions.getSessions(eventId: eventId)
            tables = response.tables.sorted { $0.tableNumber < $1.tableNumber }
            sessions = response.sessions
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func registerForSession(_ sessionId: String) async {
        guard let services else { return }
        isRegistering = true
        error = nil
        do {
            try await services.sessions.registerForSession(sessionId: sessionId)
            await loadSessions()
        } catch {
            self.error = error.localizedDescription
        }
        isRegistering = false
    }

    func cancelRegistration(_ sessionId: String) async {
        guard let services else { return }
        error = nil
        do {
            try await services.sessions.cancelSessionRegistration(sessionId: sessionId)
            await loadSessions()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func sessionsForTable(_ tableId: String) -> [GameSession] {
        sessions.filter { $0.tableId == tableId }.sorted { $0.slotIndex < $1.slotIndex }
    }

    func hasConflict(session: GameSession) -> Bool {
        sessions.contains { s in
            s.id != session.id &&
            s.slotIndex == session.slotIndex &&
            s.isUserRegistered
        }
    }
}
