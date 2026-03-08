import SwiftUI

struct SignupView: View {
    @Environment(AuthViewModel.self) private var authVM
    @Environment(\.services) private var services
    @Environment(\.dismiss) private var dismiss

    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var displayName = ""
    @State private var username = ""
    @State private var usernameAvailable: Bool?
    @State private var usernameCheckMessage: String?
    @State private var checkTask: Task<Void, Never>?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    VStack(spacing: 12) {
                        Image("Logo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 80, height: 80)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))

                        Text("Create Account")
                            .font(.md3HeadlineMedium)
                            .foregroundStyle(Color.md3Primary)
                    }
                    .padding(.top, 20)

                    if let error = authVM.error {
                        ErrorBannerView(message: error) {
                            authVM.error = nil
                        }
                    }

                    VStack(spacing: 16) {
                        md3TextField("Display Name", text: $displayName)
                            .textContentType(.name)

                        VStack(alignment: .leading, spacing: 4) {
                            md3TextField("Username", text: $username)
                                .textContentType(.username)
                                .autocapitalization(.none)
                                .onChange(of: username) { _, newValue in
                                    checkUsername(newValue)
                                }

                            if let usernameCheckMessage {
                                HStack(spacing: 4) {
                                    Image(systemName: usernameAvailable == true ? "checkmark.circle.fill" : "xmark.circle.fill")
                                        .foregroundStyle(usernameAvailable == true ? .green : .md3Error)
                                    Text(usernameCheckMessage)
                                        .font(.md3LabelSmall)
                                        .foregroundStyle(usernameAvailable == true ? .green : .md3Error)
                                }
                            }
                        }

                        md3TextField("Email", text: $email)
                            .textContentType(.emailAddress)
                            .autocapitalization(.none)
                            .keyboardType(.emailAddress)

                        md3SecureField("Password", text: $password)
                            .textContentType(.newPassword)

                        md3SecureField("Confirm Password", text: $confirmPassword)
                            .textContentType(.newPassword)

                        if !password.isEmpty && !confirmPassword.isEmpty && password != confirmPassword {
                            Text("Passwords don't match")
                                .font(.md3LabelSmall)
                                .foregroundStyle(Color.md3Error)
                        }

                        Button {
                            Task {
                                await authVM.signup(email: email, password: password, displayName: displayName, username: username)
                                if authVM.isAuthenticated {
                                    dismiss()
                                }
                            }
                        } label: {
                            if authVM.isLoading {
                                ProgressView()
                                    .tint(.white)
                                    .primaryButtonStyle()
                            } else {
                                Text("Create Account")
                                    .primaryButtonStyle()
                            }
                        }
                        .disabled(!isValid || authVM.isLoading)
                    }
                    .padding(.horizontal)

                    Button {
                        dismiss()
                    } label: {
                        HStack(spacing: 4) {
                            Text("Already have an account?")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("Sign In")
                                .foregroundStyle(Color.md3Primary)
                                .fontWeight(.semibold)
                        }
                        .font(.md3BodyMedium)
                    }
                }
                .padding(.bottom, 40)
            }
            .background(Color.md3Surface)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func md3TextField(_ placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .font(.md3BodyLarge)
            .padding(12)
            .background(Color.md3Surface)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.extraSmall))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.extraSmall)
                    .stroke(Color.md3Outline, lineWidth: 1)
            )
    }

    private func md3SecureField(_ placeholder: String, text: Binding<String>) -> some View {
        SecureField(placeholder, text: text)
            .font(.md3BodyLarge)
            .padding(12)
            .background(Color.md3Surface)
            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.extraSmall))
            .overlay(
                RoundedRectangle(cornerRadius: MD3Shape.extraSmall)
                    .stroke(Color.md3Outline, lineWidth: 1)
            )
    }

    private var isValid: Bool {
        !email.isEmpty &&
        !password.isEmpty &&
        password == confirmPassword &&
        password.count >= 6 &&
        !username.isEmpty &&
        !displayName.isEmpty &&
        usernameAvailable == true
    }

    private func checkUsername(_ name: String) {
        checkTask?.cancel()
        usernameAvailable = nil
        usernameCheckMessage = nil

        guard name.count >= 3 else {
            if !name.isEmpty {
                usernameCheckMessage = "Username must be at least 3 characters"
                usernameAvailable = false
            }
            return
        }

        checkTask = Task {
            try? await Task.sleep(for: .milliseconds(400))
            guard !Task.isCancelled else { return }
            do {
                let result = try await services.profile.checkUsername(username: name)
                guard !Task.isCancelled else { return }
                usernameAvailable = result.available
                usernameCheckMessage = result.available ? "Username is available" : (result.reason ?? "Username is taken")
            } catch {
                // Ignore errors during check
            }
        }
    }
}
