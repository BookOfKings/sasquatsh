<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ImportDeckInput } from '@/types/mtg'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'import', input: ImportDeckInput): void
}>()

type ImportSource = 'text' | 'moxfield' | 'archidekt'

const importSource = ref<ImportSource>('text')
const deckName = ref('')
const deckList = ref('')
const importUrl = ref('')
const formatId = ref<string | null>(null)
const importing = ref(false)
const error = ref<string | null>(null)

// Deck list examples
const exampleDeckList = `// Commander
1 Atraxa, Praetors' Voice

// Creatures
4 Birds of Paradise
4 Noble Hierarch
2 Vorinclex, Voice of Hunger

// Instants
4 Path to Exile
4 Swords to Plowshares

// Sorceries
2 Wrath of God

// Lands
4 Command Tower
4 Breeding Pool
4 Temple Garden
20 Forest

// Sideboard
2 Rest in Peace
2 Stony Silence`

const isValid = computed(() => {
  if (importSource.value === 'text') {
    return deckList.value.trim().length > 0
  }
  return importUrl.value.trim().length > 0
})

function handleSubmit() {
  if (!isValid.value) return

  error.value = null
  importing.value = true

  const input: ImportDeckInput = {
    source: importSource.value,
    name: deckName.value || undefined,
    formatId: formatId.value || undefined,
  }

  if (importSource.value === 'text') {
    input.deckList = deckList.value
  } else {
    input.url = importUrl.value
  }

  emit('import', input)
}

function handleClose() {
  if (!importing.value) {
    emit('close')
  }
}

function loadExample() {
  deckList.value = exampleDeckList
}

function clearForm() {
  deckName.value = ''
  deckList.value = ''
  importUrl.value = ''
  formatId.value = null
  error.value = null
}

// Reset form when modal opens
function onOpen() {
  importing.value = false
  clearForm()
}

// Watch for modal open
import { watch } from 'vue'
watch(() => props.open, (isOpen) => {
  if (isOpen) {
    onOpen()
  }
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click="handleClose"
    >
      <div
        class="bg-white rounded-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto shadow-2xl"
        @click.stop
      >
        <!-- Header -->
        <div class="flex items-center justify-between p-4 border-b">
          <h2 class="text-xl font-bold">Import Deck</h2>
          <button
            class="text-gray-400 hover:text-gray-600"
            :disabled="importing"
            @click="handleClose"
          >
            <svg class="w-6 h-6" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <!-- Content -->
        <div class="p-4 space-y-4">
          <!-- Import Source Tabs -->
          <div class="flex border-b">
            <button
              class="px-4 py-2 font-medium transition-colors"
              :class="importSource === 'text' ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-500 hover:text-gray-700'"
              @click="importSource = 'text'"
            >
              Deck List
            </button>
            <button
              class="px-4 py-2 font-medium transition-colors"
              :class="importSource === 'moxfield' ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-500 hover:text-gray-700'"
              @click="importSource = 'moxfield'"
            >
              Moxfield
            </button>
            <button
              class="px-4 py-2 font-medium transition-colors"
              :class="importSource === 'archidekt' ? 'text-primary-600 border-b-2 border-primary-600' : 'text-gray-500 hover:text-gray-700'"
              @click="importSource = 'archidekt'"
            >
              Archidekt
            </button>
          </div>

          <!-- Deck Name -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Deck Name (optional)
            </label>
            <input
              v-model="deckName"
              type="text"
              class="input"
              placeholder="My Awesome Deck"
              :disabled="importing"
            />
          </div>

          <!-- Text Import -->
          <template v-if="importSource === 'text'">
            <div>
              <div class="flex items-center justify-between mb-1">
                <label class="block text-sm font-medium text-gray-700">
                  Deck List
                </label>
                <button
                  class="text-xs text-primary-600 hover:text-primary-700"
                  @click="loadExample"
                >
                  Load Example
                </button>
              </div>
              <textarea
                v-model="deckList"
                class="input h-64 font-mono text-sm"
                placeholder="4 Lightning Bolt
4 Counterspell
20 Island
20 Mountain

// Sideboard
2 Red Elemental Blast"
                :disabled="importing"
              ></textarea>
              <p class="text-xs text-gray-500 mt-1">
                Supported formats: "4 Card Name", "4x Card Name", "Card Name x4"
              </p>
            </div>
          </template>

          <!-- URL Import -->
          <template v-else>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                {{ importSource === 'moxfield' ? 'Moxfield' : 'Archidekt' }} URL
              </label>
              <input
                v-model="importUrl"
                type="url"
                class="input"
                :placeholder="importSource === 'moxfield' ? 'https://www.moxfield.com/decks/...' : 'https://archidekt.com/decks/...'"
                :disabled="importing"
              />
              <p class="text-xs text-gray-500 mt-1">
                <template v-if="importSource === 'moxfield'">
                  The deck must be public on Moxfield
                </template>
                <template v-else>
                  The deck must be public on Archidekt
                </template>
              </p>
            </div>
          </template>

          <!-- Error Message -->
          <div v-if="error" class="p-3 bg-red-50 text-red-600 rounded-lg text-sm">
            {{ error }}
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-3 p-4 border-t bg-gray-50">
          <button
            class="btn btn-secondary"
            :disabled="importing"
            @click="handleClose"
          >
            Cancel
          </button>
          <button
            class="btn btn-primary"
            :disabled="!isValid || importing"
            @click="handleSubmit"
          >
            <template v-if="importing">
              <svg class="w-4 h-4 animate-spin mr-2" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Importing...
            </template>
            <template v-else>
              Import Deck
            </template>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
