<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getAllBadges,
  getMyBadges,
  computeMyBadges,
  togglePinBadge,
  type Badge,
  type UserBadge,
} from '@/services/badgesApi'

const auth = useAuthStore()

const allBadges = ref<Badge[]>([])
const myBadges = ref<UserBadge[]>([])
const loading = ref(true)
const computing = ref(false)
const newlyEarned = ref(0)
const activeCategory = ref<string | null>(null)

const earnedBadgeIds = computed(() => new Set(myBadges.value.map(b => b.badge_id)))
const pinnedCount = computed(() => myBadges.value.filter(b => b.is_pinned).length)

const categories = [
  { key: 'hosting', label: 'Hosting', icon: 'M20 25 L20 33 L29 33 L29 25 Z M20 25 L24.5 21 L29 25' },
  { key: 'attendance', label: 'Attendance', icon: 'M19 28 L22 31 L30 23' },
  { key: 'planning', label: 'Planning', icon: 'M19 21 L19 35 L30 35 L30 21 Z M19 25 L30 25' },
  { key: 'social', label: 'Social', icon: 'M12 25 A3 3 0 1 0 12 19 A3 3 0 1 0 12 25 M6 34 Q6 30 12 30 Q18 30 18 34' },
  { key: 'collection', label: 'Collection', icon: 'M19 23 L19 33 L29 33 L29 23 Z' },
  { key: 'game_system', label: 'Game Systems', icon: 'M19 23 L19 33 L29 33 L29 23 Z M22 26 A1 1 0 1 0 22 24 M26 30 A1 1 0 1 0 26 28' },
  { key: 'items', label: 'Items', icon: 'M19 22 L19 34 L29 34 L29 22 Z M19 26 L29 26 M22 22 L22 19 L26 19 L26 22' },
  { key: 'special', label: 'Special', icon: 'M24 19 L26.5 25 L33 25 L28 29 L30 36 L24 32 L18 36 L20 29 L15 25 L21.5 25' },
]

const filteredBadges = computed(() => {
  if (!activeCategory.value) return allBadges.value
  return allBadges.value.filter(b => b.category === activeCategory.value)
})

const tierColors: Record<string, { bg: string; border: string; text: string; label: string }> = {
  bronze: { bg: 'bg-amber-50', border: 'border-amber-300', text: 'text-amber-700', label: 'Bronze' },
  silver: { bg: 'bg-gray-50', border: 'border-gray-300', text: 'text-gray-600', label: 'Silver' },
  gold: { bg: 'bg-yellow-50', border: 'border-yellow-400', text: 'text-yellow-700', label: 'Gold' },
  platinum: { bg: 'bg-emerald-50', border: 'border-emerald-400', text: 'text-emerald-700', label: 'Platinum' },
}

// Stats
const totalBadges = computed(() => allBadges.value.length)
const earnedCount = computed(() => myBadges.value.length)
const progressPercent = computed(() => totalBadges.value ? Math.round((earnedCount.value / totalBadges.value) * 100) : 0)

const categoryStats = computed(() => {
  const stats: Record<string, { total: number; earned: number }> = {}
  for (const badge of allBadges.value) {
    if (!stats[badge.category]) stats[badge.category] = { total: 0, earned: 0 }
    stats[badge.category]!.total++
    if (earnedBadgeIds.value.has(badge.id)) stats[badge.category]!.earned++
  }
  return stats
})

onMounted(async () => {
  try {
    const token = await auth.getIdToken()
    const [badges, earned] = await Promise.all([
      getAllBadges(),
      token ? getMyBadges(token) : Promise.resolve([]),
    ])
    allBadges.value = badges
    myBadges.value = earned
  } catch (err) {
    console.error('Failed to load badges:', err)
  } finally {
    loading.value = false
  }
})

async function refreshBadges() {
  const token = await auth.getIdToken()
  if (!token) return
  computing.value = true
  try {
    const result = await computeMyBadges(token)
    myBadges.value = result.badges
    newlyEarned.value = result.newlyEarned
    if (result.newlyEarned > 0) {
      setTimeout(() => { newlyEarned.value = 0 }, 5000)
    }
  } catch (err) {
    console.error('Failed to compute badges:', err)
  } finally {
    computing.value = false
  }
}

async function handlePin(badgeId: number) {
  const token = await auth.getIdToken()
  if (!token) return
  const badge = myBadges.value.find(b => b.badge_id === badgeId)
  if (!badge) return
  if (!badge.is_pinned && pinnedCount.value >= 3) return

  try {
    const result = await togglePinBadge(token, badgeId)
    badge.is_pinned = result.pinned
  } catch (err) {
    console.error('Failed to toggle pin:', err)
  }
}

function getUserBadgeForId(badgeId: number): UserBadge | undefined {
  return myBadges.value.find(b => b.badge_id === badgeId)
}
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-2">
      <h1 class="text-2xl font-bold text-gray-900">Achievements</h1>
      <button
        v-if="auth.isAuthenticated.value"
        @click="refreshBadges"
        :disabled="computing"
        class="btn-primary text-sm px-4 py-2"
      >
        <svg v-if="computing" class="w-4 h-4 animate-spin mr-1" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        {{ computing ? 'Checking...' : 'Check for New Badges' }}
      </button>
    </div>
    <p class="text-gray-600 mb-6">Earn badges by hosting games, attending events, building your collection, and being active in the community.</p>

    <!-- Newly earned toast -->
    <div
      v-if="newlyEarned > 0"
      class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm font-medium flex items-center gap-2"
    >
      <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
        <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
      </svg>
      You earned {{ newlyEarned }} new badge{{ newlyEarned > 1 ? 's' : '' }}!
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-primary-500" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <template v-else>
      <!-- Overall Progress -->
      <div class="card p-4 mb-6">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm font-medium text-gray-700">Overall Progress</span>
          <span class="text-sm text-gray-500">{{ earnedCount }} / {{ totalBadges }} badges ({{ progressPercent }}%)</span>
        </div>
        <div class="w-full bg-gray-200 rounded-full h-3">
          <div
            class="bg-primary-500 h-3 rounded-full transition-all duration-500"
            :style="{ width: progressPercent + '%' }"
          ></div>
        </div>
      </div>

      <!-- Category Progress Cards -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        <button
          v-for="cat in categories"
          :key="cat.key"
          @click="activeCategory = activeCategory === cat.key ? null : cat.key"
          class="p-3 rounded-lg border transition-all text-left"
          :class="activeCategory === cat.key
            ? 'border-primary-400 bg-primary-50 shadow-sm'
            : 'border-gray-200 bg-white hover:border-gray-300'"
        >
          <div class="text-xs font-medium text-gray-500 mb-1">{{ cat.label }}</div>
          <div class="flex items-center justify-between">
            <span class="text-lg font-bold text-gray-900">
              {{ categoryStats[cat.key]?.earned ?? 0 }}<span class="text-sm font-normal text-gray-400">/{{ categoryStats[cat.key]?.total ?? 0 }}</span>
            </span>
          </div>
          <div class="w-full bg-gray-100 rounded-full h-1.5 mt-1">
            <div
              class="bg-primary-500 h-1.5 rounded-full transition-all"
              :style="{ width: ((categoryStats[cat.key]?.total ?? 0) > 0 ? ((categoryStats[cat.key]?.earned ?? 0) / (categoryStats[cat.key]?.total ?? 1)) * 100 : 0) + '%' }"
            ></div>
          </div>
        </button>
      </div>

      <!-- Filter label -->
      <div v-if="activeCategory" class="flex items-center justify-between mb-4">
        <h2 class="font-semibold text-gray-700">
          {{ categories.find(c => c.key === activeCategory)?.label }} Badges
        </h2>
        <button @click="activeCategory = null" class="text-sm text-primary-600 hover:underline">Show All</button>
      </div>

      <!-- Badge Grid -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="badge in filteredBadges"
          :key="badge.id"
          class="relative border rounded-lg p-4 transition-all"
          :class="earnedBadgeIds.has(badge.id)
            ? `${tierColors[badge.tier]?.bg} ${tierColors[badge.tier]?.border} border-2`
            : 'bg-gray-50 border-gray-200 opacity-60'"
        >
          <div class="flex items-start gap-3">
            <!-- Badge Icon -->
            <div
              class="w-12 h-12 flex-shrink-0"
              :class="!earnedBadgeIds.has(badge.id) ? 'grayscale' : ''"
            >
              <div v-if="badge.icon_svg" v-html="badge.icon_svg" class="w-full h-full"></div>
              <div v-else class="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center">
                <svg class="w-6 h-6 text-gray-400" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>
                </svg>
              </div>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <h3 class="font-medium text-sm truncate">{{ badge.name }}</h3>
                <span
                  class="text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                  :class="tierColors[badge.tier]?.text + ' ' + tierColors[badge.tier]?.bg"
                >
                  {{ tierColors[badge.tier]?.label }}
                </span>
              </div>
              <p class="text-xs text-gray-500 mt-0.5">{{ badge.description }}</p>

              <!-- Earned info -->
              <div v-if="getUserBadgeForId(badge.id)" class="mt-2 flex items-center gap-2">
                <span class="text-[10px] text-gray-400">
                  Earned {{ new Date(getUserBadgeForId(badge.id)!.earned_at).toLocaleDateString() }}
                </span>
                <button
                  @click="handlePin(badge.id)"
                  class="text-[10px] px-1.5 py-0.5 rounded-full transition-colors"
                  :class="getUserBadgeForId(badge.id)!.is_pinned
                    ? 'bg-primary-100 text-primary-700'
                    : 'bg-gray-100 text-gray-500 hover:bg-gray-200'"
                  :title="getUserBadgeForId(badge.id)!.is_pinned ? 'Unpin from avatar' : (pinnedCount >= 3 ? 'Max 3 pinned' : 'Pin to avatar')"
                  :disabled="!getUserBadgeForId(badge.id)!.is_pinned && pinnedCount >= 3"
                >
                  {{ getUserBadgeForId(badge.id)!.is_pinned ? 'Pinned' : 'Pin' }}
                </button>
              </div>

              <!-- Locked info -->
              <div v-else class="mt-2">
                <span class="text-[10px] text-gray-400">
                  Requires: {{ badge.requirement_count }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
