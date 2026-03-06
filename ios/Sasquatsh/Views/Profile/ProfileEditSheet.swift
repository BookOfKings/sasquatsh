import SwiftUI

struct ProfileEditSheet: View {
    let profile: UserProfile
    @Bindable var vm: ProfileViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var displayName: String = ""
    @State private var username: String = ""
    @State private var bio: String = ""
    @State private var homeCity: String = ""
    @State private var homeState: String = ""
    @State private var homePostalCode: String = ""
    @State private var maxTravelMiles: String = ""
    @State private var favoriteGames: String = ""
    @State private var timezone: AppTimezone = .eastern
    @State private var activeCity: String = ""
    @State private var activeState: String = ""
    @State private var activeLocationExpires: Date = Date()
    @State private var hasActiveLocation: Bool = false
    @State private var activeEventLocationId: String?
    @State private var activeLocationHall: String = ""
    @State private var activeLocationRoom: String = ""
    @State private var activeLocationTable: String = ""
    @State private var selectedVenue: EventLocation?
    @State private var selectedGameTypes: Set<GameCategory> = []
    @State private var showVenueSelector = false
    @State private var isSaving = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Basic Info") {
                    TextField("Display Name", text: $displayName)
                    TextField("Username", text: $username)
                        .autocapitalization(.none)
                }

                Section("About") {
                    TextField("Bio", text: $bio, axis: .vertical)
                        .lineLimit(3...6)
                }

                Section("Location") {
                    TextField("City", text: $homeCity)
                    TextField("State", text: $homeState)
                    TextField("Postal Code", text: $homePostalCode)
                    TextField("Max Travel (miles)", text: $maxTravelMiles)
                        .keyboardType(.numberPad)
                }

                Section("Timezone") {
                    Picker("Timezone", selection: $timezone) {
                        ForEach(AppTimezone.allCases) { tz in
                            Text(tz.displayName).tag(tz)
                        }
                    }
                }

                Section("Active Location") {
                    Toggle("I'm traveling / at a convention", isOn: $hasActiveLocation)

                    if hasActiveLocation {
                        if let venue = selectedVenue {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(venue.name)
                                        .font(.md3BodyMedium)
                                    Text("\(venue.city), \(venue.state)")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                                Spacer()
                                Button {
                                    selectedVenue = nil
                                    activeEventLocationId = nil
                                    activeCity = ""
                                    activeState = ""
                                } label: {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        Button {
                            showVenueSelector = true
                        } label: {
                            Label("Choose a Venue", systemImage: "building.2")
                        }

                        TextField("City", text: $activeCity)
                        TextField("State", text: $activeState)

                        TextField("Hall", text: $activeLocationHall)
                        TextField("Room", text: $activeLocationRoom)
                        TextField("Table", text: $activeLocationTable)

                        DatePicker("Until", selection: $activeLocationExpires, in: Date()..., displayedComponents: .date)

                        Button(role: .destructive) {
                            clearActiveLocation()
                        } label: {
                            Label("Clear Active Location", systemImage: "xmark")
                        }
                    }
                }

                Section("Favorite Games (comma separated)") {
                    TextField("e.g. Catan, Wingspan, Gloomhaven", text: $favoriteGames, axis: .vertical)
                        .lineLimit(2...4)
                }

                Section("Preferred Game Types") {
                    FlowLayout(spacing: 8) {
                        ForEach(GameCategory.allCases) { cat in
                            Button {
                                if selectedGameTypes.contains(cat) {
                                    selectedGameTypes.remove(cat)
                                } else {
                                    selectedGameTypes.insert(cat)
                                }
                            } label: {
                                BadgeView(
                                    text: cat.displayName,
                                    color: selectedGameTypes.contains(cat) ? .md3PrimaryContainer : .md3SurfaceVariant
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 4)
                }

                if let error = vm.error {
                    Section {
                        Text(error)
                            .foregroundStyle(Color.md3Error)
                    }
                }
            }
            .navigationTitle("Edit Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task { await save() }
                    }
                    .disabled(isSaving)
                }
            }
            .onAppear {
                displayName = profile.displayName ?? ""
                username = profile.username
                bio = profile.bio ?? ""
                homeCity = profile.homeCity ?? ""
                homeState = profile.homeState ?? ""
                homePostalCode = profile.homePostalCode ?? ""
                maxTravelMiles = profile.maxTravelMiles.map { String($0) } ?? ""
                favoriteGames = profile.favoriteGames?.joined(separator: ", ") ?? ""

                if let tz = profile.timezone, let appTz = AppTimezone(rawValue: tz) {
                    timezone = appTz
                }

                if let city = profile.activeCity, !city.isEmpty {
                    hasActiveLocation = true
                    activeCity = city
                    activeState = profile.activeState ?? ""
                    activeEventLocationId = profile.activeEventLocationId
                    activeLocationHall = profile.activeLocationHall ?? ""
                    activeLocationRoom = profile.activeLocationRoom ?? ""
                    activeLocationTable = profile.activeLocationTable ?? ""
                    if let expiresStr = profile.activeLocationExpiresAt {
                        let formatter = ISO8601DateFormatter()
                        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                        if let date = formatter.date(from: expiresStr) {
                            activeLocationExpires = date
                        }
                    }
                }

                if let types = profile.preferredGameTypes {
                    selectedGameTypes = Set(types.compactMap { GameCategory(rawValue: $0) })
                }
            }
            .sheet(isPresented: $showVenueSelector) {
                VenueSelector { venue in
                    selectedVenue = venue
                    activeEventLocationId = venue.id
                    activeCity = venue.city
                    activeState = venue.state
                }
            }
        }
    }

    private func clearActiveLocation() {
        hasActiveLocation = false
        activeCity = ""
        activeState = ""
        activeEventLocationId = nil
        activeLocationHall = ""
        activeLocationRoom = ""
        activeLocationTable = ""
        activeLocationExpires = Date()
        selectedVenue = nil
    }

    private func save() async {
        isSaving = true
        let games = favoriteGames
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        let gameTypes = selectedGameTypes.isEmpty ? nil : selectedGameTypes.map(\.rawValue)

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]

        var input = UpdateProfileInput(
            username: username != profile.username ? username : nil,
            displayName: displayName.isEmpty ? nil : displayName,
            homeCity: homeCity.isEmpty ? nil : homeCity,
            homeState: homeState.isEmpty ? nil : homeState,
            homePostalCode: homePostalCode.isEmpty ? nil : homePostalCode,
            timezone: timezone.rawValue,
            bio: bio.isEmpty ? nil : bio,
            favoriteGames: games.isEmpty ? nil : games,
            preferredGameTypes: gameTypes
        )

        if let miles = Int(maxTravelMiles) {
            input.maxTravelMiles = miles
        }

        if hasActiveLocation {
            input.activeCity = activeCity.isEmpty ? nil : activeCity
            input.activeState = activeState.isEmpty ? nil : activeState
            input.activeEventLocationId = activeEventLocationId
            input.activeLocationHall = activeLocationHall.isEmpty ? nil : activeLocationHall
            input.activeLocationRoom = activeLocationRoom.isEmpty ? nil : activeLocationRoom
            input.activeLocationTable = activeLocationTable.isEmpty ? nil : activeLocationTable
            input.activeLocationExpiresAt = formatter.string(from: activeLocationExpires)
        }

        await vm.updateProfile(input: input)

        isSaving = false
        if vm.error == nil {
            dismiss()
        }
    }
}
