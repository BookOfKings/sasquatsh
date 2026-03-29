<script setup lang="ts">
interface Group {
  id: string
  name: string
}

defineProps<{
  title: string
  description: string
  groupId?: string
  groups: Group[]
  disabled?: boolean
  errors?: {
    title?: string
  }
  titlePlaceholder?: string
  descriptionPlaceholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:title', value: string): void
  (e: 'update:description', value: string): void
  (e: 'update:groupId', value: string | undefined): void
}>()
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-semibold text-gray-900">Basic Information</h3>

    <!-- Group Selector -->
    <div v-if="groups.length > 0">
      <label for="groupId" class="label">Host for Group (optional)</label>
      <select
        id="groupId"
        :value="groupId"
        class="input"
        :disabled="disabled"
        @change="$emit('update:groupId', ($event.target as HTMLSelectElement).value || undefined)"
      >
        <option :value="undefined">No group - personal event</option>
        <option
          v-for="group in groups"
          :key="group.id"
          :value="group.id"
        >
          {{ group.name }}
        </option>
      </select>
      <p class="text-sm text-gray-500 mt-1">
        Group members will see this event on the group page.
      </p>
    </div>

    <!-- Title -->
    <div>
      <label for="title" class="label">Event Title *</label>
      <input
        id="title"
        :value="title"
        type="text"
        class="input"
        :class="{ 'input-error': errors?.title }"
        :placeholder="titlePlaceholder || 'e.g., Friday Night Games'"
        :disabled="disabled"
        @input="$emit('update:title', ($event.target as HTMLInputElement).value)"
      />
      <p v-if="errors?.title" class="text-sm text-red-500 mt-1">{{ errors.title }}</p>
    </div>

    <!-- Description -->
    <div>
      <label for="description" class="label">Description</label>
      <textarea
        id="description"
        :value="description"
        rows="3"
        class="input"
        :placeholder="descriptionPlaceholder || 'Tell people about your event...'"
        :disabled="disabled"
        @input="$emit('update:description', ($event.target as HTMLTextAreaElement).value)"
      />
    </div>

    <!-- Slot for game-specific content (BGG search, etc.) -->
    <slot />
  </div>
</template>
