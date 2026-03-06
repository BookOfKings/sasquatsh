import SwiftUI

struct CreateEventView: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = CreateEditEventViewModel()
    @State private var showGameSearch = false
    @State private var showVenueSelector = false

    var groupId: String?

    var body: some View {
        NavigationStack {
            eventForm
                .navigationTitle("Create Game")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
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
                    }
                }
                .task {
                    vm.configure(services: services)
                    vm.groupId = groupId
                    await vm.loadAvailableGroups()
                }
                .sheet(isPresented: $showGameSearch) {
                    BGGGameSearchSheet { result in
                        Task {
                            await vm.addGame(from: result)
                        }
                    }
                }
                .sheet(isPresented: $showVenueSelector) {
                    VenueSelector { venue in
                        vm.selectVenue(venue)
                    }
                }
        }
    }

    private var eventForm: some View {
        Form {
            basicInfoSection
            dateTimeSection
            locationSection
            gameSettingsSection

            if let error = vm.error {
                Section {
                    Text(error).foregroundStyle(Color.md3Error)
                }
            }
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

            TextField("Title", text: $vm.title)

            Button {
                showGameSearch = true
            } label: {
                Label("Search BoardGameGeek", systemImage: "magnifyingglass")
            }

            if vm.isFetchingGameDetails {
                HStack {
                    ProgressView()
                        .tint(Color.md3Primary)
                    Text("Loading game details...")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }

            ForEach(Array(vm.selectedGames.enumerated()), id: \.element.id) { index, game in
                SelectedGameCard(
                    game: game,
                    isPrimary: index == 0,
                    onSetPrimary: { vm.setPrimaryGame(at: index) },
                    onRemove: { vm.removeGame(at: index) }
                )
                .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
            }

            TextField("Game Title", text: $vm.gameTitle)

            Picker("Category", selection: $vm.gameCategory) {
                Text("None").tag(GameCategory?.none)
                ForEach(GameCategory.allCases) { cat in
                    Text(cat.displayName).tag(GameCategory?.some(cat))
                }
            }

            TextField("Description", text: $vm.description, axis: .vertical)
                .lineLimit(3...6)
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
