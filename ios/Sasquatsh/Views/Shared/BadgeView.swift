import SwiftUI

struct BadgeView: View {
    let text: String
    var color: Color = .md3SurfaceContainerHigh

    var body: some View {
        Text(text)
            .font(.md3LabelSmall)
            .foregroundStyle(Color.md3OnSurfaceVariant)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.small)
                    .stroke(Color.md3OutlineVariant, lineWidth: 1)
            )
    }
}
