import SwiftUI

struct DashboardView: View {
    var switchTab: ((Int) -> Void)?

    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = DashboardViewModel()
    @State private var raffleVM = RaffleViewModel()
    @State private var showCreateEvent = false

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.registeredEvents.isEmpty && vm.myGroups.isEmpty {
                LoadingView()
            } else {
                VStack(spacing: 16) {
                    // User header
                    userHeader

                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    }

                    // Pending Group Invitations
                    if !vm.pendingInvitations.isEmpty {
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                Image(systemName: "envelope.badge.fill")
                                    .foregroundStyle(Color.md3Tertiary)
                                Text("Group Invitations")
                                    .font(.md3TitleMedium)
                                    .foregroundStyle(Color.md3OnSurface)
                            }

                            ForEach(vm.pendingInvitations) { invite in
                                HStack(spacing: 10) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(invite.group?.name ?? "Group")
                                            .font(.md3TitleSmall)
                                            .foregroundStyle(Color.md3OnSurface)
                                        if let invitedBy = invite.invitedBy {
                                            Text("Invited by \(invitedBy.displayName ?? "someone")")
                                                .font(.md3BodySmall)
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                        }
                                    }

                                    Spacer()

                                    Button {
                                        Task { await vm.respondToInvitation(invite, accept: false) }
                                    } label: {
                                        Text("Decline")
                                            .font(.md3LabelLarge)
                                            .foregroundStyle(Color.md3OnSurfaceVariant)
                                    }

                                    Button {
                                        Task { await vm.respondToInvitation(invite, accept: true) }
                                    } label: {
                                        Text("Accept")
                                            .font(.md3LabelLarge)
                                            .padding(.horizontal, 14)
                                            .frame(height: 32)
                                            .background(Color.md3Primary)
                                            .foregroundStyle(Color.md3OnPrimary)
                                            .clipShape(Capsule())
                                    }
                                }
                                .padding(12)
                                .background(Color.md3TertiaryContainer.opacity(0.3))
                                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            }
                        }
                        .padding()
                        .cardStyle()
                    }

                    // My Upcoming Games
                    dashboardSection(
                        title: "My Upcoming Games",
                        icon: "calendar.badge.clock",
                        items: vm.registeredEvents,
                        emptyMessage: "You haven't signed up for any games yet.",
                        emptyButtonTitle: "Browse Games",
                        emptyButtonTab: 1
                    ) { event in
                        NavigationLink {
                            EventDetailView(eventId: event.id)
                        } label: {
                            compactEventRow(event)
                        }
                        .buttonStyle(.plain)
                    }

                    // Games I'm Hosting
                    dashboardSection(
                        title: "Games I'm Hosting",
                        icon: "star.fill",
                        items: vm.hostedEvents,
                        emptyMessage: "You haven't hosted any games yet.",
                        emptyButtonTitle: "Host Your First Game",
                        emptyAction: { showCreateEvent = true }
                    ) { event in
                        NavigationLink {
                            EventDetailView(eventId: event.id)
                        } label: {
                            compactEventRow(event)
                        }
                        .buttonStyle(.plain)
                    }

                    // Games Being Planned
                    dashboardSection(
                        title: "Games Being Planned",
                        icon: "calendar.badge.plus",
                        items: vm.planningSessions,
                        emptyMessage: "No active planning sessions.",
                        emptyButtonTitle: "View Groups",
                        emptyButtonTab: 2
                    ) { session in
                        NavigationLink {
                            PlanningSessionDetailView(sessionId: session.id)
                        } label: {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(session.title)
                                        .font(.md3BodyMedium)
                                        .fontWeight(.medium)
                                        .lineLimit(1)
                                    Text(session.responseDeadline.toDate?.displayDateTime ?? session.responseDeadline)
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                                Spacer()
                                BadgeView(text: "Open", color: .md3TertiaryContainer)
                                Image(systemName: "chevron.right")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            .padding(.vertical, 4)
                        }
                        .buttonStyle(.plain)
                    }

                    // Groups I Manage
                    dashboardSection(
                        title: "Groups I Manage",
                        icon: "person.3.fill",
                        items: vm.managedGroups,
                        emptyMessage: "You're not managing any groups yet.",
                        emptyButtonTitle: "Create Group",
                        emptyButtonTab: 2
                    ) { group in
                        NavigationLink {
                            GroupDetailView(groupId: group.id)
                        } label: {
                            compactGroupRow(group)
                        }
                        .buttonStyle(.plain)
                    }

                    // Groups I'm In
                    dashboardSection(
                        title: "Groups I'm In",
                        icon: "person.3",
                        items: vm.memberGroups,
                        emptyMessage: "You haven't joined any groups yet.",
                        emptyButtonTitle: "Browse Groups",
                        emptyButtonTab: 2
                    ) { group in
                        NavigationLink {
                            GroupDetailView(groupId: group.id)
                        } label: {
                            compactGroupRow(group)
                        }
                        .buttonStyle(.plain)
                    }

                    // Raffle banner
                    if let raffle = raffleVM.raffle {
                        NavigationLink {
                            RaffleDetailView()
                        } label: {
                            RaffleBannerView(raffle: raffle)
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal)
                    }

                    // Ad banner
                    AdBannerView(placement: "dashboard")

                    // Upgrade banner for free tier
                    if (authVM.user?.effectiveTier ?? .free) == .free {
                        NavigationLink {
                            PricingView()
                        } label: {
                            VStack(spacing: 10) {
                                HStack(spacing: 8) {
                                    Image(systemName: "sparkles")
                                        .font(.system(size: 20))
                                        .foregroundStyle(Color.md3Tertiary)
                                    Text("Unlock More Features")
                                        .font(.md3TitleMedium)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }

                                Text("Get planning sessions, event chat, more groups, and no ads with Basic — starting at $4.99/mo")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .padding()
                            .background(
                                LinearGradient(
                                    colors: [Color.md3PrimaryContainer.opacity(0.3), Color.md3TertiaryContainer.opacity(0.3)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .sheet(isPresented: $showCreateEvent, onDismiss: {
            Task { await vm.loadDashboard() }
        }) {
            CreateEventView()
        }
        .refreshable {
            await vm.loadDashboard()
            await raffleVM.loadActiveRaffle()
        }
        .task {
            vm.configure(services: services)
            raffleVM.configure(services: services)
            await vm.loadDashboard()
            await raffleVM.loadActiveRaffle()
        }
        .onAppear {
            // Reload when returning from navigation (task only runs once on first appear)
            if vm.services != nil && !vm.isLoading {
                Task { await vm.loadDashboard() }
            }
        }
    }

    // MARK: - User Header

    private var userHeader: some View {
        HStack(spacing: 12) {
            UserAvatarView(
                url: authVM.user?.avatarUrl,
                name: authVM.user?.displayName ?? authVM.user?.username,
                size: 48
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(authVM.user?.displayName ?? authVM.user?.username ?? "")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                HStack(spacing: 6) {
                    if let username = authVM.user?.username {
                        Text("@\(username)")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    NavigationLink {
                        PricingView()
                    } label: {
                        SubscriptionBadgeView(tier: authVM.user?.effectiveTier ?? .free)
                    }
                }
            }

            Spacer()

            Button {
                showCreateEvent = true
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "plus")
                    Text("Host a Game")
                }
                .font(.md3LabelMedium)
                .foregroundStyle(Color.md3OnPrimary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.md3Primary)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            }
        }
        .padding()
        .background(Color.md3Surface)
    }

    // MARK: - Dashboard Section

    private func dashboardSection<Item: Identifiable, Content: View>(
        title: String,
        icon: String,
        items: [Item],
        emptyMessage: String,
        emptyButtonTitle: String,
        emptyButtonTab: Int? = nil,
        emptyAction: (() -> Void)? = nil,
        @ViewBuilder rowContent: @escaping (Item) -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .foregroundStyle(Color.md3Primary)
                Text(title)
                    .font(.md3TitleSmall)
                    .foregroundStyle(Color.md3OnSurface)
            }

            if items.isEmpty {
                VStack(spacing: 12) {
                    Text(emptyMessage)
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)

                    Button {
                        if let action = emptyAction {
                            action()
                        } else if let tab = emptyButtonTab {
                            switchTab?(tab)
                        }
                    } label: {
                        Text(emptyButtonTitle)
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnPrimary)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(Color.md3Primary)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                    }
                }
                .padding(.vertical, 8)
            } else {
                ForEach(items) { item in
                    rowContent(item)
                }
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

    // MARK: - Row Views

    private func compactEventRow(_ event: EventSummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(event.title)
                    .font(.md3BodyMedium)
                    .fontWeight(.medium)
                    .lineLimit(1)
                HStack(spacing: 8) {
                    Text(event.eventDate.toDate?.displayDate ?? event.eventDate)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    if let startTime = event.startTime {
                        Text(startTime.to12HourTime)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
            Spacer()
            Text("\(event.confirmedCount)/\(event.maxPlayers ?? 0)")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3Primary)
            Image(systemName: "chevron.right")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .padding(.vertical, 4)
    }

    private func compactGroupRow(_ group: GroupSummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(group.name)
                    .font(.md3BodyMedium)
                    .fontWeight(.medium)
                    .lineLimit(1)
                Text("\(group.memberCount) members")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            Spacer()
            if let role = group.userRole {
                BadgeView(text: role.displayName, color: .md3PrimaryContainer)
            }
            Image(systemName: "chevron.right")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .padding(.vertical, 4)
    }
}
