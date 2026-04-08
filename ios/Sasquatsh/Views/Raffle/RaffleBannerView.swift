import SwiftUI

struct RaffleBannerView: View {
    let raffle: Raffle

    var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .topTrailing) {
                // Banner image or gradient
                if let urlStr = raffle.bannerImageUrl ?? raffle.prizeImageUrl,
                   let url = URL(string: urlStr) {
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

                // Entry count badge
                if let total = raffle.userTotalEntries, total > 0 {
                    HStack(spacing: 4) {
                        Image(systemName: "ticket.fill")
                            .font(.md3LabelSmall)
                        Text("\(total) entries")
                            .font(.md3LabelSmall)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(.ultraThinMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    .padding(8)
                }
            }

            // Info section
            VStack(alignment: .leading, spacing: 4) {
                Text("Monthly Raffle")
                    .font(.md3LabelSmall)
                    .foregroundStyle(Color.md3Primary)

                Text(raffle.prizeName)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                    .lineLimit(1)

                HStack {
                    if let time = raffle.timeRemaining {
                        Label(time, systemImage: "clock")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    Spacer()
                    if let value = raffle.prizeValueFormatted {
                        Text(value)
                            .font(.md3LabelMedium)
                            .foregroundStyle(Color.md3Tertiary)
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
