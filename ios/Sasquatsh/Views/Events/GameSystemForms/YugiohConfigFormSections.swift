import SwiftUI

struct YugiohConfigFormSections: View {
    @Binding var config: YugiohConfigState

    var body: some View {
        yugiohFormatSection
        yugiohEventStructureSection
        yugiohDeckRulesSection
        yugiohPrizesSection
    }

    // MARK: - Format

    private var yugiohFormatSection: some View {
        Section("Yu-Gi-Oh! Format") {
            Picker("Format", selection: $config.formatId) {
                Text("Select Format").tag(String?.none)
                Text("Advanced").tag(String?.some("advanced"))
                Text("Traditional").tag(String?.some("traditional"))
                Text("Speed Duel").tag(String?.some("speed_duel"))
                Text("Time Wizard").tag(String?.some("time_wizard"))
                Text("Casual").tag(String?.some("casual"))
            }
            .onChange(of: config.formatId) { _, _ in
                config.applyFormatDefaults()
            }
        }
    }

    // MARK: - Event Structure

    private var yugiohEventStructureSection: some View {
        Section("Event Structure") {
            Picker("Event Type", selection: Binding(
                get: { config.eventType ?? "casual" },
                set: { config.eventType = $0 }
            )) {
                Text("Casual").tag("casual")
                Text("Locals").tag("locals")
                Text("OTS Championship").tag("ots")
                Text("Regional").tag("regional")
                Text("YCS").tag("ycs")
                Text("Nationals").tag("nationals")
                Text("Worlds").tag("worlds")
            }
            .onChange(of: config.eventType) { _, _ in
                config.applyEventTypeDefaults()
            }

            if config.eventType != nil && config.eventType != "casual" {
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

    private var yugiohDeckRulesSection: some View {
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

            Toggle("Allow Side Deck", isOn: $config.allowSideDeck)
            Toggle("Enforce Format Legality", isOn: $config.enforceFormatLegality)

            TextField("House Rules", text: $config.houseRulesNotes, axis: .vertical)
                .lineLimit(2...4)
        }
    }

    // MARK: - Prizes & Official Play

    private var yugiohPrizesSection: some View {
        Section("Prizes & Official Play") {
            Toggle("Official Event", isOn: $config.isOfficialEvent)

            if config.isOfficialEvent {
                Toggle("Awards OTS Points", isOn: $config.awardsOtsPoints)
            }

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
