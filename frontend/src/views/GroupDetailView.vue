<script setup lang="ts">
import { onMounted, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGroupStore } from '@/stores/useGroupStore'
import { useAuthStore } from '@/stores/useAuthStore'

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

onMounted(() => {
  groupStore.loadGroup(groupSlug.value)
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

function goBack() {
  router.push('/groups')
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
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
            <h1 class="text-2xl font-bold text-gray-900">{{ group.name }}</h1>
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
          <span v-if="!group.isPublic" class="chip-warning">
            <svg class="w-3 h-3 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,17A2,2 0 0,0 14,15C14,13.89 13.1,13 12,13A2,2 0 0,0 10,15A2,2 0 0,0 12,17M18,8A2,2 0 0,1 20,10V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V10C4,8.89 4.9,8 6,8H7V6A5,5 0 0,1 12,1A5,5 0 0,1 17,6V8H18M12,3A3,3 0 0,0 9,6V8H15V6A3,3 0 0,0 12,3Z"/>
            </svg>
            Private
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
        <div v-if="auth.isAuthenticated.value">
          <button
            class="btn-primary"
            @click="handleJoin"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M15,14C12.33,14 7,15.33 7,18V20H23V18C23,15.33 17.67,14 15,14M6,10V7H4V10H1V12H4V15H6V12H9V10M15,12A4,4 0 0,0 19,8A4,4 0 0,0 15,4A4,4 0 0,0 11,8A4,4 0 0,0 15,12Z"/>
            </svg>
            Join Group
          </button>
        </div>

        <div v-else>
          <button class="btn-outline" @click="goToLogin">
            Sign in to join
          </button>
        </div>
      </div>

      <!-- Upcoming Games Section (Placeholder) -->
      <div class="card">
        <div class="p-4 border-b border-gray-100">
          <h2 class="font-semibold flex items-center gap-2">
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7,6H17A6,6 0 0,1 23,12A6,6 0 0,1 17,18C15.22,18 13.63,17.23 12.53,16H11.47C10.37,17.23 8.78,18 7,18A6,6 0 0,1 1,12A6,6 0 0,1 7,6M6,9V11H4V13H6V15H8V13H10V11H8V9H6M15.5,12A1.5,1.5 0 0,0 14,13.5A1.5,1.5 0 0,0 15.5,15A1.5,1.5 0 0,0 17,13.5A1.5,1.5 0 0,0 15.5,12M18.5,9A1.5,1.5 0 0,0 17,10.5A1.5,1.5 0 0,0 18.5,12A1.5,1.5 0 0,0 20,10.5A1.5,1.5 0 0,0 18.5,9Z"/>
            </svg>
            Upcoming Games
          </h2>
        </div>
        <p class="text-gray-500 text-center py-8">
          No upcoming games scheduled for this group yet.
        </p>
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
  </div>
</template>
