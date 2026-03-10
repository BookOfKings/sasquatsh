<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useGroupStore } from '@/stores/useGroupStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { searchUsersByUsername } from '@/services/profileApi'
import UserAvatar from '@/components/common/UserAvatar.vue'
import type { GroupMember, JoinRequest, GroupInvitation, MemberRole } from '@/types/groups'

const props = defineProps<{
  groupId: string
  isOwner: boolean
}>()

const emit = defineEmits<{
  (e: 'toast', message: string, type: 'success' | 'error'): void
}>()

const groupStore = useGroupStore()
const auth = useAuthStore()

const activeTab = ref<'members' | 'requests' | 'invitations'>('members')
const loading = ref(false)
const showInviteModal = ref(false)
const showTransferModal = ref(false)
const transferTargetMember = ref<GroupMember | null>(null)
const inviteMaxUses = ref<number | null>(null)
const inviteExpiresIn = ref<number | null>(7)
const invitePhone = ref('')
const inviteEmail = ref('')
const copiedInviteCode = ref<string | null>(null)
const lastCreatedInviteLink = ref<string | null>(null)

// User search state
const userSearchQuery = ref('')
const userSearchResults = ref<Array<{ id: string; username: string; displayName: string | null; avatarUrl: string | null; isFoundingMember?: boolean; isAdmin?: boolean }>>([])
const selectedUsers = ref<Array<{ id: string; username: string; displayName: string | null; avatarUrl: string | null; isFoundingMember?: boolean; isAdmin?: boolean }>>([])
const searchingUsers = ref(false)
let userSearchTimer: ReturnType<typeof setTimeout> | null = null

// Mobile detection
const isMobile = computed(() => {
  if (typeof navigator === 'undefined') return false
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
})

const members = computed(() => groupStore.groupMembers.value)
const requests = computed(() => groupStore.joinRequests.value)
const invitations = computed(() => groupStore.invitations.value)

onMounted(async () => {
  await loadData()
})

watch(() => props.groupId, async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  await Promise.all([
    groupStore.loadGroupMembers(props.groupId),
    groupStore.loadJoinRequests(props.groupId),
    groupStore.loadInvitations(props.groupId),
  ])
  loading.value = false
}

async function handleRemoveMember(member: GroupMember) {
  if (!confirm(`Remove ${member.displayName || member.username || 'this member'} from the group?`)) return

  const result = await groupStore.removeMember(props.groupId, member.userId)
  emit('toast', result.message, result.ok ? 'success' : 'error')
}

async function handleChangeRole(member: GroupMember, newRole: MemberRole) {
  const result = await groupStore.changeMemberRole(props.groupId, member.userId, newRole)
  emit('toast', result.message, result.ok ? 'success' : 'error')
}

function openTransferModal(member: GroupMember) {
  transferTargetMember.value = member
  showTransferModal.value = true
}

async function handleTransferOwnership() {
  if (!transferTargetMember.value) return

  const result = await groupStore.transferOwnership(props.groupId, transferTargetMember.value.userId)
  emit('toast', result.message, result.ok ? 'success' : 'error')
  showTransferModal.value = false
  transferTargetMember.value = null
}

async function handleApprove(request: JoinRequest) {
  const result = await groupStore.approveRequest(props.groupId, request.userId)
  emit('toast', result.message, result.ok ? 'success' : 'error')
}

async function handleReject(request: JoinRequest) {
  const result = await groupStore.rejectRequest(props.groupId, request.userId)
  emit('toast', result.message, result.ok ? 'success' : 'error')
}

async function handleCreateInvite(sendVia: 'copy' | 'sms' | 'email' = 'copy') {
  const result = await groupStore.createInvitation(props.groupId, {
    email: sendVia === 'email' && inviteEmail.value.trim() ? inviteEmail.value.trim() : undefined,
    maxUses: inviteMaxUses.value || undefined,
    expiresInDays: inviteExpiresIn.value || undefined,
  })

  if (result.ok && result.invitation) {
    const link = `${window.location.origin}/groups/invite/${result.invitation.inviteCode}`
    lastCreatedInviteLink.value = link

    if (sendVia === 'sms' && invitePhone.value.trim()) {
      // Open SMS app with pre-filled message
      const message = encodeURIComponent(`You're invited to join our group on Sasquatsh! ${link}`)
      const phone = invitePhone.value.replace(/\D/g, '') // Remove non-digits
      window.open(`sms:${phone}?body=${message}`, '_self')
      emit('toast', 'Opening SMS...', 'success')
    } else if (sendVia === 'email' && inviteEmail.value.trim()) {
      // Open email app with pre-filled message
      const subject = encodeURIComponent(`You're invited to join a group on Sasquatsh`)
      const body = encodeURIComponent(
        `Hi!\n\nYou've been invited to join our group on Sasquatsh, a platform for organizing board game nights.\n\nClick here to view the invitation and join:\n${link}\n\nSee you at game night!`
      )
      window.open(`mailto:${inviteEmail.value.trim()}?subject=${subject}&body=${body}`, '_self')
      emit('toast', 'Opening email...', 'success')
    } else {
      copyInviteLink(result.invitation.inviteCode)
      emit('toast', 'Invitation link copied!', 'success')
    }
  } else {
    emit('toast', result.message, 'error')
  }

  // Reset form
  invitePhone.value = ''
  inviteEmail.value = ''
  showInviteModal.value = false
}

async function handleRevokeInvite(invitation: GroupInvitation) {
  if (!confirm('Revoke this invitation?')) return

  const result = await groupStore.revokeInvitation(props.groupId, invitation.id)
  emit('toast', result.message, result.ok ? 'success' : 'error')
}

function copyInviteLink(code: string) {
  const link = `${window.location.origin}/groups/invite/${code}`
  navigator.clipboard.writeText(link)
  copiedInviteCode.value = code
  setTimeout(() => {
    copiedInviteCode.value = null
  }, 2000)
}

function getRoleBadgeClass(role: string) {
  switch (role) {
    case 'owner':
      return 'bg-purple-100 text-purple-700'
    case 'admin':
      return 'bg-blue-100 text-blue-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

// User search with debounce
watch(userSearchQuery, (query) => {
  if (userSearchTimer) {
    clearTimeout(userSearchTimer)
  }

  if (!query.trim() || query.trim().length < 2) {
    userSearchResults.value = []
    return
  }

  userSearchTimer = setTimeout(async () => {
    await searchUsers(query.trim())
  }, 300)
})

async function searchUsers(query: string) {
  searchingUsers.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    const results = await searchUsersByUsername(token, query)
    // Filter out already selected users and current group members
    const memberUserIds = members.value.map(m => m.userId)
    const selectedUserIds = selectedUsers.value.map(u => u.id)
    userSearchResults.value = results.filter(
      u => !memberUserIds.includes(u.id) && !selectedUserIds.includes(u.id)
    )
  } catch (err) {
    console.error('Failed to search users:', err)
    userSearchResults.value = []
  } finally {
    searchingUsers.value = false
  }
}

function addUserToInviteList(user: { id: string; username: string; displayName: string | null; avatarUrl: string | null }) {
  if (!selectedUsers.value.find(u => u.id === user.id)) {
    selectedUsers.value.push(user)
  }
  userSearchQuery.value = ''
  userSearchResults.value = []
}

function removeUserFromInviteList(userId: string) {
  selectedUsers.value = selectedUsers.value.filter(u => u.id !== userId)
}

async function handleSendInAppInvites() {
  if (selectedUsers.value.length === 0) return

  let successCount = 0
  let failCount = 0

  for (const user of selectedUsers.value) {
    const result = await groupStore.createInvitation(props.groupId, {
      userId: user.id,
      maxUses: 1,
      expiresInDays: inviteExpiresIn.value || undefined,
    })
    if (result.ok) {
      successCount++
    } else {
      failCount++
    }
  }

  if (successCount > 0) {
    emit('toast', `Sent ${successCount} invitation${successCount > 1 ? 's' : ''}`, 'success')
  }
  if (failCount > 0) {
    emit('toast', `Failed to send ${failCount} invitation${failCount > 1 ? 's' : ''}`, 'error')
  }

  // Reset state
  selectedUsers.value = []
  userSearchQuery.value = ''
  showInviteModal.value = false
}

function resetInviteModal() {
  invitePhone.value = ''
  inviteEmail.value = ''
  userSearchQuery.value = ''
  userSearchResults.value = []
  selectedUsers.value = []
  showInviteModal.value = false
}
</script>

<template>
  <div class="card">
    <!-- Tabs -->
    <div class="flex border-b border-gray-200">
      <button
        @click="activeTab = 'members'"
        :class="[
          'flex-1 py-3 px-4 text-sm font-medium text-center border-b-2 transition-colors',
          activeTab === 'members'
            ? 'border-primary-500 text-primary-600'
            : 'border-transparent text-gray-500 hover:text-gray-700'
        ]"
      >
        Members ({{ members.length }})
      </button>
      <button
        @click="activeTab = 'requests'"
        :class="[
          'flex-1 py-3 px-4 text-sm font-medium text-center border-b-2 transition-colors',
          activeTab === 'requests'
            ? 'border-primary-500 text-primary-600'
            : 'border-transparent text-gray-500 hover:text-gray-700'
        ]"
      >
        Requests
        <span v-if="requests.length > 0" class="ml-1 bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">
          {{ requests.length }}
        </span>
      </button>
      <button
        @click="activeTab = 'invitations'"
        :class="[
          'flex-1 py-3 px-4 text-sm font-medium text-center border-b-2 transition-colors',
          activeTab === 'invitations'
            ? 'border-primary-500 text-primary-600'
            : 'border-transparent text-gray-500 hover:text-gray-700'
        ]"
      >
        Invitations
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="p-4">
      <div class="h-1 w-full bg-gray-200 rounded-full overflow-hidden">
        <div class="h-full bg-primary-500 rounded-full animate-pulse" style="width: 60%"></div>
      </div>
    </div>

    <!-- Members Tab -->
    <div v-else-if="activeTab === 'members'" class="p-4">
      <div v-if="members.length === 0" class="text-center text-gray-500 py-4">
        No members yet
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="member in members"
          :key="member.id"
          class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
        >
          <div class="flex items-center gap-3">
            <UserAvatar
              :avatar-url="member.avatarUrl"
              :display-name="member.displayName"
              :is-founding-member="member.isFoundingMember"
              :is-admin="member.isAdmin"
              size="md"
            />
            <div>
              <div class="font-medium text-gray-900">{{ member.displayName || member.username || 'Unknown' }}</div>
              <div class="text-sm text-gray-500">
                <span v-if="member.username">@{{ member.username }} · </span>
                Joined {{ formatDate(member.joinedAt) }}
              </div>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span :class="['text-xs px-2 py-1 rounded-full', getRoleBadgeClass(member.role)]">
              {{ member.role }}
            </span>
            <template v-if="isOwner && member.role !== 'owner'">
              <select
                :value="member.role"
                @change="handleChangeRole(member, ($event.target as HTMLSelectElement).value as MemberRole)"
                class="text-xs border border-gray-300 rounded px-2 py-1"
              >
                <option value="member">Member</option>
                <option value="admin">Admin</option>
              </select>
              <button
                @click="openTransferModal(member)"
                class="text-purple-500 hover:text-purple-700 p-1"
                title="Transfer leadership"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21,9L17,5V8H10V10H17V13M7,11L3,15L7,19V16H14V14H7V11Z"/>
                </svg>
              </button>
              <button
                @click="handleRemoveMember(member)"
                class="text-red-500 hover:text-red-700 p-1"
                title="Remove member"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
            </template>
            <template v-else-if="!isOwner && member.role === 'member'">
              <button
                @click="handleRemoveMember(member)"
                class="text-red-500 hover:text-red-700 p-1"
                title="Remove member"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- Requests Tab -->
    <div v-else-if="activeTab === 'requests'" class="p-4">
      <div v-if="requests.length === 0" class="text-center text-gray-500 py-4">
        No pending requests
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="request in requests"
          :key="request.id"
          class="p-3 bg-yellow-50 rounded-lg border border-yellow-200"
        >
          <div class="flex items-start justify-between">
            <div class="flex items-center gap-3">
              <UserAvatar
                :avatar-url="request.avatarUrl"
                :display-name="request.displayName"
                :is-founding-member="request.isFoundingMember"
                :is-admin="request.isAdmin"
                size="md"
              />
              <div>
                <div class="font-medium text-gray-900">{{ request.displayName || request.username || 'Unknown' }}</div>
                <div class="text-sm text-gray-500">
                  <span v-if="request.username">@{{ request.username }} · </span>
                  Requested {{ formatDate(request.createdAt) }}
                </div>
              </div>
            </div>
            <div class="flex gap-2">
              <button
                @click="handleApprove(request)"
                class="btn-primary text-sm px-3 py-1"
              >
                Approve
              </button>
              <button
                @click="handleReject(request)"
                class="btn-outline text-sm px-3 py-1"
              >
                Reject
              </button>
            </div>
          </div>
          <div v-if="request.message" class="mt-2 p-2 bg-white rounded text-sm text-gray-600">
            "{{ request.message }}"
          </div>
        </div>
      </div>
    </div>

    <!-- Invitations Tab -->
    <div v-else-if="activeTab === 'invitations'" class="p-4">
      <button
        @click="showInviteModal = true"
        class="btn-primary w-full mb-4"
      >
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
        </svg>
        Create Invitation Link
      </button>

      <div v-if="invitations.length === 0" class="text-center text-gray-500 py-4">
        No active invitations
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="invitation in invitations"
          :key="invitation.id"
          class="p-3 bg-gray-50 rounded-lg"
        >
          <div class="flex items-center justify-between mb-2">
            <code class="text-sm bg-white px-2 py-1 rounded border">{{ invitation.inviteCode }}</code>
            <div class="flex gap-2">
              <button
                @click="copyInviteLink(invitation.inviteCode)"
                class="btn-outline text-sm px-3 py-1"
              >
                {{ copiedInviteCode === invitation.inviteCode ? 'Copied!' : 'Copy Link' }}
              </button>
              <button
                @click="handleRevokeInvite(invitation)"
                class="text-red-500 hover:text-red-700 p-1"
                title="Revoke"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="text-sm text-gray-500">
            Uses: {{ invitation.usesCount }}{{ invitation.maxUses ? `/${invitation.maxUses}` : '' }}
            <span v-if="invitation.expiresAt" class="ml-2">
              Expires: {{ formatDate(invitation.expiresAt) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Invitation Modal -->
    <div
      v-if="showInviteModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="resetInviteModal"
    >
      <div class="bg-white rounded-xl p-6 max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">Invite Members</h3>

        <div class="space-y-4">
          <!-- Search Users Section -->
          <div class="border-b border-gray-200 pb-4">
            <p class="text-sm font-medium text-gray-700 mb-2">Invite Sasquatsh Users</p>
            <div class="relative">
              <input
                v-model="userSearchQuery"
                type="text"
                class="input pl-9"
                placeholder="Search by username..."
              />
              <svg class="w-5 h-5 text-gray-400 absolute left-2.5 top-1/2 -translate-y-1/2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
              </svg>
              <div v-if="searchingUsers" class="absolute right-3 top-1/2 -translate-y-1/2">
                <svg class="w-4 h-4 animate-spin text-gray-400" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
              </div>
            </div>

            <!-- Search Results -->
            <div v-if="userSearchResults.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-40 overflow-y-auto">
              <button
                v-for="user in userSearchResults"
                :key="user.id"
                @click="addUserToInviteList(user)"
                class="w-full flex items-center gap-3 p-2 hover:bg-gray-50 transition-colors text-left"
              >
                <UserAvatar
                  :avatar-url="user.avatarUrl"
                  :display-name="user.displayName"
                  :is-founding-member="user.isFoundingMember"
                  :is-admin="user.isAdmin"
                  size="sm"
                  class="flex-shrink-0"
                />
                <div class="min-w-0">
                  <div class="font-medium text-sm truncate">{{ user.displayName || user.username }}</div>
                  <div class="text-xs text-gray-500">@{{ user.username }}</div>
                </div>
              </button>
            </div>

            <!-- Selected Users -->
            <div v-if="selectedUsers.length > 0" class="mt-3">
              <p class="text-xs text-gray-500 mb-2">Selected ({{ selectedUsers.length }})</p>
              <div class="flex flex-wrap gap-2">
                <div
                  v-for="user in selectedUsers"
                  :key="user.id"
                  class="flex items-center gap-2 bg-primary-50 px-2 py-1 rounded-full"
                >
                  <UserAvatar
                    :avatar-url="user.avatarUrl"
                    :display-name="user.displayName"
                    :is-founding-member="user.isFoundingMember"
                    :is-admin="user.isAdmin"
                    size="xs"
                    class="flex-shrink-0"
                  />
                  <span class="text-sm text-primary-700">@{{ user.username }}</span>
                  <button
                    @click="removeUserFromInviteList(user.id)"
                    class="text-primary-500 hover:text-primary-700"
                  >
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                    </svg>
                  </button>
                </div>
              </div>
              <button
                @click="handleSendInAppInvites"
                class="mt-3 w-full flex items-center justify-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg font-medium transition-colors"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M2,21L23,12L2,3V10L17,12L2,14V21Z"/>
                </svg>
                Send {{ selectedUsers.length }} In-App Invitation{{ selectedUsers.length > 1 ? 's' : '' }}
              </button>
            </div>
          </div>

          <!-- Link Settings -->
          <div>
            <p class="text-sm font-medium text-gray-700 mb-2">Or Create Shareable Link</p>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">Max Uses</label>
                <select v-model="inviteMaxUses" class="input text-sm">
                  <option :value="1">Single use</option>
                  <option :value="5">5 uses</option>
                  <option :value="10">10 uses</option>
                  <option :value="null">Unlimited</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">Expires In</label>
                <select v-model="inviteExpiresIn" class="input text-sm">
                  <option :value="1">1 day</option>
                  <option :value="7">7 days</option>
                  <option :value="30">30 days</option>
                  <option :value="null">Never</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Send Directly Section -->
          <div class="border-t border-gray-200 pt-4">
            <p class="text-sm font-medium text-gray-700 mb-3">Send link via (optional)</p>

            <div class="space-y-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">Email</label>
                <input
                  v-model="inviteEmail"
                  type="email"
                  class="input"
                  placeholder="friend@example.com"
                />
              </div>

              <div v-if="isMobile">
                <label class="block text-xs text-gray-500 mb-1">Phone (SMS)</label>
                <input
                  v-model="invitePhone"
                  type="tel"
                  class="input"
                  placeholder="(555) 123-4567"
                />
              </div>
            </div>
            <p class="text-xs text-gray-500 mt-2">Opens your default email or SMS app with the invite pre-filled</p>
          </div>
        </div>

        <div class="flex flex-col gap-2 mt-6">
          <div class="flex gap-3">
            <button @click="resetInviteModal" class="btn-outline flex-1">
              Cancel
            </button>
            <button @click="handleCreateInvite('copy')" class="btn-primary flex-1">
              Create & Copy Link
            </button>
          </div>

          <div v-if="inviteEmail.trim() || (isMobile && invitePhone.trim())" class="flex gap-2">
            <button
              v-if="inviteEmail.trim()"
              @click="handleCreateInvite('email')"
              class="flex-1 flex items-center justify-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg font-medium transition-colors"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z"/>
              </svg>
              Send Email
            </button>
            <button
              v-if="isMobile && invitePhone.trim()"
              @click="handleCreateInvite('sms')"
              class="flex-1 flex items-center justify-center gap-2 px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg font-medium transition-colors"
            >
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20,15.5C18.75,15.5 17.55,15.3 16.43,14.93C16.08,14.82 15.69,14.9 15.41,15.18L13.21,17.38C10.38,15.94 8.06,13.62 6.62,10.79L8.82,8.59C9.1,8.31 9.18,7.92 9.07,7.57C8.7,6.45 8.5,5.25 8.5,4A1,1 0 0,0 7.5,3H4A1,1 0 0,0 3,4A17,17 0 0,0 20,21A1,1 0 0,0 21,20V16.5A1,1 0 0,0 20,15.5Z"/>
              </svg>
              Send SMS
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Transfer Ownership Modal -->
    <div
      v-if="showTransferModal && transferTargetMember"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="showTransferModal = false"
    >
      <div class="bg-white rounded-xl p-6 max-w-md w-full mx-4">
        <div class="flex items-center gap-3 mb-4">
          <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
            <svg class="w-6 h-6 text-purple-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21,9L17,5V8H10V10H17V13M7,11L3,15L7,19V16H14V14H7V11Z"/>
            </svg>
          </div>
          <h3 class="text-lg font-semibold">Transfer Leadership</h3>
        </div>

        <p class="text-gray-600 mb-4">
          Are you sure you want to transfer group leadership to
          <strong>{{ transferTargetMember.displayName || 'this member' }}</strong>?
        </p>

        <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mb-4">
          <p class="text-sm text-yellow-800">
            This action will make them the new owner and you will become an admin. This cannot be undone by you.
          </p>
        </div>

        <div class="flex gap-3">
          <button @click="showTransferModal = false" class="btn-outline flex-1">
            Cancel
          </button>
          <button @click="handleTransferOwnership" class="bg-purple-500 hover:bg-purple-600 text-white px-4 py-2 rounded-lg font-medium flex-1">
            Transfer Leadership
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
