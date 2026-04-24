<script setup lang="ts">
defineProps<{
  modelValue: string | null | undefined
  required?: boolean
  disabled?: boolean
  placeholder?: string
}>()

defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const usStates = [
  'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
  'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
  'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
  'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
  'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY',
]
</script>

<template>
  <select
    :value="modelValue || ''"
    class="input"
    :required="required"
    :disabled="disabled"
    @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
  >
    <option value="">{{ placeholder || 'Select state' }}</option>
    <option v-for="state in usStates" :key="state" :value="state">
      {{ state }}
    </option>
  </select>
</template>
