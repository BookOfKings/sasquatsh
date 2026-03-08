import SwiftUI

struct EventCard: View {
    let event: EventSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(event.title)
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                        .lineLimit(1)

                    if let gameTitle = event.gameTitle, !gameTitle.isEmpty {
                        Text(gameTitle)
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }

                Spacer()

                if event.isCharityEvent {
                    BadgeView(text: "Charity", color: .md3TertiaryContainer)
                }
            }

            HStack(spacing: 16) {
                Label {
                    Text(formattedDate)
                        .font(.md3BodySmall)
                } icon: {
                    Image(systemName: "calendar")
                        .foregroundStyle(Color.md3Primary)
                }

                if let startTime = event.startTime {
                    Label {
                        Text(startTime)
                            .font(.md3BodySmall)
                    } icon: {
                        Image(systemName: "clock")
                            .foregroundStyle(Color.md3Primary)
                    }
                }
            }

            HStack(spacing: 16) {
                if let city = event.city, let state = event.state {
                    Label {
                        Text("\(city), \(state)")
                            .font(.md3BodySmall)
                    } icon: {
                        Image(systemName: "mappin")
                            .foregroundStyle(Color.md3Tertiary)
                    }
                }

                Spacer()

                if let maxPlayers = event.maxPlayers {
                    Label {
                        Text("\(event.confirmedCount)/\(maxPlayers)")
                            .font(.md3BodySmall)
                            .foregroundStyle(spotsColor)
                    } icon: {
                        Image(systemName: "person.2")
                            .foregroundStyle(spotsColor)
                    }
                }
            }

            if let difficulty = event.difficultyLevel {
                HStack {
                    BadgeView(text: difficulty.capitalized, color: difficultyColor(difficulty))
                    if let category = event.gameCategory {
                        BadgeView(text: GameCategory(rawValue: category)?.displayName ?? category, color: .md3PrimaryContainer)
                    }
                }
            }
        }
        .padding()
        .cardStyle()
    }

    private var formattedDate: String {
        event.eventDate.toDate?.displayDate ?? event.eventDate
    }

    private var spotsColor: Color {
        guard let maxPlayers = event.maxPlayers else { return .md3Primary }
        let remaining = maxPlayers - event.confirmedCount
        if remaining <= 0 { return .md3Error }
        if remaining <= 2 { return .orange }
        return .md3Primary
    }

    private func difficultyColor(_ level: String) -> Color {
        switch level {
        case "beginner": return .md3PrimaryContainer
        case "intermediate": return .md3TertiaryContainer
        case "advanced": return .md3SecondaryContainer
        default: return .md3PrimaryContainer
        }
    }
}
