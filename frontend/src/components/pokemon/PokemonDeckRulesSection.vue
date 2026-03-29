<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonFormat, PokemonEventType } from '@/types/pokemon'
import { FORMAT_DECK_DESCRIPTIONS, LIMITED_EVENT_TYPES } from '@/types/pokemon'

const props = defineProps<{
  selectedFormat: PokemonFormat | null
  formatId: string | null
  eventType: PokemonEventType
  allowProxies: boolean
  proxyLimit: number | null
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  allowDeckChanges: boolean
  enforceFormatLegality: boolean
  houseRulesNotes: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:allowProxies', value: boolean): void
  (e: 'update:proxyLimit', value: number | null): void
  (e: 'update:requireDeckRegistration', value: boolean): void
  (e: 'update:deckSubmissionDeadline', value: string | null): void
  (e: 'update:allowDeckChanges', value: boolean): void
  (e: 'update:enforceFormatLegality', value: boolean): void
  (e: 'update:houseRulesNotes', value: string | null): void
}>()

// Is this a limited event (prerelease/draft)?
const isLimitedEvent = computed(() =>
  LIMITED_EVENT_TYPES.includes(props.eventType)
)

// Format-specific deck description
const formatDeckDescription = computed(() => {
  if (isLimitedEvent.value) {
    if (props.eventType === 'prerelease') {
      return 'Decks built on-site from Build & Battle Kits (40+ cards)'
    }
    if (props.eventType === 'draft') {
      return 'Decks built on-site from drafted packs (40+ cards)'
    }
  }
  if (props.formatId && FORMAT_DECK_DESCRIPTIONS[props.formatId]) {
    return FORMAT_DECK_DESCRIPTIONS[props.formatId]
  }
  return null
})

// Format rules summary
const formatRules = computed(() => {
  if (isLimitedEvent.value) {
    return {
      deckSize: '40+ cards',
      maxCopies: 'Up to 4 copies per card (except Basic Energy)',
      rotation: 'Uses cards from provided product',
    }
  }
  if (!props.selectedFormat) return null

  return {
    deckSize: `${props.selectedFormat.minDeckSize} cards`,
    maxCopies: props.selectedFormat.maxCopies === 1
      ? 'Singleton (1 copy except Basic Energy)'
      : `Up to ${props.selectedFormat.maxCopies} copies per card (except Basic Energy)`,
    rotation: props.selectedFormat.isRotating
      ? 'Rotates yearly when new sets release'
      : 'Non-rotating format',
  }
})

// Toggle proxy limit visibility
function handleProxyToggle(checked: boolean) {
  emit('update:allowProxies', checked)
  if (!checked) {
    emit('update:proxyLimit', null)
  }
}

// Toggle deck registration
function handleDeckRegistrationToggle(checked: boolean) {
  emit('update:requireDeckRegistration', checked)
  if (!checked) {
    emit('update:deckSubmissionDeadline', null)
    emit('update:allowDeckChanges', false)
  }
}
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Deck Rules</h3>

    <!-- Format-Aware Deck Summary -->
    <div v-if="formatDeckDescription || formatRules" class="bg-yellow-50 border border-yellow-100 rounded-lg p-4">
      <h4 class="text-sm font-medium text-yellow-800 mb-2 flex items-center gap-2">
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,3H14.82C14.25,1.44 12.53,0.64 11,1.2C10.14,1.5 9.5,2.16 9.18,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M12,3A1,1 0 0,1 13,4A1,1 0 0,1 12,5A1,1 0 0,1 11,4A1,1 0 0,1 12,3" />
        </svg>
        <template v-if="isLimitedEvent">
          Limited Event Deck Rules
        </template>
        <template v-else-if="selectedFormat">
          {{ selectedFormat.name }} Format Rules
        </template>
        <template v-else>
          Deck Requirements
        </template>
      </h4>

      <!-- Format description -->
      <p v-if="formatDeckDescription" class="text-sm text-yellow-700 mb-2">
        {{ formatDeckDescription }}
      </p>

      <!-- Rules list -->
      <ul v-if="formatRules" class="text-sm text-yellow-700 space-y-1">
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          Deck size: {{ formatRules.deckSize }}
        </li>
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          {{ formatRules.maxCopies }}
        </li>
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          {{ formatRules.rotation }}
        </li>
      </ul>
    </div>

    <!-- Format Legality (not for limited events) -->
    <div v-if="!isLimitedEvent" class="space-y-2">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="enforceFormatLegality"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="$emit('update:enforceFormatLegality', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Enforce Format Legality</span>
          <p class="text-xs text-gray-500">Decks must use only format-legal cards</p>
        </div>
      </label>
    </div>

    <!-- House Rules Section -->
    <div class="space-y-4 border-t border-gray-200 pt-4">
      <h4 class="text-sm font-medium text-gray-700">House Rules</h4>

      <!-- Allow Proxies -->
      <div class="space-y-2">
        <label class="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            :checked="allowProxies"
            :disabled="disabled"
            class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
            @change="handleProxyToggle(($event.target as HTMLInputElement).checked)"
          />
          <div>
            <span class="text-sm font-medium text-gray-700">Allow Proxies</span>
            <p class="text-xs text-gray-500">Permit non-official card copies</p>
          </div>
        </label>

        <div v-if="allowProxies" class="ml-7 space-y-2">
          <label class="block text-sm text-gray-700">
            Proxy Limit (optional)
          </label>
          <input
            type="number"
            :value="proxyLimit ?? ''"
            :disabled="disabled"
            class="input w-32"
            min="1"
            max="60"
            placeholder="No limit"
            @input="$emit('update:proxyLimit', ($event.target as HTMLInputElement).value ? parseInt(($event.target as HTMLInputElement).value) : null)"
          />
          <p class="text-xs text-gray-500">
            Leave blank for unlimited proxies.
          </p>
        </div>
      </div>

      <!-- House Rules Notes -->
      <div class="space-y-2">
        <label class="block text-sm font-medium text-gray-700">
          Additional House Rules
        </label>
        <textarea
          :value="houseRulesNotes ?? ''"
          :disabled="disabled"
          class="input w-full"
          rows="2"
          placeholder="e.g., No EX/GX allowed, limit 2 Rare Candy per deck..."
          @input="$emit('update:houseRulesNotes', ($event.target as HTMLTextAreaElement).value || null)"
        />
        <p class="text-xs text-gray-500">
          Describe any custom rules or banned cards for this event.
        </p>
      </div>
    </div>

    <!-- Deck Registration Section (not for limited events) -->
    <div v-if="!isLimitedEvent" class="space-y-4 border-t border-gray-200 pt-4">
      <h4 class="text-sm font-medium text-gray-700">Deck Registration</h4>

      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="requireDeckRegistration"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="handleDeckRegistrationToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Require Deck Registration</span>
          <p class="text-xs text-gray-500">Players must submit their decklist before the event</p>
        </div>
      </label>

      <div v-if="requireDeckRegistration" class="ml-7 space-y-4">
        <!-- Submission Deadline -->
        <div>
          <label class="block text-sm text-gray-700 mb-1">
            Submission Deadline
          </label>
          <input
            type="datetime-local"
            :value="deckSubmissionDeadline ?? ''"
            :disabled="disabled"
            class="input"
            @input="$emit('update:deckSubmissionDeadline', ($event.target as HTMLInputElement).value || null)"
          />
          <p class="text-xs text-gray-500 mt-1">
            When players must submit their deck by. Leave blank for "before event starts".
          </p>
        </div>

        <!-- Allow Deck Changes -->
        <label class="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            :checked="allowDeckChanges"
            :disabled="disabled"
            class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
            @change="$emit('update:allowDeckChanges', ($event.target as HTMLInputElement).checked)"
          />
          <div>
            <span class="text-sm text-gray-700">Allow Deck Changes After Submission</span>
            <p class="text-xs text-gray-500">Players can modify their deck after submitting</p>
          </div>
        </label>
      </div>

      <!-- Info about deck registration -->
      <div v-if="requireDeckRegistration" class="bg-blue-50 border border-blue-100 rounded-lg p-3">
        <p class="text-sm text-blue-800">
          <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
          </svg>
          Players will be prompted to select or import a deck when they register. You'll be able to view all decklists from the event dashboard.
        </p>
      </div>
    </div>
  </div>
</template>
