import SwiftUI

struct ContentView: View {
    @Environment(AuthViewModel.self) private var authVM
    @Environment(DeepLinkHandler.self) private var deepLinkHandler

    var body: some View {
        Group {
            if !authVM.isInitialized {
                launchScreen
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
        .dynamicTypeSize(...DynamicTypeSize.small)
        .font(.md3BodyLarge)
        .tint(Color.md3Primary)
    }

    private var launchScreen: some View {
        ZStack {
            Color.primaryBrand.ignoresSafeArea()
            VStack(spacing: 20) {
                Image("LogoWhite")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 120, height: 120)
                ProgressView()
                    .tint(.white)
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
