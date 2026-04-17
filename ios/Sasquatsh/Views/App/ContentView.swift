import SwiftUI

struct ContentView: View {
    @Environment(AuthViewModel.self) private var authVM
    @Environment(DeepLinkHandler.self) private var deepLinkHandler

    @State private var showSplash = true

    var body: some View {
        Group {
            if showSplash || !authVM.isInitialized {
                launchScreen
                    .onAppear {
                        Task {
                            try? await Task.sleep(for: .seconds(3))
                            withAnimation(.easeOut(duration: 0.4)) {
                                showSplash = false
                            }
                        }
                    }
            } else if authVM.isAuthenticated {
                MainTabView()
                    .sheet(item: gameInviteBinding) { code in
                        InvitationAcceptView(inviteCode: code.value)
                    }
                    .sheet(item: groupInviteBinding) { code in
                        GroupInviteAcceptView(inviteCode: code.value)
                    }
            } else {
                LoginView()
            }
        }
        .tint(Color.md3Primary)
        .preferredColorScheme(AppearanceManager.shared.colorScheme)
    }

    private var launchScreen: some View {
        ZStack {
            Color.primaryBrand.ignoresSafeArea()
            VStack(spacing: 16) {
                Image("LogoWhite")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 120, height: 120)
                Text("Sasquatsh")
                    .font(.system(size: 28, weight: .bold, design: .default))
                    .foregroundStyle(.white)
                D20SpinnerView(size: 120, color: UIColor(red: 0.388, green: 0.400, blue: 0.945, alpha: 1), numberColor: .black)
                    .frame(width: 120, height: 120)
            }
        }
    }

    private var gameInviteBinding: Binding<IdentifiableString?> {
        Binding(
            get: { deepLinkHandler.pendingGameInviteCode.map { IdentifiableString(value: $0) } },
            set: { _ in deepLinkHandler.clearGameInvite() }
        )
    }

    private var groupInviteBinding: Binding<IdentifiableString?> {
        Binding(
            get: { deepLinkHandler.pendingGroupInviteCode.map { IdentifiableString(value: $0) } },
            set: { _ in deepLinkHandler.clearGroupInvite() }
        )
    }
}

struct IdentifiableString: Identifiable {
    let id = UUID()
    let value: String
}
