import SwiftUI

struct VenueSelector: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss
    var onSelect: (EventLocation) -> Void

    @State private var locations: [EventLocation] = []
    @State private var searchText = ""
    @State private var isLoading = false
    @State private var showSubmitVenue = false

    private var filteredLocations: [EventLocation] {
        guard !searchText.isEmpty else { return locations }
        let query = searchText.lowercased()
        return locations.filter {
            $0.name.lowercased().contains(query) ||
            $0.city.lowercased().contains(query) ||
            $0.state.lowercased().contains(query)
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                SearchBarView(text: $searchText, placeholder: "Search venues...")
                    .padding(.horizontal)
                    .padding(.top, 8)

                if isLoading {
                    Spacer()
                    LoadingView()
                    Spacer()
                } else if filteredLocations.isEmpty {
                    Spacer()
                    EmptyStateView(
                        icon: "building.2",
                        title: "No Venues Found",
                        message: searchText.isEmpty ? "No venues available yet" : "No venues match your search"
                    )
                    Spacer()
                } else {
                    List(filteredLocations) { location in
                        Button {
                            onSelect(location)
                            dismiss()
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(location.name)
                                        .font(.md3BodyLarge)
                                        .foregroundStyle(Color.md3OnSurface)

                                    Spacer()

                                    if let count = location.eventCount, count > 0 {
                                        BadgeView(text: "\(count) games")
                                    }
                                }

                                Text("\(location.city), \(location.state)")
                                    .font(.md3BodySmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)

                                Text(scheduleLabel(for: location))
                                    .font(.md3LabelSmall)
                                    .foregroundStyle(Color.md3OnSurfaceVariant)
                            }
                            .padding(.vertical, 4)
                        }
                        .buttonStyle(.plain)
                    }
                    .listStyle(.plain)
                }

                Button {
                    showSubmitVenue = true
                } label: {
                    Label("Submit a Venue", systemImage: "plus.circle")
                        .primaryButtonStyle()
                }
                .padding()
            }
            .navigationTitle("Choose a Venue")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .task {
                await loadLocations()
            }
            .sheet(isPresented: $showSubmitVenue) {
                SubmitVenueSheet {
                    await loadLocations()
                }
            }
        }
    }

    private func loadLocations() async {
        isLoading = true
        do {
            locations = try await services.social.getEventLocations()
        } catch {
            locations = []
        }
        isLoading = false
    }

    private func scheduleLabel(for location: EventLocation) -> String {
        if location.isPermanent == true {
            return "Permanent"
        }
        if let days = location.recurringDays, !days.isEmpty {
            let dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
            let labels = days.compactMap { $0 >= 0 && $0 < 7 ? dayNames[$0] : nil }
            return "Recurring: \(labels.joined(separator: ", "))"
        }
        if let start = location.startDate, let end = location.endDate {
            return "\(start) - \(end)"
        }
        if let start = location.startDate {
            return "From \(start)"
        }
        return ""
    }
}
