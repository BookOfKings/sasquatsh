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
    @State private var selectedTab = 0
    @State private var recurringGamesVM = RecurringGamesViewModel()
    @State private var showTransferConfirm = false
    @State private var transferTargetId: String?
    @State private var transferTargetName = ""
    @State private var showInviteSearch = false

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.group == nil {
                LoadingView()
            } else if let group = vm.group {
                VStack(alignment: .leading, spacing: 20) {
                    headerSection(group)

                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    }

                    if let msg = vm.actionMessage {
                        Text(msg)
                            .font(.md3BodyMedium)
                            .foregroundStyle(.green)
                            .padding(.horizontal)
                    }

                    joinActions(group)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            groupTabButton("Members", tag: 0)
                            groupTabButton("Chat", tag: 1)
                            groupTabButton("Games", tag: 2)
                            groupTabButton("Planning", tag: 3)
                            if isAdmin { groupTabButton("Requests", tag: 4) }
                            if isAdmin { groupTabButton("Invites", tag: 5) }
                        }
                        .padding(.horizontal)
                    }

                    switch selectedTab {
                    case 0: membersSection
                    case 1: groupChatSection
                    case 2: recurringGamesSection
                    case 3: planningSection
                    case 4: joinRequestsSection
                    case 5: invitationsSection
                    default: EmptyView()
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
        .sheet(isPresented: $showEditGroup) {
            if let group = vm.group {
                EditGroupView(group: group)
            }
        }
        .sheet(isPresented: $showCreatePlanning) {
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

    private func groupTabButton(_ title: String, tag: Int) -> some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedTab = tag
            }
        } label: {
            Text(title)
                .font(.md3LabelLarge)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(selectedTab == tag ? Color.md3Primary : Color.md3SurfaceContainerHigh)
                .foregroundStyle(selectedTab == tag ? Color.md3OnPrimary : Color.md3OnSurfaceVariant)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Sections

    private func headerSection(_ group: GameGroup) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                if let logoUrl = group.logoUrl, let url = URL(string: logoUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        RoundedRectangle(cornerRadius: MD3Shape.medium)
                            .fill(Color.md3PrimaryContainer)
                    }
                    .frame(width: 60, height: 60)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(group.name)
                        .font(.md3HeadlineMedium)
                    HStack(spacing: 8) {
                        BadgeView(text: group.groupType.displayName, color: .md3TertiaryContainer)
                        BadgeView(text: group.joinPolicy.displayName, color: .md3PrimaryContainer)
                    }
                }
            }

            if let description = group.description, !description.isEmpty {
                Text(description)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            HStack(spacing: 16) {
                if let city = group.locationCity, let state = group.locationState {
                    Label("\(city), \(state)", systemImage: "mappin")
                        .font(.md3BodySmall)
                }
                Label("\(group.memberCount ?? vm.members.count) members", systemImage: "person.3")
                    .font(.md3BodySmall)
            }
        }
        .padding(.horizontal)
    }

    private func joinActions(_ group: GameGroup) -> some View {
        Group {
            if !isMember {
                switch group.joinPolicy {
                case .open:
                    Button {
                        Task { await vm.join() }
                    } label: {
                        Text("Join Group")
                            .primaryButtonStyle()
                    }
                    .padding(.horizontal)
                case .request:
                    Button {
                        showJoinRequestMessage = true
                    } label: {
                        Text("Request to Join")
                            .primaryButtonStyle()
                    }
                    .padding(.horizontal)
                case .invite_only:
                    Text("This group is invite only")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .padding(.horizontal)
                }
            } else if !isAdmin {
                Button {
                    Task { await vm.leave() }
                } label: {
                    Text("Leave Group")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3Error)
                }
                .padding(.horizontal)
            }
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
                    let tier = authVM.user?.subscriptionTier ?? .free
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

    @ViewBuilder
    private var groupChatSection: some View {
        let tier = authVM.user?.subscriptionTier ?? .free
        if TierConfig.hasFeature(tier, feature: \.chat) {
            ChatPanelView(contextType: "group", contextId: groupId)
                .frame(height: 500)
                .padding(.horizontal)
        } else {
            VStack(spacing: 12) {
                Image(systemName: "lock.fill")
                    .font(.title2)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Text("Upgrade to Basic to chat")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 40)
        }
    }

    private var membersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(vm.members) { member in
                HStack {
                    UserAvatarView(url: member.avatarUrl, name: member.displayName, size: 36, userId: member.userId)
                    VStack(alignment: .leading) {
                        Text(member.displayName ?? "Member")
                            .font(.md3BodyMedium)
                        Text(member.role.displayName)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    Spacer()

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
                                Button("Transfer Ownership") {
                                    transferTargetId = member.userId
                                    transferTargetName = member.displayName ?? "this member"
                                    showTransferConfirm = true
                                }
                            }
                            Button("Remove", role: .destructive) {
                                Task { await vm.removeMember(userId: member.userId) }
                            }
                        } label: {
                            Image(systemName: "ellipsis")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private var planningSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Planning Sessions")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                if isMember {
                    Button {
                        showCreatePlanning = true
                    } label: {
                        Image(systemName: "plus.circle")
                            .foregroundStyle(Color.md3Primary)
                    }
                }
            }

            if vm.planningSessions.isEmpty {
                Text("No planning sessions")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                ForEach(vm.planningSessions) { session in
                    NavigationLink {
                        PlanningSessionDetailView(sessionId: session.id)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(session.title)
                                    .font(.md3BodyMedium)
                                    .fontWeight(.medium)
                                Text("Deadline: \(session.responseDeadline.toDate?.displayDate ?? session.responseDeadline)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            Spacer()
                            BadgeView(text: session.status.displayName, color: session.status == .open ? .md3TertiaryContainer : .md3PrimaryContainer)
                        }
                        .padding(.vertical, 4)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    private var joinRequestsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Join Requests")
                .font(.md3TitleMedium)
                .foregroundStyle(Color.md3OnSurface)

            if vm.joinRequests.isEmpty {
                Text("No pending requests")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                ForEach(vm.joinRequests) { request in
                    HStack {
                        UserAvatarView(url: request.avatarUrl, name: request.displayName, size: 36)
                        VStack(alignment: .leading) {
                            Text(request.displayName ?? "User")
                                .font(.md3BodyMedium)
                            if let message = request.message {
                                Text(message)
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        Spacer()
                        Button {
                            Task { await vm.approveRequest(userId: request.userId) }
                        } label: {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                        }
                        Button {
                            Task { await vm.rejectRequest(userId: request.userId) }
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(Color.md3Error)
                        }
                    }
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
        .task { await vm.loadJoinRequests() }
    }

    private var invitationsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Invitations")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                Button {
                    Task {
                        if let invitation = await vm.createInvitation() {
                            let url = "https://\(AppConfig.webDomain)/groups/invite/\(invitation.inviteCode)"
                            UIPasteboard.general.string = url
                        }
                    }
                } label: {
                    Label("Create Link", systemImage: "link.badge.plus")
                        .font(.md3LabelSmall)
                }
            }

            if vm.invitations.isEmpty {
                Text("No active invitations")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else {
                ForEach(vm.invitations) { invitation in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(invitation.inviteCode)
                                .font(.system(.caption, design: .monospaced))
                            Text("Used \(invitation.usesCount)/\(invitation.maxUses ?? 0 == 0 ? "unlimited" : String(invitation.maxUses!))")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        Spacer()
                        Button {
                            let url = "https://\(AppConfig.webDomain)/groups/invite/\(invitation.inviteCode)"
                            UIPasteboard.general.string = url
                        } label: {
                            Image(systemName: "doc.on.doc")
                                .font(.md3BodySmall)
                        }
                        Button {
                            Task { await vm.revokeInvitation(invitation.id) }
                        } label: {
                            Image(systemName: "trash")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3Error)
                        }
                    }
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
        .cardStyle()
        .padding(.horizontal)
        .task { await vm.loadInvitations() }
    }
}
