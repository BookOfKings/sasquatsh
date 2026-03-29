<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventConfig } from '@/types/mtg'

const props = defineProps<{
  config: MtgEventConfig
  formatName?: string | null
}>()

// Is this a limited format where deck is built on-site?
const isLimited = computed(() => {
  const limitedFormats = ['draft', 'sealed', 'cube', 'cube_draft']
  return props.config.formatId && limitedFormats.includes(props.config.formatId)
})

// Is Commander format?
const isCommander = computed(() => {
  const commanderFormats = ['commander', 'oathbreaker', 'brawl']
  return props.config.formatId && commanderFormats.includes(props.config.formatId)
})

// Does player need to bring a deck?
const needsDeck = computed(() => !isLimited.value)

// Items to bring based on format
const itemsToBring = computed(() => {
  const items: { icon: string; text: string; description?: string }[] = []

  if (isLimited.value) {
    // Limited formats - deck built on-site
    items.push({
      icon: 'check',
      text: 'Nothing required',
      description: 'Your deck will be built at the event',
    })
    items.push({
      icon: 'land',
      text: 'Basic lands',
      description: 'Usually provided, but bring your own to be safe',
    })
  } else if (isCommander.value) {
    // Commander formats
    items.push({
      icon: 'deck',
      text: 'Your Commander deck',
      description: props.config.powerLevelRange
        ? `Within ${props.config.powerLevelRange} power level`
        : undefined,
    })
    items.push({
      icon: 'dice',
      text: 'Dice & counters',
      description: 'D20s, +1/+1 counters, tokens',
    })
    items.push({
      icon: 'life',
      text: 'Life tracking',
      description: 'App, spindown, or pen & paper',
    })
  } else {
    // Constructed formats (Standard, Modern, etc.)
    items.push({
      icon: 'deck',
      text: 'Your constructed deck',
      description: props.config.requireDeckRegistration
        ? 'Deck registration required'
        : undefined,
    })
    if (props.config.matchStyle === 'bo3') {
      items.push({
        icon: 'sideboard',
        text: '15-card sideboard',
        description: 'For Best-of-3 matches',
      })
    }
    items.push({
      icon: 'life',
      text: 'Life tracking',
      description: 'App, spindown, or pen & paper',
    })
  }

  // Entry fee reminder
  if (props.config.entryFee && props.config.entryFee > 0) {
    items.push({
      icon: 'money',
      text: `Entry fee: $${props.config.entryFee.toFixed(2)}`,
      description: 'Payment at the event',
    })
  }

  return items
})
</script>

<template>
  <div class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,19H5V5H19M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M7,9H9V7H7V9M7,13H9V11H7V13M7,17H9V15H7V17M11,17H17V15H11V17M11,13H17V11H11V13M11,9H17V7H11V9Z"/>
        </svg>
        What to Bring
      </h3>
    </div>

    <div class="p-4 space-y-4">
      <!-- Deck Required Indicator -->
      <div
        class="flex items-center gap-3 p-3 rounded-lg"
        :class="needsDeck ? 'bg-amber-50 border border-amber-200' : 'bg-green-50 border border-green-200'"
      >
        <div
          class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
          :class="needsDeck ? 'bg-amber-100' : 'bg-green-100'"
        >
          <svg
            v-if="needsDeck"
            class="w-5 h-5 text-amber-600"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M6,2H18A2,2 0 0,1 20,4V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V4A2,2 0 0,1 6,2M6,4V20H18V4H6M13,7V9H11V7H13M13,11V17H11V11H13Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-green-600"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
        </div>
        <div>
          <p class="font-medium" :class="needsDeck ? 'text-amber-900' : 'text-green-900'">
            {{ needsDeck ? 'Bring Your Deck' : 'No Deck Required' }}
          </p>
          <p class="text-sm" :class="needsDeck ? 'text-amber-700' : 'text-green-700'">
            {{ needsDeck
              ? `Bring a ${formatName || 'format'}-legal deck to play`
              : 'Deck will be built at the event from packs' }}
          </p>
        </div>
      </div>

      <!-- Items List -->
      <div class="space-y-3">
        <div
          v-for="(item, index) in itemsToBring"
          :key="index"
          class="flex items-start gap-3"
        >
          <!-- Icons -->
          <div class="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0">
            <!-- Check icon -->
            <svg v-if="item.icon === 'check'" class="w-4 h-4 text-green-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
            </svg>
            <!-- Deck icon -->
            <svg v-else-if="item.icon === 'deck'" class="w-4 h-4 text-purple-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M6,2H18A2,2 0 0,1 20,4V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V4A2,2 0 0,1 6,2M6,4V20H18V4H6M13,7V9H11V7H13M13,11V17H11V11H13Z"/>
            </svg>
            <!-- Sideboard icon -->
            <svg v-else-if="item.icon === 'sideboard'" class="w-4 h-4 text-indigo-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M4,6H2V20A2,2 0 0,0 4,22H18V20H4V6M20,2H8A2,2 0 0,0 6,4V16A2,2 0 0,0 8,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H8V4H20V16M13,15V13H15V11H13V9H11V11H9V13H11V15H13Z"/>
            </svg>
            <!-- Dice icon -->
            <svg v-else-if="item.icon === 'dice'" class="w-4 h-4 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15Z"/>
            </svg>
            <!-- Life icon -->
            <svg v-else-if="item.icon === 'life'" class="w-4 h-4 text-red-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"/>
            </svg>
            <!-- Land icon -->
            <svg v-else-if="item.icon === 'land'" class="w-4 h-4 text-green-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M14,6L10.25,11L13.1,14.8L11.5,16C9.81,13.75 7,10 7,10L1,18H23L14,6Z"/>
            </svg>
            <!-- Money icon -->
            <svg v-else-if="item.icon === 'money'" class="w-4 h-4 text-green-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M7,15H9C9,16.08 10.37,17 12,17C13.63,17 15,16.08 15,15C15,13.9 13.96,13.5 11.76,12.97C9.64,12.44 7,11.78 7,9C7,7.21 8.47,5.69 10.5,5.18V3H13.5V5.18C15.53,5.69 17,7.21 17,9H15C15,7.92 13.63,7 12,7C10.37,7 9,7.92 9,9C9,10.1 10.04,10.5 12.24,11.03C14.36,11.56 17,12.22 17,15C17,16.79 15.53,18.31 13.5,18.82V21H10.5V18.82C8.47,18.31 7,16.79 7,15Z"/>
            </svg>
          </div>
          <div>
            <p class="font-medium text-gray-900">{{ item.text }}</p>
            <p v-if="item.description" class="text-sm text-gray-600">{{ item.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
