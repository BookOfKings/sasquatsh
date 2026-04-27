import SwiftUI

struct BadgeEarnedPopup: View {
    let badges: [UserBadge]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                // Celebration header
                VStack(spacing: 8) {
                    Text("🎉")
                        .font(.system(size: 48))

                    Text(badges.count == 1 ? "Badge Earned!" : "\(badges.count) Badges Earned!")
                        .font(.md3HeadlineMedium)
                        .foregroundStyle(Color.md3OnSurface)

                    Text("Congratulations on your achievement!")
                        .font(.md3BodyMedium)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                .padding(.top, 16)

                // Badge list
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(badges) { userBadge in
                            let badge = userBadge.badge
                            HStack(spacing: 14) {
                                ZStack {
                                    Circle()
                                        .fill(categoryColor(badge.category).opacity(0.15))
                                        .frame(width: 52, height: 52)
                                    Text(categoryEmoji(badge.category))
                                        .font(.system(size: 24))
                                }

                                VStack(alignment: .leading, spacing: 4) {
                                    Text(badge.name)
                                        .font(.md3TitleSmall)
                                        .foregroundStyle(Color.md3OnSurface)
                                    Text(badge.description)
                                        .font(.md3BodySmall)
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                        .lineLimit(2)
                                    Text(badge.category.capitalized)
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(categoryColor(badge.category))
                                }

                                Spacer()
                            }
                            .padding(12)
                            .background(
                                RoundedRectangle(cornerRadius: MD3Shape.medium)
                                    .fill(categoryColor(badge.category).opacity(0.05))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: MD3Shape.medium)
                                            .stroke(categoryColor(badge.category).opacity(0.2), lineWidth: 1)
                                    )
                            )
                        }
                    }
                    .padding(.horizontal)
                }

                Button {
                    dismiss()
                } label: {
                    Text("Awesome!")
                        .primaryButtonStyle()
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
            }
            .background(Color.md3SurfaceContainer)
        }
    }

    private func categoryColor(_ category: String) -> Color {
        switch category {
        case "social": return .blue
        case "hosting": return .orange
        case "gaming": return .purple
        case "collection": return .green
        case "community": return .pink
        case "planning": return .teal
        case "special": return .yellow
        case "veteran": return .red
        default: return .md3Primary
        }
    }

    private func categoryEmoji(_ category: String) -> String {
        switch category {
        case "social": return "🤝"
        case "hosting": return "🎲"
        case "gaming": return "🏆"
        case "collection": return "📚"
        case "community": return "👥"
        case "planning": return "📅"
        case "special": return "⭐"
        case "veteran": return "🎖️"
        default: return "🏅"
        }
    }
}
