import SwiftUI

struct RaffleBannerView: View {
    let raffle: Raffle

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .topTrailing) {
                // Banner image or gradient
                if let url = resolveImageURL(raffle.bannerImageUrl) ?? resolveImageURL(raffle.prizeImageUrl) {
                    AsyncImage(url: url) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        bannerGradient
                    }
                    .frame(height: 120)
                    .clipped()
                } else {
                    bannerGradient
                        .frame(height: 120)
                }

                // Monthly Raffle label overlay
                Text("RAFFLE")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(.black.opacity(0.4))
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    .padding(8)
            }

            // Info section
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Monthly Raffle")
                        .font(.md3LabelSmall)
                        .foregroundStyle(Color.md3Primary)
                    Spacer()
                    if let value = raffle.prizeValueFormatted {
                        Text(value)
                            .font(.md3LabelLarge)
                            .foregroundStyle(Color.md3Tertiary)
                    }
                }

                Text(raffle.prizeName)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)

                if let desc = raffle.description, !desc.isEmpty {
                    Text(desc)
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                        .lineLimit(2)
                }

                Divider()

                // Stats row
                HStack(spacing: 16) {
                    if let time = raffle.timeRemaining {
                        Label(time, systemImage: "clock")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }

                    if let stats = raffle.stats {
                        Label("\(stats.displayTotalEntries) entries", systemImage: "ticket")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)

                        Label("\(stats.displayParticipants) players", systemImage: "person.2")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }

                // User's entries
                if let total = raffle.userTotalEntries, total > 0 {
                    HStack(spacing: 6) {
                        Image(systemName: "star.fill")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3Primary)
                        Text("You have \(total) entr\(total == 1 ? "y" : "ies")")
                            .font(.md3BodySmall)
                            .fontWeight(.medium)
                            .foregroundStyle(Color.md3Primary)
                    }
                } else {
                    HStack(spacing: 6) {
                        Image(systemName: "info.circle")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Text("Host or attend games to earn entries!")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                }
            }
            .padding(12)
        }
        .background(Color.md3Surface)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
        .overlay(
            RoundedRectangle(cornerRadius: MD3Shape.medium)
                .stroke(Color.md3OutlineVariant, lineWidth: 1)
        )
    }

    private func resolveImageURL(_ urlStr: String?) -> URL? {
        guard let urlStr, !urlStr.isEmpty else { return nil }
        if urlStr.hasPrefix("http") {
            return URL(string: urlStr)
        }
        // Relative path — resolve against website
        return URL(string: "https://sasquatsh.com\(urlStr)")
    }

    private var bannerGradient: some View {
        LinearGradient(
            colors: [.orange, .red.opacity(0.8)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .overlay(
            VStack {
                Image(systemName: "trophy.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(.white.opacity(0.6))
            }
        )
    }
}
