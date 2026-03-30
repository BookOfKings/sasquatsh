<script setup lang="ts">
import { computed } from 'vue'
import { MISSION_PACKS } from '@/types/warhammer40k'

const props = defineProps<{
  missionPack: string | null
  missionNotes: string | null
}>()

const emit = defineEmits<{
  (e: 'update:missionPack', value: string | null): void
  (e: 'update:missionNotes', value: string | null): void
}>()

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
