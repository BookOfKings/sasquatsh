import SwiftUI

struct ProfileEditSheet: View {
    let profile: UserProfile
    @Bindable var vm: ProfileViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var displayName = ""
    @State private var username = ""
    @State private var bio = ""
    @State private var homeCity = ""
    @State private var homeState = ""
    @State private var homePostalCode = ""
    @State private var maxTravelMiles = ""
    @State private var favoriteGames = ""
    @State private var timezone: AppTimezone = .eastern
    @State private var activeCity = ""
    @State private var activeState = ""
    @State private var activeLocationExpires = Date()
    @State private var hasActiveLocation = false
    @State private var activeEventLocationId: String?
    @State private var activeLocationHall = ""
    @State private var activeLocationRoom = ""
    @State private var activeLocationTable = ""
    @State private var selectedVenue: EventLocation?
    @State private var selectedGameTypes: Set<GameCategory> = []
    @State private var showVenueSelector = false
    @State private var isSaving = false
    @State private var collectionVisibility = "private"
    @State private var birthYear = ""
    @State private var bggUsername = ""

    private var validationIssues: [String] {
        var issues: [String] = []
        if username.trimmingCharacters(in: .whitespaces).isEmpty {
            issues.append("Username is required")
        } else if username.count < 3 {
            issues.append("Username must be at least 3 characters")
        } else if username.contains(" ") {
            issues.append("Username cannot contain spaces")
        }
        if !birthYear.isEmpty {
            if let year = Int(birthYear) {
                let currentYear = Calendar.current.component(.year, from: Date())
                if year < 1900 || year > currentYear {
                    issues.append("Birth year must be between 1900 and \(currentYear)")
                }
            } else {
                issues.append("Birth year must be a number")
            }
        }
        if !maxTravelMiles.isEmpty, Int(maxTravelMiles) == nil {
            issues.append("Max travel must be a number")
        }
        return issues
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Basic Info"), footer: Text("Your display name is shown to other users. Username is used for @mentions and referral links.")) {
                    HStack {
                        Text("Display Name")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("Your name", text: $displayName)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Username")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("username", text: $username)
                            .autocapitalization(.none)
                            .autocorrectionDisabled()
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Birth Year")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("Optional", text: $birthYear)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }

                Section(header: Text("About You")) {
                    TextField("Tell others about yourself...", text: $bio, axis: .vertical)
                        .lineLimit(3...6)
                }

                Section(header: Text("Home Location"), footer: Text("Used to find games and groups near you.")) {
                    HStack {
                        Text("City")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("Your city", text: $homeCity)
                            .multilineTextAlignment(.trailing)
                    }
                    USStatePicker(selection: $homeState)
                    HStack {
                        Text("Postal Code")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("ZIP", text: $homePostalCode)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                    HStack {
                        Text("Max Travel")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("miles", text: $maxTravelMiles)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                    }
                }

                Section(header: Text("Timezone")) {
                    Picker("Timezone", selection: $timezone) {
                        ForEach(AppTimezone.allCases) { tz in
                            Text(tz.displayName).tag(tz)
                        }
                    }
                }

                Section(header: Text("BoardGameGeek"), footer: Text("Link your BGG account to import your game collection.")) {
                    HStack {
                        Text("BGG Username")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        TextField("Your BGG username", text: $bggUsername)
                            .autocapitalization(.none)
                            .autocorrectionDisabled()
                            .multilineTextAlignment(.trailing)
                    }
                }

                Section(header: Text("Active Location"), footer: Text("Set this when you're traveling or at a convention to find games nearby.")) {
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

                        HStack {
                            Text("City")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            TextField("City", text: $activeCity)
                                .multilineTextAlignment(.trailing)
                        }
                        USStatePicker(selection: $activeState)
                        HStack {
                            Text("Hall")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            TextField("Optional", text: $activeLocationHall)
                                .multilineTextAlignment(.trailing)
                        }
                        HStack {
                            Text("Room")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            TextField("Optional", text: $activeLocationRoom)
                                .multilineTextAlignment(.trailing)
                        }
                        HStack {
                            Text("Table")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            TextField("Optional", text: $activeLocationTable)
                                .multilineTextAlignment(.trailing)
                        }

                        DatePicker("Until", selection: $activeLocationExpires, in: Date()..., displayedComponents: .date)

                        Button(role: .destructive) {
                            clearActiveLocation()
                        } label: {
                            Label("Clear Active Location", systemImage: "xmark")
                        }
                    }
                }

                Section(header: Text("Game Collection")) {
                    Picker("Visibility", selection: $collectionVisibility) {
                        Text("Private").tag("private")
                        Text("Public").tag("public")
                    }
                }

                Section(header: Text("Favorite Games"), footer: Text("Separate multiple games with commas.")) {
                    TextField("e.g. Catan, Wingspan, Gloomhaven", text: $favoriteGames, axis: .vertical)
                        .lineLimit(2...4)
                }

                Section(header: Text("Preferred Game Types")) {
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

                if !validationIssues.isEmpty {
                    Section("Issues") {
                        ForEach(validationIssues, id: \.self) { issue in
                            HStack(spacing: 6) {
                                Image(systemName: "exclamationmark.circle.fill")
                                    .foregroundStyle(.orange)
                                Text(issue)
                                    .font(.md3BodySmall)
                                    .foregroundStyle(.orange)
                            }
                        }
                    }
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
                    .disabled(isSaving || !validationIssues.isEmpty)
                }
            }
            .onAppear { loadFromProfile() }
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

    private func loadFromProfile() {
        displayName = profile.displayName ?? ""
        username = profile.username
        bio = profile.bio ?? ""
        homeCity = profile.homeCity ?? ""
        homeState = profile.homeState ?? ""
        homePostalCode = profile.homePostalCode ?? ""
        maxTravelMiles = profile.maxTravelMiles.map { String($0) } ?? ""
        favoriteGames = profile.favoriteGames?.joined(separator: ", ") ?? ""
        bggUsername = profile.bggUsername ?? ""

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
        collectionVisibility = profile.collectionVisibility ?? "private"
        birthYear = profile.birthYear.map { String($0) } ?? ""
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
            preferredGameTypes: gameTypes,
            collectionVisibility: collectionVisibility,
            bggUsername: bggUsername.isEmpty ? nil : bggUsername
        )

        if let miles = Int(maxTravelMiles) {
            input.maxTravelMiles = miles
        }
        if let year = Int(birthYear), year > 1900 && year <= Calendar.current.component(.year, from: Date()) {
            input.birthYear = year
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
