<script setup lang="ts">
import { useVersionCheck } from '@/composables/useVersionCheck'

const { updateAvailable, refresh, dismiss } = useVersionCheck()
</script>

<template>
  <Transition name="slide-down">
    <div
      v-if="updateAvailable"
      class="fixed top-0 left-0 right-0 z-[100] bg-primary-600 text-white px-4 py-3 shadow-lg"
    >
      <div class="container-wide flex items-center justify-between gap-4">
        <div class="flex items-center gap-3">
          <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,10.12H14.22L16.96,7.3C14.23,4.6 9.81,4.5 7.08,7.2C4.35,9.91 4.35,14.28 7.08,17C9.81,19.7 14.23,19.7 16.96,17C18.32,15.65 19,14.08 19,12.1H21C21,14.08 20.12,16.65 18.36,18.39C14.85,21.87 9.15,21.87 5.64,18.39C2.14,14.92 2.11,9.28 5.62,5.81C9.13,2.34 14.76,2.34 18.27,5.81L21,3V10.12M12.5,8V12.25L16,14.33L15.28,15.54L11,13V8H12.5Z"/>
          </svg>
          <span class="text-sm font-medium">
            A new version is available!
          </span>
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="dismiss"
            class="text-sm text-white/80 hover:text-white px-2 py-1"
          >
            Later
          </button>
          <button
            @click="refresh"
            class="bg-white text-primary-600 text-sm font-medium px-4 py-1.5 rounded-lg hover:bg-primary-50 transition-colors"
          >
            Refresh Now
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.slide-down-enter-active,
.slide-down-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
