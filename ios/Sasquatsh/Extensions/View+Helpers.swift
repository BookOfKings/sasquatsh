import SwiftUI

extension View {
    // MARK: - MD3 Cards

    func cardStyle() -> some View {
        self
            .background(Color.md3SurfaceContainerLowest)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .shadow(
                color: .black.opacity(MD3Elevation.level1.shadowOpacity),
                radius: MD3Elevation.level1.shadowRadius,
                x: 0,
                y: MD3Elevation.level1.shadowY
            )
    }

    func md3ElevatedCard() -> some View {
        self
            .background(Color.md3SurfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .shadow(
                color: .black.opacity(MD3Elevation.level1.shadowOpacity),
                radius: MD3Elevation.level1.shadowRadius,
                x: 0,
                y: MD3Elevation.level1.shadowY
            )
    }

    func md3OutlinedCard() -> some View {
        self
            .background(Color.md3SurfaceContainerLowest)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.medium)
                    .stroke(Color.md3OutlineVariant, lineWidth: 1)
            )
    }

    // MARK: - MD3 Buttons

    func primaryButtonStyle() -> some View {
        self
            .font(.md3LabelLarge)
            .foregroundStyle(Color.md3OnPrimary)
            .frame(maxWidth: .infinity)
            .frame(height: 40)
            .background(Color.md3Primary)
            .clipShape(Capsule())
    }

    func secondaryButtonStyle() -> some View {
        self
            .font(.md3LabelLarge)
            .foregroundStyle(Color.md3OnSecondaryContainer)
            .frame(maxWidth: .infinity)
            .frame(height: 40)
            .background(Color.md3SecondaryContainer)
            .clipShape(Capsule())
    }

    func outlinedButtonStyle() -> some View {
        self
            .font(.md3LabelLarge)
            .foregroundStyle(Color.md3Primary)
            .frame(maxWidth: .infinity)
            .frame(height: 40)
            .background(Color.clear)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(Color.md3Outline, lineWidth: 1))
    }
}
