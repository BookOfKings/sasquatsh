import SwiftUI

@Observable
@MainActor
final class GroupListViewModel {
    var groups: [GroupSummary] = []
    var myGroupCount = 0
    var isLoading = false
    var error: String?
    var searchText = ""
    var selectedType: GroupType?
    var filterCity = ""
    var filterState = ""

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    var filter: GroupSearchFilter {
        GroupSearchFilter(
            search: searchText.isEmpty ? nil : searchText,
            groupType: selectedType,
            city: filterCity.isEmpty ? nil : filterCity,
            state: filterState.isEmpty ? nil : filterState
        )
    }

    var hasActiveFilters: Bool {
        selectedType != nil || !filterCity.isEmpty || !filterState.isEmpty
    }

    func loadGroups() async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            async let publicGroups = services.groups.getPublicGroups(filter: filter)
            async let myGroups = services.groups.getMyGroups()
            groups = try await publicGroups
            myGroupCount = (try? await myGroups.count) ?? 0
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func clearFilters() {
        searchText = ""
        selectedType = nil
        filterCity = ""
        filterState = ""
    }
}
