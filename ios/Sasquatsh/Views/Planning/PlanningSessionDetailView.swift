import SwiftUI

struct PlanningSessionDetailView: View {
    let sessionId: String
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = PlanningSessionViewModel()
    @State private var showSuggestGame = false
    @State private var selectedAvailability: [String: Bool] = [:]
    @State private var cannotAttendAny = false
    @State private var showAddItem = false
    @State private var newItemName = ""
    @State private var newItemCategory: ItemCategory = .food
    @State private var newItemQuantity = ""
    @State private var showAddInvitees = false
    @State private var showShareLink = false
    @State private var currentStep = 0 // 0=dates, 1=games, 2=items, 3=people, 4=finalize
    @State private var showCancelConfirm = false

    // Finalize form state
    @State private var selectedDateId: String?
    @State private var selectedGameId: String?
    @State private var navigateToEventId: String?
    @State private var tableGameAssignments: [Int: String] = [:] // table number -> game suggestion ID

    /// Creator's effective tier — participants inherit the creator's features
    private var creatorTier: SubscriptionTier {
        vm.session?.createdBy?.effectiveTier ?? .free
    }

    private var isCreator: Bool {
        vm.session?.createdByUserId == authVM.user?.id
    }

    private var stepCount: Int {
        isCreator ? 5 : 4
    }

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.session == nil {
                LoadingView()
            } else if vm.session == nil {
                VStack(spacing: 12) {
                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    } else {
                        Text("Session not found")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    Button {
                        Task { await vm.loadSession(id: sessionId) }
                    } label: {
                        Text("Retry")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                    }
                }
                .padding()
            } else if let session = vm.session {
                VStack(alignment: .leading, spacing: 16) {
                    // Header card
                    headerSection(session)

                    sessionContent(session)
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Planning Session")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showSuggestGame, onDismiss: {
            Task { await vm.loadSession(id: sessionId) }
        }) {
            GameSuggestSheet(
                hostUserId: vm.session?.createdByUserId,
                hostName: vm.session?.createdBy?.displayName ?? vm.session?.createdBy?.username,
                alreadySuggestedBggIds: myAlreadySuggestedBggIds
            ) { results in
                Task {
                    for result in results {
                        let input = SuggestGameInput(
                            gameName: result.name,
                            bggId: result.bggId,
                            thumbnailUrl: result.thumbnailUrl
                        )
                        await vm.suggestGame(input)
                    }
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
        .sheet(isPresented: $showAddInvitees) {
            PlanningAddInviteesSheet(sessionId: sessionId, groupId: vm.session?.groupId ?? "") {
                Task { await vm.loadSession(id: sessionId) }
            }
        }
        .sheet(isPresented: $showShareLink) {
            if let session = vm.session {
                ShareInviteLinkSheet(
                    groupId: session.groupId,
                    linkType: "session",
                    planningSessionId: sessionId,
                    title: "Share Session Invite"
                )
            }
        }
        .alert("Cancel Session", isPresented: $showCancelConfirm) {
            Button("Cancel Session", role: .destructive) {
                Task { await vm.cancel() }
            }
            Button("Keep Open", role: .cancel) {}
        } message: {
            Text("Are you sure you want to cancel this planning session?")
        }
        .navigationDestination(item: $navigateToEventId) { eventId in
            EventDetailView(eventId: eventId)
        }
        .refreshable { await vm.loadSession(id: sessionId) }
        .task {
            vm.configure(services: services)
            await vm.loadSession(id: sessionId)
            if let session = vm.session {
                prefillAvailability(session)
                if hasResponded(session) {
                    currentStep = 1
                }
                if let tc = session.tableCount, tc >= 2 {
                    tableCountInput = tc
                }
            }
        }
        .onChange(of: vm.session?.invitees?.description) {
            // Re-prefill availability when session reloads (after submit)
            if let session = vm.session {
                prefillAvailability(session)
            }
        }
    }

    @ViewBuilder
    private func sessionContent(_ session: PlanningSession) -> some View {
        // Status banners
        if let error = vm.error {
            ErrorBannerView(message: error) { vm.error = nil }
                .padding(.horizontal)
        }
        if let msg = vm.actionMessage {
            Text(msg)
                .font(.md3BodyMedium)
                .foregroundStyle(.green)
                .padding(.horizontal)
        }

        // Finalized / Cancelled state
        if session.status == .finalized {
            finalizedBanner(session)
        } else if session.status == .cancelled {
            cancelledBanner
        }

        // Step navigation
        if session.status == .open {
            stepperView(session)
        }

        // Step content
        stepContent(session)

        // Chat (always visible, collapsible)
        planningChatSection

        // Creator actions at bottom
        if session.status == .open && isCreator {
            creatorBottomActions
        }
    }

    @ViewBuilder
    private func stepContent(_ session: PlanningSession) -> some View {
        switch currentStep {
        case 0: datesStep(session)
        case 1: gamesStep(session)
        case 2: itemsStep(session)
        case 3: peopleStep(session)
        case 4:
            if isCreator {
                finalizeStep(session)
            }
        default: EmptyView()
        }
    }

    private var creatorBottomActions: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                Button { showAddInvitees = true } label: {
                    Label("Invite More", systemImage: "person.badge.plus")
                        .secondaryButtonStyle()
                }
                Button { showShareLink = true } label: {
                    Image(systemName: "qrcode")
                        .font(.md3LabelLarge)
                        .frame(width: 48, height: 40)
                        .background(Color.md3SecondaryContainer)
                        .foregroundStyle(Color.md3OnSecondaryContainer)
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal)

            Button { showCancelConfirm = true } label: {
                Text("Cancel Session")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3Error)
                    .frame(maxWidth: .infinity)
            }
            .padding(.bottom, 8)
        }
    }

    /// BggIds the current user has already suggested — they can't add these again
    private var myAlreadySuggestedBggIds: Set<Int> {
        guard let userId = authVM.user?.id,
              let suggestions = vm.session?.gameSuggestions else { return [] }
        return Set(suggestions.filter { $0.suggestedByUserId == userId }.compactMap { $0.bggId })
    }

    // MARK: - Helpers

    private func isInvitee(_ session: PlanningSession) -> Bool {
        guard let userId = authVM.user?.id else { return false }
        return session.invitees?.contains { $0.userId == userId } ?? false
    }

    private func hasResponded(_ session: PlanningSession) -> Bool {
        guard let userId = authVM.user?.id else { return false }
        return session.invitees?.first { $0.userId == userId }?.hasResponded ?? false
    }

    private var respondedCount: Int {
        vm.session?.invitees?.filter { $0.hasResponded }.count ?? 0
    }

    private var totalInvitees: Int {
        vm.session?.invitees?.count ?? 0
    }

    private func prefillAvailability(_ session: PlanningSession) {
        guard let userId = authVM.user?.id,
              let dates = session.dates else { return }
        for date in dates {
            if let votes = date.votes {
                if let vote = votes.first(where: { $0.userId == userId }) {
                    selectedAvailability[date.id] = vote.isAvailable
                }
            }
        }
        if let invitee = session.invitees?.first(where: { $0.userId == userId }) {
            cannotAttendAny = invitee.cannotAttendAny ?? false
        }
    }

    // MARK: - Header

    private func headerSection(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            // Title + share
            HStack(alignment: .top) {
                Text(session.title)
                    .font(.md3HeadlineMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if session.status == .open {
                    Button { showShareLink = true } label: {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 16))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .frame(width: 32, height: 32)
                            .background(Color.md3SurfaceContainerHigh)
                            .clipShape(Circle())
                    }
                }
            }

            if let description = session.description {
                Text(description)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            // Status + deadline row
            HStack(spacing: 12) {
                BadgeView(text: session.status.displayName, color: statusColor(session.status))

                if session.openToGroup == true {
                    BadgeView(text: "Open to Group", color: .md3TertiaryContainer)
                }
            }

            // Creator info
            if let creator = session.createdBy {
                HStack(spacing: 8) {
                    UserAvatarView(url: creator.avatarUrl, name: creator.displayName, size: 28, userId: creator.id)
                    Text("Hosted by \(creator.displayName ?? creator.username ?? "Unknown")")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }

            // Deadline
            HStack(spacing: 6) {
                Image(systemName: "clock")
                    .font(.system(size: 13))
                    .foregroundStyle(deadlineColor(session.responseDeadline))
                Text("Deadline: \(session.responseDeadline.toDate?.displayDate ?? session.responseDeadline)")
                    .font(.md3BodySmall)
                    .foregroundStyle(deadlineColor(session.responseDeadline))
            }

            // Response progress
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("\(respondedCount) of \(totalInvitees) responded")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Spacer()
                    if hasResponded(session) {
                        HStack(spacing: 4) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 12))
                                .foregroundStyle(.green)
                            Text("You've responded")
                                .font(.md3LabelSmall)
                                .foregroundStyle(.green)
                        }
                    }
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.md3SurfaceContainerHigh)
                            .frame(height: 6)
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.md3Primary)
                            .frame(width: totalInvitees > 0 ? geo.size.width * CGFloat(respondedCount) / CGFloat(totalInvitees) : 0, height: 6)
                    }
                }
                .frame(height: 6)
            }

            // Participation slots
            if let maxP = session.maxParticipants, maxP > 0 {
                let slotsUsed = vm.session?.invitees?.filter { $0.hasSlot == true }.count ?? 0
                HStack(spacing: 6) {
                    Image(systemName: "person.2.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.md3Primary)
                    Text("\(slotsUsed) of \(maxP) slots filled")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func statusColor(_ status: PlanningStatus) -> Color {
        switch status {
        case .open: return .md3TertiaryContainer
        case .finalized: return Color.blue.opacity(0.15)
        case .cancelled: return Color.red.opacity(0.15)
        }
    }

    private func deadlineColor(_ deadline: String) -> Color {
        guard let date = deadline.toDate else { return Color.md3OnSurfaceVariant }
        if date < Date() { return Color.md3Error }
        if date.timeIntervalSinceNow < 86400 { return .orange }
        return Color.md3OnSurfaceVariant
    }

    // MARK: - Status Banners

    private func finalizedBanner(_ session: PlanningSession) -> some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundStyle(.blue)
                Text("This session has been finalized")
                    .font(.md3BodyMedium)
                    .fontWeight(.medium)
            }
            if let finalizedDate = session.finalizedDate {
                Text("Event date: \(finalizedDate.toDate?.displayDate ?? finalizedDate)")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            if let eventId = session.createdEventId {
                NavigationLink {
                    EventDetailView(eventId: eventId)
                } label: {
                    Text("View Event")
                        .font(.md3LabelLarge)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 8)
                        .background(Color.md3Primary)
                        .clipShape(Capsule())
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color.blue.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .padding(.horizontal)
    }

    private var cancelledBanner: some View {
        HStack(spacing: 8) {
            Image(systemName: "xmark.seal.fill")
                .foregroundStyle(Color.md3Error)
            Text("This session has been cancelled")
                .font(.md3BodyMedium)
                .fontWeight(.medium)
                .foregroundStyle(Color.md3Error)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color.md3Error.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .padding(.horizontal)
    }

    // MARK: - Stepper

    private func stepperView(_ session: PlanningSession) -> some View {
        let labels = isCreator
            ? ["Dates", "Games", "Items", "People", "Finalize"]
            : ["Dates", "Games", "Items", "People"]
        let icons = isCreator
            ? ["calendar", "dice", "bag", "person.3", "checkmark.seal"]
            : ["calendar", "dice", "bag", "person.3"]

        return HStack(spacing: 0) {
            ForEach(0..<stepCount, id: \.self) { step in
                if step > 0 {
                    VStack(spacing: 0) {
                        Rectangle()
                            .fill(step <= currentStep ? Color.md3Primary.opacity(0.5) : Color.md3OutlineVariant)
                            .frame(height: 2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 16)
                }

                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        currentStep = step
                    }
                } label: {
                    VStack(spacing: 4) {
                        ZStack {
                            Circle()
                                .fill(step == currentStep ? Color.md3Primary : (step < currentStep ? .green : Color.md3SurfaceContainerHigh))
                                .frame(width: 36, height: 36)
                            if step < currentStep {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(.white)
                            } else {
                                Image(systemName: icons[step])
                                    .font(.system(size: 14))
                                    .foregroundStyle(step == currentStep ? .white : Color.md3OnSurfaceVariant)
                            }
                        }
                        Text(labels[step])
                            .font(.system(size: 10, weight: step == currentStep ? .semibold : .regular))
                            .foregroundStyle(step == currentStep ? Color.md3Primary : Color.md3OnSurfaceVariant)
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Step 1: Dates

    private func datesStep(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            // Date availability grid
            if let dates = session.dates, !dates.isEmpty {
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

            // Response form
            if session.status == .open, isInvitee(session) {
                VStack(alignment: .leading, spacing: 12) {
                    Text(hasResponded(session) ? "Update Response" : "Your Availability")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)

                    Toggle("Cannot attend any dates", isOn: $cannotAttendAny)

                    if !cannotAttendAny, let dates = session.dates {
                        ForEach(dates) { date in
                            HStack {
                                Toggle(
                                    isOn: Binding(
                                        get: { selectedAvailability[date.id] ?? false },
                                        set: { selectedAvailability[date.id] = $0 }
                                    )
                                ) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(date.proposedDate.toDate?.displayDate ?? date.proposedDate)
                                            .font(.md3BodyMedium)
                                        if let time = date.startTime {
                                            Text("at \(time)")
                                                .font(.md3BodySmall)
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                        }
                                    }
                                }
                            }
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
                            if vm.error == nil {
                                withAnimation { currentStep = 1 }
                            }
                        }
                    } label: {
                        Text(hasResponded(session) ? "Update Response" : "Submit Response")
                            .primaryButtonStyle()
                    }
                }
                .padding()
                .cardStyle()
                .padding(.horizontal)
            }

            // Navigation
            stepNav(back: nil, forward: 1)
        }
    }

    // MARK: - Step 2: Games

    private func gamesStep(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Game Suggestions")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                    Spacer()
                    if session.status == .open {
                        Button { showSuggestGame = true } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "plus")
                                    .font(.system(size: 12, weight: .semibold))
                                Text("Suggest")
                                    .font(.md3LabelMedium)
                            }
                            .foregroundStyle(Color.md3OnPrimary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.md3Primary)
                            .clipShape(Capsule())
                        }
                    }
                }

                if let suggestions = session.gameSuggestions, !suggestions.isEmpty {
                    Text("Vote for games you'd like to play. Games with 2+ votes are prioritized.")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)

                    ForEach(suggestions.sorted { $0.voteCount > $1.voteCount }) { game in
                        gameSuggestionRow(game, session: session)
                    }
                } else {
                    VStack(spacing: 8) {
                        Image(systemName: "dice")
                            .font(.title2)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Text("No game suggestions yet")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Text("Tap \"Suggest\" to search BoardGameGeek")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                }
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)

            stepNav(back: 0, forward: 2)
        }
    }

    private func gameSuggestionRow(_ game: GameSuggestion, session: PlanningSession) -> some View {
        HStack(spacing: 12) {
            if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                AsyncImage(url: imageURL) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.md3SurfaceContainerHigh)
                    .frame(width: 48, height: 48)
                    .overlay {
                        Image(systemName: "dice")
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(game.gameName)
                    .font(.md3BodyMedium)
                    .fontWeight(.medium)
                HStack(spacing: 8) {
                    if let min = game.minPlayers, let max = game.maxPlayers {
                        Label("\(min)-\(max)", systemImage: "person.2")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    if let time = game.playingTime, time > 0 {
                        Label("\(time) min", systemImage: "clock")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
                if let suggestedBy = game.suggestedBy {
                    Text("by \(suggestedBy.displayName ?? suggestedBy.username ?? "someone")")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                }
            }

            Spacer()

            // Vote button
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
                .font(.md3LabelLarge)
                .foregroundStyle(game.hasVoted ? .white : Color.md3OnSurfaceVariant)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(game.hasVoted ? Color.md3Primary : Color.md3SurfaceContainerHigh)
                .clipShape(Capsule())
            }

            // Remove button (creator or suggester)
            if session.status == .open &&
                (isCreator || game.suggestedByUserId == authVM.user?.id) {
                Button {
                    Task { await vm.removeSuggestion(suggestionId: game.id) }
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.md3Error)
                }
            }
        }
        .padding(10)
        .background(game.hasVoted ? Color.md3Primary.opacity(0.06) : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
    }

    // MARK: - Step 3: Items

    private func itemsStep(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            itemsSection(session)
            stepNav(back: 1, forward: 3)
        }
    }

    private func categoryIcon(_ category: ItemCategory) -> String {
        switch category {
        case .food: return "fork.knife"
        case .drinks: return "cup.and.saucer"
        case .supplies: return "wrench.and.screwdriver"
        case .other: return "ellipsis.circle"
        }
    }

    private func itemsSection(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "bag.fill")
                        .foregroundStyle(.green)
                    Text("Items to Bring")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                }
                Spacer()
                if session.status == .open && TierConfig.hasFeature(creatorTier, feature: \.items) {
                    Button { showAddItem = true } label: {
                        Image(systemName: "plus.circle")
                            .foregroundStyle(Color.md3Primary)
                    }
                }
            }

            if !TierConfig.hasFeature(creatorTier, feature: \.items) {
                VStack(spacing: 8) {
                    Image(systemName: "lock.fill")
                        .font(.title2)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("Items feature requires the session host to have a Pro subscription")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
            } else if let items = session.items, !items.isEmpty {
                ForEach(items) { item in
                    HStack(spacing: 10) {
                        Image(systemName: categoryIcon(item.category))
                            .foregroundStyle(categoryColor(item.category))
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
                                    .foregroundStyle(.green)
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
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(Color.md3Error.opacity(0.08))
                                        .clipShape(Capsule())
                                }
                            } else if item.claimedByUserId == nil {
                                Button {
                                    Task { await vm.claimItem(itemId: item.id) }
                                } label: {
                                    Text("I'll bring this")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3Primary)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(Color.md3Primary.opacity(0.08))
                                        .clipShape(Capsule())
                                }
                            }

                            if isCreator || item.addedByUserId == authVM.user?.id {
                                Button {
                                    Task { await vm.removeItem(itemId: item.id) }
                                } label: {
                                    Image(systemName: "trash")
                                        .font(.system(size: 13))
                                        .foregroundStyle(Color.md3Error)
                                }
                            }
                        }
                    }
                    .padding(8)
                    .background(item.claimedByUserId != nil ? Color.green.opacity(0.06) : Color.clear)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }
            } else {
                Text("No items yet — add things for people to bring!")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .padding(.vertical, 8)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private func categoryColor(_ category: ItemCategory) -> Color {
        switch category {
        case .food: return .orange
        case .drinks: return .blue
        case .supplies: return .purple
        case .other: return Color.md3OnSurfaceVariant
        }
    }

    // MARK: - Step 4: People

    private func peopleStep(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Invited Members")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                    Spacer()
                    if let max = session.maxParticipants, max > 0 {
                        Text("Limited to \(max)")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.md3SurfaceContainerHigh)
                            .clipShape(Capsule())
                    }
                }

                if let invitees = session.invitees, !invitees.isEmpty {
                    let columns = [GridItem(.adaptive(minimum: 80), spacing: 12)]
                    LazyVGrid(columns: columns, spacing: 16) {
                        ForEach(invitees) { invitee in
                            inviteeCell(invitee, session: session)
                        }
                    }
                } else {
                    Text("No one has been invited yet")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }

                if session.status == .open && isCreator {
                    Button { showAddInvitees = true } label: {
                        Label("Invite More", systemImage: "person.badge.plus")
                            .font(.md3LabelLarge)
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(Color.md3SecondaryContainer)
                            .foregroundStyle(Color.md3OnSecondaryContainer)
                            .clipShape(Capsule())
                    }
                }
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)

            if isCreator {
                stepNav(back: 2, forward: 4)
            } else {
                stepNav(back: 2, forward: nil)
            }
        }
    }

    private func inviteeCell(_ invitee: PlanningInvitee, session: PlanningSession) -> some View {
        VStack(spacing: 6) {
            ZStack(alignment: .bottomTrailing) {
                UserAvatarView(
                    url: invitee.user?.avatarUrl,
                    name: invitee.user?.displayName,
                    size: 48,
                    userId: invitee.userId
                )
                .opacity(invitee.hasResponded || invitee.userId == session.createdByUserId ? 1.0 : 0.5)
                .overlay {
                    if invitee.hasSlot == true {
                        Circle()
                            .stroke(.green, lineWidth: 2)
                    }
                }

                // Status badge
                if invitee.userId != session.createdByUserId {
                    if invitee.hasResponded {
                        let canAttend = !(invitee.cannotAttendAny ?? false)
                        Circle()
                            .fill(canAttend ? .green : Color.md3Error)
                            .frame(width: 16, height: 16)
                            .overlay {
                                Image(systemName: canAttend ? "checkmark" : "xmark")
                                    .font(.system(size: 8, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                            .offset(x: 2, y: 2)
                    }
                }
            }

            Text(invitee.user?.displayName ?? "Unknown")
                .font(.system(size: 11))
                .foregroundStyle(Color.md3OnSurface)
                .lineLimit(1)

            // Status label
            if invitee.userId == session.createdByUserId {
                Text("Organizer")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundStyle(Color.md3Primary)
            } else if invitee.hasSlot == true {
                Text("Has Slot")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundStyle(.green)
            } else if invitee.hasResponded {
                Text(invitee.cannotAttendAny == true ? "Unavailable" : "Responded")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundStyle(invitee.cannotAttendAny == true ? Color.md3Error : Color.md3OnSurfaceVariant)
            } else {
                Text("Pending")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.6))
            }
        }
    }

    // MARK: - Step 5: Finalize (creator only)

    @State private var tableCountInput: Int = 2

    private var isMultiTable: Bool {
        (vm.session?.tableCount ?? 0) >= 2
    }

    private func finalizeStep(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            // Event mode toggle
            VStack(alignment: .leading, spacing: 12) {
                Text("Event Mode")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)

                HStack(spacing: 12) {
                    eventModeButton(
                        icon: "calendar",
                        title: "Single Event",
                        subtitle: "One game session",
                        isActive: !isMultiTable,
                        color: Color.md3Primary
                    ) {
                        Task {
                            await vm.updateSettings(tableCount: nil)
                        }
                    }

                    eventModeButton(
                        icon: "square.grid.2x2.fill",
                        title: "Multi-Table",
                        subtitle: "Multiple games at once",
                        isActive: isMultiTable,
                        color: .purple
                    ) {
                        Task {
                            await vm.updateSettings(tableCount: tableCountInput)
                        }
                    }
                }

                if isMultiTable {
                    HStack {
                        Text("Number of Tables:")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Spacer()
                        Picker("Tables", selection: $tableCountInput) {
                            ForEach(2...10, id: \.self) { n in
                                Text("\(n)").tag(n)
                            }
                        }
                        .pickerStyle(.menu)

                        if tableCountInput != (session.tableCount ?? 2) {
                            Button {
                                Task {
                                    await vm.updateSettings(tableCount: tableCountInput)
                                }
                            } label: {
                                Text("Update")
                                    .font(.md3LabelMedium)
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.md3Primary)
                                    .clipShape(Capsule())
                            }
                        }
                    }
                }
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)

            VStack(alignment: .leading, spacing: 16) {
                Text("Finalize Session")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)

                Text("Finalizing will create a game event with the selected date and game.")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)

                // Date selection
                VStack(alignment: .leading, spacing: 8) {
                    Text("Select Date")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3OnSurface)

                    if let dates = session.dates, !dates.isEmpty {
                        ForEach(dates.sorted { ($0.availableCount ?? 0) > ($1.availableCount ?? 0) }) { date in
                            Button {
                                selectedDateId = date.id
                            } label: {
                                HStack {
                                    Image(systemName: selectedDateId == date.id ? "largecircle.fill.circle" : "circle")
                                        .foregroundStyle(selectedDateId == date.id ? Color.md3Primary : Color.md3OnSurfaceVariant)
                                    Text(date.proposedDate.toDate?.displayDate ?? date.proposedDate)
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    if let time = date.startTime {
                                        Text("at \(time)")
                                            .font(.md3BodySmall)
                                            .foregroundStyle(Color.md3OnSurfaceVariant)
                                    }
                                    Spacer()
                                    Text("\(date.availableCount ?? 0) available")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                                .padding(10)
                                .background(selectedDateId == date.id ? Color.md3Primary.opacity(0.08) : Color.md3SurfaceContainerHigh)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                // Game selection — different UI for single vs multi-table
                if isMultiTable {
                    multiTableGameAssignment(session)
                } else {
                    singleGameSelection(session)
                }

                // Validation
                finalizeValidation(session)

                // Finalize button
                Button {
                    Task {
                        if isMultiTable && !tableGameAssignments.isEmpty {
                            // Save table assignments first, then finalize
                            let saved = await vm.scheduleSessions(assignments: tableGameAssignments)
                            guard saved else { return }
                        }
                        if let eventId = await vm.finalize(selectedDateId: selectedDateId, selectedGameId: isMultiTable ? nil : selectedGameId) {
                            navigateToEventId = eventId
                        }
                    }
                } label: {
                    Text("Create Game Event")
                        .primaryButtonStyle()
                }
                .disabled(!canFinalize(session))
            }
            .padding()
            .cardStyle()
            .padding(.horizontal)

            stepNav(back: 3, forward: nil)
        }
    }

    // MARK: - Single Game Selection (single-table mode)

    private func singleGameSelection(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Select Game")
                .font(.md3LabelLarge)
                .foregroundStyle(Color.md3OnSurface)

            if let games = session.gameSuggestions, !games.isEmpty {
                ForEach(games.sorted { $0.voteCount > $1.voteCount }) { game in
                    Button {
                        selectedGameId = game.id
                    } label: {
                        HStack {
                            Image(systemName: selectedGameId == game.id ? "largecircle.fill.circle" : "circle")
                                .foregroundStyle(selectedGameId == game.id ? Color.md3Primary : Color.md3OnSurfaceVariant)
                            gameThumb(game)
                            Text(game.gameName)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurface)
                            Spacer()
                            voteCount(game)
                        }
                        .padding(10)
                        .background(selectedGameId == game.id ? Color.md3Primary.opacity(0.08) : Color.md3SurfaceContainerHigh)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    }
                    .buttonStyle(.plain)
                }
            } else {
                Text("No games suggested")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
        }
    }

    // MARK: - Multi-Table Game Assignment

    private func multiTableGameAssignment(_ session: PlanningSession) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Assign Games to Tables")
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                BadgeView(text: "\(session.tableCount ?? 2) tables", color: .purple.opacity(0.15))
            }

            let tables = Array(1...(session.tableCount ?? 2))
            let games = session.gameSuggestions?.sorted { $0.voteCount > $1.voteCount } ?? []

            if games.isEmpty {
                Text("No games suggested — add games in the Games step first")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                ForEach(tables, id: \.self) { tableNum in
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 6) {
                            Image(systemName: "tablecells")
                                .font(.system(size: 13))
                                .foregroundStyle(.purple)
                            Text("Table \(tableNum)")
                                .font(.md3BodyMedium)
                                .fontWeight(.medium)
                        }

                        let assignedGameId = tableGameAssignments[tableNum]
                        let alreadyAssignedIds = Set(tableGameAssignments.filter { $0.key != tableNum }.values)
                        Menu {
                            Button("None") {
                                tableGameAssignments.removeValue(forKey: tableNum)
                            }
                            ForEach(games) { game in
                                if !alreadyAssignedIds.contains(game.id) {
                                    Button {
                                        tableGameAssignments[tableNum] = game.id
                                    } label: {
                                        HStack {
                                            Text(game.gameName)
                                            if tableGameAssignments[tableNum] == game.id {
                                                Image(systemName: "checkmark")
                                            }
                                        }
                                    }
                                }
                            }
                        } label: {
                            HStack {
                                if let gameId = assignedGameId,
                                   let game = games.first(where: { $0.id == gameId }) {
                                    gameThumb(game)
                                    Text(game.gameName)
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Spacer()
                                    voteCount(game)
                                } else {
                                    Image(systemName: "plus.circle.dashed")
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                    Text("Assign a game...")
                                        .font(.md3BodyMedium)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                    Spacer()
                                }
                                Image(systemName: "chevron.up.chevron.down")
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            .padding(10)
                            .background(assignedGameId != nil ? Color.purple.opacity(0.06) : Color.md3SurfaceContainerHigh)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                        }
                    }
                }

                let assigned = tableGameAssignments.count
                let total = session.tableCount ?? 2
                Text("\(assigned) of \(total) tables assigned")
                    .font(.md3LabelSmall)
                    .foregroundStyle(assigned == total ? .green : Color.md3OnSurfaceVariant)
            }
        }
    }

    // MARK: - Finalize Helpers

    @ViewBuilder
    private func finalizeValidation(_ session: PlanningSession) -> some View {
        let issues = finalizeIssues(session)
        if !issues.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                ForEach(issues, id: \.self) { issue in
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                        Text(issue)
                            .font(.md3BodySmall)
                            .foregroundStyle(.orange)
                    }
                }
            }
            .padding(10)
            .background(Color.orange.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
        }
    }

    private func finalizeIssues(_ session: PlanningSession) -> [String] {
        var issues: [String] = []
        if selectedDateId == nil {
            issues.append("Select a date above")
        }
        if isMultiTable {
            if tableGameAssignments.isEmpty {
                issues.append("Assign at least one game to a table")
            }
        }
        return issues
    }

    private func canFinalize(_ session: PlanningSession) -> Bool {
        if selectedDateId == nil { return false }
        if isMultiTable && tableGameAssignments.isEmpty { return false }
        return true
    }

    private func gameThumb(_ game: GameSuggestion) -> some View {
        Group {
            if let url = game.thumbnailUrl, let imageURL = URL(string: url) {
                AsyncImage(url: imageURL) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Color.md3SurfaceVariant
                }
                .frame(width: 32, height: 32)
                .clipShape(RoundedRectangle(cornerRadius: 6))
            }
        }
    }

    private func voteCount(_ game: GameSuggestion) -> some View {
        HStack(spacing: 2) {
            Image(systemName: "hand.thumbsup.fill")
                .font(.system(size: 10))
            Text("\(game.voteCount)")
        }
        .font(.md3LabelSmall)
        .foregroundStyle(Color.md3OnSurfaceVariant)
    }

    private func eventModeButton(icon: String, title: String, subtitle: String, isActive: Bool, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: MD3Shape.medium)
                        .fill(isActive ? color.opacity(0.12) : Color.md3SurfaceContainerHigh)
                        .overlay(
                            RoundedRectangle(cornerRadius: MD3Shape.medium)
                                .stroke(isActive ? color : Color.clear, lineWidth: 2)
                        )
                    VStack(spacing: 6) {
                        Image(systemName: icon)
                            .font(.system(size: 22))
                            .foregroundStyle(isActive ? color : Color.md3OnSurfaceVariant)
                        Text(title)
                            .font(.md3LabelLarge)
                            .fontWeight(.medium)
                            .foregroundStyle(isActive ? color : Color.md3OnSurface)
                        Text(subtitle)
                            .font(.system(size: 10))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        if isActive {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 14))
                                .foregroundStyle(color)
                        }
                    }
                }
                .frame(height: 110)
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: - Chat

    @State private var showChat = false

    @ViewBuilder
    private var planningChatSection: some View {
        if TierConfig.hasFeature(creatorTier, feature: \.chat) {
            VStack(alignment: .leading, spacing: 0) {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        showChat.toggle()
                    }
                } label: {
                    HStack {
                        HStack(spacing: 8) {
                            Image(systemName: "bubble.left.and.bubble.right.fill")
                                .font(.system(size: 16))
                                .foregroundStyle(Color.md3Primary)
                            Text("Planning Chat")
                                .font(.md3TitleMedium)
                                .fontWeight(.semibold)
                                .foregroundStyle(Color.md3OnSurface)
                        }
                        Spacer()
                        Image(systemName: "chevron.down")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .rotationEffect(.degrees(showChat ? 180 : 0))
                    }
                    .padding()
                }
                .buttonStyle(.plain)

                if showChat {
                    Divider().foregroundStyle(Color.md3OutlineVariant)
                    ChatPanelView(contextType: "planning", contextId: sessionId)
                        .frame(height: 400)
                }
            }
            .cardStyle()
            .padding(.horizontal)
        } else {
            HStack(spacing: 8) {
                Image(systemName: "lock.fill")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Text("Chat requires the session host to have a Basic or higher subscription")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .cardStyle()
            .padding(.horizontal)
        }
    }

    // MARK: - Step Navigation

    private func stepNav(back: Int?, forward: Int?) -> some View {
        HStack {
            if let back {
                Button {
                    withAnimation { currentStep = back }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("Back")
                    }
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }
            Spacer()
            if let forward, forward < stepCount {
                let labels = isCreator
                    ? ["Dates", "Games", "Items", "People", "Finalize"]
                    : ["Dates", "Games", "Items", "People"]
                Button {
                    withAnimation { currentStep = forward }
                } label: {
                    HStack(spacing: 4) {
                        Text("Next: \(labels[forward])")
                        Image(systemName: "chevron.right")
                    }
                    .font(.md3LabelLarge)
                    .foregroundStyle(Color.md3Primary)
                }
            }
        }
        .padding(.horizontal)
    }
}
