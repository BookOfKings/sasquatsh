import SwiftUI

@Observable
@MainActor
final class RecurringGamesViewModel {
    var games: [RecurringGame] = []
    var isLoading = false
    var error: String?
    var showForm = false
    var editingGame: RecurringGame?
    var showDeleteConfirm = false
    var deletingGame: RecurringGame?
    var deleteFutureEvents = false

    private var groupId = ""
    private var services: ServiceContainer?

    func configure(services: ServiceContainer, groupId: String) {
        self.services = services
        self.groupId = groupId
    }

    func loadGames() async {
        guard let services, !groupId.isEmpty else { return }
        isLoading = true
        error = nil
        do {
            games = try await services.recurringGames.getRecurringGames(groupId: groupId)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func createGame(input: CreateRecurringGameInput) async {
        guard let services else { return }
        error = nil
        do {
            let game = try await services.recurringGames.createRecurringGame(input: input)
            games.append(game)
            showForm = false
        } catch {
            self.error = error.localizedDescription
        }
    }

    func updateGame(id: String, input: UpdateRecurringGameInput) async {
        guard let services else { return }
        error = nil
        do {
            let updated = try await services.recurringGames.updateRecurringGame(id: id, input: input)
            if let index = games.firstIndex(where: { $0.id == id }) {
                games[index] = updated
            }
            editingGame = nil
            showForm = false
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deleteGame() async {
        guard let services, let game = deletingGame else { return }
        error = nil
        do {
            try await services.recurringGames.deleteRecurringGame(id: game.id, deleteFutureEvents: deleteFutureEvents)
            games.removeAll { $0.id == game.id }
            deletingGame = nil
            deleteFutureEvents = false
        } catch {
            self.error = error.localizedDescription
        }
    }

    func toggleActive(game: RecurringGame) async {
        let input = UpdateRecurringGameInput(isActive: !game.isActive)
        await updateGame(id: game.id, input: input)
    }
}
