<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getChatMessages,
  sendChatMessage,
  deleteChatMessage,
  reportChatMessage,
  subscribeToChatMessages,
  unsubscribeFromChat,
} from '@/services/chatApi'
import type { ChatMessage, ChatContextType, ChatReportReason } from '@/types/chat'
import { REPORT_REASON_LABELS } from '@/types/chat'
import type { RealtimeChannel } from '@supabase/supabase-js'
import UserAvatar from '@/components/common/UserAvatar.vue'

const props = defineProps<{
  contextType: ChatContextType
  contextId: string
}>()

const auth = useAuthStore()

// State
const messages = ref<ChatMessage[]>([])
const newMessage = ref('')
const loading = ref(true)
const sending = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const error = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
let channel: RealtimeChannel | null = null

// Report modal state
const showReportModal = ref(false)
const reportingMessage = ref<ChatMessage | null>(null)
const reportReason = ref<ChatReportReason>('inappropriate')
const reportDetails = ref('')
const reportSubmitting = ref(false)
const reportSuccess = ref(false)

// Computed
const currentUserId = computed(() => auth.user.value?.id)
const canSend = computed(() => newMessage.value.trim().length > 0 && !sending.value)

// Get short timezone abbreviation
function getTimezoneAbbr(): string {
  const tz = Intl.DateTimeFormat().resolvedOptions().timeZone
  // Get abbreviation like "EST", "PST", etc.
  const abbr = new Date().toLocaleTimeString('en-US', { timeZoneName: 'short' }).split(' ').pop() || tz
  return abbr
}

// Format timestamp with timezone
function formatTime(isoString: string): string {
  const date = new Date(isoString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  const tz = getTimezoneAbbr()

  if (diffDays === 0) {
    return `${date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} ${tz}`
  } else if (diffDays === 1) {
    return `Yesterday ${date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} ${tz}`
  } else if (diffDays < 7) {
    return `${date.toLocaleDateString([], { weekday: 'short', hour: 'numeric', minute: '2-digit' })} ${tz}`
  } else {
    return `${date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })} ${tz}`
  }
}

// Scroll to bottom
function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// Load initial messages
async function loadMessages() {
  loading.value = true
  error.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) {
      error.value = 'Not authenticated'
      return
    }

    messages.value = await getChatMessages(token, props.contextType, props.contextId, { limit: 50 })
    hasMore.value = messages.value.length === 50
    scrollToBottom()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load messages'
  } finally {
    loading.value = false
  }
}

// Load older messages (pagination)
async function loadMoreMessages() {
  if (!hasMore.value || loadingMore.value || messages.value.length === 0) return

  loadingMore.value = true
  const oldestMessage = messages.value[0]

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const olderMessages = await getChatMessages(
      token,
      props.contextType,
      props.contextId,
      { limit: 50, before: oldestMessage?.createdAt }
    )

    if (olderMessages.length > 0) {
      messages.value = [...olderMessages, ...messages.value]
    }
    hasMore.value = olderMessages.length === 50
  } catch (err) {
    console.error('Failed to load more messages:', err)
  } finally {
    loadingMore.value = false
  }
}

// Send message
async function handleSend() {
  if (!canSend.value) return

  const content = newMessage.value.trim()
  newMessage.value = ''
  sending.value = true

  try {
    const token = await auth.getIdToken()
    if (!token) return

    const message = await sendChatMessage(token, props.contextType, props.contextId, content)

    // Add message if not already added by realtime
    if (!messages.value.some(m => m.id === message.id)) {
      messages.value.push(message)
      scrollToBottom()
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to send message'
    newMessage.value = content // Restore message on error
  } finally {
    sending.value = false
  }
}

// Delete message
async function handleDelete(messageId: string) {
  if (!confirm('Delete this message?')) return

  try {
    const token = await auth.getIdToken()
    if (!token) return

    await deleteChatMessage(token, props.contextType, props.contextId, messageId)
    messages.value = messages.value.filter(m => m.id !== messageId)
  } catch (err) {
    console.error('Failed to delete message:', err)
  }
}

// Open report modal
function openReportModal(message: ChatMessage) {
  reportingMessage.value = message
  reportReason.value = 'inappropriate'
  reportDetails.value = ''
  reportSuccess.value = false
  showReportModal.value = true
}

// Close report modal
function closeReportModal() {
  showReportModal.value = false
  reportingMessage.value = null
}

// Submit report
async function handleReport() {
  if (!reportingMessage.value) return

  reportSubmitting.value = true
  try {
    const token = await auth.getIdToken()
    if (!token) return

    await reportChatMessage(
      token,
      props.contextType,
      props.contextId,
      reportingMessage.value.id,
      reportReason.value,
      reportDetails.value.trim() || undefined
    )

    reportSuccess.value = true
    setTimeout(() => {
      closeReportModal()
    }, 1500)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to submit report'
  } finally {
    reportSubmitting.value = false
  }
}

// Handle new message from realtime
function handleNewMessage(message: ChatMessage) {
  // Avoid duplicates
  if (messages.value.some(m => m.id === message.id)) return

  messages.value.push(message)
  scrollToBottom()
}

// Handle message deletion from realtime
function handleMessageDeleted(messageId: string) {
  messages.value = messages.value.filter(m => m.id !== messageId)
}

// Setup realtime subscription
function setupRealtimeSubscription() {
  channel = subscribeToChatMessages(
    props.contextType,
    props.contextId,
    handleNewMessage,
    handleMessageDeleted
  )
}

// Handle Enter key
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

// Handle scroll for infinite scroll
function handleScroll() {
  if (!messagesContainer.value) return

  // Load more when scrolled near the top
  if (messagesContainer.value.scrollTop < 100) {
    loadMoreMessages()
  }
}

// Lifecycle
onMounted(async () => {
  await loadMessages()
  setupRealtimeSubscription()
})

onUnmounted(() => {
  if (channel) {
    unsubscribeFromChat(channel)
  }
})

// Watch for context changes (if used in dynamic contexts)
watch(
  () => [props.contextType, props.contextId],
  async () => {
    if (channel) {
      unsubscribeFromChat(channel)
    }
    messages.value = []
    await loadMessages()
    setupRealtimeSubscription()
  }
)
</script>

<template>
  <div class="flex flex-col h-full bg-white overflow-hidden">
    <!-- Messages Container -->
    <div
      ref="messagesContainer"
      class="flex-1 overflow-y-auto p-4 space-y-4"
      @scroll="handleScroll"
    >
      <!-- Load More Button -->
      <button
        v-if="hasMore && !loadingMore && messages.length > 0"
        class="w-full text-center text-sm text-primary-600 hover:text-primary-700 py-2"
        @click="loadMoreMessages"
      >
        Load earlier messages
      </button>

      <!-- Loading More Indicator -->
      <div v-if="loadingMore" class="text-center py-2">
        <div class="inline-block w-5 h-5 border-2 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex items-center justify-center py-8">
        <div class="w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <!-- Empty State -->
      <div v-else-if="messages.length === 0" class="flex flex-col items-center justify-center py-8 text-gray-500">
        <svg class="w-12 h-12 mb-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H6L4,18V4H20"/>
        </svg>
        <p>No messages yet. Start the conversation!</p>
      </div>

      <!-- Messages -->
      <template v-else>
        <div
          v-for="message in messages"
          :key="message.id"
          class="flex gap-3"
          :class="message.userId === currentUserId ? 'flex-row-reverse' : ''"
        >
          <!-- Avatar -->
          <UserAvatar
            :avatar-url="message.user?.avatarUrl"
            :display-name="message.user?.displayName"
            :is-founding-member="message.user?.isFoundingMember"
            :is-admin="message.user?.isAdmin"
            size="sm"
            :show-badge="false"
            class="flex-shrink-0"
          />

          <!-- Message Bubble -->
          <div
            class="max-w-[70%] group"
            :class="message.userId === currentUserId ? 'text-right' : ''"
          >
            <!-- User Name (for others' messages) -->
            <div
              v-if="message.userId !== currentUserId"
              class="text-xs text-gray-500 mb-1"
            >
              {{ message.user?.displayName || 'Unknown User' }}
            </div>

            <!-- Message Content -->
            <div
              class="inline-block px-3 py-2 rounded-lg text-sm"
              :class="[
                message.userId === currentUserId
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 text-gray-900'
              ]"
            >
              <p class="whitespace-pre-wrap break-words">{{ message.content }}</p>
            </div>

            <!-- Timestamp & Actions -->
            <div
              class="flex items-center gap-2 mt-1 text-xs text-gray-400"
              :class="message.userId === currentUserId ? 'justify-end' : ''"
            >
              <span>{{ formatTime(message.createdAt) }}</span>
              <!-- Delete own message -->
              <button
                v-if="message.userId === currentUserId"
                class="opacity-0 group-hover:opacity-100 transition-opacity text-gray-400 hover:text-red-500"
                title="Delete message"
                @click="handleDelete(message.id)"
              >
                <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19,4H15.5L14.5,3H9.5L8.5,4H5V6H19M6,19A2,2 0 0,0 8,21H16A2,2 0 0,0 18,19V7H6V19Z"/>
                </svg>
              </button>
              <!-- Report others' messages -->
              <button
                v-if="message.userId !== currentUserId"
                class="opacity-0 group-hover:opacity-100 transition-opacity text-gray-400 hover:text-orange-500"
                title="Report message"
                @click="openReportModal(message)"
              >
                <svg class="w-3 h-3" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M13,14H11V10H13M13,18H11V16H13M1,21H23L12,2L1,21Z"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="px-4 py-2 bg-red-50 text-red-600 text-sm border-t border-red-100">
      {{ error }}
      <button class="ml-2 underline" @click="error = ''">Dismiss</button>
    </div>

    <!-- Input Area -->
    <div class="flex-shrink-0 border-t border-gray-200 p-3">
      <div class="flex gap-2">
        <textarea
          v-model="newMessage"
          placeholder="Type a message..."
          class="flex-1 resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none"
          rows="1"
          :disabled="sending || !auth.isAuthenticated.value"
          @keydown="handleKeydown"
        ></textarea>
        <button
          class="px-4 py-2 bg-primary-500 text-white rounded-lg hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          :disabled="!canSend || !auth.isAuthenticated.value"
          @click="handleSend"
        >
          <svg v-if="sending" class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M2,21L23,12L2,3V10L17,12L2,14V21Z"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Report Modal -->
    <Teleport to="body">
      <div
        v-if="showReportModal"
        class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
        @click.self="closeReportModal"
      >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full">
          <div class="px-4 py-3 border-b border-gray-200">
            <h3 class="font-semibold text-gray-900">Report Message</h3>
          </div>

          <div class="p-4 space-y-4">
            <!-- Success State -->
            <div v-if="reportSuccess" class="text-center py-4">
              <svg class="w-12 h-12 text-green-500 mx-auto mb-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
              </svg>
              <p class="text-green-600 font-medium">Report submitted</p>
              <p class="text-sm text-gray-500">Thank you for helping keep our community safe.</p>
            </div>

            <template v-else>
              <!-- Reported Message Preview -->
              <div class="bg-gray-50 rounded-lg p-3 text-sm">
                <p class="text-gray-500 text-xs mb-1">Message from {{ reportingMessage?.user?.displayName || 'Unknown' }}:</p>
                <p class="text-gray-700 line-clamp-3">{{ reportingMessage?.content }}</p>
              </div>

              <!-- Reason Select -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Reason for report</label>
                <select
                  v-model="reportReason"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
                >
                  <option v-for="(label, value) in REPORT_REASON_LABELS" :key="value" :value="value">
                    {{ label }}
                  </option>
                </select>
              </div>

              <!-- Details (optional) -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">
                  Additional details <span class="text-gray-400 font-normal">(optional)</span>
                </label>
                <textarea
                  v-model="reportDetails"
                  placeholder="Provide any additional context..."
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500 resize-none"
                  rows="3"
                ></textarea>
              </div>
            </template>
          </div>

          <div v-if="!reportSuccess" class="px-4 py-3 border-t border-gray-200 flex justify-end gap-2">
            <button
              class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800"
              @click="closeReportModal"
            >
              Cancel
            </button>
            <button
              class="px-4 py-2 text-sm bg-orange-500 text-white rounded-lg hover:bg-orange-600 disabled:opacity-50"
              :disabled="reportSubmitting"
              @click="handleReport"
            >
              <span v-if="reportSubmitting">Submitting...</span>
              <span v-else>Submit Report</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
