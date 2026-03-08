import SwiftUI

struct LoadingView: View {
    var message: String = "Loading..."

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .controlSize(.large)
                .tint(Color.md3Primary)
            Text(message)
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
