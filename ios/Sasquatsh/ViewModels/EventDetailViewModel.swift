import SwiftUI

@Observable
@MainActor
final class EventDetailViewModel {
    var event: Event?
    var isLoading = false
    var error: String?
    var actionMessage: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadEvent(id: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            event = try await services.events.getEvent(id: id)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func register() async {
        guard let services, let event else { return }
        error = nil
        do {
            try await services.events.registerForEvent(eventId: event.id)
            actionMessage = "Registered successfully!"
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func cancelRegistration() async {
        guard let services, let event else { return }
        error = nil
        do {
            try await services.events.cancelRegistration(eventId: event.id)
            actionMessage = "Registration cancelled"
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func claimItem(_ itemId: String) async {
        guard let services, let event else { return }
        error = nil
        do {
            try await services.events.claimItem(itemId: itemId)
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func unclaimItem(_ itemId: String) async {
        guard let services, let event else { return }
        error = nil
        do {
            try await services.events.unclaimItem(itemId: itemId)
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func addItem(name: String, category: String?) async {
        guard let services, let event else { return }
        error = nil
        do {
            let input = CreateEventItemInput(itemName: name, itemCategory: category)
            _ = try await services.events.addItem(eventId: event.id, input: input)
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func addGame(_ input: AddEventGameInput) async {
        guard let services, let event else { return }
        error = nil
        do {
            var gameInput = input
            gameInput.eventId = event.id
            _ = try await services.events.addGame(input: gameInput)
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func removeGame(_ gameId: String) async {
        guard let services, let event else { return }
        error = nil
        do {
            try await services.events.removeGame(gameId: gameId)
            await loadEvent(id: event.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deleteEvent() async -> Bool {
        guard let services, let event else { return false }
        error = nil
        do {
            try await services.events.deleteEvent(id: event.id)
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func isRegistered(userId: String) -> Bool {
        event?.registrations?.contains { $0.userId == userId } ?? false
    }

    func isHost(userId: String) -> Bool {
        event?.hostUserId == userId
    }
}
