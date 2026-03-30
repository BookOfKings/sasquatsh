<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
}>()

const emit = defineEmits<{
  (e: 'update:hasPrizes', value: boolean): void
  (e: 'update:prizeStructure', value: string | null): void
  (e: 'update:entryFee', value: number | null): void
  (e: 'update:entryFeeCurrency', value: string): void
}>()

const hasEntryFee = computed(() => props.entryFee !== null && props.entryFee > 0)

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

const showDisclaimer = computed(() => hasEntryFee.value || props.hasPrizes)

function handleEntryFeeToggle(checked: boolean) {
  if (checked) {
    emit('update:entryFee', 5)
  } else {
    emit('update:entryFee', null)
  }
}

function handlePrizesToggle(checked: boolean) {
  emit('update:hasPrizes', checked)
  if (!checked) {
    emit('update:prizeStructure', null)
  }
}
</script>

<template>
  <div class="space-y-6">
    <h3 class="text-lg font-semibold text-gray-900">Entry & Prizes</h3>

    <!-- Entry Fee -->
    <div class="space-y-3">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="hasEntryFee"
          class="h-4 w-4 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="handleEntryFeeToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Entry Fee Required</span>
          <p class="text-xs text-gray-500">Charge an entry fee for this event</p>
        </div>
      </label>

      <div v-if="hasEntryFee" class="ml-7 space-y-3">
        <div class="flex items-center gap-3">
          <div class="flex items-center border border-gray-300 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-red-500 focus-within:border-red-500">
            <select
              :value="entryFeeCurrency"
              class="px-2 py-2 bg-gray-50 border-r border-gray-300 text-sm focus:outline-none"
              @change="emit('update:entryFeeCurrency', ($event.target as HTMLSelectElement).value)"
            >
              <option value="USD">USD $</option>
              <option value="EUR">EUR &euro;</option>
              <option value="GBP">GBP &pound;</option>
              <option value="CAD">CAD $</option>
              <option value="AUD">AUD $</option>
              <option value="JPY">JPY &yen;</option>
            </select>
            <input
              type="number"
              :value="entryFee"
              class="px-3 py-2 w-24 text-sm focus:outline-none"
              min="0"
              step="0.01"
              placeholder="0.00"
              @input="emit('update:entryFee', parseFloat(($event.target as HTMLInputElement).value) || null)"
            />
          </div>
          <span class="text-sm text-gray-500">per player</span>
        </div>

        <div v-if="entryFeeSummary" class="bg-gray-50 rounded-lg px-3 py-2">
          <p class="text-sm text-gray-700 font-medium">
            {{ entryFeeSummary }}
          </p>
        </div>
      </div>
    </div>

    <!-- Prize Support -->
    <div class="space-y-3 border-t border-gray-200 pt-4">
      <label class="flex items-center gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="hasPrizes"
          class="h-4 w-4 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="handlePrizesToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Prize Support</span>
          <p class="text-xs text-gray-500">Offer prizes for winners</p>
        </div>
      </label>

      <p v-if="hasEntryFee && !hasPrizes" class="text-xs text-amber-600 ml-7">
        Entry fee enabled without prize support. Consider adding prizes or clarifying where fees go.
      </p>

      <div v-if="hasPrizes" class="ml-7 space-y-3">
        <label class="block text-sm text-gray-700">Prize Structure</label>
        <textarea
          :value="prizeStructure ?? ''"
          class="input w-full"
          rows="3"
          placeholder="e.g., 1st: Store credit, 2nd: Warhammer kit, Best Painted: Paint set..."
          @input="emit('update:prizeStructure', ($event.target as HTMLTextAreaElement).value || null)"
        />
        <p class="text-xs text-gray-500">
          Describe the prizes and how they will be distributed.
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
