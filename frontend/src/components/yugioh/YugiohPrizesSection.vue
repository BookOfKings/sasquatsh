<script setup lang="ts">
import { computed } from 'vue'
import type { YugiohEventType } from '@/types/yugioh'
import { OFFICIAL_EVENT_TYPES } from '@/types/yugioh'

const props = defineProps<{
  eventType: YugiohEventType
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  isOfficialEvent: boolean
  awardsOtsPoints: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:hasPrizes', value: boolean): void
  (e: 'update:prizeStructure', value: string | null): void
  (e: 'update:entryFee', value: number | null): void
  (e: 'update:entryFeeCurrency', value: string): void
  (e: 'update:isOfficialEvent', value: boolean): void
  (e: 'update:awardsOtsPoints', value: boolean): void
}>()

// Is this an official event type that can award OTS points?
const canAwardOtsPoints = computed(() =>
  OFFICIAL_EVENT_TYPES.includes(props.eventType)
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
    JPY: '¥',
  }
  const symbol = symbols[props.entryFeeCurrency] || props.entryFeeCurrency
  return `${symbol}${props.entryFee.toFixed(2)} per player`
})

// Toggle OTS points - sync official event flag
function handleOtsPointsToggle(checked: boolean) {
  emit('update:awardsOtsPoints', checked)
  if (checked) {
    emit('update:isOfficialEvent', true)
  }
}

// Toggle official event
function handleOfficialEventToggle(checked: boolean) {
  emit('update:isOfficialEvent', checked)
  if (!checked) {
    emit('update:awardsOtsPoints', false)
  }
}

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

// Common prize structure templates - Yu-Gi-Oh! specific
const prizeTemplates = [
  { label: 'OTS Packs', value: '1st: 6 OTS packs, 2nd: 4 OTS packs, 3-4th: 2 OTS packs each, 5-8th: 1 pack' },
  { label: 'Store Credit', value: 'Store credit based on standings' },
  { label: 'Booster Boxes', value: '1st: Booster box, 2nd: 18 packs, 3-4th: 9 packs each' },
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
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="handleEntryFeeToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Entry Fee Required</span>
          <p class="text-xs text-gray-500">Charge an entry fee for this event</p>
        </div>
      </label>

      <div v-if="hasEntryFee" class="ml-7 space-y-3">
        <div class="flex items-center gap-3">
          <div class="flex items-center border border-gray-300 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 focus-within:border-blue-500">
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
              <option value="JPY">JPY ¥</option>
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

    <!-- Official Event & OTS Points Section -->
    <div v-if="canAwardOtsPoints" class="space-y-3 border-t border-gray-200 pt-4">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="isOfficialEvent"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="handleOfficialEventToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Konami-Sanctioned Event</span>
          <p class="text-xs text-gray-500">This is an official Konami-sanctioned tournament</p>
        </div>
      </label>

      <div v-if="isOfficialEvent" class="ml-7 space-y-3">
        <!-- OTS Points Toggle -->
        <label class="flex items-center gap-3 cursor-pointer">
          <input
            type="checkbox"
            :checked="awardsOtsPoints"
            :disabled="disabled"
            class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            @change="handleOtsPointsToggle(($event.target as HTMLInputElement).checked)"
          />
          <div>
            <span class="text-sm font-medium text-gray-700">Awards OTS Points</span>
            <p class="text-xs text-gray-500">Players earn OTS Championship Points</p>
          </div>
        </label>

        <!-- Official Event Info -->
        <div class="bg-blue-50 border border-blue-100 rounded-lg p-3">
          <p class="text-sm text-blue-800">
            <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
            </svg>
            Official events require OTS store partnership and players must have valid CARD GAME IDs.
          </p>
        </div>

        <!-- Self-attestation confirmation -->
        <label v-if="awardsOtsPoints" class="flex items-start gap-3 cursor-pointer bg-gray-50 rounded-lg p-3">
          <input
            type="checkbox"
            checked
            disabled
            class="h-4 w-4 mt-0.5 rounded border-gray-300 text-blue-600"
          />
          <div>
            <span class="text-sm font-medium text-gray-700">OTS Store Confirmed</span>
            <p class="text-xs text-gray-500 mt-1">
              By hosting this as an official event, you confirm this venue is an approved Official Tournament Store.
            </p>
          </div>
        </label>
      </div>
    </div>

    <!-- Prize Support Section -->
    <div class="space-y-3 border-t border-gray-200 pt-4">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="hasPrizes"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="handlePrizesToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Prize Support</span>
          <p class="text-xs text-gray-500">Offer prizes for winners</p>
        </div>
      </label>

      <!-- No prizes helper when entry fee is on but prizes are off -->
      <p v-if="hasEntryFee && !hasPrizes" class="text-xs text-amber-600 ml-7">
        Entry fee enabled without prize support. Consider adding prizes or clarifying where fees go.
      </p>

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
                  ? 'border-blue-500 text-blue-700 bg-blue-50'
                  : 'border-gray-300 hover:border-blue-400 bg-white',
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
          placeholder="e.g., 1st: 6 OTS packs, 2nd: 4 OTS packs, 3-4th: 2 OTS packs each"
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
