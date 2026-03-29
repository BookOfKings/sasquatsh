<script setup lang="ts">
import { computed } from 'vue'
import { POWER_LEVEL_DESCRIPTIONS } from '@/types/mtg'

const props = defineProps<{
  modelValue: number | null
  showDescription?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | null): void
}>()

const powerLevels = computed(() => {
  return Object.entries(POWER_LEVEL_DESCRIPTIONS).map(([level, description]) => ({
    level: parseInt(level),
    description,
  }))
})

const selectedDescription = computed(() => {
  if (props.modelValue === null) return null
  return POWER_LEVEL_DESCRIPTIONS[props.modelValue] || null
})

function handleChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:modelValue', value ? parseInt(value) : null)
}

// Color coding for power levels
function getPowerLevelColor(level: number): string {
  if (level <= 3) return 'bg-green-100 text-green-800'
  if (level <= 5) return 'bg-blue-100 text-blue-800'
  if (level <= 7) return 'bg-yellow-100 text-yellow-800'
  if (level <= 9) return 'bg-orange-100 text-orange-800'
  return 'bg-red-100 text-red-800'
}
</script>

<template>
  <div>
    <select
      :value="modelValue ?? ''"
      class="input"
      :disabled="disabled"
      @change="handleChange"
    >
      <option value="">Select power level...</option>
      <option v-for="pl in powerLevels" :key="pl.level" :value="pl.level">
        {{ pl.level }} - {{ pl.description.split(' - ')[0] }}
      </option>
    </select>

    <!-- Power level description -->
    <p v-if="showDescription && selectedDescription" class="text-sm text-gray-500 mt-1">
      {{ selectedDescription }}
    </p>

    <!-- Visual power level indicator -->
    <div v-if="modelValue !== null" class="mt-2">
      <div class="flex items-center gap-1">
        <span
          v-for="level in 10"
          :key="level"
          class="w-6 h-6 rounded flex items-center justify-center text-xs font-medium transition-all"
          :class="[
            level <= modelValue
              ? getPowerLevelColor(level)
              : 'bg-gray-100 text-gray-400'
          ]"
        >
          {{ level }}
        </span>
      </div>
    </div>
  </div>
</template>
