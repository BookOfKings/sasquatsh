import SwiftUI

struct PlayerRequestListView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = PlayerRequestListViewModel()
    @State private var showCreateRequest = false
    @State private var selectedTab = 0
    @State private var selectedLocationId: String?
    @State private var filterCity = ""
    @State private var filterState = ""
    @State private var profileLoaded = false

    var body: some View {
        VStack(spacing: 0) {
            Picker("View", selection: $selectedTab) {
                Text("Active").tag(0)
                Text("My Requests").tag(1)
            }
            .pickerStyle(.segmented)
            .padding()

            ScrollView {
                VStack(spacing: 12) {
                    // Info banner
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "info.circle.fill")
                            .foregroundStyle(Color.md3Tertiary)
                        Text("If someone bails on your game, post a request here. Requests expire in **15 minutes**.")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    .padding()
                    .background(Color.md3TertiaryContainer.opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
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

                    if selectedTab == 0 {
                        browseView
                    } else {
                        myRequestsView
                    }
                }
                .padding(.vertical)
            }
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showCreateRequest = true
                } label: {
                    Image(systemName: "plus")
                        .foregroundStyle(Color.md3Primary)
                }
            }
        }
        .sheet(isPresented: $showCreateRequest) {
            CreatePlayerRequestView(vm: vm)
        }
        .refreshable {
            await vm.loadRequests(filters: currentFilters)
            await vm.loadMyRequests()
        }
        .task {
            vm.configure(services: services)
            if !profileLoaded {
                profileLoaded = true
                if let profile = try? await services.profile.getMyProfile() {
                    if filterCity.isEmpty, let city = profile.activeCity ?? profile.homeCity {
                        filterCity = city
                    }
                    if filterState.isEmpty, let state = profile.activeState ?? profile.homeState {
                        filterState = state
                    }
                }
            }
            await vm.loadRequests(filters: currentFilters)
            await vm.loadMyRequests()
        }
    }

    private var currentFilters: PlayerRequestFilters {
        PlayerRequestFilters(
            city: filterCity.isEmpty ? nil : filterCity,
            state: filterState.isEmpty ? nil : filterState,
            eventLocationId: selectedLocationId
        )
    }

    private var browseView: some View {
        Group {
            // Hot Locations Bar
            HotLocationsBar(selectedId: selectedLocationId) { location in
                if selectedLocationId == location.id {
                    selectedLocationId = nil
                } else {
                    selectedLocationId = location.id
                }
                Task { await vm.loadRequests(filters: currentFilters) }
            }
            .padding(.horizontal)

            // City / State filter
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    TextField("City", text: $filterCity)
                        .textFieldStyle(.roundedBorder)
                    TextField("State", text: $filterState)
                        .textFieldStyle(.roundedBorder)
                        .frame(width: 80)
                }
                HStack(spacing: 8) {
                    Button {
                        selectedLocationId = nil
                        Task { await vm.loadRequests(filters: currentFilters) }
                    } label: {
                        Text("Apply")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnPrimary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                            .background(Color.md3Primary)
                            .clipShape(Capsule())
                    }
                    Button {
                        filterCity = ""
                        filterState = ""
                        selectedLocationId = nil
                        Task { await vm.loadRequests() }
                    } label: {
                        Text("Clear")
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                            .background(Color.md3SurfaceContainerHigh)
                            .clipShape(Capsule())
                    }
                    Spacer()
                }
            }
            .padding(.horizontal)

            if vm.isLoading && vm.requests.isEmpty {
                LoadingView()
            } else if vm.requests.isEmpty {
                EmptyStateView(
                    icon: "checkmark.circle",
                    title: "No Active Requests",
                    message: "No one needs players right now. Check back later!"
                )
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(vm.requests) { request in
                        requestCard(request)
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    private var myRequestsView: some View {
        Group {
            if vm.myRequests.isEmpty {
                EmptyStateView(
                    icon: "text.bubble",
                    title: "No Requests",
                    message: "Need players for your game? Post a request!",
                    buttonTitle: "Need Players",
                    action: { showCreateRequest = true }
                )
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(vm.myRequests) { request in
                        myRequestCard(request)
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    private func requestCard(_ request: PlayerRequest) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                if let host = request.host {
                    UserAvatarView(url: host.avatarUrl, name: host.displayName, size: 36)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(request.event?.title ?? "Game Night")
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                    Text("Hosted by \(request.host?.displayName ?? request.host?.username ?? "Unknown")")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text(timeRemaining(request.expiresAt))
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3Tertiary)
                    BadgeView(text: "\(request.playerCountNeeded) needed", color: .md3TertiaryContainer)
                }
            }

            // Event details
            if let event = request.event {
                VStack(alignment: .leading, spacing: 4) {
                    if let game = event.gameTitle {
                        Text(game)
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Primary)
                    }
                    HStack(spacing: 12) {
                        Label(event.eventDate.toDate?.displayDate ?? event.eventDate, systemImage: "calendar")
                        Label(event.startTime, systemImage: "clock")
                    }
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)

                    if let loc = event.locationDetails ?? [event.city, event.state].compactMap({ $0 }).joined(separator: ", ").nilIfEmpty {
                        Label(loc, systemImage: "mappin")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.md3SurfaceContainerHigh)
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            }

            if let desc = request.description, !desc.isEmpty {
                Text("\"\(desc)\"")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .italic()
            }

            if let eventId = request.event?.id {
                NavigationLink {
                    EventDetailView(eventId: eventId)
                } label: {
                    Text("View Event & Join")
                        .primaryButtonStyle()
                }
            }
        }
        .padding()
        .cardStyle()
    }

    private func myRequestCard(_ request: PlayerRequest) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(request.event?.title ?? "Game Night")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                BadgeView(text: statusText(request.status), color: statusColor(request.status))
                if request.status == "open" {
                    Text(timeRemaining(request.expiresAt))
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3Tertiary)
                }
            }

            Text("\(request.event?.gameTitle ?? "No game specified") - Needs \(request.playerCountNeeded) player\(request.playerCountNeeded > 1 ? "s" : "")")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)

            if let desc = request.description, !desc.isEmpty {
                Text(desc)
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            if request.status == "open" {
                HStack(spacing: 8) {
                    Button {
                        Task { await vm.fillRequest(id: request.id) }
                    } label: {
                        Text("Found Players")
                            .primaryButtonStyle()
                    }
                    Button {
                        Task { await vm.cancelRequest(id: request.id) }
                    } label: {
                        Text("Cancel")
                            .outlinedButtonStyle()
                    }
                }
            } else {
                Button(role: .destructive) {
                    Task { await vm.deleteRequest(id: request.id) }
                } label: {
                    Label("Delete", systemImage: "trash")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3Error)
                }
            }
        }
        .padding()
        .cardStyle()
        .opacity(request.status == "open" ? 1 : 0.6)
    }

    private func timeRemaining(_ expiresAt: String) -> String {
        guard let date = ISO8601DateFormatter().date(from: expiresAt) else { return "" }
        let diff = date.timeIntervalSinceNow
        if diff <= 0 { return "Expired" }
        let minutes = Int(diff) / 60
        let seconds = Int(diff) % 60
        if minutes > 0 { return "\(minutes)m \(seconds)s" }
        return "\(seconds)s"
    }

    private func statusText(_ status: String) -> String {
        switch status {
        case "open": return "Active"
        case "filled": return "Filled"
        case "cancelled": return "Cancelled"
        default: return status
        }
    }

    private func statusColor(_ status: String) -> Color {
        switch status {
        case "open": return .md3PrimaryContainer
        case "filled": return .md3SecondaryContainer
        case "cancelled": return .md3SurfaceContainerHigh
        default: return .md3SurfaceContainerHigh
        }
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}
