<script setup lang="ts">
import { onMounted, computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGroupStore } from '@/stores/useGroupStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { getGroupPlanningSessions } from '@/services/planningApi'
import type { PlanningSession } from '@/types/planning'
import GroupAdminPanel from '@/components/groups/GroupAdminPanel.vue'
import EditGroupModal from '@/components/groups/EditGroupModal.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const route = useRoute()
const router = useRouter()
const groupStore = useGroupStore()
const auth = useAuthStore()

const toast = reactive({
  visible: false,
  message: '',
  type: 'success' as 'success' | 'error',
})

const groupSlug = computed(() => route.params.slug as string)
const group = computed(() => groupStore.currentGroup.value)
const userMembership = computed(() => groupStore.userMembership.value)
const isOwner = computed(() => userMembership.value?.role === 'owner')
const isAdmin = computed(() => ['owner', 'admin'].includes(userMembership.value?.role || ''))
const isMember = computed(() => !!userMembership.value)

const showEditModal = ref(false)
const planningSessions = ref<PlanningSession[]>([])
const loadingPlans = ref(false)
const showChat = ref(false)

onMounted(async () => {
  await groupStore.loadGroup(groupSlug.value)
  if (group.value && auth.isAuthenticated.value) {
    await groupStore.loadGroupMembers(group.value.id)
    await loadPlanningSessions()
  }
})

async function loadPlanningSessions() {
  if (!group.value || !isMember.value) return
  loadingPlans.value = true
  try {
    const token = await auth.getIdToken()
    if (token) {
      planningSessions.value = await getGroupPlanningSessions(token, group.value.id)
    }
  } catch (err) {
    console.error('Failed to load planning sessions:', err)
  } finally {
    loadingPlans.value = false
  }
}

watch(() => auth.isAuthenticated.value, async (isAuth) => {
  if (isAuth && group.value) {
    await groupStore.loadGroupMembers(group.value.id)
    // loadPlanningSessions checks isMember internally
    await loadPlanningSessions()
  }
})

function formatGroupType(type: string): string {
  const labels: Record<string, string> = {
    geographic: 'Local Community',
    interest: 'Interest Group',
    both: 'Community',
  }
  return labels[type] || type
}

function showMessage(ok: boolean, message: string) {
  toast.message = message
  toast.type = ok ? 'success' : 'error'
  toast.visible = true
  setTimeout(() => {
    toast.visible = false
  }, 3500)
}

async function handleJoin() {
  if (!group.value) return
  const result = await groupStore.joinGroup(group.value.id)
  showMessage(result.ok, result.message)
}

async function handleRequestJoin() {
  if (!group.value) return
  const result = await groupStore.requestToJoin(group.value.id)
  showMessage(result.ok, result.message)
}

async function handleLeave() {
  if (!group.value) return
  if (!confirm('Are you sure you want to leave this group?')) return

  const result = await groupStore.leaveGroup(group.value.id)
  showMessage(result.ok, result.message)
  if (result.ok) {
    router.push('/groups')
  }
}

function goBack() {
  router.push('/groups')
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function handleGroupUpdated() {
  showEditModal.value = false
  showMessage(true, 'Group updated successfully')
  // Reload the group to get updated data
  await groupStore.loadGroup(groupSlug.value)
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Back Button -->
    <button class="btn-ghost mb-4" @click="goBack">
      <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
      </svg>
      Back to Groups
    </button>

    <!-- Loading -->
    <div v-if="groupStore.loading.value" class="h-1 w-full bg-gray-200 rounded-full overflow-hidden mb-4">
      <div class="h-full bg-primary-500 rounded-full animate-pulse" style="width: 60%"></div>
    </div>

    <template v-if="group">
      <!-- Main Group Card -->
      <div class="card p-6 mb-6">
        <!-- Header -->
        <div class="flex items-start gap-4 mb-4">
          <div class="w-16 h-16 rounded-xl bg-primary-100 flex items-center justify-center overflow-hidden flex-shrink-0">
            <img
              v-if="group.logoUrl"
              :src="group.logoUrl"
              class="w-full h-full object-cover"
            />
            <svg v-else class="w-8 h-8 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
            </svg>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h1 class="text-2xl font-bold text-gray-900">{{ group.name }}</h1>
              <button
                v-if="isAdmin"
                @click="showEditModal = true"
                class="p-1.5 text-gray-400 hover:text-primary-500 hover:bg-gray-100 rounded-lg transition-colors"
                title="Edit group"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                </svg>
              </button>
            </div>
            <p class="text-gray-500">
              {{ formatGroupType(group.groupType) }}
              <span v-if="group.creator"> &bull; Created by {{ group.creator.displayName || 'Unknown' }}</span>
            </p>
          </div>
        </div>

        <!-- Tags -->
        <div class="flex flex-wrap gap-2 mb-6">
          <span class="chip-primary">
            {{ group.memberCount }} member{{ group.memberCount !== 1 ? 's' : '' }}
          </span>
          <span v-if="group.joinPolicy === 'invite_only'" class="chip-warning">
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
            </svg>
            Invite Only
          </span>
          <span v-else-if="group.joinPolicy === 'request'" class="text-xs px-2 py-1 rounded-full bg-yellow-100 text-yellow-700">
            Request to Join
          </span>
          <span v-else class="text-xs px-2 py-1 rounded-full bg-green-100 text-green-700">
            Open
          </span>
          <span v-if="userMembership" :class="[
            'text-xs px-2 py-1 rounded-full',
            userMembership.role === 'owner' ? 'bg-purple-100 text-purple-700' :
            userMembership.role === 'admin' ? 'bg-blue-100 text-blue-700' :
            'bg-green-100 text-green-700'
          ]">
            {{ userMembership.role }}
          </span>
        </div>

        <!-- Info -->
        <div class="space-y-4 mb-6">
          <!-- Location -->
          <div v-if="group.locationCity || group.locationState" class="flex items-start gap-3">
            <svg class="w-5 h-5 text-primary-500 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
            </svg>
            <div>
              <div class="font-medium">{{ [group.locationCity, group.locationState].filter(Boolean).join(', ') }}</div>
              <div v-if="group.locationRadiusMiles" class="text-sm text-gray-500">
                {{ group.locationRadiusMiles }} mile radius
              </div>
            </div>
          </div>
        </div>

        <!-- Description -->
        <div v-if="group.description" class="mb-6">
          <h3 class="font-semibold mb-2">About this group</h3>
          <p class="text-gray-600">{{ group.description }}</p>
        </div>

        <!-- Actions -->
        <div class="flex flex-wrap gap-3">
          <!-- Authenticated user actions -->
          <template v-if="auth.isAuthenticated.value">
            <!-- Not a member -->
            <template v-if="!isMember">
              <!-- Open group - join directly -->
              <button
                v-if="group.joinPolicy === 'open'"
                class="btn-primary"
                @click="handleJoin"
              >
                <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
                </svg>
                Join Group
              </button>
              <!-- Request to join group -->
              <button
                v-else-if="group.joinPolicy === 'request'"
                class="btn-primary"
                @click="handleRequestJoin"
              >
                <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,15C12.81,15 13.5,14.7 14.11,14.11C14.7,13.5 15,12.81 15,12C15,11.19 14.7,10.5 14.11,9.89C13.5,9.3 12.81,9 12,9C11.19,9 10.5,9.3 9.89,9.89C9.3,10.5 9,11.19 9,12C9,12.81 9.3,13.5 9.89,14.11C10.5,14.7 11.19,15 12,15M12,2C14.75,2 17.1,3 19.05,4.95C21,6.9 22,9.25 22,12V13.45C22,14.45 21.65,15.3 21,16C20.3,16.67 19.5,17 18.5,17C17.3,17 16.31,16.5 15.56,15.5C14.56,16.5 13.38,17 12,17C10.63,17 9.45,16.5 8.46,15.54C7.5,14.55 7,13.38 7,12C7,10.63 7.5,9.45 8.46,8.46C9.45,7.5 10.63,7 12,7C13.38,7 14.55,7.5 15.54,8.46C16.5,9.45 17,10.63 17,12V13.45C17,13.86 17.16,14.22 17.46,14.53C17.76,14.84 18.11,15 18.5,15C18.92,15 19.27,14.84 19.57,14.53C19.87,14.22 20,13.86 20,13.45V12C20,9.81 19.23,7.93 17.65,6.35C16.07,4.77 14.19,4 12,4C9.81,4 7.93,4.77 6.35,6.35C4.77,7.93 4,9.81 4,12C4,14.19 4.77,16.07 6.35,17.65C7.93,19.23 9.81,20 12,20H17V22H12C9.25,22 6.9,21 4.95,19.05C3,17.1 2,14.75 2,12C2,9.25 3,6.9 4.95,4.95C6.9,3 9.25,2 12,2Z"/>
                </svg>
                Request to Join
              </button>
              <!-- Invite only group -->
              <div v-else class="text-sm text-gray-500 bg-gray-100 px-4 py-2 rounded-lg">
                <svg class="w-4 h-4 inline mr-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
                </svg>
                Invitation required to join
              </div>
            </template>

            <!-- Member - show leave button -->
            <button
              v-if="isMember && !isOwner"
              class="btn-outline text-red-600 border-red-300 hover:bg-red-50"
              @click="handleLeave"
            >
              <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M16,17V14H9V10H16V7L21,12L16,17M14,2A2,2 0 0,1 16,4V6H14V4H5V20H14V18H16V20A2,2 0 0,1 14,22H5A2,2 0 0,1 3,20V4A2,2 0 0,1 5,2H14Z"/>
              </svg>
              Leave Group
            </button>
          </template>

          <!-- Not authenticated -->
          <template v-else>
            <button class="btn-outline" @click="goToLogin">
              Sign in to join
            </button>
          </template>
        </div>
      </div>

      <!-- Admin Panel (for owner/admin) -->
      <GroupAdminPanel
        v-if="isAdmin && group"
        :group-id="group.id"
        :is-owner="isOwner"
        @toast="(msg, type) => showMessage(type === 'success', msg)"
        class="mb-6"
      />

      <!-- Members Section (for all members, when not admin - admins see this in admin panel) -->
      <div v-if="isMember && !isAdmin && group" class="card mb-6">
        <div class="p-4 border-b border-gray-100 flex items-center justify-between">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
            </svg>
            Members ({{ groupStore.groupMembers.value.length }})
          </h2>
        </div>
        <div class="divide-y divide-gray-100">
          <div
            v-for="member in groupStore.groupMembers.value"
            :key="member.id"
            class="flex items-center gap-3 p-4"
          >
            <div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="member.avatarUrl"
                :src="member.avatarUrl"
                class="w-full h-full object-cover"
                alt=""
              />
              <svg v-else class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-medium text-gray-900">{{ member.displayName || member.username || 'Unknown' }}</div>
              <div v-if="member.username" class="text-sm text-gray-500">@{{ member.username }}</div>
            </div>
            <span :class="[
              'text-xs px-2 py-1 rounded-full',
              member.role === 'owner' ? 'bg-purple-100 text-purple-700' :
              member.role === 'admin' ? 'bg-blue-100 text-blue-700' :
              'bg-gray-100 text-gray-700'
            ]">
              {{ member.role }}
            </span>
          </div>
        </div>
      </div>

      <!-- Planning Sessions Section -->
      <div v-if="isMember" class="card mb-6">
        <div class="p-4 border-b border-gray-100 flex items-center justify-between">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M11,9H9V12H6V14H9V17H11V14H14V12H11V9Z"/>
            </svg>
            Game Planning
          </h2>
          <button
            v-if="isAdmin"
            class="btn-primary text-sm"
            @click="router.push(`/groups/${groupSlug}/plan`)"
          >
            <svg class="w-4 h-4 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Host a Game
          </button>
        </div>
        <div v-if="loadingPlans" class="p-6 text-center">
          <svg class="w-6 h-6 mx-auto text-gray-400 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <div v-else-if="planningSessions.filter(s => s.status === 'open').length > 0" class="divide-y divide-gray-100">
          <button
            v-for="session in planningSessions.filter(s => s.status === 'open')"
            :key="session.id"
            class="w-full flex items-center gap-4 p-4 hover:bg-gray-50 transition-colors text-left"
            @click="router.push(`/planning/${session.id}`)"
          >
            <div class="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <div class="font-medium">{{ session.title }}</div>
              <div class="text-sm text-gray-500">
                Deadline: {{ new Date(session.responseDeadline).toLocaleDateString() }}
              </div>
            </div>
            <span class="chip-success text-xs">Open</span>
            <svg class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
              <path d="M8.59,16.58L13.17,12L8.59,7.41L10,6L16,12L10,18L8.59,16.58Z"/>
            </svg>
          </button>
        </div>
        <div v-else class="p-6 text-center text-gray-500">
          <p>No active planning sessions.</p>
          <p v-if="isAdmin" class="text-sm mt-1">Click "Host a Game" to start coordinating your next game night.</p>
        </div>
      </div>

      <!-- Upcoming Games Section -->
      <div class="card mb-6">
        <div class="p-4 border-b border-gray-100">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
            </svg>
            Upcoming Games
          </h2>
        </div>
        <p class="text-gray-500 text-center py-8">
          No upcoming games scheduled for this group yet.
        </p>
      </div>

      <!-- Group Chat (visible to members only) -->
      <div v-if="isMember && group" class="card">
        <div class="p-4 border-b border-gray-100">
          <button
            class="w-full flex items-center justify-between text-left"
            @click="showChat = !showChat"
          >
            <h2 class="font-semibold flex items-center gap-2">
              <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H6L4,18V4H20"/>
              </svg>
              Group Chat
            </h2>
            <svg
              class="w-5 h-5 text-gray-400 transition-transform"
              :class="{ 'rotate-180': showChat }"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z"/>
            </svg>
          </button>
        </div>
        <div v-if="showChat" class="h-96">
          <ChatPanel
            context-type="group"
            :context-id="group.id"
            title="Group Discussion"
          />
        </div>
      </div>
    </template>

    <!-- Error State -->
    <div v-else-if="groupStore.error.value && !groupStore.loading.value" class="card p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      <p class="text-gray-500">{{ groupStore.error.value }}</p>
      <button class="btn-primary mt-4" @click="goBack">
        Back to Groups
      </button>
    </div>

    <!-- Toast Notification -->
    <div
      v-if="toast.visible"
      class="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 px-4 py-3 rounded-lg shadow-lg"
      :class="toast.type === 'success' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- Edit Group Modal -->
    <EditGroupModal
      v-if="showEditModal && group"
      :group="group"
      @close="showEditModal = false"
      @saved="handleGroupUpdated"
    />
  </div>
</template>
