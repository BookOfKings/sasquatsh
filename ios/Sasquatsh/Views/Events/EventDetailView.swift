import SwiftUI

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

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.event == nil {
                LoadingView()
            } else if let event = vm.event {
                VStack(alignment: .leading, spacing: 20) {
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

                    detailsSection(event)
                    gameSystemConfigSection(event)

                    if let desc = event.description, !desc.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Description")
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3OnSurface)
                            Text(desc)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding(.horizontal)
                    }

                    actionButtons(event)
                    gamesSection(event)
                    itemsSection(event)
                    if event.isMultiTable == true {
                        SessionScheduleView(eventId: eventId)
                    }
                    registrationsSection(event)
                    chatSection
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle(vm.event?.title ?? "Game")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let event = vm.event, let userId = authVM.user?.id, vm.isHost(userId: userId) {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button { showEditEvent = true } label: {
                            Label("Edit", systemImage: "pencil")
                        }
                        Button { showShareSheet = true } label: {
                            Label("Share", systemImage: "square.and.arrow.up")
                        }
                        Button(role: .destructive) { showDeleteConfirm = true } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .sheet(isPresented: $showEditEvent) {
            if let event = vm.event {
                EditEventView(event: event)
            }
        }
        .sheet(isPresented: $showBGGSearch) {
            BGGGameSearchSheet { result in
                Task {
                    let input = AddEventGameInput(
                        eventId: eventId,
                        bggId: result.bggId,
                        gameName: result.name
                    )
                    await vm.addGame(input)
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
                currentTier: authVM.user?.subscriptionTier ?? .free
            )
        }
        .sheet(isPresented: $showItemsUpgradePrompt) {
            UpgradePromptView(
                limitType: .games,
                currentTier: authVM.user?.subscriptionTier ?? .free
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
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                if let host = event.host {
                    UserAvatarView(url: host.avatarUrl, name: host.displayName, size: 36)
                    VStack(alignment: .leading) {
                        Text("Hosted by")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Text(host.displayName ?? "Unknown")
                            .font(.md3TitleSmall)
                    }
                }
                Spacer()
                if let system = event.gameSystem, system != .boardGame {
                    BadgeView(text: system.shortName, color: system.badgeColor)
                }
                BadgeView(text: event.status.capitalized, color: statusColor(event.status))
                if event.hostIsPlaying == false {
                    BadgeView(text: "Not Playing", color: .md3SecondaryContainer)
                }
            }
        }
        .padding(.horizontal)
    }

    private func detailsSection(_ event: Event) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 20) {
                detailRow(icon: "calendar", text: event.eventDate.toDate?.displayDate ?? event.eventDate)
                detailRow(icon: "clock", text: event.startTime ?? "TBD")
            }

            HStack(spacing: 20) {
                detailRow(icon: "timer", text: "\(event.durationMinutes ?? 0) min")
                detailRow(icon: "person.2", text: "\(event.confirmedCount)/\(event.maxPlayers ?? 0) players")
            }

            if let city = event.city, let state = event.state {
                detailRow(icon: "mappin", text: "\(event.addressLine1.map { "\($0), " } ?? "")\(city), \(state)")
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
                    let tier = authVM.user?.subscriptionTier ?? .free
                    let currentCount = event.games?.count ?? 0
                    if TierConfig.canAddGame(tier, currentCount: currentCount) {
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

    private func itemsSection(_ event: Event) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Items Needed")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if let userId = authVM.user?.id, vm.isHost(userId: userId) {
                    Button {
                        let tier = authVM.user?.subscriptionTier ?? .free
                        if TierConfig.hasFeature(tier, feature: \.items) {
                            showAddItem = true
                        } else {
                            showItemsUpgradePrompt = true
                        }
                    } label: {
                        Image(systemName: "plus.circle")
                            .foregroundStyle(Color.md3Primary)
                    }
                }
            }

            if let items = event.items, !items.isEmpty {
                ForEach(items) { item in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(item.itemName)
                                .font(.md3BodyMedium)
                            Text(item.itemCategory.capitalized)
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        Spacer()
                        if let claimedBy = item.claimedByName {
                            Text(claimedBy)
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            if item.claimedByUserId == authVM.user?.id {
                                Button("Unclaim") {
                                    Task { await vm.unclaimItem(item.id) }
                                }
                                .font(.md3LabelSmall)
                                .foregroundStyle(Color.md3Error)
                            }
                        } else {
                            Button("Claim") {
                                Task { await vm.claimItem(item.id) }
                            }
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3Primary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            } else {
                Text("No items needed")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
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

    private func registrationsSection(_ event: Event) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Registered Players (\(event.confirmedCount)/\(event.maxPlayers ?? 0))")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if let regs = event.registrations, !regs.isEmpty {
                ForEach(regs) { reg in
                    HStack {
                        UserAvatarView(url: reg.user?.avatarUrl, name: reg.user?.displayName, size: 32)
                        Text(reg.user?.displayName ?? "Player")
                            .font(.md3BodyMedium)
                        Spacer()
                        Text(reg.registeredAt.toDate?.relativeDisplay ?? "")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            } else {
                Text("No registrations yet")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Chat

    @ViewBuilder
    private var chatSection: some View {
        let tier = authVM.user?.subscriptionTier ?? .free
        VStack(alignment: .leading, spacing: 12) {
            Text("Chat")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if TierConfig.hasFeature(tier, feature: \.chat) {
                ChatPanelView(contextType: "event", contextId: eventId)
                    .frame(height: 400)
            } else {
                HStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("Upgrade to Basic to chat")
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
            VStack(alignment: .leading, spacing: 8) {
                Text("MTG Details")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                detailRow(icon: "rectangle.stack", text: "Format: \(config.formatDisplayName)")
                if !config.eventTypeDisplayName.isEmpty {
                    detailRow(icon: "flag", text: "Event Type: \(config.eventTypeDisplayName)")
                }
                if let powerLevel = config.powerLevelDisplayName {
                    detailRow(icon: "gauge.medium", text: "Power Level: \(powerLevel)")
                }
                if let matchStyle = config.matchStyle {
                    detailRow(icon: "sportscourt", text: matchStyle == "bo3" ? "Best of 3" : "Best of 1")
                }
                if config.allowProxies == true {
                    detailRow(icon: "doc.on.doc", text: "Proxies Allowed\(config.proxyLimit.map { " (limit: \($0))" } ?? "")")
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

    // MARK: - Helpers

    private func detailRow(icon: String, text: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .foregroundStyle(Color.md3Primary)
                .frame(width: 20)
            Text(text)
                .font(.md3BodyMedium)
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
}
