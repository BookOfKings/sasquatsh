import SwiftUI

struct FirstPlayerPickersView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            CompactNavBar(title: "First Player Pickers") { dismiss() }
            ScrollView {
                VStack(spacing: 16) {
                    NavigationLink {
                        FirstPlayerPickerView()
                    } label: {
                        pickerCard(
                            icon: "hand.raised.fingers.spread.fill",
                            title: "Finger Picker",
                            description: "Everyone puts a finger on the screen — one gets picked to go first!",
                            color: Color.md3Primary
                        )
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        SpinWheelPickerView()
                    } label: {
                        pickerCard(
                            icon: "circle.dotted.and.circle",
                            title: "Spin the Wheel",
                            description: "Price is Right style! Spin the wheel to pick who goes first.",
                            color: Color.md3Error
                        )
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        CardDrawPickerView()
                    } label: {
                        pickerCard(
                            icon: "suit.spade.fill",
                            title: "High Card Draw",
                            description: "Draw from a deck of cards — highest card goes first! Ties get a redraw.",
                            color: Color(red: 0.1, green: 0.2, blue: 0.6)
                        )
                    }
                    .buttonStyle(.plain)

                    NavigationLink {
                        StatementPickerView()
                    } label: {
                        pickerCard(
                            icon: "quote.opening",
                            title: "Whoever Last Picker",
                            description: "3 random prompts to decide who goes first — no luck needed!",
                            color: Color.md3Tertiary
                        )
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal)
                .padding(.bottom)
                .padding(.top, 4)
            }
            .background(Color.md3SurfaceContainer)
        }
        .background(Color.md3SurfaceContainer)
        .toolbar(.hidden, for: .navigationBar)
    }

    private func pickerCard(icon: String, title: String, description: String, color: Color) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 28))
                .foregroundStyle(color)
                .frame(width: 50, height: 50)
                .background(color.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Text(description)
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .lineLimit(2)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.md3BodySmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .padding()
        .cardStyle()
    }
}
