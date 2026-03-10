<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  avatarUrl?: string | null
  username?: string
  displayName?: string | null
  isFoundingMember?: boolean
  isAdmin?: boolean
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  showBadge?: boolean
}>(), {
  showBadge: true
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
  <div class="relative inline-block">
    <div
      :class="[
        sizeClasses[props.size || 'md'],
        'rounded-full bg-primary-500 flex items-center justify-center overflow-hidden',
        ringClass
      ]"
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
  </div>
</template>
