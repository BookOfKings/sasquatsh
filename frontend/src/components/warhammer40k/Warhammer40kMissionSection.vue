<script setup lang="ts">
import { computed } from 'vue'
import {
  MISSION_PACKS,
  MISSION_SELECTION_LABELS,
  SECONDARY_OBJECTIVES_LABELS,
} from '@/types/warhammer40k'
import type { Warhammer40kMissionSelection, Warhammer40kSecondaryObjectives } from '@/types/warhammer40k'

const props = defineProps<{
  missionPack: string | null
  missionNotes: string | null
  missionSelection?: Warhammer40kMissionSelection | null
  preSelectedMissions?: string[] | null
  secondaryObjectives?: Warhammer40kSecondaryObjectives | null
}>()

const emit = defineEmits<{
  (e: 'update:missionPack', value: string | null): void
  (e: 'update:missionNotes', value: string | null): void
  (e: 'update:missionSelection', value: Warhammer40kMissionSelection | null): void
  (e: 'update:preSelectedMissions', value: string[] | null): void
  (e: 'update:secondaryObjectives', value: Warhammer40kSecondaryObjectives | null): void
}>()

const missionSelectionOptions = Object.keys(MISSION_SELECTION_LABELS) as Warhammer40kMissionSelection[]
const secondaryObjectivesOptions = Object.keys(SECONDARY_OBJECTIVES_LABELS) as Warhammer40kSecondaryObjectives[]

// Ensure we always have a 3-element array for pre-selected missions
const localMissions = computed(() => {
  const missions = props.preSelectedMissions || []
  return [missions[0] || '', missions[1] || '', missions[2] || '']
})

function handleMissionInput(index: number, event: Event) {
  const value = (event.target as HTMLInputElement).value
  const updated = [...localMissions.value]
  updated[index] = value
  // Only emit non-empty missions, or null if all empty
  const hasAny = updated.some(m => m.trim())
  emit('update:preSelectedMissions', hasAny ? updated : null)
}

const selectedPackDescription = computed(() => {
  if (!props.missionPack) return null
  const pack = MISSION_PACKS.find((p) => p.id === props.missionPack)
  return pack?.description ?? null
})

function handleMissionPackChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:missionPack', value || null)
}

function handleMissionNotesInput(event: Event) {
  const value = (event.target as HTMLTextAreaElement).value
  emit('update:missionNotes', value || null)
}
</script>

<template>
  <div class="space-y-6">
    <h3 class="text-lg font-semibold text-gray-900">Mission</h3>

    <!-- Mission Pack -->
    <div class="space-y-2">
      <label class="block text-sm font-medium text-gray-700">Mission Pack</label>
      <select
        :value="missionPack ?? ''"
        class="input w-full"
        @change="handleMissionPackChange"
      >
        <option value="">No specific mission pack</option>
        <option
          v-for="pack in MISSION_PACKS"
          :key="pack.id"
          :value="pack.id"
        >
          {{ pack.name }}
        </option>
      </select>

      <p v-if="selectedPackDescription" class="text-sm text-gray-500 italic">
        {{ selectedPackDescription }}
      </p>
    </div>

    <!-- Mission Selection -->
    <div v-if="missionSelection !== undefined" class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">Mission Selection</label>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="option in missionSelectionOptions"
          :key="option"
          type="button"
          class="px-4 py-2 rounded-lg text-sm font-medium border transition-all duration-150"
          :class="[
            missionSelection === option
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="emit('update:missionSelection', option)"
        >
          {{ MISSION_SELECTION_LABELS[option] }}
        </button>
      </div>

      <!-- Pre-selected missions inputs -->
      <div v-if="missionSelection === 'pre_selected'" class="space-y-2 ml-2 mt-2">
        <div v-for="(_, idx) in 3" :key="idx">
          <label class="block text-xs font-medium text-gray-600 mb-1">Mission {{ idx + 1 }}</label>
          <input
            type="text"
            :value="localMissions[idx]"
            class="input w-full"
            :placeholder="`Enter mission ${idx + 1} name or description`"
            @input="handleMissionInput(idx, $event)"
          />
        </div>
      </div>
    </div>

    <!-- Secondary Objectives -->
    <div v-if="secondaryObjectives !== undefined" class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">Secondary Objectives</label>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="option in secondaryObjectivesOptions"
          :key="option"
          type="button"
          class="px-4 py-2 rounded-lg text-sm font-medium border transition-all duration-150"
          :class="[
            secondaryObjectives === option
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="emit('update:secondaryObjectives', option)"
        >
          {{ SECONDARY_OBJECTIVES_LABELS[option] }}
        </button>
      </div>
    </div>

    <!-- Mission Notes -->
    <div class="space-y-2">
      <label class="block text-sm font-medium text-gray-700">Mission Notes</label>
      <textarea
        :value="missionNotes ?? ''"
        class="input w-full"
        rows="3"
        placeholder="Describe the mission setup, special rules, etc."
        @input="handleMissionNotesInput"
      />
      <p class="text-xs text-gray-500">
        Add any details about the mission, deployment zones, or special rules players should know.
      </p>
    </div>
  </div>
</template>
