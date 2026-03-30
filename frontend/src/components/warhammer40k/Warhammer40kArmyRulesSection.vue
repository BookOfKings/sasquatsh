<script setup lang="ts">
defineProps<{
  battleReadyRequired: boolean
  wysiwygRequired: boolean
  forgeWorldAllowed: boolean
  legendsAllowed: boolean
  armyRulesNotes: string | null
  allowProxies: boolean
  proxyNotes: string | null
}>()

const emit = defineEmits<{
  (e: 'update:battleReadyRequired', value: boolean): void
  (e: 'update:wysiwygRequired', value: boolean): void
  (e: 'update:forgeWorldAllowed', value: boolean): void
  (e: 'update:legendsAllowed', value: boolean): void
  (e: 'update:armyRulesNotes', value: string | null): void
  (e: 'update:allowProxies', value: boolean): void
  (e: 'update:proxyNotes', value: string | null): void
}>()

function handleProxiesToggle(checked: boolean) {
  emit('update:allowProxies', checked)
  if (!checked) {
    emit('update:proxyNotes', null)
  }
}
</script>

<template>
  <div class="space-y-6">
    <h3 class="text-lg font-semibold text-gray-900">Army Rules</h3>

    <!-- Painting & Modeling -->
    <div class="space-y-3">
      <span class="block text-sm font-medium text-gray-700">Painting & Modeling</span>

      <label class="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="battleReadyRequired"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="emit('update:battleReadyRequired', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Battle Ready Required</span>
          <p class="text-xs text-gray-500">All models must meet Battle Ready painting standard (3 colors minimum)</p>
        </div>
      </label>

      <label class="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="wysiwygRequired"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="emit('update:wysiwygRequired', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">WYSIWYG Required</span>
          <p class="text-xs text-gray-500">What You See Is What You Get — models must visually represent their wargear</p>
        </div>
      </label>
    </div>

    <!-- Allowed Sources -->
    <div class="space-y-3">
      <span class="block text-sm font-medium text-gray-700">Allowed Sources</span>

      <label class="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="forgeWorldAllowed"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="emit('update:forgeWorldAllowed', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Forge World Allowed</span>
          <p class="text-xs text-gray-500">Forge World / Imperial Armour datasheets permitted</p>
        </div>
      </label>

      <label class="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="legendsAllowed"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="emit('update:legendsAllowed', ($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Legends Allowed</span>
          <p class="text-xs text-gray-500">Warhammer Legends datasheets permitted</p>
        </div>
      </label>
    </div>

    <!-- Proxy Models -->
    <div class="space-y-3">
      <span class="block text-sm font-medium text-gray-700">Proxy Models</span>

      <label class="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          :checked="allowProxies"
          class="h-4 w-4 mt-0.5 rounded border-gray-300 text-red-600 focus:ring-red-500"
          @change="handleProxiesToggle(($event.target as HTMLInputElement).checked)"
        />
        <div>
          <span class="text-sm font-medium text-gray-700">Allow Proxy Models</span>
          <p class="text-xs text-gray-500">Players may use substitute models to represent units</p>
        </div>
      </label>

      <div v-if="allowProxies" class="ml-7">
        <textarea
          :value="proxyNotes ?? ''"
          class="input w-full"
          rows="2"
          placeholder="e.g., Proxies must be on correct base sizes, clearly labeled..."
          @input="emit('update:proxyNotes', ($event.target as HTMLTextAreaElement).value || null)"
        />
        <p class="text-xs text-gray-500 mt-1">Specify any proxy guidelines or restrictions.</p>
      </div>
    </div>

    <!-- Additional Army Rules -->
    <div class="space-y-2">
      <label class="block text-sm font-medium text-gray-700">Additional Army Rules</label>
      <textarea
        :value="armyRulesNotes ?? ''"
        class="input w-full"
        rows="3"
        placeholder="Any additional army building restrictions, house rules, or notes..."
        @input="emit('update:armyRulesNotes', ($event.target as HTMLTextAreaElement).value || null)"
      />
    </div>
  </div>
</template>
