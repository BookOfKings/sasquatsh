import SwiftUI

struct DeckImportSheet: View {
    var onImport: (ImportDeckInput) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab = 0
    @State private var deckList = ""
    @State private var importUrl = ""
    @State private var deckName = ""
    @State private var formatId: String? = nil

    var body: some View {
        NavigationStack {
            Form {
                Section("Deck Name (Optional)") {
                    TextField("My Deck", text: $deckName)
                }

                Section("Format") {
                    Picker("Format", selection: $formatId) {
                        Text("None").tag(String?.none)
                        ForEach(MtgFormatCategory.allCases) { cat in
                            ForEach(cat.formats, id: \.id) { format in
                                Text(format.name).tag(String?.some(format.id))
                            }
                        }
                    }
                }

                Picker("Import From", selection: $selectedTab) {
                    Text("Text").tag(0)
                    Text("URL").tag(1)
                }
                .pickerStyle(.segmented)

                if selectedTab == 0 {
                    Section("Deck List") {
                        TextField("Paste your deck list here...\n\n4 Lightning Bolt\n4 Counterspell\n20 Island", text: $deckList, axis: .vertical)
                            .lineLimit(8...20)
                            .font(.system(.body, design: .monospaced))
                    }
                } else {
                    Section("URL") {
                        TextField("Moxfield or Archidekt URL", text: $importUrl)
                            .keyboardType(.URL)
                            .autocapitalization(.none)
                    }
                }
            }
            .navigationTitle("Import Deck")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Import") {
                        let input = ImportDeckInput(
                            name: deckName.isEmpty ? nil : deckName,
                            formatId: formatId,
                            deckList: selectedTab == 0 && !deckList.isEmpty ? deckList : nil,
                            url: selectedTab == 1 && !importUrl.isEmpty ? importUrl : nil
                        )
                        onImport(input)
                        dismiss()
                    }
                    .disabled(selectedTab == 0 ? deckList.isEmpty : importUrl.isEmpty)
                }
            }
        }
        .presentationDetents([.large])
    }
}
