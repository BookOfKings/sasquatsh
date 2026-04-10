import SwiftUI

struct CardDetailSheet: View {
    let card: ScryfallCard
    var onAdd: ((String) -> Void)?

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Card image
                    if let url = card.largeImageUrl.flatMap({ URL(string: $0) }) {
                        AsyncImage(url: url) { image in
                            image.resizable().aspectRatio(contentMode: .fit)
                        } placeholder: {
                            Color.md3SurfaceVariant.frame(height: 350)
                        }
                        .frame(maxHeight: 350)
                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                        .padding(.horizontal)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        // Name + mana cost
                        HStack {
                            Text(card.name)
                                .font(.md3TitleLarge)
                            Spacer()
                            if let mana = card.manaCost {
                                Text(mana)
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                        }

                        // Type line
                        if let type = card.typeLine {
                            Text(type)
                                .font(.md3TitleSmall)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }

                        Divider()

                        // Oracle text
                        if let text = card.oracleText, !text.isEmpty {
                            Text(text)
                                .font(.md3BodyMedium)
                        }

                        // Power/Toughness
                        if let power = card.power, let toughness = card.toughness {
                            Text("\(power)/\(toughness)")
                                .font(.md3TitleSmall)
                                .foregroundStyle(Color.md3Primary)
                        }

                        // Set + Rarity
                        HStack {
                            if let set = card.setCode {
                                BadgeView(text: set.uppercased(), color: .md3SecondaryContainer)
                            }
                            if let rarity = card.rarity {
                                BadgeView(text: rarity.capitalized, color: .md3TertiaryContainer)
                            }
                        }

                        // Add buttons
                        if let onAdd {
                            Divider()
                            HStack(spacing: 12) {
                                Button { onAdd("main") } label: {
                                    Text("Add to Main")
                                        .font(.md3LabelLarge)
                                        .foregroundStyle(Color.md3OnPrimary)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 40)
                                        .background(Color.md3Primary)
                                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                                }
                                Button { onAdd("sideboard") } label: {
                                    Text("Sideboard")
                                        .font(.md3LabelLarge)
                                        .foregroundStyle(Color.md3Primary)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 40)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: MD3Shape.medium)
                                                .stroke(Color.md3Primary, lineWidth: 1)
                                        )
                                }
                            }
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
            .background(Color.md3SurfaceContainer)
            .navigationTitle("Card Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
