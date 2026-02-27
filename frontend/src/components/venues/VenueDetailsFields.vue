<script setup lang="ts">
defineProps<{
  hall?: string | null
  room?: string | null
  table?: string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:hall', value: string | null): void
  (e: 'update:room', value: string | null): void
  (e: 'update:table', value: string | null): void
}>()

function handleHallInput(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  emit('update:hall', value || null)
}

function handleRoomInput(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  emit('update:room', value || null)
}

function handleTableInput(event: Event) {
  const value = (event.target as HTMLInputElement).value.trim()
  emit('update:table', value || null)
}
</script>

<template>
  <div class="grid grid-cols-3 gap-3">
    <div>
      <label class="label">Hall</label>
      <input
        :value="hall ?? ''"
        type="text"
        class="input"
        placeholder="e.g., Hall A"
        :disabled="disabled"
        maxlength="100"
        @input="handleHallInput"
      />
    </div>
    <div>
      <label class="label">Room</label>
      <input
        :value="room ?? ''"
        type="text"
        class="input"
        placeholder="e.g., Room 101"
        :disabled="disabled"
        maxlength="100"
        @input="handleRoomInput"
      />
    </div>
    <div>
      <label class="label">Table</label>
      <input
        :value="table ?? ''"
        type="text"
        class="input"
        placeholder="e.g., T-42"
        :disabled="disabled"
        maxlength="50"
        @input="handleTableInput"
      />
    </div>
  </div>
</template>
