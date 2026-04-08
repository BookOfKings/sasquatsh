import SwiftUI

@Observable
@MainActor
final class RaffleViewModel {
    var raffle: Raffle?
    var isLoading = false
    var error: String?
    var mailInName = ""
    var mailInAddress = ""
    var isSubmittingMailIn = false
    var mailInSuccess = false

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadActiveRaffle() async {
        guard let services else { return }
        isLoading = true
        error = nil
        raffle = try? await services.raffle.getActiveRaffle()
        isLoading = false
    }

    func submitMailInEntry() async {
        guard let services, let raffle else { return }
        let name = mailInName.trimmingCharacters(in: .whitespacesAndNewlines)
        let address = mailInAddress.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty, !address.isEmpty else {
            error = "Name and address are required"
            return
        }
        isSubmittingMailIn = true
        error = nil
        do {
            try await services.raffle.submitMailInEntry(raffleId: raffle.id, name: name, address: address)
            mailInSuccess = true
            mailInName = ""
            mailInAddress = ""
            await loadActiveRaffle()
        } catch {
            self.error = error.localizedDescription
        }
        isSubmittingMailIn = false
    }

    var entriesByType: [(type: RaffleEntryType, count: Int)] {
        guard let entries = raffle?.userEntries else { return [] }
        var result: [(RaffleEntryType, Int)] = []
        for entryType in RaffleEntryType.allCases {
            let count = entries.filter { $0.entryType == entryType.rawValue }
                .reduce(0) { $0 + $1.entryCount }
            if count > 0 {
                result.append((entryType, count))
            }
        }
        return result
    }
}
