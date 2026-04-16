import SwiftUI

struct UserProfileSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    let userId: String
    @State private var profile: PublicProfile?
    @State private var badges: [UserBadge] = []
    @State private var collection: [CollectionGame] = []
    @State private var isLoading = true

    var body: some View {
        NavigationStack {
            ScrollView {
                if isLoading {
                    VStack {
                        Spacer(minLength: 80)
                        ProgressView()
                        Spacer()
                    }
                } else if let profile {
                    VStack(spacing: 16) {
                        // Avatar + name
                        VStack(spacing: 10) {
                            UserAvatarView(url: profile.avatarUrl, name: profile.displayName, size: 72)

                            Text(profile.displayName ?? "Unknown")
                                .font(.md3HeadlineSmall)
                                .foregroundStyle(Color.md3OnSurface)

                            Text("@\(profile.username)")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }

                        // Location
                        if let city = profile.homeCity, let state = profile.homeState {
                            Label("\(city), \(state)", systemImage: "mappin.circle.fill")
                                .font(.md3BodySmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }

                        // Bio
                        if let bio = profile.bio, !bio.isEmpty {
                            Text(bio)
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurface)
                                .multilineTextAlignment(.center)
                                .lineLimit(3)
                                .padding(.horizontal, 24)
                        }

                        // Pinned badges
                        let pinned = badges.filter(\.isPinned)
                        if !pinned.isEmpty {
                            VStack(spacing: 8) {
                                Text("Pinned Badges")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)

                                HStack(spacing: 12) {
                                    ForEach(pinned) { ub in
                                        VStack(spacing: 4) {
                                            badgeIcon(ub.badge)
                                            Text(ub.badge.name)
                                                .font(.md3BodySmall)
                                                .foregroundStyle(Color.md3OnSurface)
                                                .lineLimit(1)
                                        }
                                        .frame(width: 70)
                                    }
                                }
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .padding(.horizontal, 16)
                        }

                        // Badges summary
                        if !badges.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Image(systemName: "trophy.fill")
                                        .foregroundStyle(Color.md3Tertiary)
                                    Text("\(badges.count) Badges Earned")
                                        .font(.md3TitleSmall)
                                        .foregroundStyle(Color.md3OnSurface)
                                }

                                // Show first 8 badges
                                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 4), spacing: 8) {
                                    ForEach(badges.prefix(8)) { ub in
                                        VStack(spacing: 2) {
                                            badgeIcon(ub.badge)
                                            Text(ub.badge.name)
                                                .font(.system(size: 9))
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                                .lineLimit(1)
                                        }
                                    }
                                }

                                if badges.count > 8 {
                                    Text("+\(badges.count - 8) more")
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3Primary)
                                        .frame(maxWidth: .infinity, alignment: .center)
                                }
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .padding(.horizontal, 16)
                        }

                        // Game collection
                        if !collection.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Image(systemName: "dice")
                                        .foregroundStyle(Color.md3Primary)
                                    Text("Game Collection (\(collection.count))")
                                        .font(.md3TitleSmall)
                                        .foregroundStyle(Color.md3OnSurface)
                                }

                                ForEach(collection.prefix(5)) { game in
                                    HStack(spacing: 10) {
                                        if let urlStr = game.thumbnailUrl, let url = URL(string: urlStr) {
                                            AsyncImage(url: url) { image in
                                                image.resizable().aspectRatio(contentMode: .fill)
                                            } placeholder: {
                                                Color.md3SurfaceVariant
                                            }
                                            .frame(width: 32, height: 32)
                                            .clipShape(RoundedRectangle(cornerRadius: 4))
                                        }

                                        Text(game.gameName)
                                            .font(.md3BodySmall)
                                            .foregroundStyle(Color.md3OnSurface)
                                            .lineLimit(1)

                                        Spacer()

                                        if let min = game.minPlayers, let max = game.maxPlayers {
                                            Text(min == max ? "\(min)p" : "\(min)-\(max)p")
                                                .font(.md3BodySmall)
                                                .foregroundStyle(Color.md3OnSurfaceVariant)
                                        }
                                    }
                                }

                                if collection.count > 5 {
                                    NavigationLink {
                                        UserCollectionListView(games: collection, userName: profile.displayName ?? "User")
                                    } label: {
                                        Text("View all \(collection.count) games →")
                                            .font(.md3LabelLarge)
                                            .foregroundStyle(Color.md3Primary)
                                            .frame(maxWidth: .infinity, alignment: .center)
                                    }
                                }
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .padding(.horizontal, 16)
                        }

                        // Favorite games
                        if let favorites = profile.favoriteGames, !favorites.isEmpty {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Favorite Games")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)

                                FlowLayout(spacing: 6) {
                                    ForEach(favorites, id: \.self) { game in
                                        Text(game)
                                            .font(.md3BodySmall)
                                            .padding(.horizontal, 10)
                                            .padding(.vertical, 4)
                                            .background(Color.md3PrimaryContainer.opacity(0.5))
                                            .foregroundStyle(Color.md3OnPrimaryContainer)
                                            .clipShape(Capsule())
                                    }
                                }
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            .padding(.horizontal, 16)
                        }

                        Spacer(minLength: 16)
                    }
                    .padding(.top, 8)
                }
            }
            .background(Color.md3SurfaceContainer)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .task {
            await loadProfile()
        }
    }

    private func badgeIcon(_ badge: Badge) -> some View {
        ZStack {
            Circle()
                .fill(tierColor(badge.tier).opacity(0.15))
                .frame(width: 36, height: 36)
            Image(systemName: categoryIcon(badge.category))
                .font(.system(size: 16))
                .foregroundStyle(tierColor(badge.tier))
        }
    }

    private func loadProfile() async {
        isLoading = true
        do {
            async let profileTask = services.profile.getPublicProfile(userId: userId)
            async let badgesTask: () = loadBadges()
            async let collectionTask: () = loadCollection()

            profile = try await profileTask
            _ = await (badgesTask, collectionTask)
        } catch {
            print("Failed to load profile: \(error)")
        }
        isLoading = false
    }

    private func loadBadges() async {
        do {
            let response: BadgesResponse = try await services.api.get("badges", queryItems: [
                .init(name: "action", value: "user"),
                .init(name: "userId", value: userId)
            ], authenticated: false)
            badges = response.badges
        } catch {
            // Non-fatal
        }
    }

    private func loadCollection() async {
        do {
            struct CollectionWrapper: Decodable { let games: [CollectionGame]? }
            let response: CollectionWrapper = try await services.api.get("collections", queryItems: [
                .init(name: "userId", value: userId)
            ], authenticated: false)
            collection = response.games ?? []
        } catch {
            // Non-fatal — collection may be private
        }
    }

    private func tierColor(_ tier: String) -> Color {
        switch tier {
        case "bronze": return Color(red: 0.8, green: 0.5, blue: 0.2)
        case "silver": return Color(red: 0.63, green: 0.63, blue: 0.63)
        case "gold": return Color(red: 1.0, green: 0.84, blue: 0.0)
        case "platinum": return Color.md3Primary
        default: return Color.md3OnSurfaceVariant
        }
    }

    private func categoryIcon(_ category: String) -> String {
        switch category {
        case "hosting": return "house.fill"
        case "attendance": return "person.fill.checkmark"
        case "planning": return "calendar"
        case "social": return "person.3.fill"
        case "collection": return "dice"
        case "game_system": return "gamecontroller.fill"
        case "items": return "bag.fill"
        case "special": return "star.fill"
        default: return "questionmark.circle"
        }
    }
}

// Simple flow layout for tags
struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = layout(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layout(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }

    private func layout(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
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
