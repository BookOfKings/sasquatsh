<script setup lang="ts">
import type { Warhammer40kTerrainType } from '@/types/warhammer40k'
import {
  TERRAIN_TYPE_LABELS,
  TERRAIN_TYPE_DESCRIPTIONS,
  TABLE_SIZES,
} from '@/types/warhammer40k'

defineProps<{
  terrainType: Warhammer40kTerrainType
  tableSize: string
}>()

const emit = defineEmits<{
  (e: 'update:terrainType', value: Warhammer40kTerrainType): void
  (e: 'update:tableSize', value: string): void
}>()

const terrainTypes = Object.keys(TERRAIN_TYPE_LABELS) as Warhammer40kTerrainType[]
</script>

<template>
  <div class="space-y-6">
    <h3 class="text-lg font-semibold text-gray-900">Terrain & Table</h3>

    <!-- Terrain Type -->
    <div class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">Terrain</label>

      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <button
          v-for="type in terrainTypes"
          :key="type"
          type="button"
          class="px-4 py-3 rounded-lg text-left border transition-all duration-150"
          :class="[
            terrainType === type
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="emit('update:terrainType', type)"
        >
          <span class="block text-sm font-medium">{{ TERRAIN_TYPE_LABELS[type] }}</span>
          <span
            class="block text-xs mt-0.5"
            :class="terrainType === type ? 'text-red-100' : 'text-gray-500'"
          >
            {{ TERRAIN_TYPE_DESCRIPTIONS[type] }}
          </span>
        </button>
      </div>
    </div>

    <!-- Table Size -->
    <div class="space-y-3">
      <label class="block text-sm font-medium text-gray-700">Table Size</label>

      <div class="flex flex-wrap gap-3">
        <button
          v-for="size in TABLE_SIZES"
          :key="size.id"
          type="button"
          class="px-4 py-3 rounded-lg text-left border transition-all duration-150"
          :class="[
            tableSize === size.id
              ? 'bg-red-600 text-white border-red-600 shadow-sm'
              : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-red-300'
          ]"
          @click="emit('update:tableSize', size.id)"
        >
          <span class="block text-sm font-medium">{{ size.label }}</span>
          <span
            class="block text-xs mt-0.5"
            :class="tableSize === size.id ? 'text-red-100' : 'text-gray-500'"
          >
            {{ size.description }}
          </span>
        </button>
      </div>
    </div>
  </div>
</template>
