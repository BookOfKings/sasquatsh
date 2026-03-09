import { computed, ref } from 'vue'
import { useAuthStore } from './useAuthStore'
import {
  getPublicGroups,
  getGroup,
  createGroup,
  updateGroup,
  deleteGroup,
  joinGroup,
  leaveGroup,
  getMyGroups,
  getGroupMembers,
  removeMember,
  changeMemberRole,
  transferOwnership,
  requestToJoin,
  getJoinRequests,
  approveRequest,
  rejectRequest,
  createInvitation,
  getInvitations,
  revokeInvitation,
  acceptInvitation,
  getMyPendingInvitations,
  respondToInvitation,
} from '@/services/groupsApi'
import type {
  Group,
  GroupSummary,
  CreateGroupInput,
  UpdateGroupInput,
  GroupSearchFilter,
  GroupMember,
  JoinRequest,
  GroupInvitation,
  CreateInvitationInput,
  MemberRole,
  PendingInvitation,
} from '@/types/groups'

const publicGroups = ref<GroupSummary[]>([])
const myGroups = ref<GroupSummary[]>([])
const currentGroup = ref<Group | null>(null)
const groupMembers = ref<GroupMember[]>([])
const joinRequests = ref<JoinRequest[]>([])
const invitations = ref<GroupInvitation[]>([])
const pendingInvitations = ref<PendingInvitation[]>([])
const userMembership = ref<GroupMember | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

export function useGroupStore() {
  const auth = useAuthStore()

  async function loadPublicGroups(filter?: GroupSearchFilter): Promise<void> {
    loading.value = true
    error.value = null

    try {
      publicGroups.value = await getPublicGroups(filter)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to load groups'
    } finally {
      loading.value = false
    }
  }

  async function loadMyGroups(): Promise<void> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        myGroups.value = []
        return
      }

      myGroups.value = await getMyGroups(token)
    } catch (err) {
      console.error('Failed to load my groups:', err)
      myGroups.value = []
    }
  }

  async function loadGroup(idOrSlug: string): Promise<void> {
    loading.value = true
    error.value = null

    try {
      currentGroup.value = await getGroup(idOrSlug)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to load group'
      currentGroup.value = null
    } finally {
      loading.value = false
    }
  }

  async function loadGroupMembers(groupId: string): Promise<void> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        groupMembers.value = []
        return
      }

      groupMembers.value = await getGroupMembers(token, groupId)

      // Find current user's membership
      const userId = auth.user.value?.id
      if (userId) {
        userMembership.value = groupMembers.value.find((m) => m.userId === userId) || null
      }
    } catch (err) {
      console.error('Failed to load members:', err)
      groupMembers.value = []
    }
  }

  async function loadJoinRequests(groupId: string): Promise<void> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        joinRequests.value = []
        return
      }

      joinRequests.value = await getJoinRequests(token, groupId)
    } catch (err) {
      console.error('Failed to load join requests:', err)
      joinRequests.value = []
    }
  }

  async function loadInvitations(groupId: string): Promise<void> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        invitations.value = []
        return
      }

      invitations.value = await getInvitations(token, groupId)
    } catch (err) {
      console.error('Failed to load invitations:', err)
      invitations.value = []
    }
  }

  async function handleCreateGroup(
    data: CreateGroupInput
  ): Promise<{ ok: boolean; message: string; group?: Group }> {
    loading.value = true
    error.value = null

    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const group = await createGroup(token, data)
      return { ok: true, message: 'Group created!', group }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create group'
      error.value = message
      return { ok: false, message }
    } finally {
      loading.value = false
    }
  }

  async function handleUpdateGroup(
    id: string,
    data: UpdateGroupInput
  ): Promise<{ ok: boolean; message: string }> {
    loading.value = true
    error.value = null

    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      currentGroup.value = await updateGroup(token, id, data)
      return { ok: true, message: 'Group updated!' }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update group'
      error.value = message
      return { ok: false, message }
    } finally {
      loading.value = false
    }
  }

  async function handleDeleteGroup(
    id: string
  ): Promise<{ ok: boolean; message: string }> {
    loading.value = true
    error.value = null

    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      await deleteGroup(token, id)
      currentGroup.value = null
      return { ok: true, message: 'Group deleted' }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete group'
      error.value = message
      return { ok: false, message }
    } finally {
      loading.value = false
    }
  }

  async function handleJoinGroup(
    groupId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await joinGroup(token, groupId)

      // Refresh group data to get updated member count
      if (currentGroup.value?.id === groupId) {
        await loadGroup(groupId)
        await loadGroupMembers(groupId)
      }

      return { ok: true, message: result.message }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to join group'
      return { ok: false, message }
    }
  }

  async function handleLeaveGroup(
    groupId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      await leaveGroup(token, groupId)

      // Clear user membership
      userMembership.value = null

      // Refresh group data to get updated member count
      if (currentGroup.value?.id === groupId) {
        await loadGroup(groupId)
      }

      return { ok: true, message: 'Left group successfully' }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to leave group'
      return { ok: false, message }
    }
  }

  async function handleRemoveMember(
    groupId: string,
    userId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      await removeMember(token, groupId, userId)

      // Refresh members list
      await loadGroupMembers(groupId)

      return { ok: true, message: 'Member removed' }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to remove member'
      return { ok: false, message }
    }
  }

  async function handleChangeMemberRole(
    groupId: string,
    userId: string,
    role: MemberRole
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await changeMemberRole(token, groupId, userId, role)

      // Refresh members list
      await loadGroupMembers(groupId)

      return { ok: true, message: result.message }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to change role'
      return { ok: false, message }
    }
  }

  async function handleTransferOwnership(
    groupId: string,
    newOwnerId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await transferOwnership(token, groupId, newOwnerId)

      // Refresh members list to reflect new roles
      await loadGroupMembers(groupId)

      return { ok: true, message: result.message }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to transfer ownership'
      return { ok: false, message }
    }
  }

  async function handleRequestToJoin(
    groupId: string,
    message?: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await requestToJoin(token, groupId, message)
      return { ok: true, message: result.message }
    } catch (err) {
      const errMessage = err instanceof Error ? err.message : 'Failed to submit request'
      return { ok: false, message: errMessage }
    }
  }

  async function handleApproveRequest(
    groupId: string,
    userId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await approveRequest(token, groupId, userId)

      // Refresh requests and members
      await loadJoinRequests(groupId)
      await loadGroupMembers(groupId)

      return { ok: true, message: result.message }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to approve request'
      return { ok: false, message }
    }
  }

  async function handleRejectRequest(
    groupId: string,
    userId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await rejectRequest(token, groupId, userId)

      // Refresh requests
      await loadJoinRequests(groupId)

      return { ok: true, message: result.message }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to reject request'
      return { ok: false, message }
    }
  }

  async function handleCreateInvitation(
    groupId: string,
    options?: CreateInvitationInput
  ): Promise<{ ok: boolean; message: string; invitation?: GroupInvitation }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const invitation = await createInvitation(token, groupId, options)

      // Refresh invitations
      await loadInvitations(groupId)

      return { ok: true, message: 'Invitation created', invitation }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create invitation'
      return { ok: false, message }
    }
  }

  async function handleRevokeInvitation(
    groupId: string,
    invitationId: string
  ): Promise<{ ok: boolean; message: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      await revokeInvitation(token, groupId, invitationId)

      // Refresh invitations
      await loadInvitations(groupId)

      return { ok: true, message: 'Invitation revoked' }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to revoke invitation'
      return { ok: false, message }
    }
  }

  async function handleAcceptInvitation(
    inviteCode: string
  ): Promise<{ ok: boolean; message: string; groupId?: string; groupName?: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await acceptInvitation(token, inviteCode)
      return {
        ok: true,
        message: result.message,
        groupId: result.groupId,
        groupName: result.groupName,
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to accept invitation'
      return { ok: false, message }
    }
  }

  async function loadPendingInvitations(): Promise<void> {
    try {
      const token = await auth.getIdToken()
      if (!token) return

      pendingInvitations.value = await getMyPendingInvitations(token)
    } catch (err) {
      console.error('Failed to load pending invitations:', err)
      pendingInvitations.value = []
    }
  }

  async function handleRespondToInvitation(
    invitationId: string,
    response: 'accept' | 'decline'
  ): Promise<{ ok: boolean; message: string; groupId?: string; groupName?: string }> {
    try {
      const token = await auth.getIdToken()
      if (!token) {
        return { ok: false, message: 'Not authenticated' }
      }

      const result = await respondToInvitation(token, invitationId, response)

      // Refresh pending invitations
      await loadPendingInvitations()

      // If accepted, refresh my groups
      if (response === 'accept') {
        await loadMyGroups()
      }

      return {
        ok: true,
        message: result.message,
        groupId: result.groupId,
        groupName: result.groupName,
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to respond to invitation'
      return { ok: false, message }
    }
  }

  return {
    publicGroups: computed(() => publicGroups.value),
    myGroups: computed(() => myGroups.value),
    currentGroup: computed(() => currentGroup.value),
    groupMembers: computed(() => groupMembers.value),
    joinRequests: computed(() => joinRequests.value),
    invitations: computed(() => invitations.value),
    pendingInvitations: computed(() => pendingInvitations.value),
    userMembership: computed(() => userMembership.value),
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    loadPublicGroups,
    loadMyGroups,
    loadGroup,
    loadGroupMembers,
    loadJoinRequests,
    loadInvitations,
    loadPendingInvitations,
    createGroup: handleCreateGroup,
    updateGroup: handleUpdateGroup,
    deleteGroup: handleDeleteGroup,
    joinGroup: handleJoinGroup,
    leaveGroup: handleLeaveGroup,
    removeMember: handleRemoveMember,
    changeMemberRole: handleChangeMemberRole,
    transferOwnership: handleTransferOwnership,
    requestToJoin: handleRequestToJoin,
    approveRequest: handleApproveRequest,
    rejectRequest: handleRejectRequest,
    createInvitation: handleCreateInvitation,
    revokeInvitation: handleRevokeInvitation,
    acceptInvitation: handleAcceptInvitation,
    respondToInvitation: handleRespondToInvitation,
  }
}
