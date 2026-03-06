import SwiftUI

struct PlanningSessionDetailView: View {
    let sessionId: String
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = PlanningSessionViewModel()
    @State private var showSuggestGame = false
    @State private var showFinalize = false
    @State private var selectedAvailability: [String: Bool] = [:]
    @State private var cannotAttendAny = false
    @State private var showAddItem = false
    @State private var newItemName = ""
    @State private var newItemCategory: ItemCategory = .food
    @State private var newItemQuantity = ""
    @State private var showUpgradePrompt = false

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.session == nil {
                LoadingView()
            } else if let session = vm.session {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        Text(session.title)
                            .font(.md3HeadlineMedium)

                        if let description = session.description {
                            Text(description)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }

                        HStack(spacing: 16) {
                            BadgeView(text: session.status.displayName, color: session.status == .open ? .md3TertiaryContainer : .md3PrimaryContainer)
                            Text("Deadline: \(session.responseDeadline.toDate?.displayDate ?? session.responseDeadline)")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                    .padding(.horizontal)

                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    }

                    if let msg = vm.actionMessage {
                        Text(msg)
                            .font(.md3BodyMedium)
                            .foregroundStyle(.green)
                            .padding(.horizontal)
                    }

                    // Date Availability Grid
                    if let dates = session.dates, !dates.isEmpty {
                        dateAvailabilitySection(dates, session: session)
                    }

                    // Response form
                    if session.status == .open, isInvitee(session) {
                        responseSection(session)
                    }

                    // Game Suggestions
                    gameSuggestionsSection(session)

                    // Items to Bring
                    itemsSection(session)

                    // Finalize
                    if session.status == .open && session.createdByUserId == authVM.user?.id {
                        Button {
                            showFinalize = true
                        } label: {
                            Text("Finalize Session")
                                .primaryButtonStyle()
                        }
                        .padding(.horizontal)

                        Button {
                            Task { await vm.cancel() }
                        } label: {
                            Text("Cancel Session")
                                .font(.md3LabelLarge)
                                .foregroundStyle(Color.md3Error)
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Planning Session")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showSuggestGame) {
            BGGGameSearchSheet { result in
                Task {
                    let input = SuggestGameInput(
                        gameName: result.name,
                        bggId: result.bggId
                    )
                    await vm.suggestGame(input)
                }
            }
        }
        .alert("Add Item", isPresented: $showAddItem) {
            TextField("Item name", text: $newItemName)
            Picker("Category", selection: $newItemCategory) {
                ForEach(ItemCategory.allCases) { cat in
                    Text(cat.displayName).tag(cat)
                }
            }
            TextField("Quantity (optional)", text: $newItemQuantity)
                .keyboardType(.numberPad)
            Button("Add") {
                let qty = Int(newItemQuantity)
                Task {
                    await vm.addItem(name: newItemName, category: newItemCategory, quantity: qty)
                    newItemName = ""
                    newItemCategory = .food
                    newItemQuantity = ""
                }
            }
            Button("Cancel", role: .cancel) {
                newItemName = ""
                newItemCategory = .food
                newItemQuantity = ""
            }
        } message: {
            Text("Add an item for someone to bring to game night.")
        }
        .sheet(isPresented: $showUpgradePrompt) {
            UpgradePromptView(limitType: .items, currentTier: authVM.user?.subscriptionTier ?? .free)
        }
        .alert("Finalize Session", isPresented: $showFinalize) {
            Button("Finalize") {
                Task {
                    let bestDateId = vm.session?.dates?.max(by: { ($0.availableCount ?? 0) < ($1.availableCount ?? 0) })?.id
                    let topGameId = vm.session?.gameSuggestions?.max(by: { $0.voteCount < $1.voteCount })?.id
                    if let eventId = await vm.finalize(selectedDateId: bestDateId, selectedGameId: topGameId) {
                        // Event created successfully
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will create a game from this planning session. Continue?")
        }
        .refreshable { await vm.loadSession(id: sessionId) }
        .task {
            vm.configure(services: services)
            await vm.loadSession(id: sessionId)
        }
    }

    private func isInvitee(_ session: PlanningSession) -> Bool {
        guard let userId = authVM.user?.id else { return false }
        return session.invitees?.contains { $0.userId == userId } ?? false
    }

    private func hasResponded(_ session: PlanningSession) -> Bool {
        guard let userId = authVM.user?.id else { return false }
        return session.invitees?.first { $0.userId == userId }?.hasResponded ?? false
    }

    private func dateAvailabilitySection(_ dates: [PlanningDate], session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Date Availability")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            DateAvailabilityGrid(dates: dates, invitees: session.invitees ?? [])
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func responseSection(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(hasResponded(session) ? "Update Response" : "Your Availability")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            Toggle("Cannot attend any dates", isOn: $cannotAttendAny)

            if !cannotAttendAny, let dates = session.dates {
                ForEach(dates) { date in
                    Toggle(
                        date.proposedDate.toDate?.displayDate ?? date.proposedDate,
                        isOn: Binding(
                            get: { selectedAvailability[date.id] ?? false },
                            set: { selectedAvailability[date.id] = $0 }
                        )
                    )
                }
            }

            Button {
                Task {
                    let availability = (session.dates ?? []).map { date in
                        DateAvailabilityInput(
                            dateId: date.id,
                            isAvailable: cannotAttendAny ? false : (selectedAvailability[date.id] ?? false)
                        )
                    }
                    await vm.submitResponse(cannotAttendAny: cannotAttendAny, dateAvailability: availability)
                }
            } label: {
                Text("Submit Response")
                    .primaryButtonStyle()
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func categoryIcon(_ category: ItemCategory) -> String {
        switch category {
        case .food: return "fork.knife"
        case .drinks: return "cup.and.saucer"
        case .supplies: return "wrench.and.screwdriver"
        case .other: return "ellipsis.circle"
        }
    }

    private var userTier: SubscriptionTier {
        authVM.user?.subscriptionTier ?? .free
    }

    private func itemsSection(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Items to Bring")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if session.status == .open && session.createdByUserId == authVM.user?.id {
                    if TierConfig.hasFeature(userTier, feature: \.items) {
                        Button {
                            showAddItem = true
                        } label: {
                            Image(systemName: "plus.circle")
                                .foregroundStyle(Color.md3Primary)
                        }
                    }
                }
            }

            if !TierConfig.hasFeature(userTier, feature: \.items) {
                VStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                        .font(.title2)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("Track items to bring with Pro")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Button {
                        showUpgradePrompt = true
                    } label: {
                        Text("Upgrade")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            } else if let items = session.items, !items.isEmpty {
                ForEach(items) { item in
                    HStack(spacing: 10) {
                        Image(systemName: categoryIcon(item.category))
                            .foregroundStyle(Color.md3Primary)
                            .frame(width: 24)

                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 4) {
                                Text(item.name)
                                    .font(.md3BodyMedium)
                                if let qty = item.quantity, qty > 1 {
                                    Text("x\(qty)")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }
                            if let claimedBy = item.claimedBy {
                                Text("Claimed by \(claimedBy.displayName ?? claimedBy.username ?? "someone")")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3Tertiary)
                            } else {
                                Text("Unclaimed")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }

                        Spacer()

                        if session.status == .open {
                            if item.claimedByUserId == authVM.user?.id {
                                Button {
                                    Task { await vm.unclaimItem(itemId: item.id) }
                                } label: {
                                    Text("Drop")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3Error)
                                }
                            } else if item.claimedByUserId == nil {
                                Button {
                                    Task { await vm.claimItem(itemId: item.id) }
                                } label: {
                                    Text("Claim")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3Primary)
                                }
                            }

                            if session.createdByUserId == authVM.user?.id {
                                Button {
                                    Task { await vm.removeItem(itemId: item.id) }
                                } label: {
                                    Image(systemName: "trash")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3Error)
                                }
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            } else {
                Text("No items yet")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func gameSuggestionsSection(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Game Suggestions")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if session.status == .open {
                    Button {
                        showSuggestGame = true
                    } label: {
                        Image(systemName: "plus.circle")
                            .foregroundStyle(Color.md3Primary)
                    }
                }
            }

            if let suggestions = session.gameSuggestions, !suggestions.isEmpty {
                ForEach(suggestions) { game in
                    HStack {
                        if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                            AsyncImage(url: imageURL) { image in
                                image.resizable().aspectRatio(contentMode: .fill)
                            } placeholder: {
                                Color.md3SurfaceVariant
                            }
                            .frame(width: 36, height: 36)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                        }

                        VStack(alignment: .leading) {
                            Text(game.gameName)
                                .font(.md3BodyMedium)
                            if let suggestedBy = game.suggestedBy?.displayName {
                                Text("by \(suggestedBy)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }

                        Spacer()

                        Button {
                            Task {
                                if game.hasVoted {
                                    await vm.unvoteGame(suggestionId: game.id)
                                } else {
                                    await vm.voteForGame(suggestionId: game.id)
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: game.hasVoted ? "hand.thumbsup.fill" : "hand.thumbsup")
                                Text("\(game.voteCount)")
                            }
                            .font(.md3BodySmall)
                            .foregroundStyle(game.hasVoted ? Color.md3Primary : Color.md3OnSurfaceVariant)
                        }
                    }
                    .padding(.vertical, 4)
                }
            } else {
                Text("No game suggestions yet")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }
}
