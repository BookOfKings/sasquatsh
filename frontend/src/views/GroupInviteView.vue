<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGroupStore } from '@/stores/useGroupStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { getInvitationPreview } from '@/services/groupsApi'
import type { InvitationPreview } from '@/types/groups'
import UserAvatar from '@/components/common/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const groupStore = useGroupStore()
const auth = useAuthStore()

const inviteCode = computed(() => route.params.code as string)
const loading = ref(true)
const accepting = ref(false)
const error = ref<string | null>(null)
const success = ref(false)
const preview = ref<InvitationPreview | null>(null)

onMounted(async () => {
  await loadPreview()
})

async function loadPreview() {
  loading.value = true
  error.value = null

  try {
    preview.value = await getInvitationPreview(inviteCode.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load invitation'
  }

  loading.value = false
}

async function acceptInvite() {
  accepting.value = true
  error.value = null

  const result = await groupStore.acceptInvitation(inviteCode.value)

  if (result.ok) {
    success.value = true
  } else {
    error.value = result.message
  }

  accepting.value = false
}

function goToGroup() {
  if (preview.value?.group.slug) {
    router.push(`/groups/${preview.value.group.slug}`)
  } else {
    router.push('/groups')
  }
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function goToSignup() {
  const query: Record<string, string> = { redirect: route.fullPath }
  if (preview.value?.invitedEmail) {
    query.email = preview.value.invitedEmail
  }
  router.push({ name: 'signup', query })
}

function goToGroups() {
  router.push('/groups')
}

function formatLocation(city: string | null, state: string | null): string {
  if (city && state) return `${city}, ${state}`
  if (city) return city
  if (state) return state
  return ''
}
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] flex items-center justify-center p-4">
    <div class="card p-8 text-center max-w-md w-full">
      <!-- Loading -->
      <div v-if="loading">
        <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-primary-100 flex items-center justify-center">
          <svg class="w-8 h-8 text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <h1 class="text-xl font-bold text-gray-900 mb-2">Loading Invitation...</h1>
      </div>

      <!-- Accepting -->
      <div v-else-if="accepting">
        <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-primary-100 flex items-center justify-center">
          <svg class="w-8 h-8 text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <h1 class="text-xl font-bold text-gray-900 mb-2">Joining Group...</h1>
        <p class="text-gray-600">Please wait while we add you to the group.</p>
      </div>

      <!-- Success -->
      <div v-else-if="success">
        <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
          <svg class="w-8 h-8 text-green-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
        </div>
        <h1 class="text-xl font-bold text-gray-900 mb-2">Welcome to the Group!</h1>
        <p class="text-gray-600 mb-6">
          You've successfully joined <strong>{{ preview?.group.name || 'the group' }}</strong>.
        </p>
        <button @click="goToGroup" class="btn-primary w-full">
          Go to Group
        </button>
      </div>

      <!-- Error -->
      <div v-else-if="error">
        <div class="w-16 h-16 mx-auto mb-4 rounded-full bg-red-100 flex items-center justify-center">
          <svg class="w-8 h-8 text-red-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
        </div>
        <h1 class="text-xl font-bold text-gray-900 mb-2">Invitation Error</h1>
        <p class="text-gray-600 mb-6">{{ error }}</p>
        <button @click="goToGroups" class="btn-outline w-full">
          Browse Groups
        </button>
      </div>

      <!-- Preview (loaded, not yet accepted) -->
      <div v-else-if="preview">
        <!-- Group Logo/Icon -->
        <div class="w-20 h-20 mx-auto mb-4 rounded-full bg-primary-100 flex items-center justify-center overflow-hidden">
          <img
            v-if="preview.group.logoUrl"
            :src="preview.group.logoUrl"
            :alt="preview.group.name"
            class="w-full h-full object-cover"
          />
          <svg v-else class="w-10 h-10 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
          </svg>
        </div>

        <h1 class="text-xl font-bold text-gray-900 mb-1">{{ preview.group.name }}</h1>

        <p v-if="formatLocation(preview.group.locationCity, preview.group.locationState)" class="text-sm text-gray-500 mb-3">
          {{ formatLocation(preview.group.locationCity, preview.group.locationState) }}
        </p>

        <p v-if="preview.group.description" class="text-gray-600 text-sm mb-4 line-clamp-3">
          {{ preview.group.description }}
        </p>

        <!-- Invited by -->
        <div class="bg-gray-50 rounded-lg p-3 mb-6">
          <p class="text-sm text-gray-500 mb-2">You've been invited by</p>
          <div class="flex items-center justify-center gap-2">
            <UserAvatar
              :avatar-url="preview.invitedBy.avatarUrl"
              :display-name="preview.invitedBy.displayName"
              :is-founding-member="preview.invitedBy.isFoundingMember"
              size="sm"
            />
            <span class="font-medium text-gray-900">{{ preview.invitedBy.displayName || 'A group member' }}</span>
          </div>
        </div>

        <!-- Actions -->
        <template v-if="auth.isAuthenticated.value">
          <button @click="acceptInvite" class="btn-primary w-full mb-3">
            Join Group
          </button>
          <button @click="goToGroups" class="btn-ghost w-full text-sm">
            No thanks
          </button>
        </template>

        <template v-else>
          <p class="text-sm text-gray-500 mb-4">Sign in or create an account to join this group</p>
          <button @click="goToLogin" class="btn-primary w-full mb-3">
            Sign In to Join
          </button>
          <button @click="goToSignup" class="btn-outline w-full">
            Create Account
          </button>
        </template>
      </div>
    </div>
  </div>
</template>
