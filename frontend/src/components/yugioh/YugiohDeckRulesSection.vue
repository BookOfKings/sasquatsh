<script setup lang="ts">
import { computed } from 'vue'
import type { YugiohFormat, YugiohEventType } from '@/types/yugioh'
import { OFFICIAL_EVENT_TYPES } from '@/types/yugioh'

const props = defineProps<{
  selectedFormat: YugiohFormat | null
  formatId: string | null
  eventType: YugiohEventType
  allowProxies: boolean
  proxyLimit: number | null
  requireDeckRegistration: boolean
  deckSubmissionDeadline: string | null
  allowSideDeck: boolean
  enforceFormatLegality: boolean
  houseRulesNotes: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:allowProxies', value: boolean): void
  (e: 'update:proxyLimit', value: number | null): void
  (e: 'update:requireDeckRegistration', value: boolean): void
  (e: 'update:deckSubmissionDeadline', value: string | null): void
  (e: 'update:allowSideDeck', value: boolean): void
  (e: 'update:enforceFormatLegality', value: boolean): void
  (e: 'update:houseRulesNotes', value: string | null): void
}>()

// Is this an official event that requires strict rules?
const isOfficialEvent = computed(() =>
  OFFICIAL_EVENT_TYPES.includes(props.eventType)
)

// Format rules summary
const formatRules = computed(() => {
  if (!props.selectedFormat) return null

  return {
    mainDeck: `${props.selectedFormat.mainDeckMin}-${props.selectedFormat.mainDeckMax} cards`,
    extraDeck: `Up to ${props.selectedFormat.extraDeckMax} cards`,
    sideDeck: `Up to ${props.selectedFormat.sideDeckMax} cards`,
    maxCopies: `Maximum ${props.selectedFormat.maxCopies} copies of each card`,
    startingLP: `${props.selectedFormat.startingLP.toLocaleString()} LP`,
    startingHand: `${props.selectedFormat.startingHand} cards`,
  }
})

// Is this a Speed Duel format?
const isSpeedDuel = computed(() => props.formatId === 'speed_duel')

// Is Advanced format where side deck is required?
const isAdvancedFormat = computed(() =>
  props.formatId === 'advanced' || props.formatId === 'traditional'
)

// Side deck is required for competitive formats
const sideDeckRequired = computed(() =>
  isAdvancedFormat.value && isOfficialEvent.value
)

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
  }
}
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Deck Rules</h3>

    <!-- Format-Aware Deck Summary -->
    <div v-if="formatRules" class="bg-blue-50 border border-blue-100 rounded-lg p-4">
      <h4 class="text-sm font-medium text-blue-800 mb-2 flex items-center gap-2">
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19M12,6L8,18H10L10.75,16H13.25L14,18H16L12,6M10.83,14L12,10.5L13.17,14H10.83Z" />
        </svg>
        <template v-if="selectedFormat">
          {{ selectedFormat.name }} Format Rules
        </template>
        <template v-else>
          Deck Requirements
        </template>
      </h4>

      <!-- Rules list -->
      <ul class="text-sm text-blue-700 space-y-1">
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          Main Deck: {{ formatRules.mainDeck }}
        </li>
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          Extra Deck: {{ formatRules.extraDeck }}
        </li>
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          Side Deck: {{ formatRules.sideDeck }}
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
          Starting: {{ formatRules.startingLP }}, {{ formatRules.startingHand }} cards
        </li>
      </ul>

      <!-- Speed Duel note -->
      <p v-if="isSpeedDuel" class="text-xs text-blue-600 mt-2 italic">
        Speed Duel uses Skill Cards. Each player selects one Skill Card before the duel.
      </p>
    </div>

    <!-- Format Legality -->
    <div class="space-y-2">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="enforceFormatLegality"
          :disabled="disabled || isOfficialEvent"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="$emit('update:enforceFormatLegality', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Enforce Format Legality</span>
          <p class="text-xs text-gray-500">Decks must follow the current Forbidden/Limited list</p>
        </div>
      </label>
      <p v-if="isOfficialEvent" class="text-xs text-amber-600 ml-7">
        Required for official Konami-sanctioned events
      </p>
    </div>

    <!-- Side Deck Toggle -->
    <div class="space-y-2">
      <label class="flex items-center gap-3" :class="sideDeckRequired ? 'cursor-not-allowed' : 'cursor-pointer'">
        <input
          type="checkbox"
          :checked="allowSideDeck || sideDeckRequired"
          :disabled="disabled || sideDeckRequired"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="$emit('update:allowSideDeck', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">
            {{ sideDeckRequired ? 'Side Deck Enabled' : 'Allow Side Deck' }}
          </span>
          <p class="text-xs text-gray-500">
            {{ sideDeckRequired ? 'Required for Advanced format tournaments' : 'Players can swap cards between games in a match' }}
          </p>
        </div>
      </label>
      <p v-if="sideDeckRequired" class="text-xs text-blue-600 ml-7 flex items-center gap-1">
        <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,5A3,3 0 0,1 15,8A3,3 0 0,1 12,11A3,3 0 0,1 9,8A3,3 0 0,1 12,5M17.13,17C15.92,18.85 14.11,20.24 12,20.92C9.89,20.24 8.08,18.85 6.87,17C6.53,16.5 6.24,16 6,15.47C6,13.82 8.71,12.47 12,12.47C15.29,12.47 18,13.79 18,15.47C17.76,16 17.47,16.5 17.13,17Z"/>
        </svg>
        Side Deck is locked on for official Advanced format tournaments
      </p>
      <p v-else-if="!allowSideDeck" class="text-xs text-gray-500 ml-7">
        When disabled, matches are played without side decking between games.
      </p>
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
            :disabled="disabled || isOfficialEvent"
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            @change="handleProxyToggle(($event.target as HTMLInputElement).checked)"
          />
          <div>
            <span class="text-sm font-medium text-gray-700">Allow Proxies</span>
            <p class="text-xs text-gray-500">Permit non-official card copies</p>
          </div>
        </label>
        <p v-if="isOfficialEvent" class="text-xs text-amber-600 ml-7">
          Proxies are not allowed in official events
        </p>

        <div v-if="allowProxies && !isOfficialEvent" class="ml-7 space-y-2">
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
          placeholder="e.g., No hand traps allowed, Goat Format card pool only, custom banned cards..."
          @input="$emit('update:houseRulesNotes', ($event.target as HTMLTextAreaElement).value || null)"
        />
        <p class="text-xs text-gray-500">
          Describe any custom rules, era restrictions, or banned cards for this event.
        </p>
      </div>
    </div>

    <!-- Deck Registration Section -->
    <div class="space-y-4 border-t border-gray-200 pt-4">
      <h4 class="text-sm font-medium text-gray-700">Deck Registration</h4>

      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="requireDeckRegistration"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
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
      </div>

      <!-- Info about deck registration -->
      <div v-if="requireDeckRegistration" class="bg-indigo-50 border border-indigo-100 rounded-lg p-3 space-y-2">
        <p class="text-sm text-indigo-800 flex items-start gap-2">
          <svg class="w-4 h-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,3H5A2,2 0 0,0 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5A2,2 0 0,0 19,3M19,19H5V5H19V19M7,7H9V9H7V7M7,11H9V17H7V11M11,7H17V9H11V7M11,11H17V13H11V11M11,15H14V17H11V15Z" />
          </svg>
          <span>Players must submit decklists before the event</span>
        </p>
        <p v-if="enforceFormatLegality" class="text-sm text-indigo-700 flex items-start gap-2 ml-6">
          <svg class="w-4 h-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          <span>Decklists may be checked against the Forbidden/Limited list</span>
        </p>
      </div>
    </div>

    <!-- Official Event Rules Notice -->
    <div v-if="isOfficialEvent" class="bg-amber-50 border border-amber-200 rounded-lg p-4 mt-4">
      <h4 class="text-sm font-medium text-amber-800 mb-2 flex items-center gap-2">
        <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,5A3,3 0 0,1 15,8A3,3 0 0,1 12,11A3,3 0 0,1 9,8A3,3 0 0,1 12,5Z"/>
        </svg>
        Official Tournament Rules
      </h4>
      <ul class="text-sm text-amber-700 space-y-1">
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          Uses Konami Forbidden & Limited List
        </li>
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          Tournament rules enforced
        </li>
        <li class="flex items-center gap-2">
          <svg class="w-3 h-3 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
          </svg>
          All cards must be tournament-legal
        </li>
      </ul>
    </div>
  </div>
</template>
