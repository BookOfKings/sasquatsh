<script setup lang="ts">
import { computed } from 'vue'
import type { MtgEventConfig } from '@/types/mtg'

const props = defineProps<{
  config: MtgEventConfig
  formatName?: string | null
}>()

// Format rules based on format ID
const formatRules = computed(() => {
  if (!props.config.formatId) return null

  const rules: Record<string, { deckSize: string; copies: string; extras?: string }> = {
    commander: { deckSize: '100 cards', copies: 'Singleton', extras: 'Commander required' },
    standard: { deckSize: '60+ cards', copies: '4x max', extras: '15-card sideboard' },
    modern: { deckSize: '60+ cards', copies: '4x max', extras: '15-card sideboard' },
    pioneer: { deckSize: '60+ cards', copies: '4x max', extras: '15-card sideboard' },
    legacy: { deckSize: '60+ cards', copies: '4x max', extras: '15-card sideboard' },
    vintage: { deckSize: '60+ cards', copies: '4x max', extras: '15-card sideboard' },
    pauper: { deckSize: '60+ cards', copies: '4x max', extras: 'Commons only' },
    oathbreaker: { deckSize: '60 cards', copies: 'Singleton', extras: 'Planeswalker commander + signature spell' },
    brawl: { deckSize: '60 cards', copies: 'Singleton', extras: 'Standard-legal commander' },
    draft: { deckSize: '40+ cards', copies: 'No limit', extras: 'Built on-site' },
    sealed: { deckSize: '40+ cards', copies: 'No limit', extras: 'Built from sealed pool' },
    cube: { deckSize: '40+ cards', copies: 'No limit', extras: 'Built from cube draft' },
    cube_draft: { deckSize: '40+ cards', copies: 'No limit', extras: 'Built from cube draft' },
    casual: { deckSize: 'Flexible', copies: 'House rules', extras: undefined },
    custom: { deckSize: 'Varies', copies: 'See house rules', extras: undefined },
  }

  return rules[props.config.formatId] || null
})

// Check if we have any deck rules to display (always show if we have format rules)
const hasRules = computed(() => {
  return (
    formatRules.value ||
    props.config.allowProxies !== undefined ||
    props.config.bannedCards.length > 0 ||
    props.config.houseRulesNotes ||
    props.config.requireDeckRegistration
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
        <svg class="w-5 h-5 text-purple-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M6,2H18A2,2 0 0,1 20,4V20A2,2 0 0,1 18,22H6A2,2 0 0,1 4,20V4A2,2 0 0,1 6,2M6,4V20H18V4H6M7,6H17V8H7V6M7,10H17V12H7V10M7,14H14V16H7V14Z"/>
        </svg>
        Deck Rules
      </h3>
    </div>

    <div class="p-4 space-y-4">
      <!-- Format Rules Summary -->
      <div v-if="formatRules" class="bg-purple-50 border border-purple-200 rounded-lg p-3">
        <p class="text-sm font-medium text-purple-900 mb-2">
          {{ formatName || 'Format' }} Rules
        </p>
        <div class="flex flex-wrap gap-3 text-sm">
          <span class="text-purple-700">
            <span class="font-medium">Deck:</span> {{ formatRules.deckSize }}
          </span>
          <span class="text-purple-700">
            <span class="font-medium">Copies:</span> {{ formatRules.copies }}
          </span>
          <span v-if="formatRules.extras" class="text-purple-700">
            {{ formatRules.extras }}
          </span>
        </div>
      </div>

      <!-- Deck Registration Required -->
      <div v-if="config.requireDeckRegistration" class="bg-blue-50 border border-blue-200 rounded-lg p-3">
        <div class="flex items-start gap-2">
          <svg class="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <div>
            <p class="font-medium text-blue-900">Deck Registration Required</p>
            <p class="text-sm text-blue-700">
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
            {{ config.allowProxies ? 'Proxies Allowed' : 'Proxies Not Allowed' }}
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

      <!-- Banned Cards -->
      <div v-if="config.bannedCards.length > 0">
        <p class="font-medium text-gray-900 mb-2">
          Additional Banned Cards
          <span class="text-sm font-normal text-gray-500">(beyond {{ formatName || 'format' }} banlist)</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <span
            v-for="card in config.bannedCards"
            :key="card"
            class="inline-flex items-center px-2.5 py-1 rounded-full text-sm bg-red-50 text-red-700 border border-red-200"
          >
            <svg class="w-3.5 h-3.5 mr-1" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2C17.53,2 22,6.47 22,12C22,17.53 17.53,22 12,22C6.47,22 2,17.53 2,12C2,6.47 6.47,2 12,2M15.59,7L12,10.59L8.41,7L7,8.41L10.59,12L7,15.59L8.41,17L12,13.41L15.59,17L17,15.59L13.41,12L17,8.41L15.59,7Z"/>
            </svg>
            {{ card }}
          </span>
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
