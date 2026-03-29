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

// Show disclaimer when either entry fee or prizes are enabled
const showDisclaimer = computed(() => hasEntryFee.value || props.hasPrizes)
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

      <div v-if="hasEntryFee" class="ml-7 flex items-center gap-2">
        <span class="text-gray-500">$</span>
        <input
          type="number"
          :value="entryFee"
          :disabled="disabled"
          class="input w-24"
          min="0"
          step="0.01"
          @input="$emit('update:entryFee', parseFloat(($event.target as HTMLInputElement).value) || null)"
        />
        <span class="text-sm text-gray-500">USD</span>
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
        <label class="block text-sm font-medium text-gray-700">Prize Structure</label>
        <textarea
          :value="prizeStructure ?? ''"
          :disabled="disabled"
          class="input w-full h-20"
          placeholder="e.g., 1st: 6 packs, 2nd: 3 packs, 3rd-4th: 1 pack each"
          @input="$emit('update:prizeStructure', ($event.target as HTMLTextAreaElement).value || null)"
        ></textarea>
      </div>
    </div>

    <!-- Summary -->
    <div v-if="hasEntryFee || hasPrizes" class="bg-gray-50 rounded-lg p-3">
      <p class="text-sm text-gray-600">
        <template v-if="hasEntryFee">
          Entry: ${{ entryFee?.toFixed(2) }}
        </template>
        <template v-if="hasEntryFee && hasPrizes"> | </template>
        <template v-if="hasPrizes">
          Prizes available
        </template>
      </p>
    </div>

    <!-- Disclaimer -->
    <div v-if="showDisclaimer" class="text-xs text-gray-500">
      <svg class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
      </svg>
      Entry fees and prizes are collected at the event. Sasquatsh does not process payments.
    </div>
  </div>
</template>
