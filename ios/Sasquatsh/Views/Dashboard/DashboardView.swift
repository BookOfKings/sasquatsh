import SwiftUI

struct DashboardView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = DashboardViewModel()

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.registeredEvents.isEmpty && vm.myGroups.isEmpty {
                LoadingView()
            } else {
                VStack(spacing: 20) {
                    // Welcome
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Welcome back!")
                                .font(.md3HeadlineMedium)
                            if let name = authVM.user?.displayName ?? authVM.user?.username {
                                Text(name)
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }
                        Spacer()
                        UserAvatarView(
                            url: authVM.user?.avatarUrl,
                            name: authVM.user?.displayName ?? authVM.user?.username,
                            size: 44
                        )
                    }
                    .padding(.horizontal)

                    // Ad banner for free tier
                    if TierConfig.getLimits(for: authVM.user?.subscriptionTier ?? .free).features.showAds {
                        UpgradeAdBanner()
                            .padding(.horizontal)
                    }

                    if let error = vm.error {
                        ErrorBannerView(message: error) { vm.error = nil }
                    }

                    dashboardSection(
                        title: "My Upcoming Games",
                        icon: "calendar.badge.clock",
                        isEmpty: vm.registeredEvents.isEmpty,
                        emptyMessage: "No upcoming games"
                    ) {
                        ForEach(vm.registeredEvents) { event in
                            NavigationLink {
                                EventDetailView(eventId: event.id)
                            } label: {
                                compactEventRow(event)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    dashboardSection(
                        title: "Games I'm Hosting",
                        icon: "star.fill",
                        isEmpty: vm.hostedEvents.isEmpty,
                        emptyMessage: "Not hosting any games"
                    ) {
                        ForEach(vm.hostedEvents) { event in
                            NavigationLink {
                                EventDetailView(eventId: event.id)
                            } label: {
                                compactEventRow(event)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    dashboardSection(
                        title: "Groups I Manage",
                        icon: "person.3.fill",
                        isEmpty: vm.managedGroups.isEmpty,
                        emptyMessage: "Not managing any groups"
                    ) {
                        ForEach(vm.managedGroups) { group in
                            NavigationLink {
                                GroupDetailView(groupId: group.id)
                            } label: {
                                compactGroupRow(group)
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    dashboardSection(
                        title: "My Groups",
                        icon: "person.3",
                        isEmpty: vm.memberGroups.isEmpty,
                        emptyMessage: "Not a member of any groups"
                    ) {
                        ForEach(vm.memberGroups) { group in
                            NavigationLink {
                                GroupDetailView(groupId: group.id)
                            } label: {
                                compactGroupRow(group)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Dashboard")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await vm.loadDashboard() }
        .task {
            vm.configure(services: services)
            await vm.loadDashboard()
        }
    }

    private func dashboardSection<Content: View>(
        title: String,
        icon: String,
        isEmpty: Bool,
        emptyMessage: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(Color.md3Primary)
                Text(title)
                    .font(.md3TitleSmall)
                    .foregroundStyle(Color.md3OnSurface)
            }

            if isEmpty {
                Text(emptyMessage)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .padding(.vertical, 8)
            } else {
                content()
            }
        }
        .padding()
        .cardStyle()
        .padding(.horizontal)
    }

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
