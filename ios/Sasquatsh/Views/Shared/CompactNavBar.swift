import SwiftUI

struct CompactNavBar: View {
    let title: String
    var onBack: (() -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            // This spacer fills the safe area behind the status bar/dynamic island
            Color.md3Surface
                .frame(height: 0)
                .ignoresSafeArea(edges: .top)

            HStack(spacing: 8) {
                if let onBack {
                    Button {
                        onBack()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.md3Primary)
                            .frame(width: 28, height: 28)
                    }
                }

                Text(title)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)

                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
        }
        .background(Color.md3Surface.ignoresSafeArea(edges: .top))
        .overlay(alignment: .bottom) { Divider() }
    }
}
