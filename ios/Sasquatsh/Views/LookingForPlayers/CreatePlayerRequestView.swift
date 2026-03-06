import SwiftUI

struct CreatePlayerRequestView: View {
    @Bindable var vm: PlayerRequestListViewModel
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    @State private var selectedEventId = ""
    @State private var description = ""
    @State private var playerCountNeeded = 1
    @State private var isLoading = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Post an urgent request for fill-in players. Your request will be visible for 15 minutes.")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }

                Section("Select Event") {
                    if vm.hostedEvents.isEmpty {
                        Text("You need to create an event first before requesting players.")
                            .font(.md3BodySmall)
                            .foregroundStyle(Color.md3Tertiary)
                    } else {
                        Picker("Event", selection: $selectedEventId) {
                            Text("Choose an event...").tag("")
                            ForEach(upcomingEvents, id: \.id) { event in
                                Text("\(event.title) - \(event.eventDate.toDate?.displayDate ?? event.eventDate)")
                                    .tag(event.id)
                            }
                        }
                    }
                }

                Section("Details") {
                    Stepper("Players Needed: \(playerCountNeeded)", value: $playerCountNeeded, in: 1...20)

                    TextField("Message (optional)", text: $description, axis: .vertical)
                        .lineLimit(2...4)
                }
            }
            .navigationTitle("Need Players")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Post") {
                        Task { await save() }
                    }
                    .disabled(selectedEventId.isEmpty || isLoading)
                }
            }
            .task {
                vm.configure(services: services)
                await vm.loadHostedEvents()
                if let first = upcomingEvents.first {
                    selectedEventId = first.id
                }
            }
        }
    }

    private var upcomingEvents: [EventSummary] {
        let today = Date()
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let todayStr = formatter.string(from: today)
        return vm.hostedEvents.filter { $0.eventDate >= todayStr && $0.status != "cancelled" }
    }

    private func save() async {
        isLoading = true
        await vm.createRequest(
            eventId: selectedEventId,
            description: description.isEmpty ? nil : description,
            playerCount: playerCountNeeded
        )
        isLoading = false
        if vm.error == nil {
            dismiss()
        }
    }
}
