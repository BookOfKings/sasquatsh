<script setup lang="ts">
import { computed } from 'vue'
import type { PokemonEventType } from '@/types/pokemon'
import { TOURNAMENT_EVENT_TYPES } from '@/types/pokemon'

const props = defineProps<{
  eventType: PokemonEventType
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  usePlayPoints: boolean
  isOfficialLocation?: boolean  // Whether venue is a registered Pokemon OP location
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:hasPrizes', value: boolean): void
  (e: 'update:prizeStructure', value: string | null): void
  (e: 'update:entryFee', value: number | null): void
  (e: 'update:entryFeeCurrency', value: string): void
  (e: 'update:usePlayPoints', value: boolean): void
}>()

// Is this a competitive tournament event?
const isTournamentEvent = computed(() =>
  TOURNAMENT_EVENT_TYPES.includes(props.eventType)
)

const hasEntryFee = computed(() => props.entryFee !== null && props.entryFee > 0)

// Show disclaimer when either entry fee or prizes are enabled
const showDisclaimer = computed(() => hasEntryFee.value || props.hasPrizes)

// Format entry fee display
const entryFeeSummary = computed(() => {
  if (!hasEntryFee.value || !props.entryFee) return null
  const symbols: Record<string, string> = {
    USD: '$',
    EUR: '€',
    GBP: '£',
    CAD: 'C$',
    AUD: 'A$',
  }
  const symbol = symbols[props.entryFeeCurrency] || props.entryFeeCurrency
  return `${symbol}${props.entryFee.toFixed(2)} per player`
})

// Play Points eligibility status
const playPointsStatus = computed(() => {
  if (props.isOfficialLocation === true) {
    return { type: 'eligible', message: 'This location is approved for Championship Points.' }
  }
  if (props.isOfficialLocation === false) {
    return { type: 'ineligible', message: 'This location is not registered for Pokemon Organized Play.' }
  }
  return { type: 'unknown', message: 'Championship Points can only be awarded at approved Pokemon Organized Play locations.' }
})

// Toggle entry fee
function handleEntryFeeToggle(checked: boolean) {
  if (checked) {
    emit('update:entryFee', 5)
  } else {
    emit('update:entryFee', null)
  }
}

// Toggle prizes
function handlePrizesToggle(checked: boolean) {
  emit('update:hasPrizes', checked)
  if (!checked) {
    emit('update:prizeStructure', null)
  }
}

// Common prize structure templates - Pokemon-specific
const prizeTemplates = [
  { label: 'Booster Packs', value: '1st: 6 booster packs, 2nd: 3 booster packs, 3rd-4th: 1 pack' },
  { label: 'Store Credit', value: 'Store credit based on standings' },
  { label: 'Promo Cards', value: 'Participation promos for all players, winner receives exclusive promo' },
  { label: 'Custom', value: '' },
]
</script>

<template>
  <div class="space-y-6">
    <h3 class="text-lg font-semibold text-gray-900">Entry & Prizes</h3>

    <!-- Entry Fee Section -->
    <div class="space-y-3">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="hasEntryFee"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="handleEntryFeeToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Entry Fee Required</span>
          <p class="text-xs text-gray-500">Charge an entry fee for this event</p>
        </div>
      </label>

      <div v-if="hasEntryFee" class="ml-7 space-y-3">
        <div class="flex items-center gap-3">
          <div class="flex items-center border border-gray-300 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-yellow-500 focus-within:border-yellow-500">
            <select
              :value="entryFeeCurrency"
              :disabled="disabled"
              class="px-2 py-2 bg-gray-50 border-r border-gray-300 text-sm focus:outline-none"
              @change="$emit('update:entryFeeCurrency', ($event.target as HTMLSelectElement).value)"
            >
              <option value="USD">USD $</option>
              <option value="EUR">EUR €</option>
              <option value="GBP">GBP £</option>
              <option value="CAD">CAD $</option>
              <option value="AUD">AUD $</option>
            </select>
            <input
              type="number"
              :value="entryFee"
              :disabled="disabled"
              class="px-3 py-2 w-24 text-sm focus:outline-none"
              min="0"
              step="0.01"
              placeholder="0.00"
              @input="$emit('update:entryFee', parseFloat(($event.target as HTMLInputElement).value) || null)"
            />
          </div>
          <span class="text-sm text-gray-500">per player</span>
        </div>

        <!-- Entry fee summary -->
        <div v-if="entryFeeSummary" class="bg-gray-50 rounded-lg px-3 py-2">
          <p class="text-sm text-gray-700 font-medium">
            <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5 text-green-600" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,17V16H9V14H13V13H10A1,1 0 0,1 9,12V9A1,1 0 0,1 10,8H11V7H13V8H15V10H11V11H14A1,1 0 0,1 15,12V15A1,1 0 0,1 14,16H13V17H11Z" />
            </svg>
            {{ entryFeeSummary }}
          </p>
        </div>
      </div>
    </div>

    <!-- Official Play! Points Section -->
    <div v-if="isTournamentEvent" class="space-y-2 border-t border-gray-200 pt-4">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="usePlayPoints"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="$emit('update:usePlayPoints', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Official Play! Points</span>
          <p class="text-xs text-gray-500">Award Championship Points for this event</p>
        </div>
      </label>

      <div v-if="usePlayPoints" class="ml-7">
        <!-- Eligibility status -->
        <div
          class="rounded-lg p-3"
          :class="{
            'bg-green-50 border border-green-100': playPointsStatus.type === 'eligible',
            'bg-red-50 border border-red-100': playPointsStatus.type === 'ineligible',
            'bg-amber-50 border border-amber-100': playPointsStatus.type === 'unknown',
          }"
        >
          <p
            class="text-sm"
            :class="{
              'text-green-800': playPointsStatus.type === 'eligible',
              'text-red-800': playPointsStatus.type === 'ineligible',
              'text-amber-800': playPointsStatus.type === 'unknown',
            }"
          >
            <svg
              class="w-4 h-4 inline-block mr-1.5 -mt-0.5"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path v-if="playPointsStatus.type === 'eligible'" d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z" />
              <path v-else-if="playPointsStatus.type === 'ineligible'" d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" />
              <path v-else d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
            </svg>
            {{ playPointsStatus.message }}
          </p>
        </div>

        <!-- Additional info about Play! Points -->
        <p class="text-xs text-gray-500 mt-2">
          To award Championship Points, you must be a registered Pokemon Organized Play location and sanctioned by Pokemon.
        </p>
      </div>
    </div>

    <!-- Prize Support Section -->
    <div class="space-y-3 border-t border-gray-200 pt-4">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="hasPrizes"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-yellow-500 focus:ring-yellow-500"
          @change="handlePrizesToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Prize Support</span>
          <p class="text-xs text-gray-500">Offer prizes for winners</p>
        </div>
      </label>

      <div v-if="hasPrizes" class="ml-7 space-y-3">
        <!-- Quick templates -->
        <div>
          <label class="block text-sm text-gray-700 mb-2">Prize Structure</label>
          <div class="flex flex-wrap gap-2 mb-3">
            <button
              v-for="template in prizeTemplates"
              :key="template.label"
              type="button"
              :disabled="disabled"
              class="px-3 py-1.5 text-sm rounded-lg border transition-all"
              :class="[
                prizeStructure === template.value
                  ? 'border-yellow-500 text-yellow-700 bg-yellow-50'
                  : 'border-gray-300 hover:border-yellow-400 bg-white',
                disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'
              ]"
              @click="$emit('update:prizeStructure', template.value)"
            >
              {{ template.label }}
            </button>
          </div>
        </div>

        <!-- Prize description textarea -->
        <textarea
          :value="prizeStructure ?? ''"
          :disabled="disabled"
          class="input w-full"
          rows="3"
          placeholder="e.g., 1st: 6 booster packs, 2nd: 3 booster packs, 3rd-4th: 1 pack each"
          @input="$emit('update:prizeStructure', ($event.target as HTMLTextAreaElement).value || null)"
        />
        <p class="text-xs text-gray-500">
          Describe the prizes and how they'll be distributed. Be specific so players know what to expect.
        </p>
      </div>
    </div>

    <!-- Disclaimer -->
    <div v-if="showDisclaimer" class="text-sm text-gray-500 flex items-start gap-2 bg-gray-50 rounded-lg p-3">
      <svg class="w-4 h-4 mt-0.5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      Entry fees and prizes are collected at the event. Sasquatsh does not process payments.
    </div>
  </div>
</template>
