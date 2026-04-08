import SwiftUI

struct ReportMessageSheet: View {
    let message: ChatMessage
    let onSubmit: (String, String?) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedReason: ChatReportReason = .inappropriate
    @State private var details = ""
    @State private var isSubmitting = false
    @State private var showSuccess = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Message") {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(message.user?.displayName ?? "Unknown")
                            .font(.md3LabelSmall)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Text(message.content)
                            .font(.md3BodyMedium)
                            .lineLimit(3)
                    }
                }

                Section("Reason") {
                    Picker("Report Reason", selection: $selectedReason) {
                        ForEach(ChatReportReason.allCases) { reason in
                            Text(reason.displayName).tag(reason)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }

                Section("Additional Details (Optional)") {
                    TextField("Provide more context...", text: $details, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle("Report Message")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Submit") {
                        isSubmitting = true
                        onSubmit(selectedReason.rawValue, details.isEmpty ? nil : details)
                        showSuccess = true
                    }
                    .disabled(isSubmitting)
                }
            }
            .alert("Report Submitted", isPresented: $showSuccess) {
                Button("OK") { dismiss() }
            } message: {
                Text("Thank you for your report. An admin will review it.")
            }
        }
        .presentationDetents([.medium])
    }
}
