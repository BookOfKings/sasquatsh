<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventConfig } from '@/types/mtg'

const props = defineProps<{
  config: MtgEventConfig
}>()

// Check if this is a limited format
const isLimited = computed(() => {
  const limitedFormats = ['draft', 'sealed', 'cube', 'cube_draft']
  return props.config.formatId && limitedFormats.includes(props.config.formatId)
})

// Get draft style display name
const draftStyleDisplay = computed(() => {
  if (!props.config.draftStyle) return null
  const styles: Record<string, { name: string; description: string }> = {
    standard: { name: 'Standard Draft', description: 'Pick one card, pass the rest' },
    rochester: { name: 'Rochester Draft', description: 'All cards visible, pick in order' },
    winston: { name: 'Winston Draft', description: '2-player draft with face-down piles' },
    grid: { name: 'Grid Draft', description: '2-player draft from a 3x3 grid' },
  }
  return styles[props.config.draftStyle] || null
})

// Check if this is sealed format
const isSealed = computed(() => props.config.formatId === 'sealed')

// Check if this is cube
const isCube = computed(() => props.config.formatId === 'cube' || props.config.formatId === 'cube_draft')
</script>

<template>
  <div v-if="isLimited" class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,3H14.82C14.4,1.84 13.3,1 12,1C10.7,1 9.6,1.84 9.18,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M12,3A1,1 0 0,1 13,4A1,1 0 0,1 12,5A1,1 0 0,1 11,4A1,1 0 0,1 12,3M7,7H17V5H19V19H5V5H7V7M12,9A2,2 0 0,0 10,11A2,2 0 0,0 12,13A2,2 0 0,0 14,11A2,2 0 0,0 12,9M17,17H7V15.58C7,14.23 9.78,13.5 12,13.5C14.22,13.5 17,14.23 17,15.58V17Z"/>
        </svg>
        Limited Format Details
      </h3>
    </div>

    <div class="p-4 space-y-4">
      <!-- Packs Per Player -->
      <div v-if="config.packsPerPlayer" class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-purple-100 flex items-center justify-center">
          <span class="text-lg font-bold text-purple-600">{{ config.packsPerPlayer }}</span>
        </div>
        <div>
          <p class="font-medium text-gray-900">Packs Per Player</p>
          <p class="text-sm text-gray-600">
            <template v-if="config.packsPerPlayer === 3">Standard draft pool</template>
            <template v-else-if="config.packsPerPlayer === 6">Standard sealed pool</template>
            <template v-else>{{ config.packsPerPlayer }} packs to build from</template>
          </p>
        </div>
      </div>

      <!-- Draft Style -->
      <div v-if="draftStyleDisplay" class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-indigo-100 flex items-center justify-center">
          <svg class="w-5 h-5 text-indigo-600" viewBox="0 0 24 24" fill="currentColor">
            <path d="M4,4H7L9,2H15L17,4H20A2,2 0 0,1 22,6V18A2,2 0 0,1 20,20H4A2,2 0 0,1 2,18V6A2,2 0 0,1 4,4M12,7A5,5 0 0,0 7,12A5,5 0 0,0 12,17A5,5 0 0,0 17,12A5,5 0 0,0 12,7M12,9A3,3 0 0,1 15,12A3,3 0 0,1 12,15A3,3 0 0,1 9,12A3,3 0 0,1 12,9Z"/>
          </svg>
        </div>
        <div>
          <p class="font-medium text-gray-900">{{ draftStyleDisplay.name }}</p>
          <p class="text-sm text-gray-600">{{ draftStyleDisplay.description }}</p>
        </div>
      </div>

      <!-- Cube Link -->
      <div v-if="isCube && config.cubeId" class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-teal-100 flex items-center justify-center">
          <svg class="w-5 h-5 text-teal-600" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,16.5C21,16.88 20.79,17.21 20.47,17.38L12.57,21.82C12.41,21.94 12.21,22 12,22C11.79,22 11.59,21.94 11.43,21.82L3.53,17.38C3.21,17.21 3,16.88 3,16.5V7.5C3,7.12 3.21,6.79 3.53,6.62L11.43,2.18C11.59,2.06 11.79,2 12,2C12.21,2 12.41,2.06 12.57,2.18L20.47,6.62C20.79,6.79 21,7.12 21,7.5V16.5M12,4.15L5,8.09V15.91L12,19.85L19,15.91V8.09L12,4.15Z"/>
          </svg>
        </div>
        <div>
          <p class="font-medium text-gray-900">Cube</p>
          <a
            :href="config.cubeId.startsWith('http') ? config.cubeId : `https://cubecobra.com/cube/overview/${config.cubeId}`"
            target="_blank"
            rel="noopener noreferrer"
            class="text-sm text-blue-600 hover:underline"
          >
            {{ config.cubeId.startsWith('http') ? 'View Cube' : config.cubeId }}
            <svg class="w-3 h-3 inline ml-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M14,3V5H17.59L7.76,14.83L9.17,16.24L19,6.41V10H21V3M19,19H5V5H12V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V12H19V19Z"/>
            </svg>
          </a>
        </div>
      </div>

      <!-- Helper Info -->
      <div class="bg-purple-50 border border-purple-200 rounded-lg p-3">
        <p class="text-sm text-purple-800">
          <svg class="w-4 h-4 inline mr-1 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <template v-if="isSealed">
            <strong>Sealed Deck:</strong> Build your deck at the event from the packs provided. Bring basic lands or ask if the host provides them.
          </template>
          <template v-else-if="isCube">
            <strong>Cube Draft:</strong> Draft from a curated card pool. Decks are built on-site.
          </template>
          <template v-else>
            <strong>Booster Draft:</strong> Draft cards from packs passed around the table, then build your deck. Bring basic lands or ask if the host provides them.
          </template>
        </p>
      </div>
    </div>
  </div>
</template>
