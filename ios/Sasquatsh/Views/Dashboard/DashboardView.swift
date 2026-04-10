import SwiftUI

struct DashboardView: View {
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

                    // My Upcoming Games
                    dashboardSection(
                        title: "My Upcoming Games",
                        icon: "calendar.badge.clock",
                        items: vm.registeredEvents,
                        emptyMessage: "You haven't signed up for any games yet.",
                        emptyButtonTitle: "Browse Games",
                        emptyButtonTab: 0
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

                    // Groups I Manage
                    dashboardSection(
                        title: "Groups I Manage",
                        icon: "person.3.fill",
                        items: vm.managedGroups,
                        emptyMessage: "You're not managing any groups yet.",
                        emptyButtonTitle: "Create Group",
                        emptyButtonTab: 1
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
                        emptyButtonTab: 1
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
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Dashboard")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showCreateEvent) {
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
    }

    // MARK: - User Header

    private var userHeader: some View {
        HStack(spacing: 12) {
            UserAvatarView(
                url: authVM.user?.avatarUrl,
                name: authVM.user?.displayName ?? authVM.user?.username,
                size: 48
            )

            VStack(alignment: .leading, spacing: 2) {
                Text(authVM.user?.displayName ?? authVM.user?.username ?? "")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Text(authVM.user?.email ?? "")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
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
                        }
                        // Tab switching would require a binding passed from MainTabView
                        // For now the button text serves as guidance
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
                        Text(startTime)
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
