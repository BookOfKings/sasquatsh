<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGroupStore } from '@/stores/useGroupStore'
import type { CreateGroupInput, GroupType } from '@/types/groups'

const router = useRouter()
const groupStore = useGroupStore()

const loading = ref(false)
const errorMessage = ref('')
const errors = reactive<Record<string, string>>({})

const form = reactive<CreateGroupInput>({
  name: '',
  description: '',
  groupType: 'both',
  locationCity: '',
  locationState: '',
  locationRadiusMiles: 25,
  isPublic: true,
})

const groupTypeOptions: { title: string; value: GroupType }[] = [
  { title: 'Community (Both)', value: 'both' },
  { title: 'Geographic (Local Area)', value: 'geographic' },
  { title: 'Interest-Based', value: 'interest' },
]

function validate(): boolean {
  errors.name = ''

  let valid = true

  if (!form.name.trim()) {
    errors.name = 'Group name is required'
    valid = false
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  errorMessage.value = ''

  const result = await groupStore.createGroup(form)

  if (result.ok && result.group) {
    router.push(`/groups/${result.group.slug}`)
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}

function goBack() {
  router.push('/groups')
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Back Button -->
    <button class="btn-ghost mb-4" @click="goBack">
      <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
        <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
      </svg>
      Back to Groups
    </button>

    <div class="card">
      <!-- Header -->
      <div class="p-6 border-b border-gray-100">
        <h1 class="text-xl font-bold flex items-center gap-2">
          <svg class="w-6 h-6 text-primary-500" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
          </svg>
          Create Group
        </h1>
      </div>

      <div class="p-6">
        <!-- Error Alert -->
        <div v-if="errorMessage" class="alert-error mb-6">
          <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <span class="flex-1">{{ errorMessage }}</span>
          <button @click="errorMessage = ''" class="text-red-600 hover:text-red-800">
            <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
            </svg>
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <!-- Basic Info -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Basic Information</h3>

            <div class="space-y-4">
              <div>
                <label for="name" class="label">Group Name *</label>
                <input
                  id="name"
                  v-model="form.name"
                  type="text"
                  class="input"
                  :class="{ 'input-error': errors.name }"
                  placeholder="e.g., Seattle Board Game Enthusiasts"
                  :disabled="loading"
                />
                <p v-if="errors.name" class="text-sm text-red-500 mt-1">{{ errors.name }}</p>
              </div>

              <div>
                <label for="description" class="label">Description</label>
                <textarea
                  id="description"
                  v-model="form.description"
                  rows="3"
                  class="input"
                  placeholder="Tell people about your group..."
                  :disabled="loading"
                />
              </div>

              <div>
                <label for="groupType" class="label">Group Type</label>
                <select
                  id="groupType"
                  v-model="form.groupType"
                  class="input"
                  :disabled="loading"
                >
                  <option
                    v-for="opt in groupTypeOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.title }}
                  </option>
                </select>
                <p class="text-sm text-gray-500 mt-1">
                  Geographic groups are for local areas, Interest groups are for specific games or themes.
                </p>
              </div>
            </div>
          </div>

          <!-- Location -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Location (Optional)</h3>

            <div class="grid grid-cols-12 gap-4">
              <div class="col-span-6">
                <label for="city" class="label">City</label>
                <input
                  id="city"
                  v-model="form.locationCity"
                  type="text"
                  class="input"
                  placeholder="Seattle"
                  :disabled="loading"
                />
              </div>
              <div class="col-span-3">
                <label for="state" class="label">State</label>
                <input
                  id="state"
                  v-model="form.locationState"
                  type="text"
                  class="input"
                  placeholder="WA"
                  :disabled="loading"
                />
              </div>
              <div class="col-span-3">
                <label for="radius" class="label">Radius (mi)</label>
                <input
                  id="radius"
                  v-model.number="form.locationRadiusMiles"
                  type="number"
                  class="input"
                  min="1"
                  :disabled="loading"
                />
              </div>
            </div>
          </div>

          <!-- Visibility -->
          <div>
            <h3 class="font-semibold text-gray-900 mb-4">Visibility</h3>

            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="form.isPublic"
                type="checkbox"
                class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
                :disabled="loading"
              />
              <span class="text-sm text-gray-700">Public group (anyone can find and join)</span>
            </label>
          </div>

          <!-- Actions -->
          <div class="border-t border-gray-200 pt-6 flex justify-end gap-3">
            <button
              type="button"
              class="btn-ghost"
              :disabled="loading"
              @click="goBack"
            >
              Cancel
            </button>
            <button
              type="submit"
              class="btn-primary"
              :disabled="loading"
            >
              <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Create Group
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
