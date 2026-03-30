<script setup lang="ts">
import type { Warhammer40kEventConfig } from '@/types/warhammer40k'

defineProps<{
  config: Warhammer40kEventConfig
}>()
</script>

<template>
  <div class="card">
    <div class="p-4 border-b border-gray-100">
      <h3 class="font-semibold flex items-center gap-2">
        <svg class="w-5 h-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,5A3,3 0 0,1 15,8A3,3 0 0,1 12,11A3,3 0 0,1 9,8A3,3 0 0,1 12,5M17.13,17C15.92,18.85 14.11,20.24 12,20.92C9.89,20.24 8.08,18.85 6.87,17C6.53,16.5 6.24,16 6,15.47C6,13.82 8.71,12.47 12,12.47C15.29,12.47 18,13.79 18,15.47C17.76,16 17.47,16.5 17.13,17Z"/>
        </svg>
        Army Rules
      </h3>
    </div>

    <div class="p-4 space-y-4">
      <!-- Rules Grid -->
      <div class="grid grid-cols-2 gap-3">
        <!-- Battle Ready -->
        <div class="flex items-center gap-2">
          <svg
            v-if="config.battleReadyRequired"
            class="w-5 h-5 text-green-500 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-gray-400 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
          <span class="text-sm" :class="config.battleReadyRequired ? 'text-gray-900 font-medium' : 'text-gray-500'">
            {{ config.battleReadyRequired ? 'Battle Ready Required' : 'Battle Ready not required' }}
          </span>
        </div>

        <!-- WYSIWYG -->
        <div class="flex items-center gap-2">
          <svg
            v-if="config.wysiwygRequired"
            class="w-5 h-5 text-green-500 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-gray-400 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
          <span class="text-sm" :class="config.wysiwygRequired ? 'text-gray-900 font-medium' : 'text-gray-500'">
            {{ config.wysiwygRequired ? 'WYSIWYG Required' : 'WYSIWYG not required' }}
          </span>
        </div>

        <!-- Forge World -->
        <div class="flex items-center gap-2">
          <svg
            v-if="config.forgeWorldAllowed"
            class="w-5 h-5 text-green-500 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-red-500 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
          <span class="text-sm" :class="config.forgeWorldAllowed ? 'text-green-700 font-medium' : 'text-red-700 font-medium'">
            {{ config.forgeWorldAllowed ? 'Forge World Allowed' : 'Forge World Not Allowed' }}
          </span>
        </div>

        <!-- Legends -->
        <div class="flex items-center gap-2">
          <svg
            v-if="config.legendsAllowed"
            class="w-5 h-5 text-green-500 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-red-500 flex-shrink-0"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
          <span class="text-sm" :class="config.legendsAllowed ? 'text-green-700 font-medium' : 'text-red-700 font-medium'">
            {{ config.legendsAllowed ? 'Legends Allowed' : 'Legends Not Allowed' }}
          </span>
        </div>
      </div>

      <!-- Proxy Policy -->
      <div class="flex items-start gap-3">
        <div
          class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
          :class="config.allowProxies ? 'bg-green-100' : 'bg-red-100'"
        >
          <svg
            v-if="config.allowProxies"
            class="w-5 h-5 text-green-600"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
          </svg>
          <svg
            v-else
            class="w-5 h-5 text-red-600"
            viewBox="0 0 24 24"
            fill="currentColor"
          >
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
        </div>
        <div>
          <p class="font-medium text-gray-900">
            {{ config.allowProxies ? 'Proxies Allowed' : 'No Proxy Models' }}
          </p>
          <p v-if="config.allowProxies && config.proxyNotes" class="text-sm text-gray-600">
            {{ config.proxyNotes }}
          </p>
          <p v-else-if="!config.allowProxies" class="text-sm text-gray-600">
            All models must be official Games Workshop miniatures
          </p>
        </div>
      </div>

      <!-- Army Rules Notes -->
      <div v-if="config.armyRulesNotes" class="bg-gray-50 rounded-lg p-3">
        <p class="text-sm font-medium text-gray-900 mb-1">Additional Army Rules</p>
        <p class="text-sm text-gray-700 whitespace-pre-wrap">{{ config.armyRulesNotes }}</p>
      </div>
    </div>
  </div>
</template>
