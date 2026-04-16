import SwiftUI

struct ChangePasswordSheet: View {
    @Environment(\.dismiss) private var dismiss
    let services: ServiceContainer

    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var isLoading = false
    @State private var error: String?
    @State private var success = false

    private var isValid: Bool {
        newPassword.count >= 6 && newPassword == confirmPassword
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField("New Password", text: $newPassword)
                    SecureField("Confirm Password", text: $confirmPassword)
                } footer: {
                    Text("Password must be at least 6 characters")
                        .font(.md3BodySmall)
                }

                if newPassword != confirmPassword && !confirmPassword.isEmpty {
                    Section {
                        Text("Passwords do not match")
                            .foregroundStyle(Color.md3Error)
                            .font(.md3BodySmall)
                    }
                }

                if let error {
                    Section {
                        Text(error)
                            .foregroundStyle(Color.md3Error)
                            .font(.md3BodySmall)
                    }
                }

                if success {
                    Section {
                        Label("Password changed successfully", systemImage: "checkmark.circle.fill")
                            .foregroundStyle(Color.md3Primary)
                            .font(.md3BodyMedium)
                    }
                }
            }
            .navigationTitle("Change Password")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        Task { await changePassword() }
                    }
                    .disabled(!isValid || isLoading)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func changePassword() async {
        isLoading = true
        error = nil
        do {
            try await services.auth.changePassword(newPassword: newPassword)
            success = true
            try? await Task.sleep(for: .seconds(1.5))
            dismiss()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
