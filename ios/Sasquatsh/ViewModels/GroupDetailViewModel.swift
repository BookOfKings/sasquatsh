import SwiftUI

@Observable
@MainActor
final class GroupDetailViewModel {
    var group: GameGroup?
    var members: [GroupMember] = []
    var joinRequests: [JoinRequest] = []
    var invitations: [GroupInvitation] = []
    var planningSessions: [PlanningSession] = []
    var groupEvents: [EventSummary] = []
    var isLoading = false
    var error: String?
    var actionMessage: String?

    private var services: ServiceContainer?

    func configure(services: ServiceContainer) {
        self.services = services
    }

    func loadGroup(id: String) async {
        guard let services else { return }
        isLoading = true
        error = nil
        do {
            group = try await services.groups.getGroup(id: id)
            members = try await services.groups.getMembers(groupId: id)
            // These require membership — don't let failures block the view
            planningSessions = (try? await services.planning.getGroupSessions(groupId: id)) ?? []
            groupEvents = (try? await services.events.getGroupEvents(groupId: id)) ?? []
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadJoinRequests() async {
        guard let services, let group else { return }
        do {
            joinRequests = try await services.groups.getJoinRequests(groupId: group.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func loadInvitations() async {
        guard let services, let group else { return }
        do {
            invitations = try await services.groups.getInvitations(groupId: group.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func join() async {
        guard let services, let group else { return }
        error = nil
        do {
            try await services.groups.joinGroup(id: group.id)
            actionMessage = "Joined group!"
            await loadGroup(id: group.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func requestToJoin(message: String?) async {
        guard let services, let group else { return }
        error = nil
        do {
            try await services.groups.requestToJoin(groupId: group.id, message: message)
            actionMessage = "Join request sent!"
        } catch {
            self.error = error.localizedDescription
        }
    }

    func leave() async {
        guard let services, let group else { return }
        error = nil
        do {
            try await services.groups.leaveGroup(id: group.id)
            actionMessage = "Left group"
            await loadGroup(id: group.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func removeMember(userId: String) async {
        guard let services, let group else { return }
        error = nil
        do {
            try await services.groups.removeMember(groupId: group.id, userId: userId)
            members.removeAll { $0.userId == userId }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func changeRole(userId: String, to role: MemberRole) async {
        guard let services, let group else { return }
        error = nil
        do {
            try await services.groups.changeRole(groupId: group.id, userId: userId, role: role)
            if let idx = members.firstIndex(where: { $0.userId == userId }) {
                members[idx].role = role
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func approveRequest(userId: String) async {
        guard let services, let group else { return }
        do {
            try await services.groups.approveRequest(groupId: group.id, userId: userId)
            joinRequests.removeAll { $0.userId == userId }
            await loadGroup(id: group.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func rejectRequest(userId: String) async {
        guard let services, let group else { return }
        do {
            try await services.groups.rejectRequest(groupId: group.id, userId: userId)
            joinRequests.removeAll { $0.userId == userId }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func createInvitation() async -> GroupInvitation? {
        guard let services, let group else { return nil }
        do {
            let invitation = try await services.groups.createInvitation(groupId: group.id, input: nil)
            invitations.insert(invitation, at: 0)
            return invitation
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    func revokeInvitation(_ inviteId: String) async {
        guard let services, let group else { return }
        do {
            try await services.groups.revokeInvitation(groupId: group.id, inviteId: inviteId)
            invitations.removeAll { $0.id == inviteId }
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deleteGroup() async -> Bool {
        guard let services, let group else { return false }
        do {
            try await services.groups.deleteGroup(id: group.id)
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func transferOwnership(to userId: String) async {
        guard let services, let group else { return }
        do {
            try await services.groups.transferOwnership(groupId: group.id, newOwnerId: userId)
            await loadGroup(id: group.id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func userRole(userId: String) -> MemberRole? {
        members.first { $0.userId == userId }?.role
    }

    func isAdmin(userId: String) -> Bool {
        let role = userRole(userId: userId)
        return role == .owner || role == .admin
    }
}
