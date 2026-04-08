import SwiftUI

struct PokemonConfigFormSections: View {
    @Binding var config: PokemonConfigState

    var body: some View {
        pokemonFormatSection
        pokemonEventStructureSection
        pokemonDeckRulesSection
        pokemonMaterialsSection
        pokemonPrizesSection
    }

    // MARK: - Format

    private var pokemonFormatSection: some View {
        Section("Pokémon TCG Format") {
            Picker("Format", selection: $config.formatId) {
                Text("Select Format").tag(String?.none)
                Text("Standard").tag(String?.some("standard"))
                Text("Expanded").tag(String?.some("expanded"))
                Text("Unlimited").tag(String?.some("unlimited"))
                Text("Theme").tag(String?.some("theme"))
            }
            .onChange(of: config.formatId) { _, _ in
                config.applyFormatDefaults()
            }
        }
    }

    // MARK: - Event Structure

    private var pokemonEventStructureSection: some View {
        Section("Event Structure") {
            Picker("Event Type", selection: $config.eventType) {
                Text("Casual").tag("casual")
                Text("League").tag("league")
                Text("League Cup").tag("league_cup")
                Text("League Challenge").tag("league_challenge")
                Text("Regional").tag("regional")
                Text("International").tag("international")
                Text("Worlds").tag("worlds")
                Text("Prerelease").tag("prerelease")
                Text("Draft").tag("draft")
            }
            .onChange(of: config.eventType) { _, _ in
                config.applyEventTypeDefaults()
            }

            if config.eventType != "casual" {
                Picker("Tournament Style", selection: Binding(
                    get: { config.tournamentStyle ?? "swiss" },
                    set: { config.tournamentStyle = $0 }
                )) {
                    Text("Swiss").tag("swiss")
                    Text("Single Elimination").tag("single_elimination")
                    Text("Double Elimination").tag("double_elimination")
                }

                Picker("Best Of", selection: $config.bestOf) {
                    Text("Best of 1").tag(1)
                    Text("Best of 3").tag(3)
                }
                .pickerStyle(.segmented)

                Stepper("Round Time: \(config.roundTimeMinutes) min", value: $config.roundTimeMinutes, in: 10...120, step: 5)

                HStack {
                    Text("Rounds")
                    Spacer()
                    TextField("Auto", value: $config.roundsCount, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }

                HStack {
                    Text("Top Cut")
                    Spacer()
                    TextField("None", value: $config.topCut, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }
            }
        }
    }

    // MARK: - Deck Rules

    private var pokemonDeckRulesSection: some View {
        Section("Deck Rules") {
            Toggle("Allow Proxies", isOn: $config.allowProxies)

            if config.allowProxies {
                HStack {
                    Text("Proxy Limit")
                    Spacer()
                    TextField("Unlimited", value: $config.proxyLimit, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }
            }

            Toggle("Require Deck Registration", isOn: $config.requireDeckRegistration)

            if config.requireDeckRegistration {
                DatePicker("Submission Deadline",
                    selection: Binding(
                        get: { config.deckSubmissionDeadline ?? Date() },
                        set: { config.deckSubmissionDeadline = $0 }
                    ),
                    displayedComponents: [.date, .hourAndMinute]
                )
            }

            Toggle("Allow Deck Changes", isOn: $config.allowDeckChanges)
            Toggle("Enforce Format Legality", isOn: $config.enforceFormatLegality)

            TextField("House Rules", text: $config.houseRulesNotes, axis: .vertical)
                .lineLimit(2...4)
        }
    }

    // MARK: - Materials

    private var pokemonMaterialsSection: some View {
        Section("Event Materials") {
            Toggle("Provides Basic Energy", isOn: $config.providesBasicEnergy)
            Toggle("Provides Damage Counters", isOn: $config.providesDamageCounters)
            Toggle("Sleeves Recommended", isOn: $config.sleevesRecommended)
            Toggle("Provides Build & Battle Kits", isOn: $config.providesBuildBattleKits)
        }
    }

    // MARK: - Prizes & Play

    private var pokemonPrizesSection: some View {
        Section("Prizes & Official Play") {
            Toggle("Has Prizes", isOn: $config.hasPrizes)

            if config.hasPrizes {
                TextField("Prize Structure", text: $config.prizeStructure, axis: .vertical)
                    .lineLimit(2...4)
            }

            HStack {
                Text("Entry Fee")
                Spacer()
                TextField("Free", text: $config.entryFee)
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 80)
                Picker("", selection: $config.entryFeeCurrency) {
                    Text("USD").tag("USD")
                    Text("EUR").tag("EUR")
                    Text("GBP").tag("GBP")
                    Text("CAD").tag("CAD")
                }
                .frame(width: 70)
            }

            Toggle("Use Play! Points", isOn: $config.usePlayPoints)
            Toggle("Official Location Confirmed", isOn: $config.organizerConfirmedOfficialLocation)

            VStack(alignment: .leading, spacing: 4) {
                Text("Age Divisions")
                    .font(.md3LabelMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                Toggle("Junior", isOn: $config.hasJuniorDivision)
                Toggle("Senior", isOn: $config.hasSeniorDivision)
                Toggle("Masters", isOn: $config.hasMastersDivision)
            }

            Toggle("Allow Spectators", isOn: $config.allowSpectators)
        }
    }
}
