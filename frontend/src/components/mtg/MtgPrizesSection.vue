<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:hasPrizes', value: boolean): void
  (e: 'update:prizeStructure', value: string | null): void
  (e: 'update:entryFee', value: number | null): void
  (e: 'update:entryFeeCurrency', value: string): void
}>()

const hasEntryFee = computed(() => props.entryFee !== null && props.entryFee > 0)

const currencies = [
  { code: 'USD', symbol: '$', name: 'US Dollar' },
  { code: 'EUR', symbol: '\u20AC', name: 'Euro' },
  { code: 'GBP', symbol: '\u00A3', name: 'British Pound' },
  { code: 'CAD', symbol: 'C$', name: 'Canadian Dollar' },
  { code: 'AUD', symbol: 'A$', name: 'Australian Dollar' },
]

const currencySymbol = computed(() =>
  currencies.find(c => c.code === props.entryFeeCurrency)?.symbol || '$'
)
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Entry & Prizes</h3>

    <!-- Entry Fee -->
    <div class="space-y-2">
      <div class="flex items-center gap-3">
        <input
          type="checkbox"
          id="hasEntryFee"
          :checked="hasEntryFee"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="$emit('update:entryFee', ($event.target as HTMLInputElement).checked ? 5 : null)"
        />
        <label for="hasEntryFee" class="text-sm font-medium text-gray-700">
          Entry Fee Required
        </label>
      </div>

      <div v-if="hasEntryFee" class="ml-7 flex items-center gap-3">
        <div class="flex items-center">
          <span class="text-gray-500 mr-1">{{ currencySymbol }}</span>
          <input
            type="number"
            :value="entryFee"
            :disabled="disabled"
            class="input w-24"
            min="0"
            step="0.01"
            @input="$emit('update:entryFee', parseFloat(($event.target as HTMLInputElement).value) || null)"
          />
        </div>
        <select
          :value="entryFeeCurrency"
          :disabled="disabled"
          class="input w-24"
          @change="$emit('update:entryFeeCurrency', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="currency in currencies" :key="currency.code" :value="currency.code">
            {{ currency.code }}
          </option>
        </select>
      </div>
    </div>

    <!-- Prize Support -->
    <div class="space-y-2">
      <div class="flex items-center gap-3">
        <input
          type="checkbox"
          id="hasPrizes"
          :checked="hasPrizes"
          :disabled="disabled"
          class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          @change="$emit('update:hasPrizes', ($event.target as HTMLInputElement).checked)"
        />
        <label for="hasPrizes" class="text-sm font-medium text-gray-700">
          Prize Support
        </label>
      </div>

      <div v-if="hasPrizes" class="ml-7 space-y-2">
        <label class="block text-sm text-gray-600">Prize Structure</label>
        <textarea
          :value="prizeStructure ?? ''"
          :disabled="disabled"
          class="input w-full h-24"
          placeholder="Describe the prizes and how they will be distributed...

Example:
1st Place: 8 packs
2nd Place: 4 packs
3rd-4th Place: 2 packs each"
          @input="$emit('update:prizeStructure', ($event.target as HTMLTextAreaElement).value || null)"
        ></textarea>
      </div>
    </div>

    <!-- Summary -->
    <div v-if="hasEntryFee || hasPrizes" class="bg-gray-50 rounded-lg p-3">
      <p class="text-sm text-gray-600">
        <template v-if="hasEntryFee">
          Entry: {{ currencySymbol }}{{ entryFee?.toFixed(2) }}
        </template>
        <template v-if="hasEntryFee && hasPrizes"> | </template>
        <template v-if="hasPrizes">
          Prizes available
        </template>
      </p>
    </div>
  </div>
</template>
