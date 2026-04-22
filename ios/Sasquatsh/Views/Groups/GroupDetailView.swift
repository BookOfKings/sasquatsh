import SwiftUI

struct GroupDetailView: View {
    let groupId: String
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @Environment(\.dismiss) private var dismiss
    @State private var vm = GroupDetailViewModel()
    @State private var showEditGroup = false
    @State private var showDeleteConfirm = false
    @State private var showCreatePlanning = false
    @State private var showJoinRequestMessage = false
    @State private var joinRequestMessage = ""
    @State private var recurringGamesVM = RecurringGamesViewModel()
    @State private var showTransferConfirm = false
    @State private var transferTargetId: String?
    @State private var transferTargetName = ""
    @State private var showInviteSearch = false
    @State private var showShareLink = false
    @State private var showChat = false
    @State private var adminTab = 0 // 0=members, 1=requests, 2=invites

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.group == nil {
                LoadingView()
            } else if let group = vm.group {
                VStack(alignment: .leading, spacing: 16) {
                    // 1. Header card
                    headerSection(group)

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

                    // Join/leave actions (inside header card on website, but fine here)
                    joinActions(group)

                    // 2. Upcoming Games card (members only)
                    if isMember {
                        upcomingGamesSection
                    }

                    // 3. Game Planning card (members only)
                    if isMember {
                        planningSection
                    }

                    // 4. Group Chat card (members only, collapsible)
                    if isMember {
                        groupChatSection
                    }

                    // 5. Recurring Games card
                    recurringGamesSection

                    // 6. Members card (for non-admins) or Admin Panel (for admins)
                    if isAdmin {
                        adminPanelSection
                    } else if isMember {
                        membersSection
                    }

                    // 7. Leave Group (non-owners only, at bottom)
                    if isMember && !isOwner {
                        Button {
                            Task { await vm.leave() }
                        } label: {
                            HStack {
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                Text("Leave Group")
                            }
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Error)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color.md3Error.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .overlay(
                                RoundedRectangle(cornerRadius: MD3Shape.medium)
                                    .stroke(Color.md3Error.opacity(0.3), lineWidth: 1)
                            )
                        }
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle(vm.group?.name ?? "Group")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if isAdmin {
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button { showShareLink = true } label: {
                            Label("Share Invite Link", systemImage: "qrcode")
                        }
                        Button { showEditGroup = true } label: {
                            Label("Edit", systemImage: "pencil")
                        }
                        Button { showCreatePlanning = true } label: {
                            Label("New Planning Session", systemImage: "calendar.badge.plus")
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
        .sheet(isPresented: $showEditGroup, onDismiss: {
            Task { await vm.loadGroup(id: groupId) }
        }) {
            if let group = vm.group {
                EditGroupView(group: group)
            }
        }
        .sheet(isPresented: $showCreatePlanning, onDismiss: {
            Task { await vm.loadGroup(id: groupId) }
        }) {
            CreatePlanningView(groupId: groupId, members: vm.members)
        }
        .alert("Delete Group", isPresented: $showDeleteConfirm) {
            Button("Delete", role: .destructive) {
                Task {
                    if await vm.deleteGroup() {
                        dismiss()
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure? This cannot be undone.")
        }
        .alert("Transfer Ownership", isPresented: $showTransferConfirm) {
            Button("Transfer", role: .destructive) {
                if let targetId = transferTargetId {
                    Task { await vm.transferOwnership(to: targetId) }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Transfer ownership to \(transferTargetName)? You will be demoted to admin.")
        }
        .sheet(isPresented: $showInviteSearch) {
            UserSearchInviteSheet(groupId: groupId)
        }
        .sheet(isPresented: $showShareLink) {
            ShareInviteLinkSheet(
                groupId: groupId,
                linkType: "group_recurring",
                title: "Share Group Invite"
            )
        }
        .alert("Request to Join", isPresented: $showJoinRequestMessage) {
            TextField("Message (optional)", text: $joinRequestMessage)
            Button("Send Request") {
                Task { await vm.requestToJoin(message: joinRequestMessage.isEmpty ? nil : joinRequestMessage) }
            }
            Button("Cancel", role: .cancel) {}
        }
        .refreshable { await vm.loadGroup(id: groupId) }
        .task {
            vm.configure(services: services)
            await vm.loadGroup(id: groupId)
        }
    }

    private var isAdmin: Bool {
        guard let userId = authVM.user?.id else { return false }
        return vm.isAdmin(userId: userId)
    }

    private var isOwner: Bool {
        guard let userId = authVM.user?.id else { return false }
        return vm.userRole(userId: userId) == .owner
    }

    private var isMember: Bool {
        guard let userId = authVM.user?.id else { return false }
        return vm.userRole(userId: userId) != nil
    }

    // MARK: - Sections

    private func headerSection(_ group: GameGroup) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            // Top row: Logo + Name + Action buttons
            HStack(alignment: .top, spacing: 14) {
                // Group logo
                if let logoUrl = group.logoUrl, let url = URL(string: logoUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        groupPlaceholderIcon
                    }
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                } else {
                    groupPlaceholderIcon
                }

                VStack(alignment: .leading, spacing: 4) {
                    // Name row with action buttons
                    HStack(alignment: .top) {
                        Text(group.name)
                            .font(.md3HeadlineMedium)
                            .foregroundStyle(Color.md3OnSurface)
                        Spacer()
                        if isMember {
                            Button { showShareLink = true } label: {
                                Image(systemName: "square.and.arrow.up")
                                    .font(.system(size: 16))
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                    .frame(width: 32, height: 32)
                                    .background(Color.md3SurfaceContainerHigh)
                                    .clipShape(Circle())
                            }
                        }
                        if isAdmin {
                            Button { showEditGroup = true } label: {
                                Image(systemName: "pencil")
                                    .font(.system(size: 14))
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                    .frame(width: 32, height: 32)
                                    .background(Color.md3SurfaceContainerHigh)
                                    .clipShape(Circle())
                            }
                        }
                    }

                    // Type + Creator
                    HStack(spacing: 4) {
                        Text(group.groupType.displayName)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        if let creator = group.creator {
                            Text("·")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("by \(creator.displayName ?? creator.username ?? "Unknown")")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                    .font(.md3BodySmall)
                }
            }

            // Badge chips row
            FlowLayout(spacing: 8) {
                // Member count
                chipBadge(
                    icon: "person.2.fill",
                    text: "\(group.memberCount ?? vm.members.count) \(group.memberCount ?? vm.members.count == 1 ? "member" : "members")",
                    bgColor: Color.md3SurfaceContainerHigh
                )

                // Join policy
                switch group.joinPolicy {
                case .open:
                    chipBadge(icon: "globe", text: "Open", bgColor: Color.green.opacity(0.15), fgColor: .green)
                case .request:
                    chipBadge(icon: "hand.raised.fill", text: "Request to Join", bgColor: Color.yellow.opacity(0.15), fgColor: .orange)
                case .invite_only:
                    chipBadge(icon: "lock.fill", text: "Invite Only", bgColor: Color.orange.opacity(0.15), fgColor: .orange)
                }

                // User role (if member)
                if let userId = authVM.user?.id, let role = vm.userRole(userId: userId) {
                    switch role {
                    case .owner:
                        chipBadge(icon: "crown.fill", text: "Owner", bgColor: Color.purple.opacity(0.15), fgColor: .purple)
                    case .admin:
                        chipBadge(icon: "shield.fill", text: "Admin", bgColor: Color.blue.opacity(0.15), fgColor: .blue)
                    case .member:
                        chipBadge(icon: "person.fill", text: "Member", bgColor: Color.green.opacity(0.15), fgColor: .green)
                    }
                }
            }

            // Location
            if let city = group.locationCity, !city.isEmpty {
                HStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.md3OnSurfaceVariant)

                    let parts = [city, group.locationState].compactMap { $0 }.filter { !$0.isEmpty }
                    Text(parts.joined(separator: ", "))
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)

                    if let radius = group.locationRadiusMiles, radius > 0 {
                        Text("· \(radius) mi radius")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                    }
                }
            }

            // Description
            if let description = group.description, !description.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("About")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3OnSurface)
                    Text(description)
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private var groupPlaceholderIcon: some View {
        RoundedRectangle(cornerRadius: 14)
            .fill(Color.md3PrimaryContainer)
            .frame(width: 64, height: 64)
            .overlay {
                Image(systemName: "person.3.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.md3OnPrimaryContainer)
            }
    }

    private func chipBadge(icon: String, text: String, bgColor: Color, fgColor: Color = .md3OnSurfaceVariant) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 11))
            Text(text)
                .font(.md3LabelSmall)
        }
        .foregroundStyle(fgColor)
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(bgColor)
        .clipShape(Capsule())
    }

    @ViewBuilder
    private func joinActions(_ group: GameGroup) -> some View {
        if !isMember {
            VStack(spacing: 8) {
                switch group.joinPolicy {
                case .open:
                    Button {
                        Task { await vm.join() }
                    } label: {
                        HStack {
                            Image(systemName: "person.badge.plus")
                            Text("Join Group")
                        }
                        .primaryButtonStyle()
                    }
                case .request:
                    Button {
                        showJoinRequestMessage = true
                    } label: {
                        HStack {
                            Image(systemName: "envelope.fill")
                            Text("Request to Join")
                        }
                        .primaryButtonStyle()
                    }
                case .invite_only:
                    HStack(spacing: 6) {
                        Image(systemName: "lock.fill")
                            .font(.md3BodySmall)
                        Text("Invitation required to join this group")
                            .font(.md3BodyMedium)
                    }
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .padding(12)
                    .frame(maxWidth: .infinity)
                    .background(Color.md3SurfaceContainerHigh)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }
            }
            .padding(.horizontal)
        }
    }

    // MARK: - Upcoming Games

    private var upcomingGamesSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header
            sectionHeader(icon: "dice.fill", title: "Upcoming Games")

            Divider().foregroundStyle(Color.md3OutlineVariant)

            if vm.groupEvents.isEmpty {
                Text("No upcoming games scheduled for this group yet.")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .padding(.horizontal)
            } else {
                VStack(spacing: 0) {
                    ForEach(vm.groupEvents.prefix(5)) { event in
                        NavigationLink {
                            EventDetailView(eventId: event.id)
                        } label: {
                            HStack(spacing: 12) {
                                // Game thumbnail
                                if let urlStr = event.primaryGameThumbnail, let url = URL(string: urlStr) {
                                    AsyncImage(url: url) { image in
                                        image.resizable().aspectRatio(contentMode: .fill)
                                    } placeholder: {
                                        Color.md3SurfaceVariant
                                    }
                                    .frame(width: 40, height: 40)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                                } else {
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(Color.md3PrimaryContainer)
                                        .frame(width: 40, height: 40)
                                        .overlay {
                                            Image(systemName: "dice.fill")
                                                .font(.system(size: 16))
                                                .foregroundStyle(Color.md3OnPrimaryContainer)
                                        }
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(event.title)
                                        .font(.md3BodyMedium)
                                        .fontWeight(.medium)
                                        .foregroundStyle(Color.md3OnSurface)
                                        .lineLimit(1)
                                    Text(event.eventDate.toDate?.displayDate ?? event.eventDate)
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }

                                Spacer()

                                if let maxPlayers = event.maxPlayers {
                                    Text("\(event.confirmedCount)/\(maxPlayers)")
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.5))
                            }
                            .padding(.horizontal)
                            .padding(.vertical, 10)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Game Planning

    private var planningSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                sectionHeaderContent(icon: "calendar.badge.plus", title: "Game Planning")
                Spacer()
                if isAdmin {
                    Button {
                        showCreatePlanning = true
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                                .font(.system(size: 12, weight: .semibold))
                            Text("Host a Game")
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
            .padding()

            Divider().foregroundStyle(Color.md3OutlineVariant)

            let openSessions = vm.planningSessions.filter { $0.status == .open }
            if openSessions.isEmpty {
                VStack(spacing: 4) {
                    Text("No active planning sessions.")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    if isAdmin {
                        Text("Click \"Host a Game\" to start coordinating your next game night.")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.7))
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
                .padding(.horizontal)
            } else {
                VStack(spacing: 0) {
                    ForEach(openSessions) { session in
                        NavigationLink {
                            PlanningSessionDetailView(sessionId: session.id)
                        } label: {
                            HStack(spacing: 12) {
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(Color.md3PrimaryContainer)
                                    .frame(width: 40, height: 40)
                                    .overlay {
                                        Image(systemName: "calendar")
                                            .font(.system(size: 16))
                                            .foregroundStyle(Color.md3OnPrimaryContainer)
                                    }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(session.title)
                                        .font(.md3BodyMedium)
                                        .fontWeight(.medium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Text("Deadline: \(session.responseDeadline.toDate?.displayDate ?? session.responseDeadline)")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }

                                Spacer()

                                BadgeView(text: "Open", color: .md3TertiaryContainer)

                                Image(systemName: "chevron.right")
                                    .font(.system(size: 12))
                                    .foregroundStyle(Color.md3OnSurfaceVariant.opacity(0.5))
                            }
                            .padding(.horizontal)
                            .padding(.vertical, 10)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Group Chat

    @ViewBuilder
    private var groupChatSection: some View {
        let tier = authVM.user?.effectiveTier ?? .free
        if TierConfig.hasFeature(tier, feature: \.chat) {
            VStack(alignment: .leading, spacing: 0) {
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        showChat.toggle()
                    }
                } label: {
                    HStack {
                        sectionHeaderContent(icon: "bubble.left.and.bubble.right.fill", title: "Group Chat")
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
                    ChatPanelView(contextType: "group", contextId: groupId)
                        .frame(height: 400)
                }
            }
            .cardStyle()
            .padding(.horizontal)
        } else {
            VStack(spacing: 12) {
                Image(systemName: "lock.fill")
                    .font(.title2)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Text("Upgrade to Basic+ to chat")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
            .cardStyle()
            .padding(.horizontal)
        }
    }

    // MARK: - Recurring Games

    private var recurringGamesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            if isAdmin {
                HStack {
                    Text("Recurring Games")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                    Spacer()
                    let tier = authVM.user?.effectiveTier ?? .free
                    if TierConfig.canCreateRecurringGame(tier, currentCount: recurringGamesVM.games.count) {
                        Button {
                            recurringGamesVM.editingGame = nil
                            recurringGamesVM.showForm = true
                        } label: {
                            Image(systemName: "plus.circle")
                                .foregroundStyle(Color.md3Primary)
                        }
                    } else if !TierConfig.hasFeature(tier, feature: \.recurringGames) {
                        HStack(spacing: 4) {
                            Image(systemName: "lock.fill")
                                .font(.md3LabelSmall)
                            Text("Upgrade")
                                .font(.md3LabelSmall)
                        }
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
                .padding(.horizontal)
            }

            if recurringGamesVM.isLoading && recurringGamesVM.games.isEmpty {
                LoadingView()
            } else if recurringGamesVM.games.isEmpty {
                Text("No recurring games scheduled")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else {
                ForEach(recurringGamesVM.games) { game in
                    RecurringGameCard(
                        game: game,
                        isAdmin: isAdmin,
                        onEdit: {
                            recurringGamesVM.editingGame = game
                            recurringGamesVM.showForm = true
                        },
                        onToggleActive: {
                            Task { await recurringGamesVM.toggleActive(game: game) }
                        },
                        onDelete: {
                            recurringGamesVM.deletingGame = game
                            recurringGamesVM.showDeleteConfirm = true
                        }
                    )
                    .padding(.horizontal)
                }
            }

            if let error = recurringGamesVM.error {
                Text(error)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3Error)
                    .padding(.horizontal)
            }
        }
        .sheet(isPresented: $recurringGamesVM.showForm) {
            RecurringGameFormSheet(
                groupId: groupId,
                game: recurringGamesVM.editingGame
            ) { createInput, updateInput in
                Task {
                    if let createInput {
                        await recurringGamesVM.createGame(input: createInput)
                    } else if let updateInput, let id = recurringGamesVM.editingGame?.id {
                        await recurringGamesVM.updateGame(id: id, input: updateInput)
                    }
                }
            }
        }
        .alert("Delete Recurring Game", isPresented: $recurringGamesVM.showDeleteConfirm) {
            Toggle("Also delete future events", isOn: $recurringGamesVM.deleteFutureEvents)
            Button("Delete", role: .destructive) {
                Task { await recurringGamesVM.deleteGame() }
            }
            Button("Cancel", role: .cancel) {
                recurringGamesVM.deletingGame = nil
            }
        } message: {
            Text("Are you sure you want to delete this recurring game?")
        }
        .task {
            recurringGamesVM.configure(services: services, groupId: groupId)
            await recurringGamesVM.loadGames()
        }
    }

    // MARK: - Members (non-admin view)

    private var membersSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            sectionHeader(icon: "person.3.fill", title: "Members (\(vm.members.count))")

            Divider().foregroundStyle(Color.md3OutlineVariant)

            VStack(spacing: 0) {
                ForEach(vm.members) { member in
                    memberRow(member)
                }
            }
        }
        .cardStyle()
        .padding(.horizontal)
    }

    private func memberRow(_ member: GroupMember) -> some View {
        HStack(spacing: 12) {
            UserAvatarView(url: member.avatarUrl, name: member.displayName, size: 40, userId: member.userId)
            VStack(alignment: .leading, spacing: 2) {
                Text(member.displayName ?? "Unknown")
                    .font(.md3BodyMedium)
                    .fontWeight(.medium)
                    .foregroundStyle(Color.md3OnSurface)
                Text("Joined \(member.joinedAt.toDate?.displayDate ?? "")")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            Spacer()

            roleBadge(member.role)

            // Admin actions
            if isAdmin && member.userId != authVM.user?.id && member.role != .owner {
                Menu {
                    if member.role == .member {
                        Button("Make Admin") {
                            Task { await vm.changeRole(userId: member.userId, to: .admin) }
                        }
                    } else if member.role == .admin {
                        Button("Remove Admin") {
                            Task { await vm.changeRole(userId: member.userId, to: .member) }
                        }
                    }
                    if isOwner {
                        Button { [name = member.displayName ?? "this member"] in
                            transferTargetId = member.userId
                            transferTargetName = name
                            showTransferConfirm = true
                        } label: {
                            Label("Transfer Ownership", systemImage: "arrow.left.arrow.right")
                        }
                    }
                    Button("Remove", role: .destructive) {
                        Task { await vm.removeMember(userId: member.userId) }
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .frame(width: 28, height: 28)
                }
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
    }

    private func roleBadge(_ role: MemberRole) -> some View {
        Text(role.displayName)
            .font(.md3LabelSmall)
            .foregroundStyle(role == .owner ? .purple : role == .admin ? .blue : Color.md3OnSurfaceVariant)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(
                role == .owner ? Color.purple.opacity(0.12) :
                role == .admin ? Color.blue.opacity(0.12) :
                Color.md3SurfaceContainerHigh
            )
            .clipShape(Capsule())
    }

    // MARK: - Admin Panel (tabbed: Members, Requests, Invitations)

    private var adminPanelSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Tab bar
            HStack(spacing: 0) {
                adminTabButton("Members (\(vm.members.count))", tag: 0)
                adminTabButton("Requests\(vm.joinRequests.isEmpty ? "" : " (\(vm.joinRequests.count))")", tag: 1)
                adminTabButton("Invitations", tag: 2)
            }

            Divider().foregroundStyle(Color.md3OutlineVariant)

            // Tab content
            switch adminTab {
            case 0: adminMembersTab
            case 1: adminRequestsTab
            case 2: adminInvitationsTab
            default: EmptyView()
            }
        }
        .cardStyle()
        .padding(.horizontal)
        .task {
            await vm.loadJoinRequests()
            await vm.loadInvitations()
        }
    }

    private func adminTabButton(_ title: String, tag: Int) -> some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                adminTab = tag
            }
        } label: {
            Text(title)
                .font(.md3LabelMedium)
                .foregroundStyle(adminTab == tag ? Color.md3Primary : Color.md3OnSurfaceVariant)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .overlay(alignment: .bottom) {
                    if adminTab == tag {
                        Rectangle()
                            .fill(Color.md3Primary)
                            .frame(height: 2)
                    }
                }
        }
        .buttonStyle(.plain)
    }

    private var adminMembersTab: some View {
        VStack(spacing: 0) {
            ForEach(vm.members) { member in
                memberRow(member)
            }
        }
    }

    private var adminRequestsTab: some View {
        VStack(alignment: .leading, spacing: 0) {
            if vm.joinRequests.isEmpty {
                Text("No pending requests")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
            } else {
                ForEach(vm.joinRequests) { request in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 12) {
                            UserAvatarView(url: request.avatarUrl, name: request.displayName, size: 40)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(request.displayName ?? "Unknown")
                                    .font(.md3BodyMedium)
                                    .fontWeight(.medium)
                                Text("Requested \(request.createdAt.toDate?.displayDate ?? "")")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            Spacer()
                            Button {
                                Task { await vm.approveRequest(userId: request.userId) }
                            } label: {
                                Text("Approve")
                                    .font(.md3LabelMedium)
                                    .foregroundStyle(Color.md3OnPrimary)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.md3Primary)
                                    .clipShape(Capsule())
                            }
                            Button {
                                Task { await vm.rejectRequest(userId: request.userId) }
                            } label: {
                                Text("Reject")
                                    .font(.md3LabelMedium)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.md3SurfaceContainerHigh)
                                    .clipShape(Capsule())
                            }
                        }
                        if let message = request.message, !message.isEmpty {
                            Text("\"\(message)\"")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                .padding(8)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(Color.md3SurfaceContainerHigh)
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.extraSmall))
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 10)
                }
            }
        }
    }

    private var adminInvitationsTab: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Create invitation button
            Button {
                Task {
                    if let invitation = await vm.createInvitation() {
                        let url = "https://\(AppConfig.webDomain)/groups/invite/\(invitation.inviteCode)"
                        UIPasteboard.general.string = url
                        vm.actionMessage = "Invite link copied!"
                    }
                }
            } label: {
                HStack {
                    Image(systemName: "plus")
                    Text("Create Invitation Link")
                }
                .primaryButtonStyle()
            }

            if vm.invitations.isEmpty {
                Text("No active invitations")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            } else {
                ForEach(vm.invitations) { invitation in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(invitation.inviteCode)
                                .font(.system(.caption, design: .monospaced))
                            Text("Uses: \(invitation.usesCount)\(invitation.maxUses.map { "/\($0)" } ?? "")")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            if let expires = invitation.expiresAt {
                                Text("Expires: \(expires.toDate?.displayDate ?? expires)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        Spacer()
                        Button {
                            let url = "https://\(AppConfig.webDomain)/groups/invite/\(invitation.inviteCode)"
                            UIPasteboard.general.string = url
                            vm.actionMessage = "Link copied!"
                        } label: {
                            Text("Copy")
                                .font(.md3LabelMedium)
                                .foregroundStyle(Color.md3OnSecondaryContainer)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(Color.md3SecondaryContainer)
                                .clipShape(Capsule())
                        }
                        Button {
                            Task { await vm.revokeInvitation(invitation.id) }
                        } label: {
                            Image(systemName: "trash")
                                .font(.system(size: 14))
                                .foregroundStyle(Color.md3Error)
                                .frame(width: 28, height: 28)
                        }
                    }
                    .padding(10)
                    .background(Color.md3SurfaceContainerHigh)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                }
            }

            Button {
                showInviteSearch = true
            } label: {
                Label("Invite by Username", systemImage: "person.badge.plus")
                    .font(.md3LabelLarge)
                    .frame(maxWidth: .infinity)
                    .frame(height: 36)
                    .background(Color.md3SecondaryContainer)
                    .foregroundStyle(Color.md3OnSecondaryContainer)
                    .clipShape(Capsule())
            }
        }
        .padding()
    }

    // MARK: - Helpers

    private func sectionHeader(icon: String, title: String) -> some View {
        sectionHeaderContent(icon: icon, title: title)
            .padding()
    }

    private func sectionHeaderContent(icon: String, title: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundStyle(Color.md3Primary)
            Text(title)
                .font(.md3TitleMedium)
                .fontWeight(.semibold)
                .foregroundStyle(Color.md3OnSurface)
        }
    }
}
