<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicProfile } from '@/services/profileApi'
import { getUserCollection } from '@/services/collectionsApi'
import type { PublicProfile } from '@/types/profile'

const props = withDefaults(defineProps<{
  avatarUrl?: string | null
  username?: string
  displayName?: string | null
  isFoundingMember?: boolean
  isAdmin?: boolean
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  showBadge?: boolean
  userId?: string | null
}>(), {
  showBadge: true,
  userId: null,
})

// Computed to determine if founding member badge should show
const shouldShowFoundingBadge = computed(() => {
  return props.isFoundingMember === true && props.showBadge === true
})

// Computed to determine if admin badge should show
const shouldShowAdminBadge = computed(() => {
  return props.isAdmin === true && props.showBadge === true
})

// Ring class based on status (admin takes priority over founding)
const ringClass = computed(() => {
  if (props.isAdmin && props.showBadge) return 'ring-2 ring-red-500'
  if (props.isFoundingMember && props.showBadge) return 'ring-2 ring-amber-400'
  return ''
})

// Popover state
const router = useRouter()
const showPopover = ref(false)
const popoverProfile = ref<PublicProfile | null>(null)
const popoverGameCount = ref<number | null>(null)
const popoverLoading = ref(false)
const popoverEl = ref<HTMLElement | null>(null)

async function handleAvatarClick() {
  if (!props.userId) return
  if (showPopover.value) {
    showPopover.value = false
    return
  }
  showPopover.value = true
  if (popoverProfile.value) return // already loaded

  popoverLoading.value = true
  try {
    const profile = await getPublicProfile(props.userId)
    popoverProfile.value = profile

    // Try to get collection count (will fail silently if private)
    if ((profile as any).collectionVisibility === 'public') {
      try {
        const games = await getUserCollection(props.userId)
        popoverGameCount.value = games.length
      } catch { /* private or error */ }
    }
  } catch {
    // profile fetch failed
  } finally {
    popoverLoading.value = false
  }
}

function goToCollection() {
  if (props.userId) {
    router.push({ name: 'user-collection', params: { userId: props.userId } })
    showPopover.value = false
  }
}

// Popover position
const popoverStyle = computed(() => {
  if (!popoverEl.value) return { top: '0px', left: '0px' }
  const rect = popoverEl.value.getBoundingClientRect()
  const top = rect.bottom + 8
  const left = Math.min(Math.max(8, rect.left - 80), document.documentElement.clientWidth - 240)
  return { top: top + 'px', left: left + 'px' }
})

const sizeClasses = {
  xs: 'w-6 h-6',
  sm: 'w-8 h-8',
  md: 'w-10 h-10',
  lg: 'w-12 h-12',
  xl: 'w-20 h-20',
}

const iconSizeClasses = {
  xs: 'w-3 h-3',
  sm: 'w-4 h-4',
  md: 'w-5 h-5',
  lg: 'w-6 h-6',
  xl: 'w-10 h-10',
}

</script>

<template>
  <div class="relative inline-block" ref="popoverEl">
    <div
      :class="[
        sizeClasses[props.size || 'md'],
        'rounded-full bg-primary-500 flex items-center justify-center overflow-hidden',
        ringClass,
        userId ? 'cursor-pointer' : ''
      ]"
      @click.stop="handleAvatarClick"
    >
      <img
        v-if="props.avatarUrl"
        :src="props.avatarUrl"
        :alt="props.displayName || props.username || 'User avatar'"
        class="w-full h-full object-cover"
      />
      <svg
        v-else
        :class="[iconSizeClasses[props.size || 'md'], 'text-white']"
        viewBox="0 0 24 24"
        fill="currentColor"
      >
        <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
      </svg>
    </div>
    <!-- Admin Badge (Sasquatch Foot) - takes priority -->
    <div
      v-if="shouldShowAdminBadge"
      style="position: absolute; bottom: -4px; right: -4px; width: 16px; height: 16px; z-index: 10;"
      title="Admin"
    >
      <svg viewBox="0 0 24 24" fill="none" class="w-full h-full drop-shadow-sm">
        <!-- Sasquatch Footprint -->
        <ellipse cx="12" cy="14" rx="6" ry="8" fill="#DC2626" stroke="#991B1B" stroke-width="1"/>
        <ellipse cx="12" cy="14" rx="4.5" ry="6" fill="#EF4444"/>
        <!-- Toes -->
        <circle cx="7" cy="5" r="2.5" fill="#DC2626" stroke="#991B1B" stroke-width="0.5"/>
        <circle cx="10.5" cy="3.5" r="2.5" fill="#DC2626" stroke="#991B1B" stroke-width="0.5"/>
        <circle cx="14.5" cy="3.5" r="2.5" fill="#DC2626" stroke="#991B1B" stroke-width="0.5"/>
        <circle cx="18" cy="5" r="2.5" fill="#DC2626" stroke="#991B1B" stroke-width="0.5"/>
        <!-- Toe highlights -->
        <circle cx="7" cy="4.5" r="1.5" fill="#EF4444"/>
        <circle cx="10.5" cy="3" r="1.5" fill="#EF4444"/>
        <circle cx="14.5" cy="3" r="1.5" fill="#EF4444"/>
        <circle cx="18" cy="4.5" r="1.5" fill="#EF4444"/>
      </svg>
    </div>
    <!-- Founding Member Badge (Star) -->
    <div
      v-else-if="shouldShowFoundingBadge"
      style="position: absolute; bottom: -4px; right: -4px; width: 16px; height: 16px; z-index: 10;"
      title="Founding Member"
    >
      <svg viewBox="0 0 24 24" fill="none" class="w-full h-full drop-shadow-sm">
        <path
          d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
          fill="#F59E0B"
          stroke="#D97706"
          stroke-width="1"
        />
        <path
          d="M12 5L14.09 9.26L18.5 9.87L15.25 13.02L16.02 17.41L12 15.27L7.98 17.41L8.75 13.02L5.5 9.87L9.91 9.26L12 5Z"
          fill="#FBBF24"
        />
      </svg>
    </div>

    <!-- User Popover -->
    <Teleport to="body">
      <div v-if="showPopover && userId" class="fixed inset-0 z-[9998]" @click="showPopover = false"></div>
      <div
        v-if="showPopover && userId"
        class="fixed z-[9999] w-56 bg-white rounded-lg shadow-xl border border-gray-200 p-3"
        :style="popoverStyle"
      >
        <!-- Loading -->
        <div v-if="popoverLoading" class="flex justify-center py-4">
          <svg class="animate-spin h-5 w-5 text-primary-500" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>

        <template v-else-if="popoverProfile">
          <!-- User info -->
          <div class="flex items-center gap-2 mb-2">
            <div
              :class="[
                'w-10 h-10 rounded-full bg-primary-500 flex items-center justify-center overflow-hidden flex-shrink-0',
                ringClass
              ]"
            >
              <img
                v-if="popoverProfile.avatarUrl"
                :src="popoverProfile.avatarUrl"
                :alt="popoverProfile.displayName || popoverProfile.username"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-5 h-5 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
              </svg>
            </div>
            <div class="min-w-0">
              <div class="font-medium text-sm truncate">{{ popoverProfile.displayName || popoverProfile.username }}</div>
              <div class="text-xs text-gray-500 truncate">@{{ popoverProfile.username }}</div>
            </div>
          </div>

          <!-- Location -->
          <div v-if="popoverProfile.homeCity || popoverProfile.homeState" class="text-xs text-gray-500 mb-2 flex items-center gap-1">
            <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
            </svg>
            {{ [popoverProfile.homeCity, popoverProfile.homeState].filter(Boolean).join(', ') }}
          </div>

          <!-- Bio -->
          <p v-if="popoverProfile.bio" class="text-xs text-gray-600 mb-2 line-clamp-2">{{ popoverProfile.bio }}</p>

          <!-- Collection link -->
          <button
            v-if="(popoverProfile as any).collectionVisibility === 'public'"
            @click="goToCollection"
            class="w-full flex items-center justify-between px-2 py-1.5 text-xs bg-gray-50 hover:bg-gray-100 rounded transition-colors"
          >
            <span class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
              </svg>
              Game Collection
            </span>
            <span v-if="popoverGameCount !== null" class="px-1.5 py-0.5 bg-primary-100 text-primary-700 rounded-full text-xs font-medium">
              {{ popoverGameCount }}
            </span>
          </button>
        </template>
      </div>
    </Teleport>
  </div>
</template>
