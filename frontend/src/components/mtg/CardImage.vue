<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ScryfallCard } from '@/types/mtg'
import { getCardImageUrl, getDoubleFacedCardImages } from '@/services/scryfallApi'

const props = defineProps<{
  card: ScryfallCard
  size?: 'small' | 'normal' | 'large'
  showHover?: boolean
  showFlip?: boolean  // Allow flipping double-faced cards
}>()

const showingBack = ref(false)
const isHovering = ref(false)

const imageUrl = computed(() => {
  const size = props.size || 'normal'

  if (props.card.isDoubleFaced && showingBack.value) {
    const images = getDoubleFacedCardImages(props.card)
    return images.back
  }

  return getCardImageUrl(props.card, size)
})

const hoverImageUrl = computed(() => {
  // Show large image on hover
  if (props.card.isDoubleFaced && showingBack.value) {
    const images = getDoubleFacedCardImages(props.card)
    // Get large version of back
    if (props.card.cardFaces?.[1]?.imageUris?.large) {
      return props.card.cardFaces[1].imageUris.large
    }
    return images.back
  }

  return getCardImageUrl(props.card, 'large')
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'small': return 'w-16 h-22'
    case 'large': return 'w-56 h-78'
    default: return 'w-40 h-56'
  }
})

function flipCard() {
  if (props.card.isDoubleFaced && props.showFlip) {
    showingBack.value = !showingBack.value
  }
}
</script>

<template>
  <div class="relative inline-block">
    <!-- Main card image -->
    <div
      class="relative cursor-pointer"
      :class="[sizeClasses, { 'hover:ring-2 hover:ring-primary-500': showHover }]"
      @mouseenter="isHovering = true"
      @mouseleave="isHovering = false"
      @click="flipCard"
    >
      <img
        v-if="imageUrl"
        :src="imageUrl"
        :alt="card.name"
        class="w-full h-full object-cover rounded-lg shadow-md"
      />
      <div v-else class="w-full h-full bg-gradient-to-br from-purple-800 to-blue-900 rounded-lg flex items-center justify-center">
        <span class="text-white text-lg font-bold">{{ card.name.charAt(0) }}</span>
      </div>

      <!-- Flip indicator for double-faced cards -->
      <div
        v-if="card.isDoubleFaced && showFlip"
        class="absolute bottom-1 right-1 bg-black/60 rounded-full p-1 text-white"
      >
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,18A6,6 0 0,1 6,12C6,11 6.25,10.03 6.7,9.2L5.24,7.74C4.46,8.97 4,10.43 4,12A8,8 0 0,0 12,20V23L16,19L12,15M12,4V1L8,5L12,9V6A6,6 0 0,1 18,12C18,13 17.75,13.97 17.3,14.8L18.76,16.26C19.54,15.03 20,13.57 20,12A8,8 0 0,0 12,4Z"/>
        </svg>
      </div>
    </div>

    <!-- Hover preview (large card) -->
    <div
      v-if="showHover && isHovering && hoverImageUrl"
      class="fixed z-[100] pointer-events-none"
      :style="{
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
      }"
    >
      <img
        :src="hoverImageUrl"
        :alt="card.name"
        class="w-80 h-auto rounded-xl shadow-2xl border-2 border-white"
      />
    </div>
  </div>
</template>
