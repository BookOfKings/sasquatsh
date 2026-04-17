import SwiftUI

struct DeckBuilderView: View {
    let deck: MtgDeck?
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    @State private var vm = DeckBuilderViewModel()
    @State private var showImport = false

    var body: some View {
        Form {
            // Deck settings
            Section("Deck Settings") {
                TextField("Deck Name", text: $vm.deckName)

                Picker("Format", selection: $vm.formatId) {
                    Text("None").tag(String?.none)
                    ForEach(MtgFormatCategory.allCases) { cat in
                        ForEach(cat.formats, id: \.id) { format in
                            Text(format.name).tag(String?.some(format.id))
                        }
                    }
                }

                TextField("Description", text: $vm.description, axis: .vertical)
                    .lineLimit(2...4)

                Stepper("Power Level: \(vm.powerLevel ?? 0)", value: Binding(
                    get: { vm.powerLevel ?? 5 },
                    set: { vm.powerLevel = $0 }
                ), in: 1...10)

                Toggle("Public", isOn: $vm.isPublic)
            }

            // Stats bar
            Section("Deck Stats") {
                HStack(spacing: 16) {
                    statBadge("Total", value: vm.totalCards)
                    statBadge("Creatures", value: vm.creatureCount)
                    statBadge("Spells", value: vm.spellCount)
                    statBadge("Lands", value: vm.landCount)
                }

                HStack {
                    Text("Avg CMC")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Spacer()
                    Text(String(format: "%.2f", vm.avgCMC))
                        .font(.md3TitleSmall)
                        .foregroundStyle(Color.md3Primary)
                }
            }

            // Card search
            Section("Add Cards") {
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    TextField("Search cards...", text: $vm.searchQuery)
                        .onChange(of: vm.searchQuery) { _, newValue in
                            vm.searchCards(query: newValue)
                        }
                    if vm.isSearching {
                    D20ProgressView(size: 32)
                            .controlSize(.small)
                            .tint(Color.md3Primary)
                    }
                    if !vm.searchQuery.isEmpty {
                        Button {
                            vm.clearSearch()
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }

                ForEach(vm.searchResults.prefix(8)) { card in
                    Button {
                        vm.addCard(card)
                        vm.clearSearch()
                    } label: {
                        HStack(spacing: 10) {
                            if let url = card.smallImageUrl.flatMap({ URL(string: $0) }) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.md3SurfaceVariant
                                }
                                .frame(width: 36, height: 50)
                                .clipShape(RoundedRectangle(cornerRadius: 4))
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(card.name)
                                    .font(.md3BodyMedium)
                                    .foregroundStyle(Color.md3OnSurface)
                                    .lineLimit(1)
                                HStack(spacing: 4) {
                                    if let type = card.typeLine {
                                        Text(type)
                                            .font(.md3LabelSmall)
                                            .foregroundStyle(Color.md3OnSurfaceVariant)
                                            .lineLimit(1)
                                    }
                                }
                            }

                            Spacer()

                            if let mana = card.manaCost, !mana.isEmpty {
                                Text(mana)
                                    .font(.md3LabelSmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }

                            Image(systemName: "plus.circle.fill")
                                .foregroundStyle(Color.md3Primary)
                        }
                    }
                }
            }

            // Deck list by type
            ForEach(vm.cardsByType, id: \.type) { group in
                Section("\(group.type) (\(group.cards.reduce(0) { $0 + $1.quantity }))") {
                    ForEach(Array(group.cards.enumerated()), id: \.element.id) { index, card in
                        HStack(spacing: 8) {
                            if let url = card.card?.smallImageUrl.flatMap({ URL(string: $0) }) {
                                AsyncImage(url: url) { image in
                                    image.resizable().aspectRatio(contentMode: .fill)
                                } placeholder: {
                                    Color.md3SurfaceVariant
                                }
                                .frame(width: 28, height: 40)
                                .clipShape(RoundedRectangle(cornerRadius: 3))
                            }

                            Text(card.card?.name ?? card.scryfallId)
                                .font(.md3BodyMedium)
                                .lineLimit(1)

                            Spacer()

                            // Quantity stepper
                            HStack(spacing: 4) {
                                Button {
                                    if let idx = vm.cards.firstIndex(where: { $0.id == card.id }) {
                                        vm.updateQuantity(at: idx, quantity: card.quantity - 1)
                                    }
                                } label: {
                                    Image(systemName: "minus.circle")
                                        .foregroundStyle(Color.md3OnSurfaceVariant)
                                }

                                Text("\(card.quantity)")
                                    .font(.md3LabelLarge)
                                    .frame(minWidth: 20)

                                Button {
                                    if let idx = vm.cards.firstIndex(where: { $0.id == card.id }) {
                                        vm.updateQuantity(at: idx, quantity: card.quantity + 1)
                                    }
                                } label: {
                                    Image(systemName: "plus.circle")
                                        .foregroundStyle(Color.md3Primary)
                                }
                            }
                        }
                    }
                }
            }

            // Sideboard
            if !vm.sideboardCards.isEmpty {
                Section("Sideboard (\(vm.sideboardCards.reduce(0) { $0 + $1.quantity }))") {
                    ForEach(vm.sideboardCards) { card in
                        HStack {
                            Text(card.card?.name ?? card.scryfallId)
                                .font(.md3BodyMedium)
                            Spacer()
                            Text("x\(card.quantity)")
                                .font(.md3LabelMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                        }
                    }
                }
            }

            if let error = vm.error {
                Section {
                    Text(error).foregroundStyle(Color.md3Error)
                }
            }
        }
        .navigationTitle(vm.isEditing ? "Edit Deck" : "New Deck")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .primaryAction) {
                HStack {
                    Button {
                        showImport = true
                    } label: {
                        Image(systemName: "square.and.arrow.down")
                    }

                    Button(vm.isSaving ? "Saving..." : "Save") {
                        Task {
                            if let _ = await vm.saveDeck() {
                                dismiss()
                            }
                        }
                    }
                    .disabled(!vm.isValid || vm.isSaving)
                }
            }
        }
        .sheet(isPresented: $showImport) {
            DeckImportSheet { input in
                Task {
                    if let _ = try? await services.mtgDecks.importDeck(input: input) {
                        dismiss()
                    }
                }
            }
        }
        .task {
            vm.configure(services: services)
            if let deck {
                vm.loadForEdit(deck: deck)
            }
        }
    }

    private func statBadge(_ label: String, value: Int) -> some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.md3TitleSmall)
                .foregroundStyle(Color.md3Primary)
            Text(label)
                .font(.md3LabelSmall)
                .foregroundStyle(Color.md3OnSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
    }
}
