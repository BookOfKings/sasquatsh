<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getAllLocations,
  createLocation,
  updateLocation,
  deleteLocation,
  mergeLocations,
  getBggCacheStats,
  importPopularGames,
  importHotGames,
  refreshStaleCache,
  getAdminDashboard,
  getAdminUsers,
  suspendUser,
  unsuspendUser,
  updateUser,
  deleteUser,
  sendPasswordReset,
  getAdminGroups,
  deleteGroup,
  getAdminNotes,
  createAdminNote,
  updateAdminNote,
  deleteAdminNote,
  getAdminBugs,
  createAdminBug,
  updateAdminBug,
  deleteAdminBug,
} from '@/services/adminApi'
import type { EventLocation } from '@/types/social'
import type { BggCacheStats, AdminStats, ServiceHealth, AdminUser, AdminGroup, AdminNote, AdminBug } from '@/services/adminApi'

const auth = useAuthStore()

const activeTab = ref<'dashboard' | 'users' | 'groups' | 'notes' | 'bugs' | 'locations' | 'bggCache'>('dashboard')

// Dashboard state
const dashboardLoading = ref(false)
const dashboardStats = ref<AdminStats | null>(null)
const serviceHealth = ref<ServiceHealth[]>([])
const lastRefresh = ref<Date | null>(null)

const loading = ref(true)
const locations = ref<EventLocation[]>([])
const errorMessage = ref('')
const successMessage = ref('')

// BGG Cache state
const cacheStats = ref<BggCacheStats | null>(null)
const cacheLoading = ref(false)
const cacheImporting = ref(false)

// User management state
const users = ref<AdminUser[]>([])
const usersTotal = ref(0)
const usersPage = ref(1)
const usersLoading = ref(false)
const userSearch = ref('')
const showSuspendedOnly = ref(false)
const suspendingUserId = ref<string | null>(null)
const suspendReason = ref('')
const showSuspendDialog = ref(false)
const userToSuspend = ref<AdminUser | null>(null)
const showUserEditDialog = ref(false)
const editingUser = ref<AdminUser | null>(null)
const userEditForm = reactive({
  displayName: '',
  username: '',
  isAdmin: false,
})
const savingUser = ref(false)
const deletingUserId = ref<string | null>(null)
const sendingPasswordReset = ref<string | null>(null)

// Group management state
const groups = ref<AdminGroup[]>([])
const groupsTotal = ref(0)
const groupsPage = ref(1)
const groupsLoading = ref(false)
const groupSearch = ref('')
const deletingGroupId = ref<string | null>(null)

// Notes state
const notes = ref<AdminNote[]>([])
const notesLoading = ref(false)
const showNoteDialog = ref(false)
const editingNote = ref<AdminNote | null>(null)
const noteForm = reactive({
  title: '',
  content: '',
  category: 'general',
})
const savingNote = ref(false)
const deletingNoteId = ref<string | null>(null)

// Bugs state
const bugs = ref<AdminBug[]>([])
const bugsLoading = ref(false)
const bugStatusFilter = ref('')
const bugPriorityFilter = ref('')
const showBugDialog = ref(false)
const editingBug = ref<AdminBug | null>(null)
const bugForm = reactive({
  title: '',
  description: '',
  stepsToReproduce: '',
  priority: 'medium' as 'low' | 'medium' | 'high' | 'critical',
  status: 'open' as 'open' | 'in_progress' | 'resolved' | 'closed' | 'wont_fix',
})
const savingBug = ref(false)
const deletingBugId = ref<string | null>(null)

// Create/Edit dialog
const showDialog = ref(false)
const editingLocation = ref<EventLocation | null>(null)
const saving = ref(false)
const form = reactive({
  name: '',
  city: '',
  state: '',
  venue: '',
  startDate: '',
  endDate: '',
})

// Merge mode
const mergeMode = ref(false)
const selectedForMerge = ref<string[]>([])

onMounted(async () => {
  await loadDashboard()
  await loadLocations()
  await loadCacheStats()
})

async function loadDashboard() {
  dashboardLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const data = await getAdminDashboard(token)
    dashboardStats.value = data.stats
    serviceHealth.value = data.services
    lastRefresh.value = new Date()
  } catch (err) {
    console.error('Failed to load dashboard:', err)
  } finally {
    dashboardLoading.value = false
  }
}

function getHealthStatusColor(status: 'healthy' | 'degraded' | 'unhealthy'): string {
  switch (status) {
    case 'healthy': return 'bg-green-100 text-green-800'
    case 'degraded': return 'bg-yellow-100 text-yellow-800'
    case 'unhealthy': return 'bg-red-100 text-red-800'
  }
}

function getHealthDotColor(status: 'healthy' | 'degraded' | 'unhealthy'): string {
  switch (status) {
    case 'healthy': return 'bg-green-500'
    case 'degraded': return 'bg-yellow-500'
    case 'unhealthy': return 'bg-red-500'
  }
}

async function loadCacheStats() {
  cacheLoading.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return
    cacheStats.value = await getBggCacheStats(token)
  } catch (err) {
    console.error('Failed to load cache stats:', err)
  } finally {
    cacheLoading.value = false
  }
}

async function handleImportPopular() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await importPopularGames(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to import games'
  } finally {
    cacheImporting.value = false
  }
}

async function handleImportHot() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await importHotGames(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to import hot games'
  } finally {
    cacheImporting.value = false
  }
}

async function handleRefreshStale() {
  cacheImporting.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await refreshStaleCache(token)
    successMessage.value = result.message
    await loadCacheStats()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to refresh cache'
  } finally {
    cacheImporting.value = false
  }
}

function formatCacheDate(dateStr: string | null): string {
  if (!dateStr) return 'Never'
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

// User Management Functions
async function loadUsers() {
  usersLoading.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminUsers(token, {
      search: userSearch.value || undefined,
      suspended: showSuspendedOnly.value || undefined,
      page: usersPage.value,
      limit: 20,
    })
    users.value = result.users
    usersTotal.value = result.total
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load users'
  } finally {
    usersLoading.value = false
  }
}

function openSuspendDialog(user: AdminUser) {
  userToSuspend.value = user
  suspendReason.value = ''
  showSuspendDialog.value = true
}

async function handleSuspendUser() {
  if (!userToSuspend.value) return

  suspendingUserId.value = userToSuspend.value.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await suspendUser(token, userToSuspend.value.id, suspendReason.value || undefined)
    successMessage.value = `User @${userToSuspend.value.username} suspended`
    showSuspendDialog.value = false
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to suspend user'
  } finally {
    suspendingUserId.value = null
  }
}

async function handleUnsuspendUser(user: AdminUser) {
  if (!confirm(`Unsuspend @${user.username}?`)) return

  suspendingUserId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await unsuspendUser(token, user.id)
    successMessage.value = `User @${user.username} unsuspended`
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to unsuspend user'
  } finally {
    suspendingUserId.value = null
  }
}

function openUserEditDialog(user: AdminUser) {
  editingUser.value = user
  userEditForm.displayName = user.displayName || ''
  userEditForm.username = user.username
  userEditForm.isAdmin = user.isAdmin
  showUserEditDialog.value = true
}

async function handleSaveUser() {
  if (!editingUser.value) return
  if (!userEditForm.username.trim()) {
    errorMessage.value = 'Username is required'
    return
  }

  savingUser.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await updateUser(token, editingUser.value.id, {
      displayName: userEditForm.displayName.trim() || undefined,
      username: userEditForm.username.trim(),
      isAdmin: userEditForm.isAdmin,
    })
    successMessage.value = `User @${userEditForm.username} updated`
    showUserEditDialog.value = false
    await loadUsers()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update user'
  } finally {
    savingUser.value = false
  }
}

async function handleDeleteUser(user: AdminUser) {
  if (!confirm(`DELETE user @${user.username}?\n\nThis will permanently delete:\n- All their group memberships\n- All their event registrations\n- All their LFP posts\n- All their planning session data\n\nThis action CANNOT be undone.`)) return
  if (!confirm(`Are you ABSOLUTELY SURE you want to delete @${user.username}? Type "delete" in the next prompt would be safer, but this is a second confirmation.`)) return

  deletingUserId.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await deleteUser(token, user.id)
    successMessage.value = result.message
    await loadUsers()
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete user'
  } finally {
    deletingUserId.value = null
  }
}

async function handleSendPasswordReset(user: AdminUser) {
  if (!confirm(`Send password reset email to ${user.email}?\n\nNote: This will only work if the user signed up with email/password. OAuth users (Google, etc.) cannot reset their password this way.`)) return

  sendingPasswordReset.value = user.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await sendPasswordReset(token, user.id)
    successMessage.value = result.message
    setTimeout(() => successMessage.value = '', 5000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to send password reset'
  } finally {
    sendingPasswordReset.value = null
  }
}

let userSearchTimeout: ReturnType<typeof setTimeout> | null = null
function handleUserSearch() {
  if (userSearchTimeout) clearTimeout(userSearchTimeout)
  userSearchTimeout = setTimeout(() => {
    usersPage.value = 1
    loadUsers()
  }, 300)
}

// Group Management Functions
async function loadGroups() {
  groupsLoading.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminGroups(token, {
      search: groupSearch.value || undefined,
      page: groupsPage.value,
      limit: 20,
    })
    groups.value = result.groups
    groupsTotal.value = result.total
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load groups'
  } finally {
    groupsLoading.value = false
  }
}

async function handleDeleteGroup(group: AdminGroup) {
  if (!confirm(`Delete group "${group.name}"? This will remove all members and cannot be undone.`)) return

  deletingGroupId.value = group.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteGroup(token, group.id)
    successMessage.value = `Group "${group.name}" deleted`
    await loadGroups()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete group'
  } finally {
    deletingGroupId.value = null
  }
}

let groupSearchTimeout: ReturnType<typeof setTimeout> | null = null
function handleGroupSearch() {
  if (groupSearchTimeout) clearTimeout(groupSearchTimeout)
  groupSearchTimeout = setTimeout(() => {
    groupsPage.value = 1
    loadGroups()
  }, 300)
}

// ============ Notes Functions ============

async function loadNotes() {
  notesLoading.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminNotes(token)
    notes.value = result.notes
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load notes'
  } finally {
    notesLoading.value = false
  }
}

function openNoteDialog(note?: AdminNote) {
  if (note) {
    editingNote.value = note
    noteForm.title = note.title
    noteForm.content = note.content
    noteForm.category = note.category
  } else {
    editingNote.value = null
    noteForm.title = ''
    noteForm.content = ''
    noteForm.category = 'general'
  }
  showNoteDialog.value = true
}

async function handleSaveNote() {
  if (!noteForm.title.trim() || !noteForm.content.trim()) {
    errorMessage.value = 'Title and content are required'
    return
  }

  savingNote.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (editingNote.value) {
      await updateAdminNote(token, editingNote.value.id, {
        title: noteForm.title.trim(),
        content: noteForm.content.trim(),
        category: noteForm.category.trim(),
      })
      successMessage.value = 'Note updated'
    } else {
      await createAdminNote(token, {
        title: noteForm.title.trim(),
        content: noteForm.content.trim(),
        category: noteForm.category.trim(),
      })
      successMessage.value = 'Note created'
    }

    showNoteDialog.value = false
    await loadNotes()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save note'
  } finally {
    savingNote.value = false
  }
}

async function handleTogglePin(note: AdminNote) {
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await updateAdminNote(token, note.id, { isPinned: !note.isPinned })
    await loadNotes()
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to update note'
  }
}

async function handleDeleteNote(note: AdminNote) {
  if (!confirm(`Delete note "${note.title}"?`)) return

  deletingNoteId.value = note.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteAdminNote(token, note.id)
    successMessage.value = 'Note deleted'
    await loadNotes()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete note'
  } finally {
    deletingNoteId.value = null
  }
}

// ============ Bugs Functions ============

async function loadBugs() {
  bugsLoading.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    const result = await getAdminBugs(token, {
      status: bugStatusFilter.value || undefined,
      priority: bugPriorityFilter.value || undefined,
    })
    bugs.value = result.bugs
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load bugs'
  } finally {
    bugsLoading.value = false
  }
}

function openBugDialog(bug?: AdminBug) {
  if (bug) {
    editingBug.value = bug
    bugForm.title = bug.title
    bugForm.description = bug.description || ''
    bugForm.stepsToReproduce = bug.stepsToReproduce || ''
    bugForm.priority = bug.priority
    bugForm.status = bug.status
  } else {
    editingBug.value = null
    bugForm.title = ''
    bugForm.description = ''
    bugForm.stepsToReproduce = ''
    bugForm.priority = 'medium'
    bugForm.status = 'open'
  }
  showBugDialog.value = true
}

async function handleSaveBug() {
  if (!bugForm.title.trim()) {
    errorMessage.value = 'Title is required'
    return
  }

  savingBug.value = true
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (editingBug.value) {
      await updateAdminBug(token, editingBug.value.id, {
        title: bugForm.title.trim(),
        description: bugForm.description.trim() || undefined,
        stepsToReproduce: bugForm.stepsToReproduce.trim() || undefined,
        priority: bugForm.priority,
        status: bugForm.status,
      })
      successMessage.value = 'Bug updated'
    } else {
      await createAdminBug(token, {
        title: bugForm.title.trim(),
        description: bugForm.description.trim() || undefined,
        stepsToReproduce: bugForm.stepsToReproduce.trim() || undefined,
        priority: bugForm.priority,
      })
      successMessage.value = 'Bug reported'
    }

    showBugDialog.value = false
    await loadBugs()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save bug'
  } finally {
    savingBug.value = false
  }
}

async function handleDeleteBug(bug: AdminBug) {
  if (!confirm(`Delete bug "${bug.title}"?`)) return

  deletingBugId.value = bug.id
  errorMessage.value = ''
  try {
    const token = await auth.getIdToken()
    if (!token) return
    await deleteAdminBug(token, bug.id)
    successMessage.value = 'Bug deleted'
    await loadBugs()
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete bug'
  } finally {
    deletingBugId.value = null
  }
}

function getPriorityColor(priority: string): string {
  switch (priority) {
    case 'critical': return 'bg-red-100 text-red-800'
    case 'high': return 'bg-orange-100 text-orange-800'
    case 'medium': return 'bg-yellow-100 text-yellow-800'
    case 'low': return 'bg-gray-100 text-gray-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

function getStatusColor(status: string): string {
  switch (status) {
    case 'open': return 'bg-blue-100 text-blue-800'
    case 'in_progress': return 'bg-purple-100 text-purple-800'
    case 'resolved': return 'bg-green-100 text-green-800'
    case 'closed': return 'bg-gray-100 text-gray-800'
    case 'wont_fix': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-800'
  }
}

async function loadLocations() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    locations.value = await getAllLocations(token)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to load locations'
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingLocation.value = null
  form.name = ''
  form.city = ''
  form.state = ''
  form.venue = ''
  form.startDate = ''
  form.endDate = ''
  showDialog.value = true
}

function openEditDialog(location: EventLocation) {
  editingLocation.value = location
  form.name = location.name
  form.city = location.city
  form.state = location.state
  form.venue = location.venue || ''
  form.startDate = location.startDate
  form.endDate = location.endDate
  showDialog.value = true
}

async function handleSave() {
  if (!form.name.trim() || !form.city.trim() || !form.state.trim() || !form.startDate || !form.endDate) {
    errorMessage.value = 'Name, city, state, and dates are required'
    return
  }

  saving.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    if (editingLocation.value) {
      // Update existing
      const updated = await updateLocation(token, editingLocation.value.id, {
        name: form.name.trim(),
        city: form.city.trim(),
        state: form.state.trim(),
        venue: form.venue.trim() || undefined,
        startDate: form.startDate,
        endDate: form.endDate,
      })
      const index = locations.value.findIndex(l => l.id === updated.id)
      if (index >= 0) locations.value[index] = updated
      successMessage.value = 'Location updated'
    } else {
      // Create new
      const created = await createLocation(token, {
        name: form.name.trim(),
        city: form.city.trim(),
        state: form.state.trim(),
        venue: form.venue.trim() || undefined,
        startDate: form.startDate,
        endDate: form.endDate,
      })
      locations.value.unshift(created)
      successMessage.value = 'Location created'
    }

    showDialog.value = false
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to save location'
  } finally {
    saving.value = false
  }
}

async function handleDelete(location: EventLocation) {
  if (!confirm(`Delete "${location.name}"? Any LFP posts using this location will have it removed.`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deleteLocation(token, location.id)
    locations.value = locations.value.filter(l => l.id !== location.id)
    successMessage.value = 'Location deleted'
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to delete location'
  }
}

function toggleMergeMode() {
  mergeMode.value = !mergeMode.value
  selectedForMerge.value = []
}

function toggleSelectForMerge(id: string) {
  if (selectedForMerge.value.includes(id)) {
    selectedForMerge.value = selectedForMerge.value.filter(i => i !== id)
  } else {
    selectedForMerge.value.push(id)
  }
}

async function handleMerge() {
  if (selectedForMerge.value.length < 2) {
    errorMessage.value = 'Select at least 2 locations to merge'
    return
  }

  const keepId = selectedForMerge.value[0]!
  const removeIds = selectedForMerge.value.slice(1)
  const keepLocation = locations.value.find(l => l.id === keepId)

  if (!confirm(`Keep "${keepLocation?.name ?? 'selected'}" and merge ${removeIds.length} other location(s) into it? This will update all LFP posts to use the kept location.`)) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await mergeLocations(token, keepId, removeIds)
    locations.value = locations.value.filter(l => !removeIds.includes(l.id))
    selectedForMerge.value = []
    mergeMode.value = false
    successMessage.value = `Merged ${removeIds.length} location(s)`
    setTimeout(() => successMessage.value = '', 3000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to merge locations'
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr + 'T00:00:00')
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  })
}

function isExpired(endDate: string): boolean {
  return new Date(endDate) < new Date()
}
</script>

<template>
  <div class="container-narrow py-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold">Site Administration</h1>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 mb-6 border-b border-gray-200">
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'dashboard'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'dashboard'"
      >
        Dashboard
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'users'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'users'; loadUsers()"
      >
        Users
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'groups'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'groups'; loadGroups()"
      >
        Groups
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'notes'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'notes'; loadNotes()"
      >
        Notes
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'bugs'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'bugs'; loadBugs()"
      >
        Bugs
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'locations'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'locations'"
      >
        Event Locations
      </button>
      <button
        class="px-4 py-2 text-sm font-medium transition-colors -mb-px"
        :class="activeTab === 'bggCache'
          ? 'text-primary-600 border-b-2 border-primary-500'
          : 'text-gray-500 hover:text-gray-700'"
        @click="activeTab = 'bggCache'"
      >
        BGG Cache
      </button>
    </div>

    <!-- Messages -->
    <div v-if="successMessage" class="alert-success mb-6">
      {{ successMessage }}
    </div>
    <div v-if="errorMessage" class="alert-error mb-6">
      {{ errorMessage }}
    </div>

    <!-- Dashboard Tab -->
    <div v-if="activeTab === 'dashboard'">
      <!-- Loading State -->
      <div v-if="dashboardLoading" class="text-center py-12">
        <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>

      <template v-else>
        <!-- Stats Section -->
        <div class="mb-8">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-semibold">Site Statistics</h2>
            <div class="flex items-center gap-3">
              <span v-if="lastRefresh" class="text-sm text-gray-500">
                Last updated: {{ lastRefresh.toLocaleTimeString() }}
              </span>
              <button
                class="btn-outline text-sm"
                :disabled="dashboardLoading"
                @click="loadDashboard"
              >
                Refresh
              </button>
            </div>
          </div>

          <div v-if="dashboardStats" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            <!-- Users -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-blue-100">
                  <svg class="w-6 h-6 text-blue-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.users.total }}</div>
                  <div class="text-sm text-gray-500">Total Users</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                <span class="text-green-600 font-medium">+{{ dashboardStats.users.last7Days }}</span> last 7 days
              </div>
            </div>

            <!-- Groups -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-purple-100">
                  <svg class="w-6 h-6 text-purple-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.groups.total }}</div>
                  <div class="text-sm text-gray-500">Groups</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.groups.public }} public, {{ dashboardStats.groups.private }} private
              </div>
            </div>

            <!-- Events -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-green-100">
                  <svg class="w-6 h-6 text-green-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.89 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.events.total }}</div>
                  <div class="text-sm text-gray-500">Events</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.events.upcoming }} upcoming
              </div>
            </div>

            <!-- Planning Sessions -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-orange-100">
                  <svg class="w-6 h-6 text-orange-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M17,12V3A1,1 0 0,0 16,2H3A1,1 0 0,0 2,3V17L6,13H16A1,1 0 0,0 17,12M21,6H19V15H6V17A1,1 0 0,0 7,18H18L22,22V7A1,1 0 0,0 21,6Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.planningSessions.total }}</div>
                  <div class="text-sm text-gray-500">Planning Sessions</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.planningSessions.open }} open
              </div>
            </div>

            <!-- LFP Requests -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-pink-100">
                  <svg class="w-6 h-6 text-pink-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M15.5,12C18,12 20,14 20,16.5C20,17.38 19.75,18.21 19.31,18.9L22.39,22L21,23.39L17.88,20.32C17.19,20.75 16.37,21 15.5,21C13,21 11,19 11,16.5C11,14 13,12 15.5,12M15.5,14A2.5,2.5 0 0,0 13,16.5A2.5,2.5 0 0,0 15.5,19A2.5,2.5 0 0,0 18,16.5A2.5,2.5 0 0,0 15.5,14M5,3H19C20.1,3 21,3.9 21,5V13.03C20.5,12.23 19.81,11.54 19,11V5H5V19H9.5C9.81,19.75 10.26,20.42 10.81,21H5C3.9,21 3,20.1 3,19V5C3,3.9 3.9,3 5,3M7,7H17V9H7V7M7,11H12.03C11.23,11.5 10.54,12.19 10,13H7V11M7,15H9.17C9.06,15.5 9,16 9,16.5V17H7V15Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.playerRequests.total }}</div>
                  <div class="text-sm text-gray-500">LFP Posts</div>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-gray-100 text-sm text-gray-600">
                {{ dashboardStats.playerRequests.active }} active
              </div>
            </div>

            <!-- BGG Cache -->
            <div class="card p-4">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-lg bg-indigo-100">
                  <svg class="w-6 h-6 text-indigo-600" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M5,3H19A2,2 0 0,1 21,5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V5A2,2 0 0,1 5,3M7,5A2,2 0 0,0 5,7A2,2 0 0,0 7,9A2,2 0 0,0 9,7A2,2 0 0,0 7,5M17,15A2,2 0 0,0 15,17A2,2 0 0,0 17,19A2,2 0 0,0 19,17A2,2 0 0,0 17,15M17,5A2,2 0 0,0 15,7A2,2 0 0,0 17,9A2,2 0 0,0 19,7A2,2 0 0,0 17,5M7,15A2,2 0 0,0 5,17A2,2 0 0,0 7,19A2,2 0 0,0 9,17A2,2 0 0,0 7,15M12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10Z"/>
                  </svg>
                </div>
                <div>
                  <div class="text-2xl font-bold">{{ dashboardStats.bggCache.total }}</div>
                  <div class="text-sm text-gray-500">Cached Games</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Service Health Section -->
        <div>
          <h2 class="text-lg font-semibold mb-4">Service Health</h2>
          <div class="card divide-y divide-gray-100">
            <div
              v-for="service in serviceHealth"
              :key="service.name"
              class="p-4 flex items-center justify-between"
            >
              <div class="flex items-center gap-3">
                <div
                  class="w-3 h-3 rounded-full"
                  :class="getHealthDotColor(service.status)"
                ></div>
                <div>
                  <div class="font-medium">{{ service.name }}</div>
                  <div v-if="service.message" class="text-sm text-gray-500">{{ service.message }}</div>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span
                  v-if="service.latencyMs"
                  class="text-sm text-gray-500"
                >
                  {{ service.latencyMs }}ms
                </span>
                <span
                  class="px-2 py-1 rounded-full text-xs font-medium"
                  :class="getHealthStatusColor(service.status)"
                >
                  {{ service.status }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Users Tab -->
    <div v-if="activeTab === 'users'">
      <div class="mb-6">
        <div class="flex gap-4 items-center">
          <div class="flex-1">
            <input
              v-model="userSearch"
              type="text"
              class="input"
              placeholder="Search by email, username, or display name..."
              @input="handleUserSearch"
            />
          </div>
          <label class="flex items-center gap-2 text-sm">
            <input
              v-model="showSuspendedOnly"
              type="checkbox"
              class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              @change="usersPage = 1; loadUsers()"
            />
            <span>Suspended only</span>
          </label>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="usersLoading" class="text-center py-12">
        <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>

      <!-- User List -->
      <template v-else>
        <div class="text-sm text-gray-500 mb-4">
          {{ usersTotal }} user{{ usersTotal !== 1 ? 's' : '' }} found
        </div>

        <div v-if="users.length === 0" class="text-center py-12 text-gray-500">
          <p>No users found matching your search.</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="user in users"
            :key="user.id"
            class="card p-4 flex items-center gap-4"
            :class="{ 'bg-red-50 border-red-200': user.isSuspended }"
          >
            <!-- Avatar -->
            <div class="flex-shrink-0">
              <img
                v-if="user.avatarUrl"
                :src="user.avatarUrl"
                :alt="user.displayName || user.username"
                class="w-10 h-10 rounded-full object-cover"
              />
              <div
                v-else
                class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 font-medium"
              >
                {{ (user.displayName || user.username || '?').charAt(0).toUpperCase() }}
              </div>
            </div>

            <!-- User info -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-900">
                  {{ user.displayName || user.username }}
                </span>
                <span class="text-gray-500">@{{ user.username }}</span>
                <span v-if="user.isAdmin" class="text-xs px-2 py-0.5 rounded-full bg-purple-100 text-purple-700">
                  Admin
                </span>
                <span v-if="user.isSuspended" class="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                  Suspended
                </span>
              </div>
              <p class="text-sm text-gray-500 truncate">{{ user.email }}</p>
              <p v-if="user.isSuspended && user.suspensionReason" class="text-sm text-red-600 mt-1">
                Reason: {{ user.suspensionReason }}
              </p>
            </div>

            <!-- Created date -->
            <div class="text-sm text-gray-500 flex-shrink-0">
              Joined {{ new Date(user.createdAt).toLocaleDateString() }}
            </div>

            <!-- Actions -->
            <div class="flex-shrink-0 flex gap-1">
              <!-- Edit button -->
              <button
                class="btn-ghost text-gray-600 text-sm"
                title="Edit user"
                @click="openUserEditDialog(user)"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.71,7.04C21.1,6.65 21.1,6 20.71,5.63L18.37,3.29C18,2.9 17.35,2.9 16.96,3.29L15.12,5.12L18.87,8.87M3,17.25V21H6.75L17.81,9.93L14.06,6.18L3,17.25Z"/>
                </svg>
              </button>

              <!-- Password Reset button -->
              <button
                class="btn-ghost text-blue-600 text-sm"
                title="Send password reset email"
                :disabled="sendingPasswordReset === user.id"
                @click="handleSendPasswordReset(user)"
              >
                <svg v-if="sendingPasswordReset === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12.63,2C18.16,2 22.64,6.5 22.64,12C22.64,17.5 18.16,22 12.63,22C9.12,22 6.05,20.18 4.26,17.43L5.84,16.18C7.25,18.47 9.76,20 12.64,20A8,8 0 0,0 20.64,12A8,8 0 0,0 12.64,4C8.56,4 5.2,7.06 4.71,11H7.47L3.73,14.73L0,11H2.69C3.19,5.95 7.45,2 12.63,2M15.59,10.24C16.09,10.25 16.5,10.65 16.5,11.16V15.77C16.5,16.27 16.09,16.69 15.58,16.69H10.05C9.54,16.69 9.13,16.27 9.13,15.77V11.16C9.13,10.65 9.54,10.25 10.04,10.24V9.23C10.04,7.7 11.29,6.46 12.81,6.46C14.34,6.46 15.59,7.7 15.59,9.23V10.24M14.54,9.23C14.54,8.28 13.77,7.5 12.81,7.5C11.86,7.5 11.09,8.28 11.09,9.23V10.24H14.54V9.23Z"/>
                </svg>
              </button>

              <!-- Suspend/Unsuspend button -->
              <button
                v-if="user.isSuspended"
                class="btn-ghost text-green-600 text-sm"
                title="Unsuspend user"
                :disabled="suspendingUserId === user.id"
                @click="handleUnsuspendUser(user)"
              >
                <svg v-if="suspendingUserId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M10,17L5,12L6.41,10.58L10,14.17L17.59,6.58L19,8M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
                </svg>
              </button>
              <button
                v-else-if="!user.isAdmin"
                class="btn-ghost text-yellow-600 text-sm"
                title="Suspend user"
                :disabled="suspendingUserId === user.id"
                @click="openSuspendDialog(user)"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,20C7.59,20 4,16.41 4,12C4,7.59 7.59,4 12,4C16.41,4 20,7.59 20,12C20,16.41 16.41,20 12,20M9,9H15V15H9"/>
                </svg>
              </button>

              <!-- Delete button -->
              <button
                v-if="!user.isAdmin"
                class="btn-ghost text-red-600 text-sm"
                title="Delete user and all data"
                :disabled="deletingUserId === user.id"
                @click="handleDeleteUser(user)"
              >
                <svg v-if="deletingUserId === user.id" class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                <svg v-else class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="usersTotal > 20" class="flex justify-center gap-2 mt-6">
          <button
            class="btn-outline text-sm"
            :disabled="usersPage === 1"
            @click="usersPage--; loadUsers()"
          >
            Previous
          </button>
          <span class="px-4 py-2 text-sm text-gray-500">
            Page {{ usersPage }} of {{ Math.ceil(usersTotal / 20) }}
          </span>
          <button
            class="btn-outline text-sm"
            :disabled="usersPage >= Math.ceil(usersTotal / 20)"
            @click="usersPage++; loadUsers()"
          >
            Next
          </button>
        </div>
      </template>
    </div>

    <!-- Groups Tab -->
    <div v-if="activeTab === 'groups'">
      <div class="mb-6">
        <input
          v-model="groupSearch"
          type="text"
          class="input"
          placeholder="Search by group name or slug..."
          @input="handleGroupSearch"
        />
      </div>

      <!-- Loading -->
      <div v-if="groupsLoading" class="text-center py-12">
        <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>

      <!-- Group List -->
      <template v-else>
        <div class="text-sm text-gray-500 mb-4">
          {{ groupsTotal }} group{{ groupsTotal !== 1 ? 's' : '' }} found
        </div>

        <div v-if="groups.length === 0" class="text-center py-12 text-gray-500">
          <p>No groups found matching your search.</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="group in groups"
            :key="group.id"
            class="card p-4 flex items-center gap-4"
          >
            <!-- Logo -->
            <div class="flex-shrink-0">
              <img
                v-if="group.logoUrl"
                :src="group.logoUrl"
                :alt="group.name"
                class="w-10 h-10 rounded-lg object-cover"
              />
              <div
                v-else
                class="w-10 h-10 rounded-lg bg-gray-200 flex items-center justify-center text-gray-500 font-medium"
              >
                {{ (group.name || 'G').charAt(0).toUpperCase() }}
              </div>
            </div>

            <!-- Group info -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2">
                <span class="font-medium text-gray-900">{{ group.name }}</span>
                <span v-if="group.isPublic" class="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">
                  Public
                </span>
                <span v-else class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                  Private
                </span>
              </div>
              <p class="text-sm text-gray-500">
                /g/{{ group.slug }} &bull; {{ group.memberCount }} member{{ group.memberCount !== 1 ? 's' : '' }}
              </p>
              <p v-if="group.description" class="text-sm text-gray-500 truncate mt-1">
                {{ group.description }}
              </p>
            </div>

            <!-- Created date -->
            <div class="text-sm text-gray-500 flex-shrink-0">
              Created {{ new Date(group.createdAt).toLocaleDateString() }}
            </div>

            <!-- Actions -->
            <div class="flex-shrink-0">
              <button
                class="btn-ghost text-red-600 text-sm"
                :disabled="deletingGroupId === group.id"
                @click="handleDeleteGroup(group)"
              >
                <svg v-if="deletingGroupId === group.id" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Delete
              </button>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="groupsTotal > 20" class="flex justify-center gap-2 mt-6">
          <button
            class="btn-outline text-sm"
            :disabled="groupsPage === 1"
            @click="groupsPage--; loadGroups()"
          >
            Previous
          </button>
          <span class="px-4 py-2 text-sm text-gray-500">
            Page {{ groupsPage }} of {{ Math.ceil(groupsTotal / 20) }}
          </span>
          <button
            class="btn-outline text-sm"
            :disabled="groupsPage >= Math.ceil(groupsTotal / 20)"
            @click="groupsPage++; loadGroups()"
          >
            Next
          </button>
        </div>
      </template>
    </div>

    <!-- Notes Tab -->
    <div v-if="activeTab === 'notes'">
      <div class="flex items-center justify-between mb-6">
        <p class="text-gray-500">Project notes and documentation</p>
        <button class="btn-primary" @click="openNoteDialog()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Add Note
        </button>
      </div>

      <!-- Loading -->
      <div v-if="notesLoading" class="text-center py-12">
        <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>

      <template v-else>
        <div v-if="notes.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-lg font-medium">No notes yet</p>
          <p>Add your first project note.</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="note in notes"
            :key="note.id"
            class="card p-4"
            :class="{ 'ring-2 ring-yellow-400': note.isPinned }"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span v-if="note.isPinned" class="text-yellow-500">
                    <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M16,12V4H17V2H7V4H8V12L6,14V16H11.2V22H12.8V16H18V14L16,12Z"/>
                    </svg>
                  </span>
                  <h3 class="font-semibold text-gray-900">{{ note.title }}</h3>
                  <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">
                    {{ note.category }}
                  </span>
                </div>
                <p class="text-gray-600 whitespace-pre-wrap">{{ note.content }}</p>
                <p class="text-xs text-gray-400 mt-2">
                  By {{ note.createdBy?.displayName || note.createdBy?.username || 'Unknown' }}
                  &bull; Updated {{ new Date(note.updatedAt).toLocaleDateString() }}
                </p>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button
                  class="btn-ghost text-gray-500 text-sm"
                  :class="{ 'text-yellow-500': note.isPinned }"
                  @click="handleTogglePin(note)"
                  :title="note.isPinned ? 'Unpin' : 'Pin'"
                >
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M16,12V4H17V2H7V4H8V12L6,14V16H11.2V22H12.8V16H18V14L16,12Z"/>
                  </svg>
                </button>
                <button class="btn-ghost text-gray-600 text-sm" @click="openNoteDialog(note)">
                  Edit
                </button>
                <button
                  class="btn-ghost text-red-600 text-sm"
                  :disabled="deletingNoteId === note.id"
                  @click="handleDeleteNote(note)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Bugs Tab -->
    <div v-if="activeTab === 'bugs'">
      <div class="flex items-center justify-between mb-6">
        <div class="flex gap-4 items-center">
          <select v-model="bugStatusFilter" class="input w-40" @change="loadBugs()">
            <option value="">All Statuses</option>
            <option value="open">Open</option>
            <option value="in_progress">In Progress</option>
            <option value="resolved">Resolved</option>
            <option value="closed">Closed</option>
            <option value="wont_fix">Won't Fix</option>
          </select>
          <select v-model="bugPriorityFilter" class="input w-40" @change="loadBugs()">
            <option value="">All Priorities</option>
            <option value="critical">Critical</option>
            <option value="high">High</option>
            <option value="medium">Medium</option>
            <option value="low">Low</option>
          </select>
        </div>
        <button class="btn-primary" @click="openBugDialog()">
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          Report Bug
        </button>
      </div>

      <!-- Loading -->
      <div v-if="bugsLoading" class="text-center py-12">
        <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>

      <template v-else>
        <div v-if="bugs.length === 0" class="text-center py-12 text-gray-500">
          <p class="text-lg font-medium">No bugs reported</p>
          <p>That's a good thing!</p>
        </div>

        <div v-else class="space-y-4">
          <div
            v-for="bug in bugs"
            :key="bug.id"
            class="card p-4"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <h3 class="font-semibold text-gray-900">{{ bug.title }}</h3>
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="getPriorityColor(bug.priority)"
                  >
                    {{ bug.priority }}
                  </span>
                  <span
                    class="text-xs px-2 py-0.5 rounded-full"
                    :class="getStatusColor(bug.status)"
                  >
                    {{ bug.status.replace('_', ' ') }}
                  </span>
                </div>
                <p v-if="bug.description" class="text-gray-600 mb-2">{{ bug.description }}</p>
                <div v-if="bug.stepsToReproduce" class="text-sm text-gray-500 bg-gray-50 rounded p-2 mb-2">
                  <strong>Steps to reproduce:</strong>
                  <p class="whitespace-pre-wrap">{{ bug.stepsToReproduce }}</p>
                </div>
                <p class="text-xs text-gray-400">
                  Reported by {{ bug.reportedBy?.displayName || bug.reportedBy?.username || 'Unknown' }}
                  &bull; {{ new Date(bug.createdAt).toLocaleDateString() }}
                  <span v-if="bug.assignedTo">
                    &bull; Assigned to {{ bug.assignedTo.displayName || bug.assignedTo.username }}
                  </span>
                </p>
              </div>
              <div class="flex gap-2 flex-shrink-0">
                <button class="btn-ghost text-gray-600 text-sm" @click="openBugDialog(bug)">
                  Edit
                </button>
                <button
                  class="btn-ghost text-red-600 text-sm"
                  :disabled="deletingBugId === bug.id"
                  @click="handleDeleteBug(bug)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- BGG Cache Tab -->
    <div v-if="activeTab === 'bggCache'">
      <div class="card p-6 mb-6">
        <h2 class="text-lg font-semibold mb-4">BoardGameGeek Cache</h2>
        <p class="text-gray-600 mb-6">
          The BGG cache stores board game data locally for fast searching. Games are fetched from BoardGameGeek and cached to avoid rate limits and slow API responses.
        </p>

        <!-- Stats -->
        <div v-if="cacheLoading" class="text-center py-4">
          <svg class="w-6 h-6 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
        </div>
        <div v-else-if="cacheStats" class="grid grid-cols-3 gap-4 mb-6">
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ cacheStats.totalGames.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">Total Games</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-2xl font-bold text-gray-900">{{ cacheStats.rankedGames.toLocaleString() }}</div>
            <div class="text-sm text-gray-500">With Rankings</div>
          </div>
          <div class="bg-gray-50 rounded-lg p-4 text-center">
            <div class="text-sm font-medium text-gray-900">{{ formatCacheDate(cacheStats.oldestCache) }}</div>
            <div class="text-sm text-gray-500">Oldest Entry</div>
          </div>
        </div>

        <!-- Actions -->
        <div class="space-y-3">
          <div class="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
            <div>
              <div class="font-medium text-blue-900">Import Popular Games</div>
              <div class="text-sm text-blue-700">Imports ~100 top-rated BGG games + current hot list</div>
            </div>
            <button
              class="btn-primary"
              :disabled="cacheImporting"
              @click="handleImportPopular"
            >
              <svg v-if="cacheImporting" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
              Import Popular
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Import Hot Games</div>
              <div class="text-sm text-gray-600">Imports BGG's current "hot" list (~50 games)</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleImportHot"
            >
              Import Hot
            </button>
          </div>

          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div>
              <div class="font-medium text-gray-900">Refresh Stale Entries</div>
              <div class="text-sm text-gray-600">Updates cache entries older than 7 days</div>
            </div>
            <button
              class="btn-outline"
              :disabled="cacheImporting"
              @click="handleRefreshStale"
            >
              Refresh Stale
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Locations Tab -->
    <div v-if="activeTab === 'locations'">
      <div class="flex items-center justify-between mb-6">
        <p class="text-gray-500">Manage event locations for Looking For Players</p>
        <div class="flex gap-2">
          <button
            v-if="!mergeMode"
            class="btn-outline"
            @click="toggleMergeMode"
          >
            Merge Duplicates
          </button>
          <button
            v-if="mergeMode"
            class="btn-ghost"
            @click="toggleMergeMode"
          >
            Cancel
          </button>
          <button
            v-if="mergeMode && selectedForMerge.length >= 2"
            class="btn-primary"
            @click="handleMerge"
          >
            Merge Selected ({{ selectedForMerge.length }})
          </button>
          <button
            v-if="!mergeMode"
            class="btn-primary"
            @click="openCreateDialog"
          >
            <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
            </svg>
            Add Location
          </button>
        </div>
      </div>

    <!-- Merge instructions -->
    <div v-if="mergeMode" class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
      <p class="text-blue-800">
        <strong>Merge Mode:</strong> Select locations to merge. The first selected location will be kept, and all others will be deleted. Any LFP posts using the deleted locations will be updated to use the kept one.
      </p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary-500 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
      </svg>
    </div>

    <!-- Location List -->
    <template v-else>
      <div v-if="locations.length === 0" class="text-center py-12 text-gray-500">
        <svg class="w-16 h-16 mx-auto mb-4 text-gray-300" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,11.5A2.5,2.5 0 0,1 9.5,9A2.5,2.5 0 0,1 12,6.5A2.5,2.5 0 0,1 14.5,9A2.5,2.5 0 0,1 12,11.5M12,2A7,7 0 0,0 5,9C5,14.25 12,22 12,22C12,22 19,14.25 19,9A7,7 0 0,0 12,2Z"/>
        </svg>
        <p class="text-lg font-medium">No event locations yet</p>
        <p>Add your first event location to get started.</p>
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="location in locations"
          :key="location.id"
          class="card p-4 flex items-center gap-4"
          :class="{
            'opacity-50': isExpired(location.endDate),
            'ring-2 ring-primary-500': selectedForMerge.includes(location.id),
          }"
        >
          <!-- Merge checkbox -->
          <div v-if="mergeMode" class="flex-shrink-0">
            <input
              type="checkbox"
              :checked="selectedForMerge.includes(location.id)"
              class="w-5 h-5 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              @change="toggleSelectForMerge(location.id)"
            />
          </div>

          <!-- Location info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <h3 class="font-semibold text-gray-900">{{ location.name }}</h3>
              <span v-if="isExpired(location.endDate)" class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">
                Expired
              </span>
              <span v-if="selectedForMerge[0] === location.id" class="text-xs px-2 py-0.5 rounded-full bg-primary-100 text-primary-700">
                Keep this one
              </span>
            </div>
            <p class="text-sm text-gray-500">
              {{ location.city }}, {{ location.state }}
              <span v-if="location.venue"> &bull; {{ location.venue }}</span>
            </p>
            <p class="text-sm text-gray-500">
              {{ formatDate(location.startDate) }} - {{ formatDate(location.endDate) }}
            </p>
          </div>

          <!-- Actions -->
          <div v-if="!mergeMode" class="flex gap-2 flex-shrink-0">
            <button
              class="btn-ghost text-gray-600 text-sm"
              @click="openEditDialog(location)"
            >
              Edit
            </button>
            <button
              class="btn-ghost text-red-600 text-sm"
              @click="handleDelete(location)"
            >
              Delete
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Create/Edit Dialog -->
    <div v-if="showDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10">
        <h3 class="text-lg font-semibold mb-4">
          {{ editingLocation ? 'Edit Location' : 'Add Event Location' }}
        </h3>

        <div class="space-y-4">
          <div>
            <label class="label">Event Name *</label>
            <input
              v-model="form.name"
              type="text"
              class="input"
              placeholder="e.g., Dice Tower West 2026"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">City *</label>
              <input
                v-model="form.city"
                type="text"
                class="input"
                placeholder="City"
              />
            </div>
            <div>
              <label class="label">State *</label>
              <input
                v-model="form.state"
                type="text"
                class="input"
                placeholder="State"
              />
            </div>
          </div>

          <div>
            <label class="label">Venue</label>
            <input
              v-model="form.venue"
              type="text"
              class="input"
              placeholder="e.g., Las Vegas Convention Center"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Start Date *</label>
              <input
                v-model="form.startDate"
                type="date"
                class="input"
              />
            </div>
            <div>
              <label class="label">End Date *</label>
              <input
                v-model="form.endDate"
                type="date"
              class="input"
              />
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showDialog = false" :disabled="saving">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSave" :disabled="saving">
            <svg v-if="saving" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingLocation ? 'Save Changes' : 'Add Location' }}
          </button>
        </div>
      </div>
    </div>
    </div>

    <!-- Suspend User Dialog -->
    <div v-if="showSuspendDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showSuspendDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Suspend User</h3>

        <p class="text-gray-600 mb-4">
          Are you sure you want to suspend <strong>@{{ userToSuspend?.username }}</strong>?
          They will not be able to log in while suspended.
        </p>

        <div class="mb-4">
          <label class="label">Reason (optional)</label>
          <textarea
            v-model="suspendReason"
            class="input h-24 resize-none"
            placeholder="Enter a reason for the suspension..."
          ></textarea>
        </div>

        <div class="flex justify-end gap-3">
          <button class="btn-ghost" @click="showSuspendDialog = false" :disabled="suspendingUserId !== null">
            Cancel
          </button>
          <button class="btn-primary bg-red-600 hover:bg-red-700" @click="handleSuspendUser" :disabled="suspendingUserId !== null">
            <svg v-if="suspendingUserId !== null" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Suspend User
          </button>
        </div>
      </div>
    </div>

    <!-- Note Dialog -->
    <div v-if="showNoteDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showNoteDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10">
        <h3 class="text-lg font-semibold mb-4">{{ editingNote ? 'Edit Note' : 'Add Note' }}</h3>

        <div class="space-y-4">
          <div>
            <label class="label">Title *</label>
            <input
              v-model="noteForm.title"
              type="text"
              class="input"
              placeholder="Note title"
            />
          </div>

          <div>
            <label class="label">Category</label>
            <input
              v-model="noteForm.category"
              type="text"
              class="input"
              placeholder="e.g., general, feature, todo"
            />
          </div>

          <div>
            <label class="label">Content *</label>
            <textarea
              v-model="noteForm.content"
              class="input h-40 resize-none"
              placeholder="Note content..."
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showNoteDialog = false" :disabled="savingNote">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSaveNote" :disabled="savingNote">
            <svg v-if="savingNote" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingNote ? 'Save Changes' : 'Add Note' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Bug Dialog -->
    <div v-if="showBugDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showBugDialog = false"></div>
      <div class="card p-6 w-full max-w-lg relative z-10 max-h-[90vh] overflow-y-auto">
        <h3 class="text-lg font-semibold mb-4">{{ editingBug ? 'Edit Bug' : 'Report Bug' }}</h3>

        <div class="space-y-4">
          <div>
            <label class="label">Title *</label>
            <input
              v-model="bugForm.title"
              type="text"
              class="input"
              placeholder="Brief description of the bug"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Priority</label>
              <select v-model="bugForm.priority" class="input">
                <option value="low">Low</option>
                <option value="medium">Medium</option>
                <option value="high">High</option>
                <option value="critical">Critical</option>
              </select>
            </div>
            <div v-if="editingBug">
              <label class="label">Status</label>
              <select v-model="bugForm.status" class="input">
                <option value="open">Open</option>
                <option value="in_progress">In Progress</option>
                <option value="resolved">Resolved</option>
                <option value="closed">Closed</option>
                <option value="wont_fix">Won't Fix</option>
              </select>
            </div>
          </div>

          <div>
            <label class="label">Description</label>
            <textarea
              v-model="bugForm.description"
              class="input h-24 resize-none"
              placeholder="Detailed description of the bug..."
            ></textarea>
          </div>

          <div>
            <label class="label">Steps to Reproduce</label>
            <textarea
              v-model="bugForm.stepsToReproduce"
              class="input h-24 resize-none"
              placeholder="1. Go to...&#10;2. Click on...&#10;3. See error"
            ></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showBugDialog = false" :disabled="savingBug">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSaveBug" :disabled="savingBug">
            <svg v-if="savingBug" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            {{ editingBug ? 'Save Changes' : 'Report Bug' }}
          </button>
        </div>
      </div>
    </div>

    <!-- User Edit Dialog -->
    <div v-if="showUserEditDialog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/50" @click="showUserEditDialog = false"></div>
      <div class="card p-6 w-full max-w-md relative z-10">
        <h3 class="text-lg font-semibold mb-4">Edit User</h3>

        <div v-if="editingUser" class="space-y-4">
          <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
            <img
              v-if="editingUser.avatarUrl"
              :src="editingUser.avatarUrl"
              :alt="editingUser.displayName || editingUser.username"
              class="w-10 h-10 rounded-full object-cover"
            />
            <div
              v-else
              class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 font-medium"
            >
              {{ (editingUser.displayName || editingUser.username || '?').charAt(0).toUpperCase() }}
            </div>
            <div>
              <div class="font-medium">{{ editingUser.email }}</div>
              <div class="text-sm text-gray-500">ID: {{ editingUser.id.slice(0, 8) }}...</div>
            </div>
          </div>

          <div>
            <label class="label">Display Name</label>
            <input
              v-model="userEditForm.displayName"
              type="text"
              class="input"
              placeholder="Display name (optional)"
            />
          </div>

          <div>
            <label class="label">Username *</label>
            <div class="flex items-center">
              <span class="text-gray-500 mr-1">@</span>
              <input
                v-model="userEditForm.username"
                type="text"
                class="input flex-1"
                placeholder="username"
              />
            </div>
            <p class="text-xs text-gray-500 mt-1">3-30 characters, starts with letter, alphanumeric + underscore only</p>
          </div>

          <div class="flex items-center gap-3">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                v-model="userEditForm.isAdmin"
                type="checkbox"
                class="w-4 h-4 rounded border-gray-300 text-primary-500 focus:ring-primary-500"
              />
              <span>Admin privileges</span>
            </label>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button class="btn-ghost" @click="showUserEditDialog = false" :disabled="savingUser">
            Cancel
          </button>
          <button class="btn-primary" @click="handleSaveUser" :disabled="savingUser">
            <svg v-if="savingUser" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Save Changes
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
