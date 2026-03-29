<script setup lang="ts">
import type { DraftStyle } from '@/types/mtg'

defineProps<{
  packsPerPlayer: number | null
  draftStyle: DraftStyle | null
  cubeId: string | null
  formatId: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:packsPerPlayer', value: number | null): void
  (e: 'update:draftStyle', value: DraftStyle | null): void
  (e: 'update:cubeId', value: string | null): void
}>()

const draftStyles: { value: DraftStyle; label: string; description: string }[] = [
  { value: 'standard', label: 'Standard Draft', description: 'Pick one card, pass the rest. Classic booster draft.' },
  { value: 'rochester', label: 'Rochester Draft', description: 'All cards visible. Each player picks in order.' },
  { value: 'winston', label: 'Winston Draft', description: '2-player draft with three face-down piles.' },
  { value: 'grid', label: 'Grid Draft', description: '2-player draft from a 3x3 grid of cards.' },
]
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center gap-2">
      <h3 class="text-lg font-semibold text-gray-900">Draft Settings</h3>
      <span class="text-xs text-green-600 bg-green-50 px-2 py-0.5 rounded">
        Limited Format
      </span>
    </div>

    <!-- Packs Per Player -->
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Packs Per Player
      </label>
      <div class="flex items-center gap-3">
        <input
          type="number"
          :value="packsPerPlayer ?? ''"
          :disabled="disabled"
          class="input w-24"
          min="1"
          max="12"
          placeholder="3"
          @input="$emit('update:packsPerPlayer', ($event.target as HTMLInputElement).value ? parseInt(($event.target as HTMLInputElement).value) : null)"
        />
        <div class="flex gap-2">
          <button
            type="button"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50"
            :class="packsPerPlayer === 3 ? 'border-blue-500 text-blue-600' : 'border-gray-300'"
            @click="$emit('update:packsPerPlayer', 3)"
          >
            3 (Draft)
          </button>
          <button
            type="button"
            class="px-3 py-1 text-sm rounded border hover:bg-gray-50"
            :class="packsPerPlayer === 6 ? 'border-blue-500 text-blue-600' : 'border-gray-300'"
            @click="$emit('update:packsPerPlayer', 6)"
          >
            6 (Sealed)
          </button>
        </div>
      </div>
    </div>

    <!-- Draft Style -->
    <div v-if="formatId === 'draft' || formatId === 'cube_draft'">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Draft Style
      </label>
      <select
        :value="draftStyle ?? ''"
        :disabled="disabled"
        class="input"
        @change="$emit('update:draftStyle', ($event.target as HTMLSelectElement).value as DraftStyle || null)"
      >
        <option value="">Select draft style...</option>
        <option v-for="style in draftStyles" :key="style.value" :value="style.value">
          {{ style.label }}
        </option>
      </select>
      <p v-if="draftStyle" class="text-sm text-gray-500 mt-1">
        {{ draftStyles.find(s => s.value === draftStyle)?.description }}
      </p>
    </div>

    <!-- Cube ID (for cube draft) -->
    <div v-if="formatId === 'cube_draft' || formatId === 'cube'">
      <label class="block text-sm font-medium text-gray-700 mb-1">
        Cube Link
      </label>
      <input
        type="text"
        :value="cubeId ?? ''"
        :disabled="disabled"
        class="input"
        placeholder="CubeCobra URL or ID"
        @input="$emit('update:cubeId', ($event.target as HTMLInputElement).value || null)"
      />
      <p class="text-xs text-gray-500 mt-1">
        Link to your cube on CubeCobra or similar site.
      </p>
    </div>

    <!-- Info box -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-3">
      <p class="text-sm text-blue-800">
        <strong>Tip:</strong> For a standard booster draft, use 3 packs per player.
        For sealed deck events, use 6 packs per player.
      </p>
    </div>
  </div>
</template>
