import SwiftUI

struct ForgotPasswordView: View {
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    @State private var email = ""
    @State private var isLoading = false
    @State private var error: String?
    @State private var success = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Spacer().frame(height: 40)

                Image("LogoWhite")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 80, height: 80)
                    .padding(20)
                    .background(Color.md3Primary)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))

                Text("Reset Password")
                    .font(.md3HeadlineMedium)
                    .foregroundStyle(Color.md3OnSurface)

                Text("Enter your email address and we'll send you a link to reset your password.")
                    .font(.md3BodyMedium)
                    .foregroundStyle(Color.md3OnSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                if let error {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text(error)
                    }
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3Error)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.md3ErrorContainer)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    .padding(.horizontal)
                }

                if success {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                        Text("Check your email! We sent a password reset link to \(email).")
                    }
                    .font(.md3BodySmall)
                    .foregroundStyle(Color.md3Primary)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.md3PrimaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.small))
                    .padding(.horizontal)
                } else {
                    VStack(spacing: 16) {
                        TextField("Email", text: $email)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .autocapitalization(.none)
                            .textFieldStyle(.roundedBorder)
                            .padding(.horizontal)

                        Button {
                            Task { await resetPassword() }
                        } label: {
                            if isLoading {
                                ProgressView()
                                    .tint(Color.md3OnPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color.md3Primary)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            } else {
                                Text("Reset Password")
                                    .font(.md3LabelLarge)
                                    .foregroundStyle(Color.md3OnPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color.md3Primary)
                                    .clipShape(RoundedRectangle(cornerRadius: MD3Shape.medium))
                            }
                        }
                        .disabled(email.isEmpty || isLoading)
                        .padding(.horizontal)
                    }
                }

                Button {
                    dismiss()
                } label: {
                    Text("Back to Sign In")
                        .font(.md3LabelLarge)
                        .foregroundStyle(Color.md3Primary)
                }
                .padding(.top, 8)
            }
        }
        .background(Color.md3SurfaceContainer)
        .navigationBarBackButtonHidden(true)
    }

    private func resetPassword() async {
        isLoading = true
        error = nil
        do {
            try await services.auth.resetPassword(email: email.trimmingCharacters(in: .whitespacesAndNewlines))
            success = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
