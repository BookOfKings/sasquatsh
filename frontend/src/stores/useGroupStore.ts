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
} from '@/services/groupsApi'
import type {
  Group,
  GroupSummary,
  CreateGroupInput,
  UpdateGroupInput,
  GroupSearchFilter,
} from '@/types/groups'

const publicGroups = ref<GroupSummary[]>([])
const currentGroup = ref<Group | null>(null)
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

  return {
    publicGroups: computed(() => publicGroups.value),
    currentGroup: computed(() => currentGroup.value),
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    loadPublicGroups,
    loadGroup,
    createGroup: handleCreateGroup,
    updateGroup: handleUpdateGroup,
    deleteGroup: handleDeleteGroup,
    joinGroup: handleJoinGroup,
    leaveGroup: handleLeaveGroup,
  }
}
