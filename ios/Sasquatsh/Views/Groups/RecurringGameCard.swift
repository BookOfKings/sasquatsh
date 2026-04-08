import SwiftUI

struct RecurringGameCard: View {
    let game: RecurringGame
    let isAdmin: Bool
    var onEdit: (() -> Void)?
    var onToggleActive: (() -> Void)?
    var onDelete: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(game.title)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Spacer()
                BadgeView(
                    text: game.isActive ? "Active" : "Paused",
                    color: game.isActive ? .md3PrimaryContainer : .md3SecondaryContainer
                )
            }

            Label(game.scheduleDescription, systemImage: "calendar.badge.clock")
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)

            HStack(spacing: 8) {
                if let system = game.gameSystem, !system.isEmpty,
                   let gs = GameSystem(rawValue: system), gs != .boardGame {
                    BadgeView(text: gs.shortName, color: gs.badgeColor)
                }

                if let gameTitle = game.gameTitle, !gameTitle.isEmpty {
                    BadgeView(text: gameTitle, color: .md3SurfaceContainerHigh)
                }

                BadgeView(text: "\(game.durationMinutes) min", color: .md3SurfaceContainerHigh)
                BadgeView(text: "\(game.maxPlayers) players", color: .md3SurfaceContainerHigh)
            }

            if let city = game.city, let state = game.state {
                Label("\(city), \(state)", systemImage: "mappin")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            } else if let details = game.locationDetails, !details.isEmpty {
                Label(details, systemImage: "mappin")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }

            if let nextDate = game.nextOccurrenceDate {
                Label("Next: \(nextDate.toDate?.displayDate ?? nextDate)", systemImage: "arrow.right.circle")
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3Primary)
            }
        }
        .padding()
        .cardStyle()
        .contextMenu {
            if isAdmin {
                Button { onEdit?() } label: {
                    Label("Edit", systemImage: "pencil")
                }
                Button {
                    onToggleActive?()
                } label: {
                    Label(game.isActive ? "Pause" : "Resume", systemImage: game.isActive ? "pause.circle" : "play.circle")
                }
                Button(role: .destructive) { onDelete?() } label: {
                    Label("Delete", systemImage: "trash")
                }
            }
        }
    }
}
