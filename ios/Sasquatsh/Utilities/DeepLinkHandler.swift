import SwiftUI

@Observable
@MainActor
final class DeepLinkHandler {
    var pendingGameInviteCode: String?
    var pendingGroupInviteCode: String?

    func handle(url: URL) {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true) else { return }

        let path = components.path

        // sasquatsh.com/invite/{code} — game invitation
        if path.hasPrefix("/invite/") {
            let code = String(path.dropFirst("/invite/".count))
            if !code.isEmpty {
                pendingGameInviteCode = code
            }
        }

        // sasquatsh.com/groups/invite/{code} — group invitation
        if path.hasPrefix("/groups/invite/") {
            let code = String(path.dropFirst("/groups/invite/".count))
            if !code.isEmpty {
                pendingGroupInviteCode = code
            }
        }
    }

    func clearGameInvite() {
        pendingGameInviteCode = nil
    }

    func clearGroupInvite() {
        pendingGroupInviteCode = nil
    }
}
