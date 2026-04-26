import SwiftUI
import EventKit

struct EventDetailView: View {
    let eventId: String
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @Environment(\.dismiss) private var dismiss
    @State private var vm = EventDetailViewModel()
    @State private var showEditEvent = false
    @State private var showAddItem = false
    @State private var showBGGSearch = false
    @State private var showShareSheet = false
    @State private var showDeleteConfirm = false
    @State private var newItemName = ""
    @State private var newItemCategory = "other"
    @State private var showUpgradePrompt = false
    @State private var upgradePromptType: LimitType = .games
    @State private var showItemsUpgradePrompt = false
    @State private var calendarMessage: String?
    @State private var showCalendarAlert = false

    /// Host's effective tier — participants inherit the host's features (matches website)
    private var hostTier: SubscriptionTier {
        vm.event?.host?.effectiveTier ?? .free
    }

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.event == nil {
                LoadingView()
            } else if let event = vm.event {
                VStack(alignment: .leading, spacing: 20) {
                    // 1. Header (title, host, date/time, location, players)
                    headerSection(event)

                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    }

                    if let msg = vm.actionMessage {
                        Text(msg)
                            .font(.md3BodyMedium)
                            .foregroundStyle(.green)
                            .padding(.horizontal)
                    }

                    // 2. Action buttons (register, calendar)
                    actionButtons(event)

                    // 3. Details (difficulty, tags)
                    detailsSection(event)

                    // 4. Description
                    if let desc = event.description, !desc.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("About This Game")
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3OnSurface)
                            Text(desc)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .cardStyle()
                        .padding(.horizontal)
                    }

                    // 5. Game system config (MTG summary at top)
                    gameSystemConfigSection(event)

                    // 6. Games list
                    gamesSection(event)

                    // 7. Multi-table schedule
                    if event.isMultiTable == true {
                        SessionScheduleView(eventId: eventId)
                    }

                    // 8. Players and items
                    playersAndItemsSection(event)

                    // 9. Chat
                    chatSection
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle(vm.event?.title ?? "Game")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if let event = vm.event {
                    let isHost = authVM.user?.id != nil && vm.isHost(userId: authVM.user!.id)
                    Menu {
                        Button { addToCalendar(event) } label: {
                            Label("Add to Calendar", systemImage: "calendar.badge.plus")
                        }
                        Button { showShareSheet = true } label: {
                            Label("Share", systemImage: "square.and.arrow.up")
                        }
                        if isHost {
                            Divider()
                            Button { showEditEvent = true } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive) { showDeleteConfirm = true } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .alert(calendarMessage ?? "", isPresented: $showCalendarAlert) {
            Button("OK") {}
        }
        .sheet(isPresented: $showEditEvent, onDismiss: {
            Task { await vm.loadEvent(id: eventId) }
        }) {
            if let event = vm.event {
                EditEventView(event: event)
            }
        }
        .sheet(isPresented: $showBGGSearch, onDismiss: {
            Task { await vm.loadEvent(id: eventId) }
        }) {
            GameSuggestSheet(
                hostUserId: vm.event?.host?.id,
                hostName: vm.event?.host?.displayName ?? vm.event?.host?.username
            ) { results in
                Task {
                    for result in results {
                        let input = AddEventGameInput(
                            eventId: eventId,
                            bggId: result.bggId,
                            gameName: result.name,
                            thumbnailUrl: result.thumbnailUrl
                        )
                        await vm.addGame(input)
                    }
                }
            }
        }
        .alert("Delete Game", isPresented: $showDeleteConfirm) {
            Button("Delete", role: .destructive) {
                Task {
                    if await vm.deleteEvent() {
                        dismiss()
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to delete this game? This cannot be undone.")
        }
        .sheet(isPresented: $showUpgradePrompt) {
            UpgradePromptView(
                limitType: upgradePromptType,
                currentTier: authVM.user?.effectiveTier ?? .free
            )
        }
        .sheet(isPresented: $showItemsUpgradePrompt) {
            UpgradePromptView(
                limitType: .games,
                currentTier: authVM.user?.effectiveTier ?? .free
            )
        }
        .refreshable { await vm.loadEvent(id: eventId) }
        .task {
            vm.configure(services: services)
            await vm.loadEvent(id: eventId)
        }
    }

    // MARK: - Sections

    private func headerSection(_ event: Event) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Title
            Text(event.title)
                .font(.md3HeadlineMedium)
                .foregroundStyle(Color.md3OnSurface)

            // Game title
            if let gameTitle = event.gameTitle, !gameTitle.isEmpty {
                Text(gameTitle)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            // Badges
            HStack(spacing: 8) {
                if let system = event.gameSystem, system != .boardGame {
                    BadgeView(text: system.shortName, color: system.badgeColor)
                }
                BadgeView(text: event.status.capitalized, color: statusColor(event.status))
                if event.isCharityEvent {
                    BadgeView(text: "Charity", color: .md3TertiaryContainer)
                }
                if event.hostIsPlaying == false {
                    BadgeView(text: "Not Playing", color: .md3SecondaryContainer)
                }
            }

            // Host info
            if let host = event.host {
                HStack(spacing: 10) {
                    UserAvatarView(url: host.avatarUrl, name: host.displayName, size: 40, userId: host.id)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Hosted by")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Text(host.displayName ?? "Unknown")
                            .font(.md3TitleSmall)
                            .foregroundStyle(Color.md3OnSurface)
                    }
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .overlay(
            RoundedRectangle(cornerRadius: MD3Shape.medium)
                .stroke(Color.md3OutlineVariant, lineWidth: 1)
        )
        .padding(.horizontal)
    }

    private func detailsSection(_ event: Event) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Details")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            HStack(spacing: 20) {
                // Date — tap to add to calendar
                Button { addToCalendar(event) } label: {
                    detailRow(icon: "calendar", text: event.eventDate.toDate?.displayDate ?? event.eventDate, tappable: true)
                }

                // Time
                Button { addToCalendar(event) } label: {
                    detailRow(icon: "clock", text: event.startTime?.to12HourTime ?? "TBD", tappable: true)
                }
            }

            HStack(spacing: 20) {
                detailRow(icon: "timer", text: "\(event.durationMinutes ?? 0) min")
                detailRow(icon: "person.2", text: "\(event.confirmedCount)/\(event.maxPlayers ?? 0) players")
            }

            // Address — tap to open in Maps
            if let city = event.city, let state = event.state {
                let address = "\(event.addressLine1.map { "\($0), " } ?? "")\(city), \(state)"
                Button { openInMaps(address: address) } label: {
                    detailRow(icon: "mappin", text: address, tappable: true)
                }
            }

            if event.venueHall != nil || event.venueRoom != nil || event.venueTable != nil {
                let parts = [
                    event.venueHall.map { "Hall: \($0)" },
                    event.venueRoom.map { "Room: \($0)" },
                    event.venueTable.map { "Table: \($0)" }
                ].compactMap { $0 }
                detailRow(icon: "building.2", text: parts.joined(separator: " \u{00B7} "))
            }

            if let tz = event.timezone, let appTz = AppTimezone(rawValue: tz) {
                detailRow(icon: "globe", text: appTz.displayName)
            }

            if let difficulty = event.difficultyLevel {
                detailRow(icon: "gauge.medium", text: difficulty.capitalized)
            }

            if let minAge = event.minAge {
                detailRow(icon: "person.badge.shield.checkmark", text: "Ages \(minAge)+")
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func actionButtons(_ event: Event) -> some View {
        VStack(spacing: 8) {
            if let userId = authVM.user?.id, !vm.isHost(userId: userId) {
                if vm.isRegistered(userId: userId) {
                    Button {
                        Task { await vm.cancelRegistration() }
                    } label: {
                        Text("Cancel Registration")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Error)
                            .frame(maxWidth: .infinity)
                            .frame(height: 40)
                            .background(Color.md3ErrorContainer)
                            .clipShape(Capsule())
                    }
                } else if event.confirmedCount < (event.maxPlayers ?? Int.max) {
                    Button {
                        Task { await vm.register() }
                    } label: {
                        Text("Register")
                            .primaryButtonStyle()
                    }
                } else {
                    Text("Game is full")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
            }
        }
        .padding(.horizontal)
    }

    private func gamesSection(_ event: Event) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Games")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                Button {
                    let currentCount = event.games?.count ?? 0
                    if TierConfig.canAddGame(hostTier, currentCount: currentCount) {
                        showBGGSearch = true
                    } else {
                        upgradePromptType = .games
                        showUpgradePrompt = true
                    }
                } label: {
                    Image(systemName: "plus.circle")
                        .foregroundStyle(Color.md3Primary)
                }
            }

            if let games = event.games, !games.isEmpty {
                ForEach(games) { game in
                    HStack {
                        if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                            AsyncImage(url: imageURL) { image in
                                image.resizable().aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Color.md3SurfaceVariant
                            }
                            .frame(width: 40, height: 40)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                        }
                        VStack(alignment: .leading) {
                            Text(game.gameName)
                                .font(.md3BodyMedium)
                            if let min = game.minPlayers, let max = game.maxPlayers {
                                Text("\(min)-\(max) players")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        Spacer()
                        if game.isPrimary {
                            BadgeView(text: "Primary", color: .md3TertiaryContainer)
                        }
                        if let userId = authVM.user?.id, vm.isHost(userId: userId) {
                            Button {
                                Task { await vm.removeGame(game.id) }
                            } label: {
                                Image(systemName: "trash")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3Error)
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            } else {
                Text("No games added yet")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func playersAndItemsSection(_ event: Event) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text("Players (\(event.confirmedCount)/\(event.maxPlayers ?? 0))")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if let userId = authVM.user?.id, vm.isHost(userId: userId) {
                    Button {
                        if TierConfig.hasFeature(hostTier, feature: \.items) {
                            showAddItem = true
                        } else {
                            showItemsUpgradePrompt = true
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "plus.circle")
                            Text("Add Item")
                                .font(.md3LabelSmall)
                        }
                        .foregroundStyle(Color.md3Primary)
                    }
                }
            }

            // Player list with items
            if let regs = event.registrations, !regs.isEmpty {
                ForEach(regs) { reg in
                    VStack(spacing: 0) {
                        HStack(spacing: 10) {
                            UserAvatarView(url: reg.user?.avatarUrl, name: reg.user?.displayName, size: 36, userId: reg.user?.id)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(reg.user?.displayName ?? "Player")
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurface)

                                // Show items this player is bringing
                                if let items = event.items?.filter({ $0.claimedByUserId == reg.userId }), !items.isEmpty {
                                    ForEach(items) { item in
                                        HStack(spacing: 4) {
                                            Image(systemName: "bag.fill")
                                                .font(.system(size: 10))
                                                .foregroundStyle(Color.md3Primary)
                                            Text(item.itemName)
                                                .font(.md3LabelSmall)
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                            if item.quantityNeeded > 1 {
                                                Text("x\(item.quantityNeeded)")
                                                    .font(.md3LabelSmall)
                                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer()

                            BadgeView(
                                text: reg.status.capitalized,
                                color: reg.status == "confirmed" ? .md3PrimaryContainer : .md3SecondaryContainer
                            )
                        }
                        .padding(.vertical, 6)

                        Divider()
                    }
                }
            } else {
                Text("No players registered yet")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .padding(.vertical, 4)
            }

            // Unclaimed items
            let unclaimedItems = event.items?.filter({ $0.claimedByUserId == nil }) ?? []
            if !unclaimedItems.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Still Needed")
                        .font(.md3TitleSmall)
                        .foregroundStyle(Color.md3Error)
                        .padding(.top, 4)

                    ForEach(unclaimedItems) { item in
                        HStack {
                            Image(systemName: "circle")
                                .font(.system(size: 10))
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text(item.itemName)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurface)
                            Text("(\(item.itemCategory.capitalized))")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Spacer()
                            Button("I'll bring this") {
                                Task { await vm.claimItem(item.id) }
                            }
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3Primary)
                        }
                    }
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
        .alert("Add Item", isPresented: $showAddItem) {
            TextField("Item name", text: $newItemName)
            Button("Add") {
                Task {
                    await vm.addItem(name: newItemName, category: newItemCategory)
                    newItemName = ""
                }
            }
            Button("Cancel", role: .cancel) { newItemName = "" }
        }
    }

    // MARK: - Chat

    @ViewBuilder
    private var chatSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Chat")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if TierConfig.hasFeature(hostTier, feature: \.chat) {
                ChatPanelView(contextType: "event", contextId: eventId)
                    .frame(height: 400)
            } else {
                HStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("Chat requires the event host to have a Basic or higher subscription")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Game System Config

    @ViewBuilder
    private func gameSystemConfigSection(_ event: Event) -> some View {
        if let config = event.mtgConfig {
            VStack(spacing: 16) {
                // 1. Game Summary — purple themed header card
                mtgGameSummary(config)

                // 2. Limited Format Details (draft/sealed/cube only)
                if ["draft", "sealed", "cube"].contains(config.formatId) {
                    mtgDraftSummary(config)
                }

                // 3. Event Structure
                mtgStructureSummary(config)

                // 4. Deck Rules
                mtgDeckRulesSummary(config)

                // 5. Entry & Prizes
                if config.hasPrizes == true || (config.entryFee ?? 0) > 0 {
                    mtgEntryPrizesSummary(config)
                }

                // 6. What to Bring
                mtgWhatToBring(config)
            }
        } else if let config = event.pokemonConfig {
            VStack(alignment: .leading, spacing: 8) {
                Text("Pokémon TCG Details")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                detailRow(icon: "rectangle.stack", text: "Format: \(config.formatDisplayName)")
                if !config.eventTypeDisplayName.isEmpty {
                    detailRow(icon: "flag", text: "Event Type: \(config.eventTypeDisplayName)")
                }
                if let bestOf = config.bestOf {
                    detailRow(icon: "sportscourt", text: "Best of \(bestOf)")
                }
                if config.allowProxies == true {
                    detailRow(icon: "doc.on.doc", text: "Proxies Allowed")
                }
                if let fee = config.entryFee, fee > 0 {
                    detailRow(icon: "dollarsign.circle", text: "Entry Fee: $\(String(format: "%.2f", fee))")
                }
                if config.hasPrizes == true {
                    detailRow(icon: "trophy", text: config.prizeStructure ?? "Prizes Available")
                }
                if config.hasJuniorDivision == true || config.hasSeniorDivision == true || config.hasMastersDivision == true {
                    let divisions = [
                        config.hasJuniorDivision == true ? "Junior" : nil,
                        config.hasSeniorDivision == true ? "Senior" : nil,
                        config.hasMastersDivision == true ? "Masters" : nil
                    ].compactMap { $0 }
                    detailRow(icon: "person.3", text: "Divisions: \(divisions.joined(separator: ", "))")
                }
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)
        } else if let config = event.yugiohConfig {
            VStack(alignment: .leading, spacing: 8) {
                Text("Yu-Gi-Oh! Details")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                detailRow(icon: "rectangle.stack", text: "Format: \(config.formatDisplayName)")
                if !config.eventTypeDisplayName.isEmpty {
                    detailRow(icon: "flag", text: "Event Type: \(config.eventTypeDisplayName)")
                }
                if let bestOf = config.bestOf {
                    detailRow(icon: "sportscourt", text: "Best of \(bestOf)")
                }
                if config.isOfficialEvent == true {
                    detailRow(icon: "checkmark.seal", text: "Official Event")
                }
                if config.awardsOtsPoints == true {
                    detailRow(icon: "star.circle", text: "Awards OTS Points")
                }
                if let fee = config.entryFee, fee > 0 {
                    detailRow(icon: "dollarsign.circle", text: "Entry Fee: $\(String(format: "%.2f", fee))")
                }
                if config.hasPrizes == true {
                    detailRow(icon: "trophy", text: config.prizeStructure ?? "Prizes Available")
                }
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)
        } else if let config = event.warhammer40kConfig {
            VStack(alignment: .leading, spacing: 8) {
                Text("Warhammer 40K Details")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                if !config.gameTypeDisplayName.isEmpty {
                    detailRow(icon: "shield", text: "Game Type: \(config.gameTypeDisplayName)")
                }
                if let points = config.pointsLimit {
                    detailRow(icon: "number", text: "\(points) Points")
                }
                if let mode = config.playerModeDisplayName {
                    detailRow(icon: "person.2", text: "Mode: \(mode)")
                }
                if !config.eventTypeDisplayName.isEmpty {
                    detailRow(icon: "flag", text: "Event Type: \(config.eventTypeDisplayName)")
                }
                if config.battleReadyRequired == true {
                    detailRow(icon: "paintbrush", text: "Battle Ready Required")
                }
                if config.wysiwygRequired == true {
                    detailRow(icon: "eye", text: "WYSIWYG Required")
                }
                if let fee = config.entryFee, fee > 0 {
                    detailRow(icon: "dollarsign.circle", text: "Entry Fee: $\(String(format: "%.2f", fee))")
                }
                if config.hasPrizes == true {
                    detailRow(icon: "trophy", text: config.prizeStructure ?? "Prizes Available")
                }
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)
        }
    }

    // MARK: - MTG Display Components

    private func mtgGameSummary(_ config: MtgEventConfig) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Logo + Format
            HStack(spacing: 10) {
                Image("mtg-logo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(height: 32)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                VStack(alignment: .leading, spacing: 2) {
                    Text(config.formatDisplayName)
                        .font(.md3TitleLarge)
                        .foregroundStyle(Color.md3OnSurface)
                    if let desc = mtgFormatDescription(config.formatId) {
                        Text(desc)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }

            // Event type + structure pills
            FlowLayout(spacing: 6) {
                if !config.eventTypeDisplayName.isEmpty {
                    mtgPill(config.eventTypeDisplayName)
                }
                if config.eventType == "pods", let podSize = config.podsSize {
                    mtgPill("\(podSize)-player pods")
                }
                if let matchStyle = config.matchStyle {
                    mtgPill(matchStyle == "bo3" ? "Best of 3" : "Best of 1")
                }
                if let rounds = config.roundsCount, rounds > 0 {
                    mtgPill("\(rounds) Rounds")
                }
                if config.roundTimeMinutes ?? 0 > 0 {
                    mtgPill("\(config.roundTimeMinutes!) min rounds")
                }
            }

            // Power level badge
            if let powerLevel = config.powerLevelDisplayName {
                let plColor = mtgPowerLevelColor(config.powerLevelRange)
                HStack(spacing: 6) {
                    Image(systemName: "gauge.medium")
                        .font(.system(size: 14))
                    Text(powerLevel)
                        .font(.md3LabelMedium)
                        .fontWeight(.medium)
                }
                .foregroundStyle(plColor)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(plColor.opacity(0.1))
                .clipShape(Capsule())
                .overlay(Capsule().stroke(plColor.opacity(0.3), lineWidth: 1))
            }
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.purple.opacity(0.08), Color.indigo.opacity(0.05)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
        )
        .overlay(RoundedRectangle(cornerRadius: MD3Shape.medium).stroke(Color.purple.opacity(0.2), lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .padding(.horizontal)
    }

    private func mtgDraftSummary(_ config: MtgEventConfig) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "shippingbox.fill")
                    .foregroundStyle(.purple)
                Text("Limited Format Details")
                    .font(.md3TitleSmall)
            }

            if let packs = config.packsPerPlayer, packs > 0 {
                HStack(spacing: 10) {
                    Image(systemName: "number.circle.fill")
                        .font(.system(size: 20))
                        .foregroundStyle(.purple.opacity(0.7))
                    VStack(alignment: .leading, spacing: 1) {
                        Text("\(packs) Packs Per Player")
                            .font(.md3BodyMedium)
                            .fontWeight(.medium)
                        Text(packs == 3 ? "Standard draft pool" : packs == 6 ? "Standard sealed pool" : "\(packs) packs")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }

            if let style = config.draftStyle, !style.isEmpty, config.formatId == "draft" {
                HStack(spacing: 10) {
                    Image(systemName: "arrow.triangle.swap")
                        .font(.system(size: 20))
                        .foregroundStyle(.indigo.opacity(0.7))
                    Text("\(style.capitalized) Draft")
                        .font(.md3BodyMedium)
                        .fontWeight(.medium)
                }
            }

            if config.formatId == "cube", let cube = config.cubeId, !cube.isEmpty {
                HStack(spacing: 10) {
                    Image(systemName: "cube.fill")
                        .font(.system(size: 20))
                        .foregroundStyle(.teal.opacity(0.7))
                    Text("Cube: \(cube)")
                        .font(.md3BodyMedium)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func mtgStructureSummary(_ config: MtgEventConfig) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "list.bullet.rectangle")
                    .foregroundStyle(.purple)
                Text("Event Structure")
                    .font(.md3TitleSmall)
            }

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], alignment: .leading, spacing: 10) {
                mtgDetailItem("Event Type", config.eventTypeDisplayName)

                if let playMode = config.playMode {
                    let label = playMode == "assigned_pods" ? "Assigned Pods" :
                                playMode == "tournament_pairings" ? "Tournament Pairings" : "Open Play"
                    mtgDetailItem("Seating", label)
                }

                if config.eventType == "pods", let podSize = config.podsSize {
                    mtgDetailItem("Pod Size", "\(podSize) players")
                }

                if let matchStyle = config.matchStyle {
                    mtgDetailItem("Match Style", matchStyle == "bo3" ? "Best of 3" : "Best of 1")
                }

                if let rounds = config.roundsCount, rounds > 0 {
                    mtgDetailItem("Rounds", "\(rounds)")
                }

                if config.roundTimeMinutes ?? 0 > 0 {
                    mtgDetailItem("Round Time", "\(config.roundTimeMinutes!) min")
                }

                if let topCut = config.topCut, topCut > 0 {
                    mtgDetailItem("Top Cut", "Top \(topCut)")
                }

                mtgDetailItem("Spectators", config.allowSpectators == true ? "Welcome" : "Not allowed")
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func mtgDeckRulesSummary(_ config: MtgEventConfig) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "doc.text.fill")
                    .foregroundStyle(.purple)
                Text("Deck Rules")
                    .font(.md3TitleSmall)
            }

            // Format rules
            if let rules = mtgFormatRules(config.formatId) {
                Text(rules)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.purple.opacity(0.05))
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    .overlay(RoundedRectangle(cornerRadius: MD3Shape.small).stroke(Color.purple.opacity(0.15), lineWidth: 1))
            }

            // Deck registration
            if config.requireDeckRegistration == true {
                HStack(spacing: 8) {
                    Image(systemName: "info.circle.fill")
                        .foregroundStyle(.blue)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Deck Registration Required")
                            .font(.md3BodySmall)
                            .fontWeight(.medium)
                        if let deadline = config.deckSubmissionDeadline {
                            Text("Submit by: \(deadline.toDate?.displayDateTime ?? deadline)")
                                .font(.md3LabelSmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.blue.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            }

            // Proxy policy
            HStack(spacing: 8) {
                Circle()
                    .fill(config.allowProxies == true ? .green : Color.md3Error)
                    .frame(width: 8, height: 8)
                if config.allowProxies == true {
                    Text("Proxies Allowed\(config.proxyLimit.map { " (limit: \($0))" } ?? "")")
                        .font(.md3BodySmall)
                } else {
                    Text("No Proxies")
                        .font(.md3BodySmall)
                }
            }

            // Banned cards
            if let banned = config.bannedCards, !banned.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Banned Cards (beyond format banlist)")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    FlowLayout(spacing: 6) {
                        ForEach(banned, id: \.self) { card in
                            Text(card)
                                .font(.md3LabelSmall)
                                .foregroundStyle(Color.md3Error)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.md3Error.opacity(0.08))
                                .clipShape(Capsule())
                        }
                    }
                }
            }

            // House rules
            if let house = config.houseRulesNotes, !house.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("House Rules")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text(house)
                        .font(.md3BodySmall)
                        .padding(10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.md3SurfaceContainerHigh)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func mtgEntryPrizesSummary(_ config: MtgEventConfig) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "trophy.fill")
                    .foregroundStyle(.purple)
                Text("Entry & Prizes")
                    .font(.md3TitleSmall)
            }

            HStack(spacing: 16) {
                if let fee = config.entryFee, fee > 0 {
                    HStack(spacing: 6) {
                        Image(systemName: "dollarsign.circle.fill")
                            .font(.system(size: 22))
                            .foregroundStyle(.green)
                        VStack(alignment: .leading) {
                            Text("Entry Fee")
                                .font(.md3LabelSmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("\(config.entryFeeCurrency ?? "USD") \(String(format: "%.2f", fee))")
                                .font(.md3BodyMedium)
                                .fontWeight(.bold)
                        }
                    }
                }
                if config.hasPrizes == true {
                    HStack(spacing: 6) {
                        Image(systemName: "trophy.fill")
                            .font(.system(size: 22))
                            .foregroundStyle(.orange)
                        VStack(alignment: .leading) {
                            Text("Prize Support")
                                .font(.md3LabelSmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("Available")
                                .font(.md3BodyMedium)
                                .fontWeight(.medium)
                        }
                    }
                }
            }

            if config.hasPrizes == true, let structure = config.prizeStructure, !structure.isEmpty {
                Text(structure)
                    .font(.md3BodySmall)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.orange.opacity(0.05))
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    .overlay(RoundedRectangle(cornerRadius: MD3Shape.small).stroke(Color.orange.opacity(0.15), lineWidth: 1))
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func mtgWhatToBring(_ config: MtgEventConfig) -> some View {
        let isLimited = ["draft", "sealed", "cube"].contains(config.formatId)
        let isCommander = ["commander", "oathbreaker", "brawl"].contains(config.formatId)
        let isBo3 = config.matchStyle == "bo3"

        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Image(systemName: "checklist")
                    .foregroundStyle(.purple)
                Text("What to Bring")
                    .font(.md3TitleSmall)
            }

            // Deck requirement
            if isLimited {
                mtgBringItem(icon: "checkmark.circle.fill", color: .green, title: "No deck required", subtitle: "Cards will be provided")
                mtgBringItem(icon: "rectangle.stack", color: .purple, title: "Basic lands", subtitle: "Bring your own or use store lands")
            } else if isCommander {
                mtgBringItem(icon: "rectangle.portrait.on.rectangle.portrait", color: .purple, title: "Commander deck", subtitle: "100-card singleton deck with commander")
                mtgBringItem(icon: "dice", color: .blue, title: "Dice & counters", subtitle: "For tracking life, counters, and tokens")
            } else {
                mtgBringItem(icon: "rectangle.portrait.on.rectangle.portrait", color: .purple, title: "Constructed deck", subtitle: "60-card minimum main deck")
                if isBo3 {
                    mtgBringItem(icon: "rectangle.stack", color: .indigo, title: "15-card sideboard", subtitle: "Required for Best of 3 matches")
                }
            }

            // Life tracking
            if !isLimited {
                mtgBringItem(icon: "heart.text.square", color: .red, title: "Life tracking", subtitle: "Pen & paper, app, or spindown die")
            }

            // Entry fee
            if let fee = config.entryFee, fee > 0 {
                mtgBringItem(icon: "dollarsign.circle", color: .green, title: "Entry fee: \(config.entryFeeCurrency ?? "USD") \(String(format: "%.2f", fee))", subtitle: "Check with host for payment methods")
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - MTG Helpers

    private func mtgPill(_ text: String) -> some View {
        Text(text)
            .font(.md3LabelSmall)
            .foregroundStyle(.purple)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(Color.white.opacity(0.8))
            .clipShape(Capsule())
            .overlay(Capsule().stroke(Color.purple.opacity(0.3), lineWidth: 1))
    }

    private func mtgDetailItem(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.md3LabelSmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
            Text(value)
                .font(.md3BodyMedium)
                .fontWeight(.medium)
        }
    }

    private func mtgBringItem(icon: String, color: Color, title: String, subtitle: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundStyle(color)
                .frame(width: 28, height: 28)
                .background(color.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 6))
            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(.md3BodySmall)
                    .fontWeight(.medium)
                Text(subtitle)
                    .font(.md3LabelSmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
    }

    private func mtgPowerLevelColor(_ range: String?) -> Color {
        switch range {
        case "casual": return .green
        case "mid": return .blue
        case "high": return .orange
        case "cedh": return .red
        default: return .purple
        }
    }

    private func mtgFormatDescription(_ formatId: String?) -> String? {
        switch formatId {
        case "commander": return "100-card singleton, legendary commander"
        case "standard": return "Rotating format, recent sets"
        case "modern": return "Non-rotating, 8th Edition forward"
        case "pioneer": return "Non-rotating, Return to Ravnica forward"
        case "legacy": return "Eternal format, all cards with banlist"
        case "vintage": return "Eternal format, most powerful cards"
        case "pauper": return "Commons only"
        case "draft": return "Build a deck from opened packs"
        case "sealed": return "Build from 6 sealed packs"
        case "cube": return "Draft from a curated card collection"
        case "oathbreaker": return "60-card, Planeswalker commander"
        case "brawl": return "60-card singleton, Standard-legal commander"
        default: return nil
        }
    }

    private func mtgFormatRules(_ formatId: String?) -> String? {
        switch formatId {
        case "commander": return "100-card deck • 1 copy per card (except basics) • Legendary commander • 40 life • Commander damage at 21"
        case "standard", "pioneer", "modern", "legacy", "vintage":
            return "60-card minimum • Up to 4 copies per card • 15-card sideboard • 20 life"
        case "pauper": return "60-card minimum • Commons only • Up to 4 copies • 20 life"
        case "oathbreaker": return "60-card deck • 1 copy per card • Planeswalker as Oathbreaker + Signature Spell • 20 life"
        case "brawl": return "60-card singleton • Standard-legal • Legendary creature or Planeswalker commander • 25 life"
        case "draft": return "40-card minimum deck from drafted cards • Any number of basic lands"
        case "sealed": return "40-card minimum from 6 sealed packs • Any number of basic lands"
        default: return nil
        }
    }

    // MARK: - Helpers

    private func detailRow(icon: String, text: String, tappable: Bool = false) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .foregroundStyle(Color.md3Primary)
                .frame(width: 20)
            Text(text)
                .font(.md3BodyMedium)
                .foregroundStyle(tappable ? Color.md3Primary : Color.md3OnSurface)
                .underline(tappable)
        }
    }

    private func openInMaps(address: String) {
        let encoded = address.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? address
        if let url = URL(string: "maps://?q=\(encoded)") {
            UIApplication.shared.open(url)
        }
    }

    private func statusColor(_ status: String) -> Color {
        switch status {
        case "published": return .md3PrimaryContainer
        case "draft": return .md3SecondaryContainer
        case "cancelled": return .md3ErrorContainer
        case "completed": return .md3TertiaryContainer
        default: return .md3PrimaryContainer
        }
    }

    private func addToCalendar(_ event: Event) {
        let store = EKEventStore()
        store.requestWriteOnlyAccessToEvents { granted, error in
            DispatchQueue.main.async {
                guard granted else {
                    calendarMessage = "Calendar access denied. Enable in Settings > Privacy > Calendars."
                    showCalendarAlert = true
                    return
                }

                let calEvent = EKEvent(eventStore: store)
                calEvent.title = event.title
                calEvent.notes = [event.gameTitle, event.description].compactMap { $0 }.joined(separator: "\n")

                // Parse date + time
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd"
                if let tz = event.timezone {
                    dateFormatter.timeZone = TimeZone(identifier: tz)
                }

                if let date = dateFormatter.date(from: event.eventDate) {
                    if let startTime = event.startTime {
                        let timeFormatter = DateFormatter()
                        timeFormatter.dateFormat = "HH:mm:ss"
                        if let tz = event.timezone {
                            timeFormatter.timeZone = TimeZone(identifier: tz)
                        }
                        if let time = timeFormatter.date(from: startTime) {
                            let calendar = Calendar.current
                            let timeComponents = calendar.dateComponents([.hour, .minute], from: time)
                            var dateComponents = calendar.dateComponents([.year, .month, .day], from: date)
                            dateComponents.hour = timeComponents.hour
                            dateComponents.minute = timeComponents.minute
                            if let tz = event.timezone {
                                dateComponents.timeZone = TimeZone(identifier: tz)
                            }
                            calEvent.startDate = calendar.date(from: dateComponents) ?? date
                        } else {
                            calEvent.startDate = date
                        }
                    } else {
                        calEvent.startDate = date
                    }

                    let duration = (event.durationMinutes ?? 120) + (event.setupMinutes ?? 0)
                    calEvent.endDate = calEvent.startDate.addingTimeInterval(TimeInterval(duration * 60))
                } else {
                    calEvent.startDate = Date()
                    calEvent.endDate = Date().addingTimeInterval(7200)
                }

                // Location
                let locationParts = [event.addressLine1, event.city, event.state].compactMap { $0 }
                if !locationParts.isEmpty {
                    calEvent.location = locationParts.joined(separator: ", ")
                }

                calEvent.calendar = store.defaultCalendarForNewEvents

                do {
                    try store.save(calEvent, span: .thisEvent)
                    calendarMessage = "Added to your calendar!"
                    showCalendarAlert = true
                } catch {
                    calendarMessage = "Failed to add: \(error.localizedDescription)"
                    showCalendarAlert = true
                }
            }
        }
    }
}
