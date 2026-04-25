import SwiftUI

struct RecurringGameFormSheet: View {
    let groupId: String
    let game: RecurringGame?
    let onSave: (CreateRecurringGameInput?, UpdateRecurringGameInput?) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var title = ""
    @State private var description = ""
    @State private var frequency = "weekly"
    @State private var dayOfWeek = 5 // Friday
    @State private var monthlyWeek: Int? = nil
    @State private var startTime = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.date(from: "19:00") ?? Date()
    }()
    @State private var durationMinutes = 120
    @State private var gameSystem: GameSystem = .boardGame
    @State private var gameTitle = ""
    @State private var maxPlayers = 8
    @State private var hostIsPlaying = true
    @State private var isPublic = true
    @State private var addressLine1 = ""
    @State private var city = ""
    @State private var state = ""

    private let dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]

    var isEditing: Bool { game != nil }

    var body: some View {
        NavigationStack {
            Form {
                Section("Details") {
                    TextField("Title", text: $title)
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(2...4)
                }

                Section("Schedule") {
                    Picker("Frequency", selection: $frequency) {
                        Text("Weekly").tag("weekly")
                        Text("Every 2 Weeks").tag("biweekly")
                        Text("Monthly").tag("monthly")
                    }
                    .pickerStyle(.segmented)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Day of Week")
                            .font(.md3LabelMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)

                        HStack(spacing: 4) {
                            ForEach(0..<7, id: \.self) { day in
                                Button {
                                    dayOfWeek = day
                                } label: {
                                    Text(dayNames[day])
                                        .font(.md3LabelSmall)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 8)
                                        .background(dayOfWeek == day ? Color.md3Primary : Color.md3SurfaceContainerHigh)
                                        .foregroundStyle(dayOfWeek == day ? Color.md3OnPrimary : Color.md3OnSurface)
                                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                }
                            }
                        }
                    }

                    if frequency == "monthly" {
                        Picker("Week of Month", selection: Binding(
                            get: { monthlyWeek ?? 1 },
                            set: { monthlyWeek = $0 }
                        )) {
                            Text("1st").tag(1)
                            Text("2nd").tag(2)
                            Text("3rd").tag(3)
                            Text("4th").tag(4)
                            Text("Last").tag(-1)
                        }
                        .pickerStyle(.segmented)
                    }

                    DatePicker("Start Time", selection: $startTime, displayedComponents: .hourAndMinute)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Duration")
                            .font(.md3LabelMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        HStack(spacing: 8) {
                            ForEach([60, 120, 180, 240], id: \.self) { mins in
                                Button {
                                    durationMinutes = mins
                                } label: {
                                    Text("\(mins / 60)h")
                                        .font(.md3LabelMedium)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(durationMinutes == mins ? Color.md3Primary : Color.md3SurfaceContainerHigh)
                                        .foregroundStyle(durationMinutes == mins ? Color.md3OnPrimary : Color.md3OnSurface)
                                        .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                }
                            }
                        }
                        Stepper("Duration: \(durationMinutes) min", value: $durationMinutes, in: 30...480, step: 15)
                    }
                }

                Section("Game") {
                    Picker("Game System", selection: $gameSystem) {
                        ForEach(GameSystem.allCases) { system in
                            Text(system.displayName).tag(system)
                        }
                    }
                    TextField("Game Title", text: $gameTitle)
                }

                Section("Settings") {
                    Stepper("Max Players: \(maxPlayers)", value: $maxPlayers, in: 2...100)
                    Toggle("Host Is Playing", isOn: $hostIsPlaying)
                    Toggle("Public", isOn: $isPublic)
                }

                Section("Location") {
                    TextField("Address", text: $addressLine1)
                    TextField("City", text: $city)
                    USStatePicker(selection: $state)
                }
            }
            .navigationTitle(isEditing ? "Edit Recurring Game" : "New Recurring Game")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isEditing ? "Save" : "Create") {
                        save()
                    }
                    .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear {
                if let game {
                    title = game.title
                    description = game.description ?? ""
                    frequency = game.frequency ?? "weekly"
                    dayOfWeek = game.dayOfWeek
                    monthlyWeek = game.monthlyWeek
                    durationMinutes = game.durationMinutes
                    maxPlayers = game.maxPlayers
                    hostIsPlaying = game.hostIsPlaying ?? true
                    isPublic = game.isPublic ?? true
                    gameSystem = game.gameSystem.flatMap { GameSystem(rawValue: $0) } ?? .boardGame
                    gameTitle = game.gameTitle ?? ""
                    addressLine1 = game.addressLine1 ?? ""
                    city = game.city ?? ""
                    state = game.state ?? ""
                    if let time = parseTime(game.startTime) {
                        startTime = time
                    }
                }
            }
        }
    }

    private func save() {
        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm"
        let timeString = timeFormatter.string(from: startTime)

        if isEditing {
            let input = UpdateRecurringGameInput(
                title: title,
                description: description.isEmpty ? nil : description,
                frequency: frequency,
                dayOfWeek: dayOfWeek,
                monthlyWeek: frequency == "monthly" ? monthlyWeek : nil,
                startTime: timeString,
                durationMinutes: durationMinutes,
                maxPlayers: maxPlayers,
                hostIsPlaying: hostIsPlaying,
                addressLine1: addressLine1.isEmpty ? nil : addressLine1,
                city: city.isEmpty ? nil : city,
                state: state.isEmpty ? nil : state,
                gameSystem: gameSystem.rawValue,
                gameTitle: gameTitle.isEmpty ? nil : gameTitle,
                isPublic: isPublic
            )
            onSave(nil, input)
        } else {
            let input = CreateRecurringGameInput(
                groupId: groupId,
                title: title,
                description: description.isEmpty ? nil : description,
                frequency: frequency,
                dayOfWeek: dayOfWeek,
                monthlyWeek: frequency == "monthly" ? monthlyWeek : nil,
                startTime: timeString,
                durationMinutes: durationMinutes,
                maxPlayers: maxPlayers,
                hostIsPlaying: hostIsPlaying,
                addressLine1: addressLine1.isEmpty ? nil : addressLine1,
                city: city.isEmpty ? nil : city,
                state: state.isEmpty ? nil : state,
                gameSystem: gameSystem.rawValue,
                gameTitle: gameTitle.isEmpty ? nil : gameTitle,
                isPublic: isPublic
            )
            onSave(input, nil)
        }
        dismiss()
    }

    private func parseTime(_ time: String) -> Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.date(from: time)
    }
}
