import SwiftUI

struct CreateEventView: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = CreateEditEventViewModel()
    @State private var showVenueSelector = false
    @State private var showUpgradePrompt = false

    var groupId: String?

    var body: some View {
        NavigationStack {
            eventForm
                .navigationTitle("Create Game")
                .navigationBarTitleDisplayMode(.inline)
                .toolbarBackground(Color.md3Surface, for: .navigationBar)
                .toolbarBackground(.visible, for: .navigationBar)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                            .foregroundStyle(Color.md3OnSurface)
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Create") {
                            Task {
                                if let _ = await vm.save() {
                                    dismiss()
                                }
                            }
                        }
                        .disabled(!vm.isValid || vm.isLoading)
                        .foregroundStyle((!vm.isValid || vm.isLoading) ? Color.md3OnSurfaceVariant.opacity(0.3) : Color.md3Primary)
                    }
                }
                .task {
                    vm.configure(services: services)
                    vm.groupId = groupId
                    await vm.loadAvailableGroups()
                }
                .sheet(isPresented: $showVenueSelector) {
                    VenueSelector { venue in
                        vm.selectVenue(venue)
                    }
                }
                .sheet(isPresented: $showUpgradePrompt) {
                    UpgradePromptView(
                        limitType: .games,
                        currentTier: authVM.user?.effectiveTier ?? .free
                    )
                }
        }
    }

    private var eventForm: some View {
        Form {
            basicInfoSection
            gameSearchSection
            gameSystemConfigSections
            dateTimeSection
            locationSection
            gameSettingsSection

            if let error = vm.error {
                Section {
                    Text(error).foregroundStyle(Color.md3Error)
                }
            }
        }
        .onChange(of: vm.gameSystem) { _, _ in
            vm.gameSystemDidChange()
        }
    }

    @ViewBuilder
    private var gameSystemConfigSections: some View {
        switch vm.gameSystem {
        case .boardGame:
            EmptyView()
        case .mtg:
            MtgConfigFormSections(config: Binding(
                get: { vm.mtgConfig ?? MtgConfigState() },
                set: { vm.mtgConfig = $0 }
            ))
        case .pokemonTcg:
            PokemonConfigFormSections(config: Binding(
                get: { vm.pokemonConfig ?? PokemonConfigState() },
                set: { vm.pokemonConfig = $0 }
            ))
        case .yugioh:
            YugiohConfigFormSections(config: Binding(
                get: { vm.yugiohConfig ?? YugiohConfigState() },
                set: { vm.yugiohConfig = $0 }
            ))
        case .warhammer40k:
            Warhammer40kConfigFormSections(config: Binding(
                get: { vm.warhammer40kConfig ?? Warhammer40kConfigState() },
                set: { vm.warhammer40kConfig = $0 }
            ))
        }
    }

    // MARK: - Basic Information

    private var basicInfoSection: some View {
        Section("Basic Information") {
            if !vm.availableGroups.isEmpty {
                Picker("Host for Group", selection: $vm.groupId) {
                    Text("Personal Event").tag(String?.none)
                    ForEach(vm.availableGroups) { group in
                        Text(group.name).tag(String?.some(group.id))
                    }
                }
            }

            Picker("Game System", selection: $vm.gameSystem) {
                ForEach(GameSystem.allCases) { system in
                    Label(system.displayName, systemImage: system.iconName)
                        .tag(system)
                }
            }

            TextField("Title", text: $vm.title)

            TextField("Description", text: $vm.description, axis: .vertical)
                .lineLimit(3...6)
        }
    }

    // MARK: - Game Search

    @ViewBuilder
    private var gameSearchSection: some View {
        if vm.isBoardGame {
            Section("Game") {
                // Inline search field
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    TextField("Search BoardGameGeek...", text: $vm.gameTitle)
                        .onChange(of: vm.gameTitle) { _, newValue in
                            vm.searchBGG(query: newValue)
                        }
                    if vm.isSearchingBGG {
                    D20ProgressView(size: 32)
                            .controlSize(.small)
                            .tint(Color.md3Primary)
                    }
                    if !vm.gameTitle.isEmpty {
                        Button {
                            vm.gameTitle = ""
                            vm.clearBGGSearch()
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }

                // Search results
                if !vm.bggSearchResults.isEmpty {
                    ForEach(vm.bggSearchResults.prefix(8)) { result in
                        Button {
                            let tier = authVM.user?.effectiveTier ?? .free
                            if TierConfig.canAddGame(tier, currentCount: vm.selectedGames.count) {
                                Task { await vm.addGame(from: result) }
                                vm.clearBGGSearch()
                            } else {
                                showUpgradePrompt = true
                            }
                        } label: {
                            HStack(spacing: 10) {
                                // Thumbnail
                                if let urlStr = result.thumbnailUrl, let url = URL(string: urlStr) {
                                    AsyncImage(url: url) { image in
                                        image.resizable().aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        Color.md3SurfaceVariant
                                    }
                                    .frame(width: 44, height: 44)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                } else {
                                    RoundedRectangle(cornerRadius: MD3Shape.small)
                                        .fill(Color.md3SurfaceVariant)
                                        .frame(width: 44, height: 44)
                                        .overlay {
                                            Image(systemName: "dice")
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                        }
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(result.name)
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                        .lineLimit(1)
                                    if let year = result.yearPublished {
                                        Text("\(year)")
                                            .font(.md3BodySmall)
                                            .foregroundStyle(Color.md3OnSurfaceVariant)
                                    }
                                }

                                Spacer()

                                Image(systemName: "plus.circle.fill")
                                    .foregroundStyle(Color.md3Primary)
                            }
                        }
                    }
                }

                // Loading game details
                if vm.isFetchingGameDetails {
                    HStack {
                    D20ProgressView(size: 32)
                            .controlSize(.small)
                            .tint(Color.md3Primary)
                        Text("Loading game details...")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }

                // Selected games
                ForEach(Array(vm.selectedGames.enumerated()), id: \.element.id) { index, game in
                    SelectedGameCard(
                        game: game,
                        isPrimary: index == 0,
                        onSetPrimary: { vm.setPrimaryGame(at: index) },
                        onRemove: { vm.removeGame(at: index) }
                    )
                    .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                }

                Picker("Category", selection: $vm.gameCategory) {
                    Text("None").tag(GameCategory?.none)
                    ForEach(GameCategory.allCases) { cat in
                        Text(cat.displayName).tag(GameCategory?.some(cat))
                    }
                }
            }
        }
    }

    // MARK: - Date & Time

    private var dateTimeSection: some View {
        Section("Date & Time") {
            DatePicker("Date", selection: $vm.eventDate, displayedComponents: .date)
            DatePicker("Start Time", selection: $vm.startTime, displayedComponents: .hourAndMinute)
            Stepper("Duration: \(vm.durationMinutes) min", value: $vm.durationMinutes, in: 15...480, step: 15)
            Stepper("Setup: \(vm.setupMinutes) min", value: $vm.setupMinutes, in: 0...120, step: 5)

            Picker("Timezone", selection: $vm.timezone) {
                ForEach(AppTimezone.allCases) { tz in
                    Text(tz.displayName).tag(tz)
                }
            }
        }
    }

    // MARK: - Location

    private var locationSection: some View {
        Section("Location") {
            Picker("Location Type", selection: $vm.useVenueMode) {
                Text("Select Venue").tag(true)
                Text("Custom Address").tag(false)
            }
            .pickerStyle(.segmented)
            .onChange(of: vm.useVenueMode) { _, newValue in
                if !newValue {
                    vm.switchToCustomAddress()
                }
            }

            if vm.useVenueMode {
                HotLocationsBar(selectedId: vm.eventLocationId) { venue in
                    vm.selectVenue(venue)
                }

                Button {
                    showVenueSelector = true
                } label: {
                    if let venue = vm.selectedVenue {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(venue.name)
                                    .font(.md3BodyLarge)
                                    .foregroundStyle(Color.md3OnSurface)
                                Text("\(venue.city), \(venue.state)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    } else {
                        Label("Choose a Venue", systemImage: "building.2")
                    }
                }

                if vm.selectedVenue != nil {
                    let tier = authVM.user?.effectiveTier ?? .free
                    if TierConfig.hasFeature(tier, feature: \.tableInfo) {
                        TextField("Hall", text: Binding(
                            get: { vm.venueHall ?? "" },
                            set: { vm.venueHall = $0.isEmpty ? nil : $0 }
                        ))
                        TextField("Room", text: Binding(
                            get: { vm.venueRoom ?? "" },
                            set: { vm.venueRoom = $0.isEmpty ? nil : $0 }
                        ))
                        TextField("Table", text: Binding(
                            get: { vm.venueTable ?? "" },
                            set: { vm.venueTable = $0.isEmpty ? nil : $0 }
                        ))
                    } else {
                        HStack {
                            Image(systemName: "lock.fill")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("Hall/Room/Table — upgrade to unlock")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }

                TextField("Location Details", text: $vm.locationDetails)
            } else {
                TextField("Address", text: $vm.addressLine1)
                TextField("City", text: $vm.city)
                TextField("State", text: $vm.state)
                TextField("Postal Code", text: $vm.postalCode)
                TextField("Location Details", text: $vm.locationDetails)
            }
        }
    }

    // MARK: - Game Settings

    private var gameSettingsSection: some View {
        Section("Game Settings") {
            Stepper("Max Players: \(vm.maxPlayers)", value: $vm.maxPlayers, in: 2...100)

            Toggle("I Am Playing", isOn: $vm.hostIsPlaying)

            Text(vm.hostIsPlaying
                ? "\(vm.maxPlayers - 1) spots for others"
                : "\(vm.maxPlayers) spots (you're not playing)")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)

            HStack {
                Text("Min Age")
                Spacer()
                TextField("None", value: $vm.minAge, format: .number)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 80)
            }

            Picker("Difficulty", selection: $vm.difficultyLevel) {
                Text("None").tag(DifficultyLevel?.none)
                ForEach(DifficultyLevel.allCases) { level in
                    Text(level.displayName).tag(DifficultyLevel?.some(level))
                }
            }

            Picker("Status", selection: $vm.status) {
                ForEach(EventStatus.allCases) { status in
                    Text(status.displayName).tag(status)
                }
            }

            Toggle("Public Game", isOn: $vm.isPublic)
            Toggle("Charity Game", isOn: $vm.isCharityEvent)
        }
    }
}
