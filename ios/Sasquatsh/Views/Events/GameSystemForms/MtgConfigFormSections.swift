import SwiftUI

struct MtgConfigFormSections: View {
    @Binding var config: MtgConfigState

    var body: some View {
        mtgFormatSection
        if config.showPowerLevel {
            mtgPowerLevelSection
        }
        mtgEventStructureSection
        mtgDeckRulesSection
        if config.isLimitedFormat {
            mtgDraftSection
        }
        mtgPrizesSection
    }

    // MARK: - Format

    private var mtgFormatSection: some View {
        Section("MTG Format") {
            Picker("Category", selection: $config.formatCategory) {
                ForEach(MtgFormatCategory.allCases) { cat in
                    Text(cat.displayName).tag(cat)
                }
            }
            .pickerStyle(.segmented)
            .onChange(of: config.formatCategory) { _, newCat in
                if let first = newCat.formats.first {
                    config.formatId = first.id
                    config.applyFormatDefaults()
                }
            }

            Picker("Format", selection: $config.formatId) {
                Text("Select Format").tag(String?.none)
                ForEach(config.formatCategory.formats, id: \.id) { format in
                    Text(format.name).tag(String?.some(format.id))
                }
            }
            .onChange(of: config.formatId) { _, _ in
                config.applyFormatDefaults()
            }

            if config.formatId == "custom" {
                TextField("Custom Format Name", text: $config.customFormatName)
            }
        }
    }

    // MARK: - Power Level

    private var mtgPowerLevelSection: some View {
        Section("Power Level") {
            Picker("Power Level", selection: Binding(
                get: { config.powerLevelRange ?? "mid" },
                set: { config.powerLevelRange = $0 }
            )) {
                Text("Casual (1-4)").tag("casual")
                Text("Mid (5-6)").tag("mid")
                Text("High (7-8)").tag("high")
                Text("cEDH (9-10)").tag("cedh")
                Text("Custom").tag("custom")
            }

            if config.powerLevelRange == "custom" {
                Stepper("Min: \(config.powerLevelMin ?? 1)", value: Binding(
                    get: { config.powerLevelMin ?? 1 },
                    set: { config.powerLevelMin = $0 }
                ), in: 1...10)

                Stepper("Max: \(config.powerLevelMax ?? 10)", value: Binding(
                    get: { config.powerLevelMax ?? 10 },
                    set: { config.powerLevelMax = $0 }
                ), in: 1...10)
            }
        }
    }

    // MARK: - Event Structure

    private var mtgEventStructureSection: some View {
        Section("Event Structure") {
            Picker("Event Type", selection: $config.eventType) {
                Text("Casual").tag("casual")
                Text("Pods").tag("pods")
                Text("Swiss").tag("swiss")
                Text("Single Elimination").tag("single_elim")
                Text("Double Elimination").tag("double_elim")
                Text("Round Robin").tag("round_robin")
            }

            if config.eventType != "casual" {
                Picker("Play Mode", selection: Binding(
                    get: { config.playMode ?? "open_play" },
                    set: { config.playMode = $0 }
                )) {
                    Text("Open Play").tag("open_play")
                    Text("Assigned Pods").tag("assigned_pods")
                    Text("Tournament Pairings").tag("tournament_pairings")
                }

                Picker("Match Style", selection: Binding(
                    get: { config.matchStyle ?? "bo1" },
                    set: { config.matchStyle = $0 }
                )) {
                    Text("Best of 1").tag("bo1")
                    Text("Best of 3").tag("bo3")
                }
                .pickerStyle(.segmented)

                Stepper("Round Time: \(config.roundTimeMinutes) min", value: $config.roundTimeMinutes, in: 10...180, step: 5)

                HStack {
                    Text("Rounds")
                    Spacer()
                    TextField("Auto", value: $config.roundsCount, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }
            }

            if config.eventType == "pods" {
                Stepper("Pod Size: \(config.podsSize)", value: $config.podsSize, in: 2...8)
            }

            if ["swiss", "single_elim", "double_elim"].contains(config.eventType) {
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

    private var mtgDeckRulesSection: some View {
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

            TextField("House Rules", text: $config.houseRulesNotes, axis: .vertical)
                .lineLimit(2...4)
        }
    }

    // MARK: - Draft / Sealed

    private var mtgDraftSection: some View {
        Section("Draft / Sealed") {
            Stepper("Packs Per Player: \(config.packsPerPlayer)", value: $config.packsPerPlayer, in: 1...6)

            Picker("Draft Style", selection: $config.draftStyle) {
                Text("Standard").tag("standard")
                Text("Rochester").tag("rochester")
                Text("Winston").tag("winston")
                Text("Grid").tag("grid")
            }

            if config.formatId == "cube" {
                TextField("Cube ID / Link", text: $config.cubeId)
            }
        }
    }

    // MARK: - Prizes

    private var mtgPrizesSection: some View {
        Section("Prizes & Entry") {
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

            Toggle("Allow Spectators", isOn: $config.allowSpectators)
        }
    }
}
