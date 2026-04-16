import SwiftUI
import PhotosUI

struct ProfileView: View {
    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = ProfileViewModel()
    @State private var showEditSheet = false
    @State private var showBlockedUsers = false
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var showDeleteAvatarConfirm = false

    var body: some View {
        ScrollView {
            if vm.isLoading && vm.profile == nil {
                LoadingView()
            } else if let profile = vm.profile {
                VStack(spacing: 20) {
                    // Avatar & Name
                    VStack(spacing: 12) {
                        ZStack(alignment: .bottomTrailing) {
                            UserAvatarView(url: profile.avatarUrl, name: profile.displayName ?? profile.username, size: 80)

                            if vm.isUploadingAvatar {
                                Circle()
                                    .fill(Color.black.opacity(0.4))
                                    .frame(width: 80, height: 80)
                                    .overlay {
                                        ProgressView()
                                            .tint(.white)
                                    }
                            }

                            Image(systemName: "camera.fill")
                                .font(.system(size: 12))
                                .foregroundStyle(Color.md3OnPrimary)
                                .padding(6)
                                .background(Color.md3Primary)
                                .clipShape(Circle())
                                .offset(x: 2, y: 2)
                        }
                        .photosPicker(isPresented: Binding(
                            get: { false },
                            set: { _ in }
                        ), selection: $selectedPhoto, matching: .images)
                        .overlay {
                            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                                Color.clear
                            }
                        }

                        if profile.avatarUrl != nil {
                            Button {
                                showDeleteAvatarConfirm = true
                            } label: {
                                Label("Remove Photo", systemImage: "trash")
                                    .font(.md3LabelSmall)
                                    .foregroundStyle(Color.md3Error)
                            }
                        }

                        Text(profile.displayName ?? profile.username)
                            .font(.md3HeadlineMedium)

                        Text("@\(profile.username)")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    .padding(.top)

                    // Stats
                    if let stats = profile.stats {
                        HStack(spacing: 32) {
                            statItem(value: stats.hostedCount, label: "Hosted")
                            statItem(value: stats.attendedCount, label: "Attended")
                            statItem(value: stats.groupCount, label: "Groups")
                        }
                        .padding()
                        .cardStyle()
                    }

                    // Subscription
                    NavigationLink {
                        BillingView()
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Subscription")
                                    .font(.md3TitleSmall)
                                    .foregroundStyle(Color.md3OnSurface)
                                Text((profile.subscriptionTier ?? .free).priceLabel)
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            Spacer()
                            SubscriptionBadgeView(tier: profile.subscriptionTier ?? .free)
                            Image(systemName: "chevron.right")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding()
                        .cardStyle()
                    }
                    .buttonStyle(.plain)

                    // MTG Decks
                    NavigationLink {
                        MyDecksView()
                    } label: {
                        HStack {
                            Image(systemName: "rectangle.stack")
                                .foregroundStyle(Color.md3Primary)
                            Text("My MTG Decks")
                                .font(.md3TitleSmall)
                                .foregroundStyle(Color.md3OnSurface)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .padding()
                        .cardStyle()
                    }
                    .buttonStyle(.plain)

                    // Bio
                    if let bio = profile.bio, !bio.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("About")
                                .font(.md3TitleSmall)
                                .foregroundStyle(Color.md3OnSurface)
                            Text(bio)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .cardStyle()
                    }

                    // Home Location
                    if let city = profile.homeCity, let state = profile.homeState {
                        HStack {
                            Image(systemName: "mappin.circle.fill")
                                .foregroundStyle(Color.md3Tertiary)
                            Text("\(city), \(state)")
                                .font(.md3BodyMedium)
                            Spacer()
                        }
                        .padding()
                        .cardStyle()
                    }

                    // Active Location
                    if let activeCity = profile.activeCity, let activeState = profile.activeState,
                       !activeCity.isEmpty {
                        HStack {
                            Image(systemName: "location.fill")
                                .foregroundStyle(Color.md3Primary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("\(activeCity), \(activeState)")
                                    .font(.md3BodyMedium)
                                if let expires = profile.activeLocationExpiresAt {
                                    Text("Until \(formattedExpiration(expires))")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }
                            }
                            Spacer()
                        }
                        .padding()
                        .cardStyle()
                    }

                    // Timezone
                    if let tz = profile.timezone, let appTz = AppTimezone(rawValue: tz) {
                        HStack {
                            Image(systemName: "globe")
                                .foregroundStyle(Color.md3Tertiary)
                            Text(appTz.displayName)
                                .font(.md3BodyMedium)
                            Spacer()
                        }
                        .padding()
                        .cardStyle()
                    }

                    // Favorite Games
                    if let games = profile.favoriteGames, !games.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Favorite Games")
                                .font(.md3TitleSmall)
                                .foregroundStyle(Color.md3OnSurface)
                            FlowLayout(spacing: 8) {
                                ForEach(games, id: \.self) { game in
                                    BadgeView(text: game, color: .md3TertiaryContainer)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .cardStyle()
                    }

                    // Preferred Game Types
                    if let types = profile.preferredGameTypes, !types.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Preferred Game Types")
                                .font(.md3TitleSmall)
                                .foregroundStyle(Color.md3OnSurface)
                            FlowLayout(spacing: 8) {
                                ForEach(types, id: \.self) { type in
                                    let displayName = GameCategory(rawValue: type)?.displayName ?? type.capitalized
                                    BadgeView(text: displayName, color: .md3PrimaryContainer)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .cardStyle()
                    }

                    // Appearance
                    VStack(spacing: 8) {
                        HStack {
                            Image(systemName: "moon.circle.fill")
                                .foregroundStyle(Color.md3Primary)
                            Text("Appearance")
                                .font(.md3TitleMedium)
                                .foregroundStyle(Color.md3OnSurface)
                            Spacer()
                        }

                        Picker("", selection: AppearanceManager.shared.$mode) {
                            Text("System").tag(AppearanceMode.system)
                            Text("Light").tag(AppearanceMode.light)
                            Text("Dark").tag(AppearanceMode.dark)
                        }
                        .pickerStyle(.segmented)
                    }
                    .padding()
                    .cardStyle()

                    // Actions
                    VStack(spacing: 12) {
                        Button {
                            showEditSheet = true
                        } label: {
                            Label("Edit Profile", systemImage: "pencil")
                                .outlinedButtonStyle()
                        }

                        Button {
                            showBlockedUsers = true
                        } label: {
                            Label("Blocked Users", systemImage: "nosign")
                                .font(.md3LabelLarge)
                                .frame(maxWidth: .infinity)
                                .frame(height: 40)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }

                        Button(role: .destructive) {
                            authVM.logout()
                        } label: {
                            Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                                .font(.md3LabelLarge)
                                .frame(maxWidth: .infinity)
                                .frame(height: 40)
                                .foregroundStyle(Color.md3Error)
                        }
                    }
                }
                .padding()
            }
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
        .refreshable { await vm.loadProfile() }
        .sheet(isPresented: $showEditSheet) {
            if let profile = vm.profile {
                ProfileEditSheet(profile: profile, vm: vm)
            }
        }
        .sheet(isPresented: $showBlockedUsers) {
            BlockedUsersView(vm: vm)
        }
        .confirmationDialog("Remove Avatar", isPresented: $showDeleteAvatarConfirm) {
            Button("Remove", role: .destructive) {
                Task { await vm.deleteAvatar() }
            }
        } message: {
            Text("Are you sure you want to remove your profile photo?")
        }
        .onChange(of: selectedPhoto) { _, newValue in
            guard let item = newValue else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    await vm.uploadAvatar(imageData: data)
                }
                selectedPhoto = nil
            }
        }
        .task {
            vm.configure(services: services)
            await vm.loadProfile()
        }
    }

    private func statItem(value: Int, label: String) -> some View {
        VStack(spacing: 4) {
            Text("\(value)")
                .font(.md3TitleLarge)
                .foregroundStyle(Color.md3Primary)
            Text(label)
                .font(.md3LabelSmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
    }

    private func formattedExpiration(_ dateString: String) -> String {
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let date = isoFormatter.date(from: dateString) else { return dateString }
        let displayFormatter = DateFormatter()
        displayFormatter.dateStyle = .medium
        displayFormatter.timeStyle = .none
        return displayFormatter.string(from: date)
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = layoutSubviews(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layoutSubviews(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }

    private func layoutSubviews(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }

        return (CGSize(width: maxWidth, height: y + rowHeight), positions)
    }
}
