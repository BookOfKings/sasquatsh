import SwiftUI

struct EditEventView: View {
    let event: Event
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = CreateEditEventViewModel()
    @State private var showVenueSelector = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Basic Information") {
                    Picker("Game System", selection: $vm.gameSystem) {
                        ForEach(GameSystem.allCases) { system in
                            Label(system.displayName, systemImage: system.iconName)
                                .tag(system)
                        }
                    }

                    TextField("Title", text: $vm.title)
                    TextField("Description", text: $vm.description, axis: .vertical)
                        .lineLimit(3...6)

                    if vm.isBoardGame {
                        Picker("Category", selection: $vm.gameCategory) {
                            Text("None").tag(GameCategory?.none)
                            ForEach(GameCategory.allCases) { cat in
                                Text(cat.displayName).tag(GameCategory?.some(cat))
                            }
                        }
                    }
                }

                editGameSystemConfigSections

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
                        USStatePicker(selection: $vm.state)
                        TextField("Postal Code", text: $vm.postalCode)
                        TextField("Location Details", text: $vm.locationDetails)
                    }
                }

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

                if let error = vm.error {
                    Section {
                        Text(error).foregroundStyle(Color.md3Error)
                    }
                }
            }
            .navigationTitle("Edit Game")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task {
                            if let _ = await vm.save() {
                                dismiss()
                            }
                        }
                    }
                    .disabled(!vm.isValid || vm.isLoading)
                }
            }
            .sheet(isPresented: $showVenueSelector) {
                VenueSelector { venue in
                    vm.selectVenue(venue)
                }
            }
            .task {
                vm.configure(services: services)
                vm.loadForEdit(event: event)
            }
        }
    }

    @ViewBuilder
    private var editGameSystemConfigSections: some View {
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
}
