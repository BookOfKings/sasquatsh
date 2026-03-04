<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useGroupStore } from '@/stores/useGroupStore'
import { useAuthStore } from '@/stores/useAuthStore'
import GroupCard from '@/components/groups/GroupCard.vue'
import AdBanner from '@/components/ads/AdBanner.vue'
import type { GroupSummary, GroupSearchFilter } from '@/types/groups'

const router = useRouter()
const groupStore = useGroupStore()
const auth = useAuthStore()

const showFilters = ref(false)
const searchText = ref('')
const groupType = ref<string | null>(null)
const city = ref('')
const state = ref('')

const groupTypeOptions = [
  { title: 'All Types', value: null },
  { title: 'Local', value: 'geographic' },
  { title: 'Interest', value: 'interest' },
  { title: 'Community', value: 'both' },
]

function buildFilter(): GroupSearchFilter | undefined {
  const filter: GroupSearchFilter = {}
  if (searchText.value.trim()) filter.search = searchText.value.trim()
  if (groupType.value) filter.groupType = groupType.value as GroupSearchFilter['groupType']
  if (city.value.trim()) filter.city = city.value.trim()
  if (state.value.trim()) filter.state = state.value.trim()

  return Object.keys(filter).length > 0 ? filter : undefined
}

async function applyFilters() {
  await groupStore.loadPublicGroups(buildFilter())
}

function clearFilters() {
  searchText.value = ''
  groupType.value = null
  city.value = ''
  state.value = ''
  applyFilters()
}

const hasActiveFilters = () => {
  return searchText.value || groupType.value || city.value || state.value
}

// Debounce search
let searchTimeout: ReturnType<typeof setTimeout> | null = null
watch(searchText, () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    applyFilters()
  }, 400)
})

onMounted(() => {
  groupStore.loadPublicGroups()
})

function handleSelectGroup(group: GroupSummary) {
  router.push(`/groups/${group.slug}`)
}

function goToCreateGroup() {
  router.push('/groups/create')
}
</script>

<template>
  <div class="container-wide py-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Groups</h1>
        <p class="text-gray-500">Find communities and gaming groups near you</p>
      </div>

      <button
        v-if="auth.isAuthenticated.value"
        class="btn-primary"
        @click="goToCreateGroup"
      >
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
        </svg>
        Create Group
      </button>
    </div>

    <!-- Search and Filter Bar -->
    <div class="card p-4 mb-6">
      <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
        <!-- Search -->
        <div class="md:col-span-5 relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
            <path d="M9.5,3A6.5,6.5 0 0,1 16,9.5C16,11.11 15.41,12.59 14.44,13.73L14.71,14H15.5L20.5,19L19,20.5L14,15.5V14.71L13.73,14.44C12.59,15.41 11.11,16 9.5,16A6.5,6.5 0 0,1 3,9.5A6.5,6.5 0 0,1 9.5,3M9.5,5C7,5 5,7 5,9.5C5,12 7,14 9.5,14C12,14 14,12 14,9.5C14,7 12,5 9.5,5Z"/>
          </svg>
          <input
            v-model="searchText"
            type="text"
            class="input pl-10"
            placeholder="Search groups..."
          />
        </div>

        <!-- City -->
        <div class="md:col-span-3 relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
          </svg>
          <input
            v-model="city"
            type="text"
            class="input pl-10"
            placeholder="City"
            @blur="applyFilters"
            @keyup.enter="applyFilters"
          />
        </div>

        <!-- State -->
        <div class="md:col-span-2">
          <input
            v-model="state"
            type="text"
            class="input"
            placeholder="State"
            @blur="applyFilters"
            @keyup.enter="applyFilters"
          />
        </div>

        <!-- Filter toggle -->
        <div class="md:col-span-2">
          <button
            class="btn w-full"
            :class="showFilters ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            @click="showFilters = !showFilters"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M14,12V19.88C14.04,20.18 13.94,20.5 13.71,20.71C13.32,21.1 12.69,21.1 12.3,20.71L10.29,18.7C10.06,18.47 9.96,18.16 10,17.87V12H9.97L4.21,4.62C3.87,4.19 3.95,3.56 4.38,3.22C4.57,3.08 4.78,3 5,3V3H19V3C19.22,3 19.43,3.08 19.62,3.22C20.05,3.56 20.13,4.19 19.79,4.62L14.03,12H14Z"/>
            </svg>
            Filters
            <span
              v-if="hasActiveFilters()"
              class="ml-2 w-2 h-2 rounded-full bg-primary-500"
            />
          </button>
        </div>
      </div>

      <!-- Expanded Filters -->
      <div v-if="showFilters" class="mt-4 pt-4 border-t border-gray-200">
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
          <div>
            <label class="label">Group Type</label>
            <select
              v-model="groupType"
              class="input"
              @change="applyFilters"
            >
              <option
                v-for="option in groupTypeOptions"
                :key="option.value ?? 'all'"
                :value="option.value"
              >
                {{ option.title }}
              </option>
            </select>
          </div>

          <div class="flex items-end">
            <button
              v-if="hasActiveFilters()"
              class="btn-ghost text-secondary-500"
              @click="clearFilters"
            >
              <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
              Clear All Filters
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Results Summary -->
    <div class="flex flex-wrap items-center gap-2 mb-4">
      <span class="text-sm text-gray-500">
        {{ groupStore.publicGroups.value.length }} group{{ groupStore.publicGroups.value.length !== 1 ? 's' : '' }} found
      </span>
    </div>

    <!-- Error Alert -->
    <div v-if="groupStore.error.value" class="alert-error mb-6">
      <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
        <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
      </svg>
      <span class="flex-1">{{ groupStore.error.value }}</span>
    </div>

    <!-- Loading -->
    <div v-if="groupStore.loading.value" class="mb-4">
      <div class="h-1 w-full bg-gray-200 rounded-full overflow-hidden">
        <div class="h-full bg-primary-500 rounded-full animate-pulse" style="width: 60%"></div>
      </div>
    </div>

    <!-- Groups Grid -->
    <div v-if="groupStore.publicGroups.value.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <GroupCard
        v-for="group in groupStore.publicGroups.value"
        :key="group.id"
        :group="group"
        @click="handleSelectGroup"
      />
    </div>

    <!-- Empty State -->
    <div v-else-if="!groupStore.loading.value" class="text-center py-12">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z"/>
      </svg>
      <p class="text-gray-500">
        No groups found. Try adjusting your filters or create a new group!
      </p>
    </div>

    <!-- Ad Banner for free tier users -->
    <AdBanner placement="groups" />
  </div>
</template>
