import SwiftUI

struct BadgesView: View {
    @Environment(\.services) private var services
    @State private var allBadges: [Badge] = []
    @State private var earnedBadges: [UserBadge] = []
    @State private var isLoading = true
    @State private var isComputing = false
    @State private var newlyEarned = 0
    @State private var error: String?
    @State private var selectedCategory: String? = nil

    private var earnedBadgeIds: Set<Int> {
        Set(earnedBadges.map(\.badgeId))
    }

    private var pinnedBadges: [UserBadge] {
        earnedBadges.filter(\.isPinned)
    }

    private let categories: [(key: String, label: String, icon: String)] = [
        ("hosting", "Hosting", "house.fill"),
        ("attendance", "Attendance", "person.fill.checkmark"),
        ("planning", "Planning", "calendar"),
        ("social", "Social", "person.3.fill"),
        ("collection", "Collection", "dice"),
        ("game_system", "Game Systems", "gamecontroller.fill"),
        ("items", "Items", "bag.fill"),
        ("special", "Special", "star.fill"),
    ]

    private var filteredBadges: [Badge] {
        guard let cat = selectedCategory else { return allBadges }
        return allBadges.filter { $0.category == cat }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Stats bar
            HStack(spacing: 20) {
                VStack(spacing: 2) {
                    Text("\(earnedBadges.count)")
                        .font(.md3TitleLarge)
                        .foregroundStyle(Color.md3Primary)
                    Text("Earned")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                VStack(spacing: 2) {
                    Text("\(allBadges.count)")
                        .font(.md3TitleLarge)
                        .foregroundStyle(Color.md3OnSurface)
                    Text("Total")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                Spacer()
                Button {
                    Task { await computeBadges() }
                } label: {
                    HStack(spacing: 6) {
                        if isComputing {
                            D20ProgressView(size: 18)
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                        Text("Check")
                    }
                    .font(.md3LabelLarge)
                    .padding(.horizontal, 16)
                    .frame(height: 32)
                    .background(Color.md3SecondaryContainer)
                    .foregroundStyle(Color.md3OnSecondaryContainer)
                    .clipShape(Capsule())
                }
                .disabled(isComputing)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.md3Surface)

            if newlyEarned > 0 {
                HStack {
                    Image(systemName: "party.popper.fill")
                    Text("You earned \(newlyEarned) new badge\(newlyEarned == 1 ? "" : "s")!")
                }
                .font(.md3LabelLarge)
                .foregroundStyle(Color.md3OnTertiaryContainer)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(Color.md3TertiaryContainer)
            }

            // Category filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    filterChip(label: "All", isSelected: selectedCategory == nil) {
                        selectedCategory = nil
                    }
                    ForEach(categories, id: \.key) { cat in
                        filterChip(label: cat.label, isSelected: selectedCategory == cat.key) {
                            selectedCategory = cat.key
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }

            if isLoading {
                Spacer()
                    D20ProgressView(size: 32)
                Spacer()
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(filteredBadges) { badge in
                            let earned = earnedBadgeIds.contains(badge.id)
                            let userBadge = earnedBadges.first { $0.badgeId == badge.id }
                            badgeCard(badge: badge, earned: earned, pinned: userBadge?.isPinned ?? false)
                        }
                    }
                    .padding(16)
                }
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationTitle("Badges")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadBadges()
        }
    }

    // MARK: - Badge Card

    private func badgeCard(badge: Badge, earned: Bool, pinned: Bool) -> some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(earned ? tierColor(badge.tier).opacity(0.15) : Color.md3SurfaceVariant.opacity(0.3))
                    .frame(width: 56, height: 56)

                // Use SF Symbol based on category since SVG rendering in SwiftUI is complex
                Image(systemName: categoryIcon(badge.category))
                    .font(.system(size: 24))
                    .foregroundStyle(earned ? tierColor(badge.tier) : Color.md3OnSurfaceVariant.opacity(0.3))

                if pinned {
                    Image(systemName: "pin.fill")
                        .font(.system(size: 10))
                        .foregroundStyle(Color.md3Tertiary)
                        .offset(x: 20, y: -20)
                }
            }

            Text(badge.name)
                .font(.md3LabelLarge)
                .foregroundStyle(earned ? Color.md3OnSurface : Color.md3OnSurfaceVariant.opacity(0.5))
                .lineLimit(1)

            Text(badge.description)
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant.opacity(earned ? 0.8 : 0.4))
                .lineLimit(2)
                .multilineTextAlignment(.center)

            // Tier badge
            Text(badge.tier.capitalized)
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(earned ? tierColor(badge.tier) : Color.md3OnSurfaceVariant.opacity(0.3))
                .padding(.horizontal, 8)
                .padding(.vertical, 2)
                .background(earned ? tierColor(badge.tier).opacity(0.12) : Color.clear)
                .clipShape(Capsule())
                .overlay(
                    Capsule().stroke(earned ? tierColor(badge.tier).opacity(0.3) : Color.md3OutlineVariant.opacity(0.3), lineWidth: 0.5)
                )
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .opacity(earned ? 1.0 : 0.6)
        .onTapGesture {
            if earned {
                Task { await togglePin(badgeId: badge.id) }
            }
        }
    }

    private func filterChip(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .semibold))
                }
                Text(label)
                    .font(.md3LabelLarge)
            }
            .padding(.horizontal, 14)
            .frame(height: 32)
            .background(isSelected ? Color.md3SecondaryContainer : Color.clear)
            .foregroundStyle(isSelected ? Color.md3OnSecondaryContainer : Color.md3OnSurfaceVariant)
            .clipShape(Capsule())
            .overlay(
                Capsule().stroke(isSelected ? Color.clear : Color.md3Outline, lineWidth: 1)
            )
        }
    }

    // MARK: - Actions

    private func loadBadges() async {
        isLoading = true
        do {
            async let allTask = services.badges.getAllBadges()
            async let myTask = services.badges.getMyBadges()
            let (all, my) = try await (allTask, myTask)
            allBadges = all.sorted { $0.sortOrder < $1.sortOrder }
            earnedBadges = my.badges
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func computeBadges() async {
        isComputing = true
        do {
            let result = try await services.badges.computeBadges()
            earnedBadges = result.badges
            newlyEarned = result.newlyEarned ?? 0
        } catch {
            self.error = error.localizedDescription
        }
        isComputing = false
    }

    private func togglePin(badgeId: Int) async {
        do {
            let result = try await services.badges.togglePin(badgeId: badgeId)
            // Update local state
            if let idx = earnedBadges.firstIndex(where: { $0.badgeId == badgeId }) {
                // Reload to get fresh pin state
                let myBadges = try await services.badges.getMyBadges()
                earnedBadges = myBadges.badges
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    // MARK: - Helpers

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
