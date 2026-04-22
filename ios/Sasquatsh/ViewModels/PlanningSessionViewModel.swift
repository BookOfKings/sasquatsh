import SwiftUI

@Observable
@MainActor
final class PlanningSessionViewModel {
    var session: PlanningSession?
    var isLoading = false
    var error: String?
    var actionMessage: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadSession(id: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            session = try await services.planning.getSession(id: id)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func submitResponse(cannotAttendAny: Bool, dateAvailability: [DateAvailabilityInput]) async {
        guard let services, let session else { return }
        error = nil
        do {
            let input = PlanningResponseInput(cannotAttendAny: cannotAttendAny, dateAvailability: dateAvailability)
            try await services.planning.submitResponse(sessionId: session.id, input: input)
            actionMessage = "Response submitted!"
            await loadSession(id: session.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func suggestGame(_ input: SuggestGameInput) async {
        guard let services, let session else { return }
        error = nil
        do {
            let suggestion = try await services.planning.suggestGame(sessionId: session.id, input: input)
            self.session?.gameSuggestions?.append(suggestion)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func voteForGame(suggestionId: String) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.voteForGame(sessionId: session.id, suggestionId: suggestionId)
            await loadSession(id: session.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func unvoteGame(suggestionId: String) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.unvoteGame(sessionId: session.id, suggestionId: suggestionId)
            await loadSession(id: session.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func removeSuggestion(suggestionId: String) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.removeSuggestion(sessionId: session.id, suggestionId: suggestionId)
            self.session?.gameSuggestions?.removeAll { $0.id == suggestionId }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func finalize(selectedDateId: String?, selectedGameId: String?) async -> String? {
        guard let services, let session else { return nil }
        error = nil
        do {
            let result = try await services.planning.finalizeSession(sessionId: session.id, selectedDateId: selectedDateId, selectedGameId: selectedGameId)
            actionMessage = result.message
            return result.eventId
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    func addItem(name: String, category: ItemCategory, quantity: Int?) async {
        guard let services, let session else { return }
        error = nil
        do {
            let input = AddPlanningItemInput(itemName: name, itemCategory: category, quantityNeeded: quantity)
            let item = try await services.planning.addItem(sessionId: session.id, input: input)
            if self.session?.items == nil {
                self.session?.items = []
            }
            self.session?.items?.append(item)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func claimItem(itemId: String) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.claimItem(sessionId: session.id, itemId: itemId)
            await loadSession(id: session.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func unclaimItem(itemId: String) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.unclaimItem(sessionId: session.id, itemId: itemId)
            await loadSession(id: session.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func removeItem(itemId: String) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.removeItem(sessionId: session.id, itemId: itemId)
            self.session?.items?.removeAll { $0.id == itemId }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func updateSettings(tableCount: Int?) async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.updateSettings(sessionId: session.id, tableCount: tableCount)
            self.session?.tableCount = tableCount
            actionMessage = tableCount != nil && tableCount! >= 2
                ? "Multi-table enabled with \(tableCount!) tables"
                : "Multi-table disabled"
        } catch {
            self.error = error.localizedDescription
        }
    }

    func cancel() async {
        guard let services, let session else { return }
        error = nil
        do {
            try await services.planning.cancelSession(sessionId: session.id)
            actionMessage = "Session cancelled"
            await loadSession(id: session.id)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
