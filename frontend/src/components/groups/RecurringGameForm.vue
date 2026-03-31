<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { RecurringGame, CreateRecurringGameInput, RecurringFrequency } from '@/types/groups'

const props = defineProps<{
  game?: RecurringGame
  groupId: string
}>()

const emit = defineEmits<{
  (e: 'save', data: CreateRecurringGameInput): void
  (e: 'cancel'): void
}>()

const isEditing = computed(() => !!props.game)
const modalTitle = computed(() => isEditing.value ? 'Edit Recurring Game' : 'Create Recurring Game')

// Form state
const title = ref(props.game?.title ?? '')
const description = ref(props.game?.description ?? '')
const frequency = ref<RecurringFrequency>(props.game?.frequency ?? 'weekly')
const dayOfWeek = ref<number | null>(props.game?.dayOfWeek ?? null)
const monthlyWeek = ref<number | null>(props.game?.monthlyWeek ?? null)
const startTime = ref(props.game?.startTime ?? '19:00')
const durationMinutes = ref(props.game?.durationMinutes ?? 120)
const gameSystem = ref(props.game?.gameSystem ?? 'board_game')
const gameTitle = ref(props.game?.gameTitle ?? '')
const locationMode = ref<'group' | 'custom'>(
  props.game?.city || props.game?.addressLine1 ? 'custom' : 'group'
)
const city = ref(props.game?.city ?? '')
const state = ref(props.game?.state ?? '')
const postalCode = ref(props.game?.postalCode ?? '')
const maxPlayers = ref(props.game?.maxPlayers ?? 4)
const hostIsPlaying = ref(props.game?.hostIsPlaying ?? true)
const isPublic = ref(props.game?.isPublic ?? true)

// Validation
const errors = ref<Record<string, string>>({})

const dayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

const frequencyOptions: { value: RecurringFrequency; label: string }[] = [
  { value: 'weekly', label: 'Weekly' },
  { value: 'biweekly', label: 'Every Other Week' },
  { value: 'monthly', label: 'Monthly' },
]

const monthlyWeekOptions = [
  { value: 1, label: '1st' },
  { value: 2, label: '2nd' },
  { value: 3, label: '3rd' },
  { value: 4, label: '4th' },
  { value: -1, label: 'Last' },
]

const gameSystemOptions = [
  { value: 'board_game', label: 'Board Game' },
  { value: 'mtg', label: 'Magic: The Gathering' },
  { value: 'pokemon_tcg', label: 'Pokemon TCG' },
  { value: 'yugioh', label: 'Yu-Gi-Oh!' },
  { value: 'warhammer40k', label: 'Warhammer 40k' },
]

const durationPresets = [
  { label: '1h', value: 60 },
  { label: '2h', value: 120 },
  { label: '3h', value: 180 },
  { label: '4h', value: 240 },
]

// Reset monthlyWeek when frequency changes away from monthly
watch(frequency, (val) => {
  if (val !== 'monthly') {
    monthlyWeek.value = null
  } else if (monthlyWeek.value === null) {
    monthlyWeek.value = 1
  }
})

function validate(): boolean {
  const errs: Record<string, string> = {}
  if (!title.value.trim()) errs.title = 'Title is required'
  if (dayOfWeek.value === null) errs.dayOfWeek = 'Please select a day'
  if (!startTime.value) errs.startTime = 'Start time is required'
  if (frequency.value === 'monthly' && monthlyWeek.value === null) {
    errs.monthlyWeek = 'Please select which week'
  }
  if (locationMode.value === 'custom' && !city.value.trim()) {
    errs.city = 'City is required for custom location'
  }
  errors.value = errs
  return Object.keys(errs).length === 0
}

function handleSave() {
  if (!validate()) return

  const data: CreateRecurringGameInput = {
    title: title.value.trim(),
    description: description.value.trim() || undefined,
    frequency: frequency.value,
    dayOfWeek: dayOfWeek.value!,
    monthlyWeek: frequency.value === 'monthly' ? monthlyWeek.value! : undefined,
    startTime: startTime.value,
    durationMinutes: durationMinutes.value,
    gameSystem: gameSystem.value,
    gameTitle: gameTitle.value.trim() || undefined,
    maxPlayers: maxPlayers.value,
    hostIsPlaying: hostIsPlaying.value,
    isPublic: isPublic.value,
  }

  if (locationMode.value === 'custom') {
    data.city = city.value.trim()
    data.state = state.value.trim() || undefined
    data.postalCode = postalCode.value.trim() || undefined
  }

  emit('save', data)
}
</script>

<template>
  <!-- Modal overlay -->
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="emit('cancel')">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
      <!-- Header -->
      <div class="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 rounded-t-xl">
        <h2 class="text-lg font-semibold text-gray-900">{{ modalTitle }}</h2>
      </div>

      <form class="p-6 space-y-5" @submit.prevent="handleSave">
        <!-- Title -->
        <div>
          <label for="rg-title" class="label">Title <span class="text-red-500">*</span></label>
          <input
            id="rg-title"
            v-model="title"
            type="text"
            class="input"
            placeholder="e.g., Friday Night Magic"
          />
          <p v-if="errors.title" class="text-sm text-red-500 mt-1">{{ errors.title }}</p>
        </div>

        <!-- Description -->
        <div>
          <label for="rg-description" class="label">Description</label>
          <textarea
            id="rg-description"
            v-model="description"
            class="input"
            rows="2"
            placeholder="What's this recurring game about?"
          />
        </div>

        <!-- Frequency -->
        <div>
          <label class="label">Frequency <span class="text-red-500">*</span></label>
          <div class="flex gap-2">
            <button
              v-for="opt in frequencyOptions"
              :key="opt.value"
              type="button"
              :class="frequency === opt.value
                ? 'bg-primary-500 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors flex-1"
              @click="frequency = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <!-- Day of Week -->
        <div>
          <label class="label">Day of Week <span class="text-red-500">*</span></label>
          <div class="flex gap-1.5">
            <button
              v-for="(label, idx) in dayLabels"
              :key="idx"
              type="button"
              :class="dayOfWeek === idx
                ? 'bg-primary-500 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
              class="w-10 h-10 rounded-lg text-sm font-medium transition-colors"
              @click="dayOfWeek = idx"
            >
              {{ label }}
            </button>
          </div>
          <p v-if="errors.dayOfWeek" class="text-sm text-red-500 mt-1">{{ errors.dayOfWeek }}</p>
        </div>

        <!-- Monthly Week (only for monthly) -->
        <div v-if="frequency === 'monthly'">
          <label class="label">Week of Month <span class="text-red-500">*</span></label>
          <div class="flex gap-2">
            <button
              v-for="opt in monthlyWeekOptions"
              :key="opt.value"
              type="button"
              :class="monthlyWeek === opt.value
                ? 'bg-primary-500 text-white'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
              class="px-3 py-2 rounded-lg text-sm font-medium transition-colors flex-1"
              @click="monthlyWeek = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>
          <p v-if="errors.monthlyWeek" class="text-sm text-red-500 mt-1">{{ errors.monthlyWeek }}</p>
        </div>

        <!-- Start Time -->
        <div>
          <label for="rg-time" class="label">Start Time <span class="text-red-500">*</span></label>
          <input
            id="rg-time"
            v-model="startTime"
            type="time"
            class="input"
          />
          <p v-if="errors.startTime" class="text-sm text-red-500 mt-1">{{ errors.startTime }}</p>
        </div>

        <!-- Duration -->
        <div>
          <label for="rg-duration" class="label">Duration (minutes)</label>
          <div class="flex items-center gap-2">
            <input
              id="rg-duration"
              v-model.number="durationMinutes"
              type="number"
              min="30"
              max="720"
              step="15"
              class="input w-24"
            />
            <div class="flex gap-1.5">
              <button
                v-for="preset in durationPresets"
                :key="preset.value"
                type="button"
                :class="durationMinutes === preset.value
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
                class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
                @click="durationMinutes = preset.value"
              >
                {{ preset.label }}
              </button>
            </div>
          </div>
        </div>

        <!-- Game System -->
        <div>
          <label for="rg-system" class="label">Game System</label>
          <select id="rg-system" v-model="gameSystem" class="input">
            <option v-for="opt in gameSystemOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>

        <!-- Game Title -->
        <div>
          <label for="rg-game-title" class="label">Game Title</label>
          <input
            id="rg-game-title"
            v-model="gameTitle"
            type="text"
            class="input"
            placeholder="e.g., Catan, Commander, Standard"
          />
        </div>

        <!-- Location -->
        <div>
          <label class="label">Location</label>
          <div class="flex gap-4 mb-3">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                v-model="locationMode"
                value="group"
                class="w-4 h-4 text-primary-500 focus:ring-primary-500"
              />
              <span class="text-sm text-gray-700">Use group location</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                v-model="locationMode"
                value="custom"
                class="w-4 h-4 text-primary-500 focus:ring-primary-500"
              />
              <span class="text-sm text-gray-700">Custom address</span>
            </label>
          </div>

          <div v-if="locationMode === 'custom'" class="space-y-3">
            <div class="grid grid-cols-12 gap-3">
              <div class="col-span-5">
                <label for="rg-city" class="label">City <span class="text-red-500">*</span></label>
                <input
                  id="rg-city"
                  v-model="city"
                  type="text"
                  class="input"
                />
                <p v-if="errors.city" class="text-sm text-red-500 mt-1">{{ errors.city }}</p>
              </div>
              <div class="col-span-4">
                <label for="rg-state" class="label">State</label>
                <input
                  id="rg-state"
                  v-model="state"
                  type="text"
                  class="input"
                />
              </div>
              <div class="col-span-3">
                <label for="rg-zip" class="label">Zip</label>
                <input
                  id="rg-zip"
                  v-model="postalCode"
                  type="text"
                  class="input"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Max Players -->
        <div>
          <label for="rg-max-players" class="label">Max Players</label>
          <input
            id="rg-max-players"
            v-model.number="maxPlayers"
            type="number"
            min="2"
            max="100"
            class="input w-24"
          />
        </div>

        <!-- Checkboxes -->
        <div class="space-y-3">
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              v-model="hostIsPlaying"
              class="w-4 h-4 rounded text-primary-500 focus:ring-primary-500"
            />
            <span class="text-sm text-gray-700">Host is playing</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              v-model="isPublic"
              class="w-4 h-4 rounded text-primary-500 focus:ring-primary-500"
            />
            <span class="text-sm text-gray-700">Public (visible to non-members)</span>
          </label>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-4 border-t border-gray-200">
          <button
            type="button"
            class="btn-secondary"
            @click="emit('cancel')"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
          >
            {{ isEditing ? 'Save Changes' : 'Create Recurring Game' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
