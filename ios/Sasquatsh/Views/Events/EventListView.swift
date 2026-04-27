import SwiftUI

struct EventListView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = EventListViewModel()
    @State private var showCreateEvent = false
    @State private var showFilters = false
    @State private var showUpgradePrompt = false
    @State private var hostedEventCount = 0
    @State private var newBadges: [UserBadge] = []
    @State private var showBadgePopup = false

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // Search
                SearchBarView(text: $vm.searchText, placeholder: "Search games...")
                    .padding(.horizontal)
                    .onChange(of: vm.searchText) { _, _ in
                        Task { await vm.loadEvents() }
                    }

                // Filter chips
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Button {
                            let tier = authVM.user?.effectiveTier ?? .free
                            if TierConfig.canHostEvent(tier, currentCount: hostedEventCount) {
                                showCreateEvent = true
                            } else {
                                showUpgradePrompt = true
                            }
                        } label: {
                            Label("Host a Game", systemImage: "plus")
                                .font(.md3LabelMedium)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.md3Primary)
                                .foregroundStyle(Color.md3OnPrimary)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                        }

                        filterButton

                        if let system = vm.selectedGameSystem {
                            BadgeView(text: system.shortName, color: system.badgeColor)
                        }

                        if vm.nearbyEnabled {
                            BadgeView(text: "Nearby \(vm.radiusMiles)mi", color: Color.md3PrimaryContainer)
                        }

                        if let city = vm.filterCity, !city.isEmpty {
                            BadgeView(text: city, color: Color.md3PrimaryContainer)
                        }

                        if let state = vm.filterState, !state.isEmpty {
                            BadgeView(text: state, color: Color.md3PrimaryContainer)
                        }

                        if vm.hasActiveFilters {
                            Button {
                                vm.clearFilters()
                                Task { await vm.loadEvents() }
                            } label: {
                                Label("Clear", systemImage: "xmark")
                                    .font(.md3LabelMedium)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.md3SurfaceContainerHigh)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: MD3Shape.small)
                                            .stroke(Color.md3OutlineVariant, lineWidth: 1)
                                    )
                            }
                        }
                    }
                    .padding(.horizontal)
                }

                if let error = vm.error {
                    ErrorBannerView(message: error) { vm.error = nil }
                }

                AdBannerView(placement: "events")

                if vm.isLoading && vm.events.isEmpty {
                    LoadingView()
                } else if vm.events.isEmpty {
                    EmptyStateView(
                        icon: "dice",
                        title: "No Games Found",
                        message: "No game nights scheduled yet",
                        buttonTitle: "Create Game",
                        action: { showCreateEvent = true }
                    )
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(vm.events) { event in
                            NavigationLink(value: event.id) {
                                EventCard(event: event)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding(.vertical)
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .navigationDestination(for: String.self) { eventId in
            EventDetailView(eventId: eventId)
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showCreateEvent = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(Color.md3Primary)
                }
            }
        }
        .sheet(isPresented: $showCreateEvent, onDismiss: {
            Task {
                await vm.loadEvents()
                await loadHostedCount()
                await computeBadges()
            }
        }) {
            CreateEventView()
        }
        .sheet(isPresented: $showBadgePopup) {
            BadgeEarnedPopup(badges: newBadges)
                .presentationDetents([.medium])
        }
        .sheet(isPresented: $showUpgradePrompt) {
            UpgradePromptView(limitType: .games, currentTier: authVM.user?.effectiveTier ?? .free)
        }
        .sheet(isPresented: $showFilters) {
            eventFilterSheet
        }
        .refreshable { await vm.loadEvents() }
        .task {
            vm.configure(services: services)
            await vm.loadUserPostalCode()
            await vm.loadEvents()
            await loadHostedCount()
        }
    }

    private func computeBadges() async {
        do {
            let response = try await services.badges.computeBadges()
            if let earned = response.newlyEarned, earned > 0 {
                newBadges = Array(response.badges.prefix(earned))
                showBadgePopup = true
            }
        } catch {}
    }

    private func loadHostedCount() async {
        let hosted = (try? await services.events.getHostedEvents()) ?? []
        let startOfToday = Calendar.current.startOfDay(for: Date())
        hostedEventCount = hosted.filter { $0.eventDate.toDate ?? .distantPast >= startOfToday }.count
    }

    private var filterButton: some View {
        Button {
            showFilters = true
        } label: {
            Label("Filters", systemImage: "line.3.horizontal.decrease.circle")
                .font(.md3LabelMedium)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.md3SurfaceContainerHigh)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                .overlay(
                    RoundedRectangle(cornerRadius: MD3Shape.small)
                        .stroke(Color.md3OutlineVariant, lineWidth: 1)
                )
        }
    }

    private var eventFilterSheet: some View {
        NavigationStack {
            Form {
                Section("Game System") {
                    Picker("Game System", selection: $vm.selectedGameSystem) {
                        Text("Any").tag(GameSystem?.none)
                        ForEach(GameSystem.allCases) { system in
                            Label(system.displayName, systemImage: system.iconName)
                                .tag(GameSystem?.some(system))
                        }
                    }
                }

                if vm.selectedGameSystem == nil || vm.selectedGameSystem == .boardGame {
                    Section("Category") {
                        Picker("Game Category", selection: $vm.selectedCategory) {
                            Text("Any").tag(GameCategory?.none)
                            ForEach(GameCategory.allCases) { cat in
                                Text(cat.displayName).tag(GameCategory?.some(cat))
                            }
                        }
                    }
                }

                Section("Difficulty") {
                    Picker("Difficulty", selection: $vm.selectedDifficulty) {
                        Text("Any").tag(DifficultyLevel?.none)
                        ForEach(DifficultyLevel.allCases) { level in
                            Text(level.displayName).tag(DifficultyLevel?.some(level))
                        }
                    }
                }

                Section("Location") {
                    TextField("City", text: Binding(
                        get: { vm.filterCity ?? "" },
                        set: { vm.filterCity = $0.isEmpty ? nil : $0 }
                    ))
                    .disabled(vm.nearbyEnabled)

                    USStatePicker(selection: Binding(
                        get: { vm.filterState ?? "" },
                        set: { vm.filterState = $0.isEmpty ? nil : $0 }
                    ))
                    .disabled(vm.nearbyEnabled)
                }

                if vm.userPostalCode != nil {
                    Section("Nearby") {
                        Toggle("Search Nearby", isOn: $vm.nearbyEnabled)

                        if vm.nearbyEnabled {
                            Picker("Radius", selection: $vm.radiusMiles) {
                                Text("10 mi").tag(10)
                                Text("25 mi").tag(25)
                                Text("50 mi").tag(50)
                                Text("100 mi").tag(100)
                            }
                            .pickerStyle(.segmented)
                        }
                    }
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Apply") {
                        showFilters = false
                        Task { await vm.loadEvents() }
                    }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showFilters = false }
                }
            }
        }
        .presentationDetents([.large])
    }
}
