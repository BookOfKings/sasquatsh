<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { updateGroup } from '@/services/groupsApi'
import { uploadImage } from '@/services/firebase'
import type { Group, GroupType, JoinPolicy } from '@/types/groups'

const props = defineProps<{
  group: Group
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const auth = useAuthStore()

const form = reactive({
  name: props.group.name,
  description: props.group.description || '',
  groupType: props.group.groupType as GroupType,
  locationCity: props.group.locationCity || '',
  locationState: props.group.locationState || '',
  locationRadiusMiles: props.group.locationRadiusMiles || null,
  joinPolicy: props.group.joinPolicy as JoinPolicy,
  logoUrl: props.group.logoUrl || '',
})

const joinPolicyOptions: { title: string; value: JoinPolicy; description: string }[] = [
  { title: 'Open', value: 'open', description: 'Anyone can join instantly' },
  { title: 'Request to Join', value: 'request', description: 'Members must be approved' },
  { title: 'Invitation Only', value: 'invite_only', description: 'Invite link required' },
]

const saving = ref(false)
const error = ref('')
const uploadingLogo = ref(false)
const logoPreview = ref<string | null>(props.group.logoUrl)
const logoFile = ref<File | null>(null)

const hasChanges = computed(() => {
  return (
    form.name !== props.group.name ||
    form.description !== (props.group.description || '') ||
    form.groupType !== props.group.groupType ||
    form.locationCity !== (props.group.locationCity || '') ||
    form.locationState !== (props.group.locationState || '') ||
    form.locationRadiusMiles !== props.group.locationRadiusMiles ||
    form.joinPolicy !== props.group.joinPolicy ||
    logoFile.value !== null
  )
})

function handleLogoSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    const file = input.files[0]

    // Validate file type
    if (!file.type.startsWith('image/')) {
      error.value = 'Please select an image file'
      return
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      error.value = 'Image must be less than 5MB'
      return
    }

    logoFile.value = file
    logoPreview.value = URL.createObjectURL(file)
    error.value = ''
  }
}

function removeLogo() {
  logoFile.value = null
  logoPreview.value = null
  form.logoUrl = ''
}

async function handleSave() {
  if (!form.name.trim()) {
    error.value = 'Group name is required'
    return
  }

  saving.value = true
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      error.value = 'Not authenticated'
      saving.value = false
      return
    }

    let logoUrl = form.logoUrl

    // Upload new logo if selected
    if (logoFile.value) {
      uploadingLogo.value = true
      const path = `groups/${props.group.id}/logo-${Date.now()}`
      logoUrl = await uploadImage(logoFile.value, path)
      uploadingLogo.value = false
    }

    await updateGroup(token, props.group.id, {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      groupType: form.groupType,
      locationCity: form.locationCity.trim() || undefined,
      locationState: form.locationState.trim() || undefined,
      locationRadiusMiles: form.locationRadiusMiles || undefined,
      joinPolicy: form.joinPolicy,
      logoUrl: logoUrl || null,
    })

    emit('saved')
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to update group'
  }

  saving.value = false
}

const usStates = [
  'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
  'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
  'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
  'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
  'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
]
</script>

<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" @click.self="emit('close')">
    <div class="card p-6 max-w-lg w-full max-h-[90vh] overflow-y-auto">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold">Edit Group</h2>
        <button @click="emit('close')" class="p-2 text-gray-400 hover:text-gray-600 rounded-lg">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
        </button>
      </div>

      <!-- Error -->
      <div v-if="error" class="alert-error mb-4">
        {{ error }}
      </div>

      <form @submit.prevent="handleSave" class="space-y-4">
        <!-- Logo Upload -->
        <div>
          <label class="label">Group Logo</label>
          <div class="flex items-center gap-4">
            <div class="w-20 h-20 rounded-xl bg-gray-100 flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                v-if="logoPreview"
                :src="logoPreview"
                class="w-full h-full object-cover"
              />
              <svg v-else class="w-10 h-10 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
              </svg>
            </div>
            <div class="flex-1">
              <input
                type="file"
                accept="image/*"
                class="hidden"
                id="logo-upload"
                @change="handleLogoSelect"
              />
              <label
                for="logo-upload"
                class="btn-outline cursor-pointer inline-block text-sm"
              >
                {{ logoPreview ? 'Change' : 'Upload' }} Logo
              </label>
              <button
                v-if="logoPreview"
                type="button"
                @click="removeLogo"
                class="ml-2 text-sm text-red-500 hover:text-red-600"
              >
                Remove
              </button>
              <p class="text-xs text-gray-500 mt-1">PNG, JPG up to 5MB</p>
            </div>
          </div>
        </div>

        <!-- Name -->
        <div>
          <label for="name" class="label">Group Name</label>
          <input
            id="name"
            v-model="form.name"
            type="text"
            class="input"
            placeholder="Enter group name"
            :disabled="saving"
          />
        </div>

        <!-- Description -->
        <div>
          <label for="description" class="label">Description</label>
          <textarea
            id="description"
            v-model="form.description"
            class="input"
            rows="3"
            placeholder="Describe your group..."
            :disabled="saving"
          ></textarea>
        </div>

        <!-- Group Type -->
        <div>
          <label for="groupType" class="label">Group Type</label>
          <select id="groupType" v-model="form.groupType" class="input" :disabled="saving">
            <option value="geographic">Local Community (Geographic)</option>
            <option value="interest">Interest Group</option>
            <option value="both">Both</option>
          </select>
        </div>

        <!-- Location -->
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label for="city" class="label">City</label>
            <input
              id="city"
              v-model="form.locationCity"
              type="text"
              class="input"
              placeholder="City"
              :disabled="saving"
            />
          </div>
          <div>
            <label for="state" class="label">State</label>
            <select id="state" v-model="form.locationState" class="input" :disabled="saving">
              <option value="">Select state</option>
              <option v-for="state in usStates" :key="state" :value="state">{{ state }}</option>
            </select>
          </div>
        </div>

        <!-- Radius -->
        <div>
          <label for="radius" class="label">Radius (miles)</label>
          <input
            id="radius"
            v-model.number="form.locationRadiusMiles"
            type="number"
            min="1"
            max="500"
            class="input"
            placeholder="e.g., 25"
            :disabled="saving"
          />
        </div>

        <!-- Join Policy -->
        <div>
          <label class="label">How Can People Join?</label>
          <div class="space-y-2">
            <label
              v-for="opt in joinPolicyOptions"
              :key="opt.value"
              class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors"
              :class="form.joinPolicy === opt.value ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:bg-gray-50'"
            >
              <input
                v-model="form.joinPolicy"
                type="radio"
                :value="opt.value"
                class="w-4 h-4 text-primary-500 focus:ring-primary-500"
                :disabled="saving"
              />
              <div>
                <div class="font-medium text-gray-900 text-sm">{{ opt.title }}</div>
                <div class="text-xs text-gray-500">{{ opt.description }}</div>
              </div>
            </label>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex gap-3 pt-4">
          <button
            type="button"
            @click="emit('close')"
            class="btn-outline flex-1"
            :disabled="saving"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary flex-1"
            :disabled="saving || !hasChanges"
          >
            <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ saving ? (uploadingLogo ? 'Uploading...' : 'Saving...') : 'Save Changes' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
