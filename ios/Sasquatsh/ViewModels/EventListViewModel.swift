import SwiftUI

@Observable
@MainActor
final class EventListViewModel {
    var events: [EventSummary] = []
    var isLoading = false
    var error: String?
    var searchText = ""
    var selectedCategory: GameCategory?
    var selectedDifficulty: DifficultyLevel?
    var nearbyEnabled = false
    var radiusMiles = 25
    var userPostalCode: String?
    var filterCity: String?
    var filterState: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    var filter: EventSearchFilter {
        if nearbyEnabled, let zip = userPostalCode {
            return EventSearchFilter(
                search: searchText.isEmpty ? nil : searchText,
                gameCategory: selectedCategory,
                difficulty: selectedDifficulty,
                nearbyZip: zip,
                radiusMiles: radiusMiles
            )
        }
        return EventSearchFilter(
            city: filterCity?.isEmpty == true ? nil : filterCity,
            state: filterState?.isEmpty == true ? nil : filterState,
            search: searchText.isEmpty ? nil : searchText,
            gameCategory: selectedCategory,
            difficulty: selectedDifficulty
        )
    }

    func loadUserPostalCode() async {
        guard let services else { return }
        do {
            let profile = try await services.profile.getMyProfile()
            userPostalCode = profile.homePostalCode
        } catch {}
    }

    func loadEvents() async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            events = try await services.events.getPublicEvents(filter: filter)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func clearFilters() {
        searchText = ""
        selectedCategory = nil
        selectedDifficulty = nil
        nearbyEnabled = false
        filterCity = nil
        filterState = nil
    }

    var hasActiveFilters: Bool {
        selectedCategory != nil || selectedDifficulty != nil ||
        nearbyEnabled || filterCity != nil || filterState != nil
    }
}
