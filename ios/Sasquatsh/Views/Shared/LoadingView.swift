import SwiftUI

struct LoadingView: View {
    var message: String = "Loading..."

    var body: some View {
        VStack(spacing: 16) {
            D20SpinnerView(size: 50)
                .frame(width: 50, height: 50)
            Text(message)
                .font(.md3BodyMedium)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
