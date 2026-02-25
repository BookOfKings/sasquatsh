<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { getGroup, getGroupMembers } from '@/services/groupsApi'
import { createPlanningSession } from '@/services/planningApi'
import type { Group, GroupMember } from '@/types/groups'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const creating = ref(false)
const group = ref<Group | null>(null)
const members = ref<GroupMember[]>([])
const errorMessage = ref('')

const form = reactive({
  title: '',
  description: '',
  responseDeadline: '',
  selectedMemberIds: new Set<string>(),
  proposedDates: [{ date: '', startTime: '19:00' }] as { date: string; startTime: string }[],
})

const slug = computed(() => route.params.slug as string)

const isValid = computed(() => {
  return (
    form.title.trim() &&
    form.responseDeadline &&
    form.selectedMemberIds.size > 0 &&
    form.proposedDates.filter(d => d.date).length >= 2
  )
})

onMounted(async () => {
  await loadGroup()
})

async function loadGroup() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    group.value = await getGroup(slug.value)
    members.value = await getGroupMembers(token, group.value.id)

    // Set default deadline to 3 days from now
    const deadline = new Date()
    deadline.setDate(deadline.getDate() + 3)
    deadline.setHours(23, 59, 0, 0)
    form.responseDeadline = deadline.toISOString().slice(0, 16)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load group'
  } finally {
    loading.value = false
  }
}

function toggleMember(userId: string) {
  if (form.selectedMemberIds.has(userId)) {
    form.selectedMemberIds.delete(userId)
  } else {
    form.selectedMemberIds.add(userId)
  }
}

function selectAllMembers() {
  members.value.forEach(m => form.selectedMemberIds.add(m.userId))
}

function deselectAllMembers() {
  form.selectedMemberIds.clear()
}

function addDate() {
  form.proposedDates.push({ date: '', startTime: '19:00' })
}

function removeDate(index: number) {
  if (form.proposedDates.length > 1) {
    form.proposedDates.splice(index, 1)
  }
}

async function handleSubmit() {
  if (!isValid.value || !group.value) return

  creating.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      router.push('/login')
      return
    }

    const session = await createPlanningSession(token, {
      groupId: group.value.id,
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      responseDeadline: new Date(form.responseDeadline).toISOString(),
      inviteeUserIds: Array.from(form.selectedMemberIds),
      proposedDates: form.proposedDates
        .filter(d => d.date)
        .map(d => ({
          date: d.date,
          startTime: d.startTime || undefined,
        })),
    })

    router.push(`/planning/${session.id}`)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create planning session'
  } finally {
    creating.value = false
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}
</script>

<template>
  <div class="container-narrow py-8">
    <!-- Loading -->
    <div v-if="loading" class="card p-8 text-center">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
      <p class="mt-4 text-gray-500">Loading group...</p>
    </div>

    <!-- Error -->
    <div v-else-if="errorMessage && !group" class="alert-error">
      {{ errorMessage }}
    </div>

    <!-- Form -->
    <template v-else-if="group">
      <div class="mb-6">
        <button
          class="text-sm text-gray-500 hover:text-gray-700 flex items-center gap-1"
          @click="router.push(`/groups/${slug}`)"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,11V13H8L13.5,18.5L12.08,19.92L4.16,12L12.08,4.08L13.5,5.5L8,11H20Z"/>
          </svg>
          Back to {{ group.name }}
        </button>
      </div>

      <h1 class="text-2xl font-bold mb-6">Plan a Game Night</h1>

      <div v-if="errorMessage" class="alert-error mb-6">
        {{ errorMessage }}
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-6">
        <!-- Basic Info -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Basic Info</h2>
          <div class="space-y-4">
            <div>
              <label for="title" class="label">Title *</label>
              <input
                id="title"
                v-model="form.title"
                type="text"
                class="input"
                placeholder="e.g., Friday Game Night"
                required
              />
            </div>

            <div>
              <label for="description" class="label">Description</label>
              <textarea
                id="description"
                v-model="form.description"
                class="input"
                rows="2"
                placeholder="Optional details about this game night..."
              ></textarea>
            </div>

            <div>
              <label for="deadline" class="label">Response Deadline *</label>
              <input
                id="deadline"
                v-model="form.responseDeadline"
                type="datetime-local"
                class="input"
                required
              />
              <p class="text-sm text-gray-500 mt-1">Members have until this time to submit their availability</p>
            </div>
          </div>
        </div>

        <!-- Select Members -->
        <div class="card p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="font-semibold">Invite Members *</h2>
            <div class="flex gap-2">
              <button type="button" class="text-sm text-primary-500 hover:text-primary-600" @click="selectAllMembers">
                Select All
              </button>
              <span class="text-gray-300">|</span>
              <button type="button" class="text-sm text-gray-500 hover:text-gray-600" @click="deselectAllMembers">
                Deselect All
              </button>
            </div>
          </div>

          <div v-if="members.length === 0" class="text-gray-500 text-center py-4">
            No members in this group yet.
          </div>

          <div v-else class="space-y-2 max-h-64 overflow-y-auto">
            <label
              v-for="member in members"
              :key="member.userId"
              class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer"
              :class="{ 'bg-primary-50': form.selectedMemberIds.has(member.userId) }"
            >
              <input
                type="checkbox"
                :checked="form.selectedMemberIds.has(member.userId)"
                class="w-5 h-5 text-primary-500 rounded border-gray-300 focus:ring-primary-500"
                @change="toggleMember(member.userId)"
              />
              <div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden flex-shrink-0">
                <img
                  v-if="member.avatarUrl"
                  :src="member.avatarUrl"
                  class="w-full h-full object-cover"
                />
                <svg v-else class="w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-medium">{{ member.displayName || 'Unknown' }}</div>
                <div class="text-sm text-gray-500 capitalize">{{ member.role }}</div>
              </div>
            </label>
          </div>

          <p class="text-sm text-gray-500 mt-3">
            {{ form.selectedMemberIds.size }} member{{ form.selectedMemberIds.size === 1 ? '' : 's' }} selected
          </p>
        </div>

        <!-- Propose Dates -->
        <div class="card p-6">
          <h2 class="font-semibold mb-4">Propose Dates *</h2>
          <p class="text-sm text-gray-500 mb-4">Add at least 2 potential dates for the game night</p>

          <div class="space-y-3">
            <div
              v-for="(dateOption, index) in form.proposedDates"
              :key="index"
              class="flex items-center gap-3"
            >
              <div class="flex-1">
                <input
                  v-model="dateOption.date"
                  type="date"
                  class="input"
                  :min="new Date().toISOString().split('T')[0]"
                />
              </div>
              <div class="w-32">
                <input
                  v-model="dateOption.startTime"
                  type="time"
                  class="input"
                />
              </div>
              <button
                v-if="form.proposedDates.length > 1"
                type="button"
                class="p-2 text-gray-400 hover:text-red-500"
                @click="removeDate(index)"
              >
                <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
                </svg>
              </button>
              <div v-else class="w-9"></div>
            </div>
          </div>

          <button
            type="button"
            class="btn-outline mt-4"
            @click="addDate"
          >
            <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Date Option
          </button>

          <div v-if="form.proposedDates.filter(d => d.date).length < 2" class="text-sm text-orange-600 mt-3">
            Please add at least 2 date options
          </div>
        </div>

        <!-- Preview -->
        <div v-if="form.proposedDates.some(d => d.date)" class="card p-6 bg-gray-50">
          <h2 class="font-semibold mb-3">Preview</h2>
          <div class="space-y-2">
            <div v-for="(dateOption, index) in form.proposedDates.filter(d => d.date)" :key="index" class="flex items-center gap-2 text-sm">
              <svg class="w-4 h-4 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1"/>
              </svg>
              <span>{{ formatDate(dateOption.date) }}</span>
              <span v-if="dateOption.startTime" class="text-gray-500">at {{ dateOption.startTime }}</span>
            </div>
          </div>
        </div>

        <!-- Submit -->
        <div class="flex justify-end gap-3">
          <button
            type="button"
            class="btn-ghost"
            @click="router.push(`/groups/${slug}`)"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            :disabled="!isValid || creating"
          >
            <svg v-if="creating" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Create Planning Session
          </button>
        </div>
      </form>
    </template>
  </div>
</template>
