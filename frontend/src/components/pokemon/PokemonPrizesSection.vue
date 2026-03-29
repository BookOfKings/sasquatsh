<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  hasPrizes: boolean
  prizeStructure: string | null
  entryFee: number | null
  entryFeeCurrency: string
  usePlayPoints: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:hasPrizes', value: boolean): void
  (e: 'update:prizeStructure', value: string | null): void
  (e: 'update:entryFee', value: number | null): void
  (e: 'update:entryFeeCurrency', value: string): void
  (e: 'update:usePlayPoints', value: boolean): void
}>()

const hasEntryFee = computed(() => props.entryFee !== null && props.entryFee > 0)

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

// Common prize structure templates
const prizeTemplates = [
  { label: 'Booster Packs', value: '1st: 8 packs, 2nd: 4 packs, 3rd/4th: 2 packs' },
  { label: 'Store Credit', value: 'Store credit based on record' },
  { label: 'Promos', value: 'Participation promos for all players' },
  { label: 'Custom', value: '' },
]
</script>

<template>
  <div class="space-y-4">
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

      <div v-if="hasEntryFee" class="ml-7 flex items-center gap-3">
        <div class="flex items-center gap-2">
          <select
            :value="entryFeeCurrency"
            :disabled="disabled"
            class="input w-20"
            @change="$emit('update:entryFeeCurrency', ($event.target as HTMLSelectElement).value)"
          >
            <option value="USD">$</option>
            <option value="EUR">€</option>
            <option value="GBP">£</option>
            <option value="CAD">C$</option>
          </select>
          <input
            type="number"
            :value="entryFee"
            :disabled="disabled"
            class="input w-24"
            min="0"
            step="0.01"
            placeholder="0.00"
            @input="$emit('update:entryFee', parseFloat(($event.target as HTMLInputElement).value) || null)"
          />
        </div>
        <span class="text-sm text-gray-500">per player</span>
      </div>
    </div>

    <!-- Play! Points Section -->
    <div class="space-y-2">
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

      <div v-if="usePlayPoints" class="ml-7 bg-amber-50 border border-amber-100 rounded-lg p-3">
        <p class="text-sm text-amber-800">
          <svg class="w-4 h-4 inline-block mr-1.5 -mt-0.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,9H11V7H13M13,17H11V11H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" />
          </svg>
          You must be a registered Pokemon Organized Play location to award Championship Points.
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
          <div class="flex flex-wrap gap-2 mb-2">
            <button
              v-for="template in prizeTemplates"
              :key="template.label"
              type="button"
              :disabled="disabled"
              class="px-3 py-1 text-sm rounded border transition-all"
              :class="[
                prizeStructure === template.value
                  ? 'border-yellow-500 text-yellow-600 bg-yellow-50'
                  : 'border-gray-300 hover:border-yellow-400',
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
          placeholder="Describe the prizes and distribution..."
          @input="$emit('update:prizeStructure', ($event.target as HTMLTextAreaElement).value || null)"
        />
        <p class="text-xs text-gray-500">
          Describe what prizes are available and how they'll be distributed.
        </p>
      </div>
    </div>
  </div>
</template>
