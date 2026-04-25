import SwiftUI

struct SubmitVenueSheet: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    var onSubmitted: (() async -> Void)?

    @State private var name = ""
    @State private var city = ""
    @State private var state = ""
    @State private var venue = ""
    @State private var locationType: LocationType = .temporary
    @State private var startDate = Date()
    @State private var endDate = Date().addingTimeInterval(86400 * 3)
    @State private var recurringDays: Set<Int> = []
    @State private var isSubmitting = false
    @State private var error: String?
    @State private var showSuccess = false

    enum LocationType: String, CaseIterable {
        case temporary = "Temporary"
        case permanent = "Permanent"
        case recurring = "Recurring"
    }

    private let dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]

    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !city.trimmingCharacters(in: .whitespaces).isEmpty &&
        !state.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Venue Details") {
                    TextField("Name", text: $name)
                    TextField("City", text: $city)
                    USStatePicker(selection: $state)
                    TextField("Venue / Building (optional)", text: $venue)
                }

                Section("Location Type") {
                    Picker("Type", selection: $locationType) {
                        ForEach(LocationType.allCases, id: \.self) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)

                    switch locationType {
                    case .temporary:
                        DatePicker("Start Date", selection: $startDate, displayedComponents: .date)
                        DatePicker("End Date", selection: $endDate, displayedComponents: .date)
                    case .recurring:
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Recurring Days")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3OnSurfaceVariant)

                            HStack(spacing: 6) {
                                ForEach(0..<7, id: \.self) { day in
                                    Button {
                                        if recurringDays.contains(day) {
                                            recurringDays.remove(day)
                                        } else {
                                            recurringDays.insert(day)
                                        }
                                    } label: {
                                        Text(dayNames[day])
                                            .font(.md3LabelSmall)
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 8)
                                            .background(recurringDays.contains(day) ? Color.md3PrimaryContainer : Color.md3SurfaceContainerHigh)
                                            .foregroundStyle(recurringDays.contains(day) ? Color.md3OnPrimaryContainer : Color.md3OnSurfaceVariant)
                                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: MD3Shape.small)
                                                    .stroke(recurringDays.contains(day) ? Color.md3Primary : Color.md3OutlineVariant, lineWidth: 1)
                                            )
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    case .permanent:
                        EmptyView()
                    }
                }

                if let error {
                    Section {
                        Text(error).foregroundStyle(Color.md3Error)
                    }
                }

                if showSuccess {
                    Section {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(Color.md3Primary)
                            Text("Venue submitted for approval!")
                                .font(.md3BodyMedium)
                                .foregroundStyle(Color.md3Primary)
                        }
                    }
                }
            }
            .navigationTitle("Submit a Venue")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Submit") {
                        Task { await submit() }
                    }
                    .disabled(!isValid || isSubmitting)
                }
            }
        }
    }

    private func submit() async {
        isSubmitting = true
        error = nil

        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"

        var input = CreateEventLocationInput(
            name: name,
            city: city,
            state: state,
            venue: venue.isEmpty ? nil : venue
        )

        switch locationType {
        case .temporary:
            input.startDate = dateFormatter.string(from: startDate)
            input.endDate = dateFormatter.string(from: endDate)
        case .permanent:
            input.isPermanent = true
        case .recurring:
            input.recurringDays = Array(recurringDays).sorted()
        }

        do {
            _ = try await services.social.createEventLocation(input: input)
            showSuccess = true
            try? await Task.sleep(for: .seconds(1.5))
            await onSubmitted?()
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        isSubmitting = false
    }
}
