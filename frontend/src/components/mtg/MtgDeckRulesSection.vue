<script setup lang="ts">
import { computed, ref } from 'vue'
import CardSearch from './CardSearch.vue'
import type { ScryfallCard } from '@/types/mtg'

const cardSearchQuery = ref('')

const props = defineProps<{
  allowProxies: boolean
  proxyLimit: number | null
  bannedCards: string[]
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:allowProxies', value: boolean): void
  (e: 'update:proxyLimit', value: number | null): void
  (e: 'update:bannedCards', value: string[]): void
  (e: 'update:requireDeckRegistration', value: boolean): void
  (e: 'update:deckSubmissionDeadline', value: string | null): void
}>()

const showBannedCardsInput = ref(false)

const hasProxyLimit = computed(() => props.allowProxies && props.proxyLimit !== null)

function handleCardSelect(card: ScryfallCard) {
  if (!props.bannedCards.includes(card.name)) {
    emit('update:bannedCards', [...props.bannedCards, card.name])
  }
  cardSearchQuery.value = ''
  showBannedCardsInput.value = false
}

function removeBannedCard(cardName: string) {
  emit('update:bannedCards', props.bannedCards.filter(c => c !== cardName))
}
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Deck Rules</h3>

    <!-- Proxy Policy -->
    <div class="space-y-2">
      <div class="flex items-center gap-3">
        <input
          type="checkbox"
          id="allowProxies"
          :checked="allowProxies"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="$emit('update:allowProxies', ($event.target as HTMLInputElement).checked)"
        />
        <label for="allowProxies" class="text-sm font-medium text-gray-700">
          Allow Proxies
        </label>
      </div>

      <div v-if="allowProxies" class="ml-7 space-y-2">
        <div class="flex items-center gap-3">
          <input
            type="checkbox"
            id="hasProxyLimit"
            :checked="hasProxyLimit"
            :disabled="disabled"
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            @change="$emit('update:proxyLimit', ($event.target as HTMLInputElement).checked ? 10 : null)"
          />
          <label for="hasProxyLimit" class="text-sm text-gray-600">
            Limit number of proxies
          </label>
        </div>

        <div v-if="hasProxyLimit" class="flex items-center gap-2">
          <label class="text-sm text-gray-600">Maximum proxies per deck:</label>
          <input
            type="number"
            :value="proxyLimit"
            :disabled="disabled"
            class="input w-20"
            min="1"
            max="100"
            @input="$emit('update:proxyLimit', parseInt(($event.target as HTMLInputElement).value) || null)"
          />
        </div>
      </div>

      <p class="text-xs text-gray-500 ml-7">
        Proxies are placeholder cards used to represent cards players don't own.
      </p>
    </div>

    <!-- Banned Cards -->
    <div class="space-y-2">
      <label class="block text-sm font-medium text-gray-700">
        Additional Banned Cards
      </label>
      <p class="text-xs text-gray-500">
        Cards banned in addition to format-specific bans.
      </p>

      <!-- Current banned cards -->
      <div v-if="bannedCards.length > 0" class="flex flex-wrap gap-2">
        <span
          v-for="card in bannedCards"
          :key="card"
          class="inline-flex items-center gap-1 px-2 py-1 bg-red-50 text-red-700 rounded-full text-sm"
        >
          {{ card }}
          <button
            type="button"
            class="text-red-500 hover:text-red-700"
            :disabled="disabled"
            @click="removeBannedCard(card)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </span>
      </div>

      <!-- Add banned card -->
      <div v-if="showBannedCardsInput" class="max-w-xs">
        <CardSearch
          v-model="cardSearchQuery"
          :disabled="disabled"
          placeholder="Search for a card..."
          @select="handleCardSelect"
        />
        <button
          type="button"
          class="text-sm text-gray-500 mt-1"
          @click="showBannedCardsInput = false"
        >
          Cancel
        </button>
      </div>
      <button
        v-else
        type="button"
        class="text-sm text-blue-600 hover:text-blue-700"
        :disabled="disabled"
        @click="showBannedCardsInput = true"
      >
        + Add banned card
      </button>
    </div>

    <!-- Deck Registration -->
    <div class="space-y-2">
      <div class="flex items-center gap-3">
        <input
          type="checkbox"
          id="requireDeckRegistration"
          :checked="requireDeckRegistration"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="$emit('update:requireDeckRegistration', ($event.target as HTMLInputElement).checked)"
        />
        <label for="requireDeckRegistration" class="text-sm font-medium text-gray-700">
          Require Deck Registration
        </label>
      </div>

      <div v-if="requireDeckRegistration" class="ml-7 space-y-2">
        <label class="block text-sm text-gray-600">Submission Deadline</label>
        <input
          type="datetime-local"
          :value="deckSubmissionDeadline?.slice(0, 16) ?? ''"
          :disabled="disabled"
          class="input"
          @input="$emit('update:deckSubmissionDeadline', ($event.target as HTMLInputElement).value ? ($event.target as HTMLInputElement).value + ':00Z' : null)"
        />
        <p class="text-xs text-gray-500">
          Players must submit their decklist before this deadline.
        </p>
      </div>
    </div>
  </div>
</template>
