import SwiftUI

@Observable
@MainActor
final class CreatePlanningViewModel {
    var title = ""
    var description = ""
    var responseDeadline = Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    var selectedMemberIds: Set<String> = []
    var proposedDates: [Date] = []

    var isLoading = false
    var error: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func addDate(_ date: Date) {
        if !proposedDates.contains(where: { Calendar.current.isDate($0, inSameDayAs: date) }) {
            proposedDates.append(date)
            proposedDates.sort()
        }
    }

    func removeDate(_ date: Date) {
        proposedDates.removeAll { Calendar.current.isDate($0, inSameDayAs: date) }
    }

    func save(groupId: String) async -> PlanningSession? {
        guard let services else { return nil }
        isLoading = true
        error = nil

        let dates = proposedDates.map {
            ProposedDateInput(date: $0.apiDateString)
        }

        let input = CreatePlanningSessionInput(
            groupId: groupId,
            title: title,
            description: description.isEmpty ? nil : description,
            responseDeadline: responseDeadline.apiDateString,
            inviteeUserIds: Array(selectedMemberIds),
            proposedDates: dates
        )

        do {
            let session = try await services.planning.createSession(input: input)
            isLoading = false
            return session
        } catch {
            self.error = error.localizedDescription
            isLoading = false
            return nil
        }
    }

    var isValid: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty &&
        !selectedMemberIds.isEmpty &&
        !proposedDates.isEmpty
    }
}
