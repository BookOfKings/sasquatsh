import SwiftUI

@Observable
@MainActor
final class CreateEditEventViewModel {
    var title = ""
    var description = ""
    var gameTitle = ""
    var gameCategory: GameCategory?
    var eventDate = Date()
    var startTime = Date()
    var durationMinutes = 120
    var setupMinutes = 15
    var addressLine1 = ""
    var city = ""
    var state = ""
    var postalCode = ""
    var locationDetails = ""
    var eventLocationId: String?
    var venueHall: String?
    var venueRoom: String?
    var venueTable: String?
    var timezone: AppTimezone = .eastern
    var hostIsPlaying: Bool = true
    var selectedVenue: EventLocation?
    var useVenueMode: Bool = true
    var difficultyLevel: DifficultyLevel?
    var maxPlayers = 6
    var isPublic = true
    var isCharityEvent = false
    var minAge: Int?
    var status: EventStatus = .published
    var groupId: String?

    var selectedGames: [BggGame] = []
    var isFetchingGameDetails = false
    var availableGroups: [GroupSummary] = []

    var isLoading = false
    var error: String?
    var isEditing = false

    private var eventId: String?
    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadAvailableGroups() async {
        guard let services else { return }
        do {
            let groups = try await services.groups.getMyGroups()
            availableGroups = groups.filter { $0.userRole == .owner || $0.userRole == .admin }
        } catch {
            availableGroups = []
        }
    }

    func selectVenue(_ venue: EventLocation) {
        selectedVenue = venue
        eventLocationId = venue.id
        city = venue.city
        state = venue.state
        useVenueMode = true
        if let venueTz = venue.timezone, let appTz = AppTimezone(rawValue: venueTz) {
            timezone = appTz
        }
    }

    func clearVenue() {
        selectedVenue = nil
        eventLocationId = nil
        venueHall = nil
        venueRoom = nil
        venueTable = nil
    }

    func switchToCustomAddress() {
        clearVenue()
        useVenueMode = false
    }

    func addGame(from searchResult: BggSearchResult) async {
        guard let services else { return }
        isFetchingGameDetails = true
        do {
            let game = try await services.bgg.getGameDetails(bggId: searchResult.bggId)
            selectedGames.append(game)
            if selectedGames.count == 1 {
                gameTitle = game.name
            }
        } catch {
            self.error = "Failed to load game details"
        }
        isFetchingGameDetails = false
    }

    func removeGame(at index: Int) {
        guard selectedGames.indices.contains(index) else { return }
        selectedGames.remove(at: index)
        if selectedGames.isEmpty {
            gameTitle = ""
        } else {
            gameTitle = selectedGames[0].name
        }
    }

    func setPrimaryGame(at index: Int) {
        guard selectedGames.indices.contains(index), index != 0 else { return }
        let game = selectedGames.remove(at: index)
        selectedGames.insert(game, at: 0)
        gameTitle = game.name
    }

    func loadForEdit(event: Event) {
        isEditing = true
        eventId = event.id
        title = event.title
        description = event.description ?? ""
        gameTitle = event.gameTitle ?? ""
        gameCategory = event.gameCategory.flatMap { GameCategory(rawValue: $0) }
        eventDate = event.eventDate.toDate ?? Date()
        durationMinutes = event.durationMinutes ?? 60
        setupMinutes = event.setupMinutes ?? 0
        addressLine1 = event.addressLine1 ?? ""
        city = event.city ?? ""
        state = event.state ?? ""
        postalCode = event.postalCode ?? ""
        locationDetails = event.locationDetails ?? ""
        eventLocationId = event.eventLocationId
        venueHall = event.venueHall
        venueRoom = event.venueRoom
        venueTable = event.venueTable
        timezone = event.timezone.flatMap { AppTimezone(rawValue: $0) } ?? .eastern
        hostIsPlaying = event.hostIsPlaying ?? true
        useVenueMode = event.eventLocationId != nil
        difficultyLevel = event.difficultyLevel.flatMap { DifficultyLevel(rawValue: $0) }
        maxPlayers = event.maxPlayers ?? 8
        isPublic = event.isPublic
        isCharityEvent = event.isCharityEvent
        minAge = event.minAge
        status = EventStatus(rawValue: event.status) ?? .published

        if let st = event.startTime, let time = parseTimeString(st) {
            startTime = time
        }
    }

    func save() async -> Event? {
        guard let services else { return nil }
        isLoading = true
        error = nil

        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"

        do {
            if isEditing, let eventId {
                let input = UpdateEventInput(
                    title: title,
                    description: description.isEmpty ? nil : description,
                    gameTitle: gameTitle.isEmpty ? nil : gameTitle,
                    gameCategory: gameCategory?.rawValue,
                    eventDate: eventDate.apiDateString,
                    startTime: timeFormatter.string(from: startTime),
                    durationMinutes: durationMinutes,
                    setupMinutes: setupMinutes,
                    addressLine1: addressLine1.isEmpty ? nil : addressLine1,
                    city: city.isEmpty ? nil : city,
                    state: state.isEmpty ? nil : state,
                    postalCode: postalCode.isEmpty ? nil : postalCode,
                    locationDetails: locationDetails.isEmpty ? nil : locationDetails,
                    eventLocationId: eventLocationId,
                    venueHall: venueHall,
                    venueRoom: venueRoom,
                    venueTable: venueTable,
                    timezone: timezone.rawValue,
                    hostIsPlaying: hostIsPlaying,
                    difficultyLevel: difficultyLevel?.rawValue,
                    maxPlayers: maxPlayers,
                    isPublic: isPublic,
                    isCharityEvent: isCharityEvent,
                    minAge: minAge,
                    status: status.rawValue
                )
                let event = try await services.events.updateEvent(id: eventId, input: input)
                isLoading = false
                return event
            } else {
                let input = CreateEventInput(
                    title: title,
                    description: description.isEmpty ? nil : description,
                    gameTitle: gameTitle.isEmpty ? nil : gameTitle,
                    gameCategory: gameCategory?.rawValue,
                    eventDate: eventDate.apiDateString,
                    startTime: timeFormatter.string(from: startTime),
                    durationMinutes: durationMinutes,
                    setupMinutes: setupMinutes,
                    addressLine1: addressLine1.isEmpty ? nil : addressLine1,
                    city: city.isEmpty ? nil : city,
                    state: state.isEmpty ? nil : state,
                    postalCode: postalCode.isEmpty ? nil : postalCode,
                    locationDetails: locationDetails.isEmpty ? nil : locationDetails,
                    eventLocationId: eventLocationId,
                    venueHall: venueHall,
                    venueRoom: venueRoom,
                    venueTable: venueTable,
                    timezone: timezone.rawValue,
                    hostIsPlaying: hostIsPlaying,
                    difficultyLevel: difficultyLevel?.rawValue,
                    maxPlayers: maxPlayers,
                    isPublic: isPublic,
                    isCharityEvent: isCharityEvent,
                    minAge: minAge,
                    status: status.rawValue,
                    groupId: groupId
                )
                let event = try await services.events.createEvent(input: input)

                for (index, game) in selectedGames.enumerated() {
                    let gameInput = AddEventGameInput(
                        eventId: event.id,
                        bggId: game.bggId,
                        gameName: game.name,
                        thumbnailUrl: game.thumbnailUrl,
                        minPlayers: game.minPlayers,
                        maxPlayers: game.maxPlayers,
                        playingTime: game.playingTime,
                        isPrimary: index == 0,
                        isAlternative: index != 0
                    )
                    _ = try await services.events.addGame(input: gameInput)
                }

                isLoading = false
                return event
            }
        } catch {
            self.error = error.localizedDescription
            isLoading = false
            return nil
        }
    }

    var isValid: Bool {
        !title.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private func parseTimeString(_ time: String) -> Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.date(from: time)
    }
}
