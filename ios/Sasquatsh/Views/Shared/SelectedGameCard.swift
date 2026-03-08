import SwiftUI

struct SelectedGameCard: View {
    let game: BggGame
    let isPrimary: Bool
    var onSetPrimary: (() -> Void)?
    var onRemove: (() -> Void)?

    var body: some View {
        HStack(spacing: 12) {
            thumbnail

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(game.name)
                        .font(.md3TitleMedium)
                        .foregroundStyle(Color.md3OnSurface)
                        .lineLimit(1)

                    if let year = game.yearPublished {
                        Text("(\(year))")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }

                HStack(spacing: 12) {
                    if let minP = game.minPlayers, let maxP = game.maxPlayers {
                        Label("\(minP)-\(maxP)", systemImage: "person.2")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }

                    if let playtime = game.playingTime {
                        Label("\(playtime) min", systemImage: "clock")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }

                    if let weight = game.weight {
                        Text(weightText(weight))
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }

            Spacer()

            if isPrimary {
                BadgeView(text: "Primary", color: .md3PrimaryContainer)
            } else {
                Button {
                    onSetPrimary?()
                } label: {
                    Image(systemName: "star")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                .buttonStyle(.plain)
            }

            Button {
                onRemove?()
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            }
            .buttonStyle(.plain)
        }
        .padding(12)
        .md3OutlinedCard()
        .overlay(
            isPrimary
                ? RoundedRectangle(cornerRadius: MD3Shape.medium)
                    .stroke(Color.md3Primary, lineWidth: 2)
                : nil
        )
    }

    @ViewBuilder
    private var thumbnail: some View {
        if let urlString = game.thumbnailUrl, let url = URL(string: urlString) {
            AsyncImage(url: url) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                dicePlaceholder
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
        } else {
            dicePlaceholder
        }
    }

    private var dicePlaceholder: some View {
        RoundedRectangle(cornerRadius: MD3Shape.small)
            .fill(Color.md3SurfaceContainerHigh)
            .frame(width: 56, height: 56)
            .overlay(
                Image(systemName: "dice")
                    .foregroundStyle(Color.md3OnSurfaceVariant)
            )
    }

    private func weightText(_ weight: Double) -> String {
        if weight < 2.0 { return "Light" }
        if weight < 3.5 { return "Medium" }
        return "Heavy"
    }
}
