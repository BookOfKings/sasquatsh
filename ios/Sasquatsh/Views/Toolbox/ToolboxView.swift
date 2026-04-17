import SwiftUI

struct ToolboxView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            CompactNavBar(title: "Gamer Toolbox") { dismiss() }

            NavigationStack {
                ScrollView {
                    VStack(spacing: 16) {
                        Text("Tools for your game night")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                            .padding(.top, 4)

                        NavigationLink {
                            FirstPlayerPickersView()
                        } label: {
                            firstPlayerCard()
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            RoundCounterView()
                        } label: {
                            toolCard(
                                icon: "number.circle.fill",
                                title: "Round Counter",
                                description: "Track game rounds with a tap. Autosaves between sessions.",
                                color: Color.md3Secondary
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            TurnTrackerView()
                        } label: {
                            toolCard(
                                icon: "arrow.triangle.2.circlepath",
                                title: "Turn Tracker",
                                description: "Keep track of whose turn it is. Never lose your place!",
                                color: Color.md3Primary
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            ScoreKeeperView()
                        } label: {
                            toolCard(
                                icon: "list.number",
                                title: "Score Keeper",
                                description: "Track scores for any game. Remembers your players!",
                                color: Color(red: 0.388, green: 0.400, blue: 0.945)
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            RandomGamePickerView()
                        } label: {
                            toolCard(
                                icon: "dice.fill",
                                title: "Random Game Picker",
                                description: "Can't decide what to play? Let fate choose for you!",
                                color: Color.md3Tertiary
                            )
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
                .background(Color.md3SurfaceContainer)
                .toolbar(.hidden, for: .navigationBar)
            }
        }
        .background(Color.md3SurfaceContainer)
    }

    private func firstPlayerCard() -> some View {
        HStack(spacing: 14) {
            Text("1ST")
                .font(.system(size: 18, weight: .black, design: .rounded))
                .foregroundStyle(Color.md3Primary)
                .frame(width: 50, height: 50)
                .background(Color.md3Primary.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))

            VStack(alignment: .leading, spacing: 4) {
                Text("First Player Pickers")
                    .font(.md3TitleMedium)
                    .foregroundStyle(Color.md3OnSurface)
                Text("Multiple ways to pick who goes first")
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

    private func toolCard(icon: String, title: String, description: String, color: Color) -> some View {
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
