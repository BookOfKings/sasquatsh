<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getShareLinkPreview,
  acceptShareLink,
  type ShareLinkPreview,
} from '@/services/shareLinksApi'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const code = computed(() => route.params.code as string)
const loading = ref(true)
const accepting = ref(false)
const error = ref<string | null>(null)
const success = ref(false)
const preview = ref<ShareLinkPreview | null>(null)

onMounted(async () => {
  try {
    preview.value = await getShareLinkPreview(code.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Invalid invite link'
  } finally {
    loading.value = false
  }
})

async function handleAccept() {
  accepting.value = true
  error.value = null

  try {
    const token = await auth.getIdToken()
    if (!token) {
      error.value = 'Please sign in first'
      return
    }

    const result = await acceptShareLink(token, code.value)
    success.value = true

    // Redirect after short delay
    setTimeout(() => {
      if (result.target?.type === 'planning_session' && result.target.id) {
        router.push(`/planning/${result.target.id}`)
      } else if (result.target?.type === 'event' && result.target.id) {
        router.push(`/games/${result.target.id}`)
      } else if (result.groupSlug) {
        router.push(`/groups/${result.groupSlug}`)
      } else {
        router.push('/groups')
      }
    }, 1500)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to accept invite'
  } finally {
    accepting.value = false
  }
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

function goToSignup() {
  router.push({ name: 'signup', query: { redirect: route.fullPath } })
}
</script>

<template>
  <div class="min-h-[60vh] flex items-center justify-center px-4 py-12">
    <!-- Loading -->
    <div v-if="loading" class="text-center">
      <svg class="animate-spin h-8 w-8 text-primary-500 mx-auto mb-4" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="text-gray-500">Loading invitation...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error && !preview" class="text-center max-w-md">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      <h2 class="text-xl font-bold text-gray-900 mb-2">Invalid Invite Link</h2>
      <p class="text-gray-500 mb-4">{{ error }}</p>
      <button @click="router.push('/')" class="btn-primary">Go Home</button>
    </div>

    <!-- Preview Card -->
    <div v-else-if="preview" class="card p-6 max-w-md w-full">
      <!-- Success state -->
      <div v-if="success" class="text-center py-4">
        <svg class="w-16 h-16 mx-auto text-green-500 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <h2 class="text-xl font-bold text-gray-900 mb-2">You're in!</h2>
        <p class="text-gray-500">Redirecting you now...</p>
      </div>

      <template v-else>
        <!-- Group info -->
        <div class="text-center mb-6">
          <div v-if="preview.group?.logoUrl" class="w-16 h-16 mx-auto mb-3 rounded-full overflow-hidden bg-gray-100">
            <img :src="preview.group.logoUrl" :alt="preview.group.name" class="w-full h-full object-cover" />
          </div>
          <div v-else class="w-16 h-16 mx-auto mb-3 rounded-full bg-primary-500 flex items-center justify-center">
            <svg class="w-8 h-8 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8Z"/>
            </svg>
          </div>

          <h2 class="text-xl font-bold text-gray-900">{{ preview.group?.name }}</h2>
          <p v-if="preview.group?.city || preview.group?.state" class="text-sm text-gray-500">
            {{ [preview.group?.city, preview.group?.state].filter(Boolean).join(', ') }}
          </p>
        </div>

        <!-- What you're joining -->
        <div v-if="preview.target" class="bg-primary-50 rounded-lg p-4 mb-4">
          <div class="text-xs text-primary-600 font-medium mb-1">
            {{ preview.target.type === 'planning_session' ? 'Game Planning Session' : 'Upcoming Game' }}
          </div>
          <div class="font-semibold text-primary-900">{{ preview.target.title }}</div>
        </div>

        <div v-else-if="preview.linkType === 'group_recurring'" class="bg-gray-50 rounded-lg p-4 mb-4">
          <div class="text-sm text-gray-600">
            Join this group and get access to their game nights
          </div>
        </div>

        <!-- Invited by -->
        <div v-if="preview.invitedBy?.displayName" class="text-sm text-gray-500 text-center mb-6">
          Invited by <span class="font-medium text-gray-700">{{ preview.invitedBy.displayName }}</span>
        </div>

        <!-- Error -->
        <div v-if="error" class="alert-error mb-4 text-sm">{{ error }}</div>

        <!-- Actions -->
        <div v-if="auth.isAuthenticated.value" class="space-y-3">
          <button
            @click="handleAccept"
            :disabled="accepting"
            class="btn-primary w-full py-3"
          >
            <svg v-if="accepting" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ preview.target ? 'Join & Go' : 'Join Group' }}
          </button>
        </div>

        <div v-else class="space-y-3">
          <button @click="goToSignup" class="btn-primary w-full py-3">
            Create Account & Join
          </button>
          <button @click="goToLogin" class="btn-outline w-full py-3">
            Sign In & Join
          </button>
        </div>
      </template>
    </div>
  </div>
</template>
