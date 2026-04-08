import SwiftUI

struct Warhammer40kConfigFormSections: View {
    @Binding var config: Warhammer40kConfigState

    var body: some View {
        gameSetupSection
        missionSection
        armyRulesSection
        terrainSection
        eventStructureSection
        if config.showCrusadeSettings {
            crusadeSection
        }
        if config.showArmyListSubmission {
            armyListSection
        }
        prizesSection
    }

    // MARK: - Game Setup

    private var gameSetupSection: some View {
        Section("Game Setup") {
            Picker("Game Type", selection: Binding(
                get: { config.gameType ?? "matched" },
                set: { config.gameType = $0 }
            )) {
                Text("Matched Play").tag("matched")
                Text("Narrative").tag("narrative")
                Text("Crusade").tag("crusade")
                Text("Open Play").tag("open")
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("Points Limit")
                    .font(.md3LabelMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach([500, 1000, 1500, 2000, 2500, 3000], id: \.self) { pts in
                            Button {
                                config.pointsLimit = pts
                                config.applyPointsDefaults()
                            } label: {
                                Text("\(pts)")
                                    .font(.md3LabelMedium)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(config.pointsLimit == pts ? Color.md3Primary : Color.md3SurfaceContainerHigh)
                                    .foregroundStyle(config.pointsLimit == pts ? Color.md3OnPrimary : Color.md3OnSurface)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                            }
                        }
                    }
                }

                Stepper("Points: \(config.pointsLimit)", value: $config.pointsLimit, in: 100...5000, step: 100)
                    .onChange(of: config.pointsLimit) { _, _ in
                        config.applyPointsDefaults()
                    }
            }

            Picker("Player Mode", selection: $config.playerMode) {
                Text("1v1").tag("1v1")
                Text("2v2").tag("2v2")
                Text("Group").tag("group")
            }
            .pickerStyle(.segmented)
        }
    }

    // MARK: - Mission

    private var missionSection: some View {
        Section("Mission") {
            Picker("Mission Pack", selection: $config.missionPack) {
                Text("None").tag("")
                Text("Leviathan").tag("leviathan")
                Text("Pariah Nexus").tag("pariah_nexus")
                Text("Chapter Approved").tag("chapter_approved")
                Text("Crusade").tag("crusade")
                Text("Custom").tag("custom")
            }

            if !config.missionPack.isEmpty {
                Picker("Mission Selection", selection: Binding(
                    get: { config.missionSelection ?? "random" },
                    set: { config.missionSelection = $0 }
                )) {
                    Text("Random").tag("random")
                    Text("Pre-Selected").tag("pre_selected")
                }
            }

            TextField("Mission Notes", text: $config.missionNotes, axis: .vertical)
                .lineLimit(2...4)
        }
    }

    // MARK: - Army Rules

    private var armyRulesSection: some View {
        Section("Army Rules") {
            Toggle("Battle Ready Required", isOn: $config.battleReadyRequired)
            Toggle("WYSIWYG Required", isOn: $config.wysiwygRequired)
            Toggle("Forge World Allowed", isOn: $config.forgeWorldAllowed)
            Toggle("Legends Allowed", isOn: $config.legendsAllowed)
            Toggle("Allow Proxies", isOn: $config.allowProxies)

            if config.allowProxies {
                TextField("Proxy Notes", text: $config.proxyNotes, axis: .vertical)
                    .lineLimit(2...3)
            }

            TextField("Army Rules Notes", text: $config.armyRulesNotes, axis: .vertical)
                .lineLimit(2...4)
        }
    }

    // MARK: - Terrain & Table

    private var terrainSection: some View {
        Section("Terrain & Table") {
            Picker("Terrain Type", selection: $config.terrainType) {
                Text("Tournament Standard").tag("tournament")
                Text("Casual").tag("casual")
                Text("Bring Your Own").tag("bring_your_own")
            }

            Picker("Table Size", selection: $config.tableSize) {
                Text("44\" x 30\" (Combat Patrol)").tag("44x30")
                Text("44\" x 60\" (Standard)").tag("44x60")
                Text("Custom").tag("custom")
            }
        }
    }

    // MARK: - Event Structure

    private var eventStructureSection: some View {
        Section("Event Structure") {
            Picker("Event Type", selection: Binding(
                get: { config.eventType ?? "casual" },
                set: { config.eventType = $0 }
            )) {
                Text("Casual").tag("casual")
                Text("Tournament").tag("tournament")
                Text("Campaign").tag("campaign")
                Text("League").tag("league")
            }

            if config.eventType == "tournament" || config.eventType == "league" {
                Picker("Tournament Style", selection: Binding(
                    get: { config.tournamentStyle ?? "swiss" },
                    set: { config.tournamentStyle = $0 }
                )) {
                    Text("Swiss").tag("swiss")
                    Text("Single Elimination").tag("single_elimination")
                    Text("Round Robin").tag("round_robin")
                }

                HStack {
                    Text("Rounds")
                    Spacer()
                    TextField("Auto", value: $config.roundsCount, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 80)
                }

                Stepper("Round Time: \(config.roundTimeMinutes) min", value: $config.roundTimeMinutes, in: 30...300, step: 15)

                Toggle("Include Top Cut", isOn: $config.includeTopCut)

                Picker("Scoring", selection: Binding(
                    get: { config.scoringType ?? "win_loss" },
                    set: { config.scoringType = $0 }
                )) {
                    Text("Win/Loss").tag("win_loss")
                    Text("Win/Draw/Loss").tag("win_draw_loss")
                    Text("Battle Points").tag("battle_points")
                }
            }
        }
    }

    // MARK: - Crusade

    private var crusadeSection: some View {
        Section("Crusade Settings") {
            Stepper("Starting Supply Limit: \(config.startingSupplyLimit)", value: $config.startingSupplyLimit, in: 500...3000, step: 250)
            Stepper("Starting Crusade Points: \(config.startingCrusadePoints)", value: $config.startingCrusadePoints, in: 0...20)

            TextField("Progression Notes", text: $config.crusadeProgressionNotes, axis: .vertical)
                .lineLimit(2...4)
        }
    }

    // MARK: - Army List Submission

    private var armyListSection: some View {
        Section("Army List Submission") {
            Toggle("Require Army List", isOn: $config.requireArmyList)

            if config.requireArmyList {
                DatePicker("Submission Deadline",
                    selection: Binding(
                        get: { config.armyListDeadline ?? Date() },
                        set: { config.armyListDeadline = $0 }
                    ),
                    displayedComponents: [.date, .hourAndMinute]
                )

                TextField("Submission Notes", text: $config.armyListNotes, axis: .vertical)
                    .lineLimit(2...3)
            }
        }
    }

    // MARK: - Prizes

    private var prizesSection: some View {
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
