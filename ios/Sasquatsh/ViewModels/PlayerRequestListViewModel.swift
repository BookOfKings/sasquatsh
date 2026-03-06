import SwiftUI

@Observable
@MainActor
final class PlayerRequestListViewModel {
    var requests: [PlayerRequest] = []
    var myRequests: [PlayerRequest] = []
    var hostedEvents: [EventSummary] = []
    var isLoading = false
    var error: String?
    var actionMessage: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadRequests(filters: PlayerRequestFilters = PlayerRequestFilters()) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            requests = try await services.social.getPlayerRequests(filters: filters)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadMyRequests() async {
        guard let services else { return }
        do {
            myRequests = try await services.social.getMyPlayerRequests()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func loadHostedEvents() async {
        guard let services else { return }
        do {
            hostedEvents = try await services.events.getHostedEvents()
        } catch {
            // Non-critical, ignore
        }
    }

    func createRequest(eventId: String, description: String?, playerCount: Int) async {
        guard let services else { return }
        error = nil
        let input = CreatePlayerRequestInput(
            eventId: eventId,
            description: description,
            playerCountNeeded: playerCount
        )
        do {
            let newRequest = try await services.social.createPlayerRequest(input: input)
            myRequests.insert(newRequest, at: 0)
            requests.insert(newRequest, at: 0)
            actionMessage = "Request posted! Expires in 15 minutes."
        } catch {
            self.error = error.localizedDescription
        }
    }

    func fillRequest(id: String) async {
        guard let services else { return }
        do {
            let updated = try await services.social.fillPlayerRequest(id: id)
            if let idx = myRequests.firstIndex(where: { $0.id == id }) {
                myRequests[idx] = updated
            }
            requests.removeAll { $0.id == id }
            actionMessage = "Marked as filled!"
        } catch {
            self.error = error.localizedDescription
        }
    }

    func cancelRequest(id: String) async {
        guard let services else { return }
        do {
            let updated = try await services.social.cancelPlayerRequest(id: id)
            if let idx = myRequests.firstIndex(where: { $0.id == id }) {
                myRequests[idx] = updated
            }
            requests.removeAll { $0.id == id }
            actionMessage = "Request cancelled"
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deleteRequest(id: String) async {
        guard let services else { return }
        do {
            try await services.social.deletePlayerRequest(id: id)
            requests.removeAll { $0.id == id }
            myRequests.removeAll { $0.id == id }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
