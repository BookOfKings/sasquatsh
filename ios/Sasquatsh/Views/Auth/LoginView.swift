import SwiftUI
import AuthenticationServices

struct LoginView: View {
    @Environment(AuthViewModel.self) private var authVM
    @State private var email = ""
    @State private var password = ""
    @State private var showSignup = false
    @State private var showForgotPassword = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Logo
                    VStack(spacing: 8) {
                        Image("Logo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 80, height: 80)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.large))

                        Text("Sasquatsh")
                            .font(.md3DisplayLarge)
                            .foregroundStyle(Color.md3Primary)

                        Text("Plan your game nights")
                            .font(.md3BodyMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                    }
                    .padding(.top, 20)

                    // Error
                    if let error = authVM.error {
                        ErrorBannerView(message: error) {
                            authVM.error = nil
                        }
                    }

                    // Form
                    VStack(spacing: 14) {
                        TextField("Email", text: $email)
                            .font(.md3BodyLarge)
                            .padding(12)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.extraSmall))
                            .overlay(
                                RoundedRectangle(cornerRadius: MD3Shape.extraSmall)
                                    .stroke(Color.md3Outline, lineWidth: 1)
                            )
                            .textContentType(.emailAddress)
                            .autocapitalization(.none)
                            .keyboardType(.emailAddress)

                        SecureField("Password", text: $password)
                            .font(.md3BodyLarge)
                            .padding(12)
                            .background(Color.md3Surface)
                            .clipShape(RoundedRectangle(cornerRadius: MD3Shape.extraSmall))
                            .overlay(
                                RoundedRectangle(cornerRadius: MD3Shape.extraSmall)
                                    .stroke(Color.md3Outline, lineWidth: 1)
                            )
                            .textContentType(.password)

                        Button {
                            Task { await authVM.login(email: email, password: password) }
                        } label: {
                            if authVM.isLoading {
                                ProgressView()
                                    .tint(.white)
                                    .primaryButtonStyle()
                            } else {
                                Text("Sign In")
                                    .primaryButtonStyle()
                            }
                        }
                        .disabled(email.isEmpty || password.isEmpty || authVM.isLoading)

                        HStack {
                            Spacer()
                            Button {
                                showForgotPassword = true
                            } label: {
                                Text("Forgot Password?")
                                    .font(.md3LabelMedium)
                                    .foregroundStyle(Color.md3Primary)
                            }
                        }
                    }
                    .padding(.horizontal)

                    // Divider
                    HStack {
                        Rectangle().fill(Color.md3OutlineVariant).frame(height: 1)
                        Text("or")
                            .font(.md3LabelMedium)
                            .foregroundStyle(Color.md3OnSurfaceVariant)
                        Rectangle().fill(Color.md3OutlineVariant).frame(height: 1)
                    }
                    .padding(.horizontal)

                    // Apple Sign-In
                    Button {
                        Task { await authVM.signInWithApple() }
                    } label: {
                        HStack {
                            Image(systemName: "apple.logo")
                            Text("Continue with Apple")
                        }
                        .font(.md3LabelLarge)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 40)
                        .background(Color.black)
                        .clipShape(Capsule())
                    }
                    .padding(.horizontal)
                    .disabled(authVM.isLoading)

                    // Google Sign-In
                    Button {
                        Task { await authVM.signInWithGoogle() }
                    } label: {
                        HStack {
                            Image(systemName: "g.circle.fill")
                            Text("Continue with Google")
                        }
                        .secondaryButtonStyle()
                    }
                    .padding(.horizontal)
                    .disabled(authVM.isLoading)

                    // Sign Up link
                    Button {
                        showSignup = true
                    } label: {
                        HStack(spacing: 4) {
                            Text("Don't have an account?")
                                .foregroundStyle(Color.md3OnSurfaceVariant)
                            Text("Sign Up")
                                .foregroundStyle(Color.md3Primary)
                                .fontWeight(.semibold)
                        }
                        .font(.md3BodyMedium)
                    }
                }
                .padding(.bottom, 20)
            }
            .background(Color.md3Surface)
            .fullScreenCover(isPresented: $showSignup) {
                SignupView()
            }
            .navigationDestination(isPresented: $showForgotPassword) {
                ForgotPasswordView()
            }
        }
    }
}
