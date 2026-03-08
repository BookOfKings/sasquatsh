import SwiftUI

struct SearchBarView: View {
    @Binding var text: String
    var placeholder: String = "Search..."

    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color.md3OnSurfaceVariant)

            TextField(placeholder, text: $text)
                .font(.md3BodyLarge)
                .textFieldStyle(.plain)
                .autocorrectionDisabled()

            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
            }
        }
        .padding(12)
        .background(Color.md3SurfaceContainerHigh)
        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.extraLarge))
    }
}
