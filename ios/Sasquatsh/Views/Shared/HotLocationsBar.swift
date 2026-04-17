import SwiftUI

struct HotLocationsBar: View {
    @Environment(\.services) private var services
    var selectedId: String?
    var onSelect: (EventLocation) -> Void

    @State private var locations: [EventLocation] = []
    @State private var isLoading = false

    var body: some View {
        Group {
            if !locations.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(locations) { location in
                            Button {
                                onSelect(location)
                            } label: {
                                HStack(spacing: 6) {
                                    Text(location.name)
                                        .font(.md3LabelMedium)
                                        .lineLimit(1)

                                    if let count = location.eventCount, count > 0 {
                                        Text("\(count)")
                                            .font(.md3LabelSmall)
                                            .foregroundStyle(Color.md3OnPrimary)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color.md3Primary)
                                            .clipShape(Capsule())
                                    }
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(selectedId == location.id ? Color.md3PrimaryContainer : Color.md3SurfaceContainerHigh)
                                .foregroundStyle(selectedId == location.id ? Color.md3OnPrimaryContainer : Color.md3OnSurfaceVariant)
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(selectedId == location.id ? Color.md3Primary : Color.md3OutlineVariant, lineWidth: 1)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }

            if isLoading {
                    D20ProgressView(size: 32)
                    .tint(Color.md3Primary)
            }
        }
        .task {
            await loadLocations()
        }
    }

    private func loadLocations() async {
        isLoading = true
        do {
            locations = try await services.social.getHotLocations()
        } catch {
            locations = []
        }
        isLoading = false
    }
}
