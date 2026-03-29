<script setup lang="ts">
import { computed, ref } from 'vue'
import CardSearch from './CardSearch.vue'
import type { ScryfallCard, MtgFormat } from '@/types/mtg'
import { FORMAT_DESCRIPTIONS } from '@/types/mtg'

const cardSearchQuery = ref('')

const props = defineProps<{
  // Format context
  selectedFormat: MtgFormat | null
  formatId: string | null
  // Existing props
  allowProxies: boolean
  proxyLimit: number | null
  bannedCards: string[]
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  houseRulesNotes: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:allowProxies', value: boolean): void
  (e: 'update:proxyLimit', value: number | null): void
  (e: 'update:bannedCards', value: string[]): void
  (e: 'update:requireDeckRegistration', value: boolean): void
  (e: 'update:deckSubmissionDeadline', value: string | null): void
  (e: 'update:houseRulesNotes', value: string | null): void
}>()

const showBannedCardsInput = ref(false)

const hasProxyLimit = computed(() => props.allowProxies && props.proxyLimit !== null)

// Format rules context
const formatRulesDescription = computed(() => {
  if (!props.formatId) return null

  // Get base description
  const description = FORMAT_DESCRIPTIONS[props.formatId] || props.selectedFormat?.description
  if (!description) return null

  // Build additional rules info
  const rules: string[] = []

  if (props.selectedFormat) {
    if (props.selectedFormat.minDeckSize) {
      rules.push(`${props.selectedFormat.minDeckSize}${props.selectedFormat.maxDeckSize ? '' : '+'} cards`)
    }
    if (props.selectedFormat.maxCopies === 1) {
      rules.push('singleton')
    } else if (props.selectedFormat.maxCopies) {
      rules.push(`max ${props.selectedFormat.maxCopies} copies`)
    }
    if (props.selectedFormat.hasCommander) {
      rules.push('commander required')
    }
    if (props.selectedFormat.hasSideboard && props.selectedFormat.sideboardSize > 0) {
      rules.push(`${props.selectedFormat.sideboardSize}-card sideboard`)
    }
  }

  return {
    description,
    rules: rules.length > 0 ? rules.join(' • ') : null
  }
})

const formatName = computed(() => {
  if (!props.formatId) return null
  return props.selectedFormat?.name || props.formatId.charAt(0).toUpperCase() + props.formatId.slice(1)
})

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

    <!-- Format Rules Context -->
    <div v-if="formatRulesDescription" class="bg-blue-50 border border-blue-100 rounded-lg p-3">
      <div class="flex items-start gap-2">
        <svg class="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div>
          <p class="text-sm font-medium text-blue-900">
            Format Rules: {{ formatName }}
          </p>
          <p class="text-sm text-blue-700 mt-0.5">
            {{ formatRulesDescription.description }}
          </p>
          <p v-if="formatRulesDescription.rules" class="text-xs text-blue-600 mt-1">
            {{ formatRulesDescription.rules }}
          </p>
        </div>
      </div>
    </div>

    <!-- House Rules Section -->
    <div class="space-y-3">
      <h4 class="text-sm font-medium text-gray-900">House Rules</h4>

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
          Cards banned in addition to the {{ formatName || 'format' }} banlist.
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

      <!-- House Rules Notes -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-700">
          House Rules Notes
        </label>
        <textarea
          :value="houseRulesNotes ?? ''"
          :disabled="disabled"
          class="input w-full h-20"
          placeholder="Any additional rules or expectations for this event..."
          @input="$emit('update:houseRulesNotes', ($event.target as HTMLTextAreaElement).value || null)"
        ></textarea>
        <p class="text-xs text-gray-500">
          Optional. Describe any custom rules, banned combos, or play expectations.
        </p>
      </div>
    </div>

    <!-- Deck Registration Section -->
    <div class="space-y-3 pt-2 border-t border-gray-200">
      <h4 class="text-sm font-medium text-gray-900">Deck Registration</h4>

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

      <div v-if="requireDeckRegistration" class="ml-7 space-y-3">
        <!-- Registration info box -->
        <div class="bg-amber-50 border border-amber-100 rounded-lg p-3">
          <p class="text-sm text-amber-800">
            Players will be asked to submit their decklist when registering for this event.
            Submitted decks can be validated against format rules before the event starts.
          </p>
        </div>

        <div class="space-y-2">
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
  </div>
</template>
