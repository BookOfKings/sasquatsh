<script setup lang="ts">
import { computed } from 'vue'
import type { YugiohEventConfig } from '@/types/yugioh'
import { YUGIOH_FORMATS } from '@/types/yugioh'

const props = defineProps<{
  config: YugiohEventConfig
}>()

// Get format info
const format = computed(() =>
  YUGIOH_FORMATS.find(f => f.id === props.config.formatId)
)

// Deck size based on format
const mainDeckSize = computed(() => {
  if (!format.value) return '40-60 cards'
  if (format.value.mainDeckMin === format.value.mainDeckMax) {
    return `${format.value.mainDeckMin} cards`
  }
  return `${format.value.mainDeckMin}-${format.value.mainDeckMax} cards`
})

// Check if we have any deck rules to display
const hasRules = computed(() => {
  return (
    props.config.formatId ||
    props.config.allowProxies !== undefined ||
    props.config.requireDeckRegistration ||
    props.config.houseRulesNotes ||
    !props.config.allowSideDeck
  )
})

// Format the deadline nicely
const deadlineFormatted = computed(() => {
  if (!props.config.deckSubmissionDeadline) return null
  const date = new Date(props.config.deckSubmissionDeadline)
  return date.toLocaleString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
})
</script>

<template>
  <div v-if="hasRules" class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-blue-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M6,2H18A2,2 0 0,1 20,4V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V4A2,2 0 0,1 6,2M6,4V20H18V4H6M7,6H17V8H7V6M7,10H17V12H7V10M7,14H14V16H7V14Z"/>
        </svg>
        Deck Rules
      </h3>
    </div>

    <div class="p-4 space-y-4">
      <!-- Format Rules Summary -->
      <div v-if="format" class="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <p class="text-sm font-medium text-blue-900 mb-2">
          {{ format.name }} Format Rules
        </p>
        <div class="grid grid-cols-2 gap-2 text-sm text-blue-700">
          <span>
            <span class="font-medium">Main Deck:</span> {{ mainDeckSize }}
          </span>
          <span>
            <span class="font-medium">Extra Deck:</span> Up to {{ format.extraDeckMax }}
          </span>
          <span>
            <span class="font-medium">Side Deck:</span> Up to {{ format.sideDeckMax }}
          </span>
          <span>
            <span class="font-medium">Copies:</span> {{ format.maxCopies }}x max per card
          </span>
          <span>
            <span class="font-medium">Starting LP:</span> {{ format.startingLP.toLocaleString() }}
          </span>
          <span>
            <span class="font-medium">Starting Hand:</span> {{ format.startingHand }} cards
          </span>
        </div>
      </div>

      <!-- Side Deck Policy -->
      <div v-if="!config.allowSideDeck" class="flex items-start gap-3">
        <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 bg-amber-100">
          <svg class="w-5 h-5 text-amber-600" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
        </div>
        <div>
          <p class="font-medium text-gray-900">No Side Decking</p>
          <p class="text-sm text-gray-600">
            Side decks are not allowed between games in a match
          </p>
        </div>
      </div>

      <!-- Deck Registration Required -->
      <div v-if="config.requireDeckRegistration" class="bg-indigo-50 border border-indigo-200 rounded-lg p-3">
        <div class="flex items-start gap-2">
          <svg class="w-5 h-5 text-indigo-600 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <div>
            <p class="font-medium text-indigo-900">Deck Registration Required</p>
            <p class="text-sm text-indigo-700">
              Submit your decklist when joining this event.
              <template v-if="deadlineFormatted">
                <br/>Deadline: <strong>{{ deadlineFormatted }}</strong>
              </template>
            </p>
          </div>
        </div>
      </div>

      <!-- Proxy Policy -->
      <div class="flex items-start gap-3">
        <div
          class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
          :class="config.allowProxies ? 'bg-green-100' : 'bg-red-100'"
        >
          <svg
            v-if="config.allowProxies"
            class="w-5 h-5 text-green-600"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-red-600"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
        </div>
        <div>
          <p class="font-medium text-gray-900">
            {{ config.allowProxies ? 'Proxies Allowed' : 'No Proxies' }}
          </p>
          <p v-if="config.allowProxies && config.proxyLimit" class="text-sm text-gray-600">
            Limited to {{ config.proxyLimit }} proxies per deck
          </p>
          <p v-else-if="config.allowProxies" class="text-sm text-gray-600">
            No limit on proxy count
          </p>
          <p v-else class="text-sm text-gray-600">
            All cards must be real, tournament-legal cards
          </p>
        </div>
      </div>

      <!-- House Rules Notes -->
      <div v-if="config.houseRulesNotes">
        <p class="font-medium text-gray-900 mb-2">House Rules</p>
        <div class="bg-gray-50 rounded-lg p-3 text-sm text-gray-700 whitespace-pre-wrap">
          {{ config.houseRulesNotes }}
        </div>
      </div>
    </div>
  </div>
</template>
