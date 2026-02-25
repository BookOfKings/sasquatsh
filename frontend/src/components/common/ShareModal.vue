<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { createInvitation, getShareUrl, getSocialShareUrls } from '@/services/socialApi'
import type { GameInvitation } from '@/types/social'

const props = defineProps<{
  eventId: string
  eventTitle: string
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const auth = useAuthStore()

const loading = ref(false)
const invitation = ref<GameInvitation | null>(null)
const errorMessage = ref('')
const copied = ref(false)

const shareUrl = computed(() => {
  if (!invitation.value) return ''
  return getShareUrl(invitation.value.inviteCode)
})

const socialUrls = computed(() => {
  if (!invitation.value) return { facebook: '', twitter: '', email: '' }
  return getSocialShareUrls(invitation.value.inviteCode, props.eventTitle)
})

async function generateLink() {
  loading.value = true
  errorMessage.value = ''

  try {
    const token = await auth.getIdToken()
    if (!token) return

    invitation.value = await createInvitation(token, {
      eventId: props.eventId,
      channel: 'link',
    })
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to create invitation'
  } finally {
    loading.value = false
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('Failed to copy:', err)
  }
}

function openSocialShare(url: string) {
  window.open(url, '_blank', 'width=600,height=400')
}

function close() {
  invitation.value = null
  errorMessage.value = ''
  emit('close')
}
</script>

<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
    <div class="fixed inset-0 bg-black/50" @click="close"></div>
    <div class="card p-6 w-full max-w-md relative z-10">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold">Share This Game</h3>
        <button class="p-1 text-gray-400 hover:text-gray-600" @click="close">
          <svg class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
          </svg>
        </button>
      </div>

      <!-- Error -->
      <div v-if="errorMessage" class="alert-error mb-4">
        {{ errorMessage }}
      </div>

      <!-- Generate Link -->
      <div v-if="!invitation" class="text-center py-4">
        <p class="text-gray-600 mb-4">
          Create a shareable link to invite others to this game night.
        </p>
        <button
          class="btn-primary"
          :disabled="loading"
          @click="generateLink"
        >
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          <svg v-else class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M10.59,13.41C11,13.8 11,14.44 10.59,14.83C10.2,15.22 9.56,15.22 9.17,14.83C7.22,12.88 7.22,9.71 9.17,7.76V7.76L12.71,4.22C14.66,2.27 17.83,2.27 19.78,4.22C21.73,6.17 21.73,9.34 19.78,11.29L18.29,12.78C18.3,11.96 18.17,11.14 17.89,10.36L18.36,9.88C19.54,8.71 19.54,6.81 18.36,5.64C17.19,4.46 15.29,4.46 14.12,5.64L10.59,9.17C9.41,10.34 9.41,12.24 10.59,13.41M13.41,9.17C13.8,8.78 14.44,8.78 14.83,9.17C16.78,11.12 16.78,14.29 14.83,16.24V16.24L11.29,19.78C9.34,21.73 6.17,21.73 4.22,19.78C2.27,17.83 2.27,14.66 4.22,12.71L5.71,11.22C5.7,12.04 5.83,12.86 6.11,13.65L5.64,14.12C4.46,15.29 4.46,17.19 5.64,18.36C6.81,19.54 8.71,19.54 9.88,18.36L13.41,14.83C14.59,13.66 14.59,11.76 13.41,10.59C13,10.2 13,9.56 13.41,9.17Z"/>
          </svg>
          Generate Invite Link
        </button>
      </div>

      <!-- Share Options -->
      <div v-else class="space-y-4">
        <!-- Copy Link -->
        <div>
          <label class="label">Invite Link</label>
          <div class="flex gap-2">
            <input
              :value="shareUrl"
              type="text"
              class="input flex-1"
              readonly
            />
            <button
              class="btn-outline"
              :class="{ 'bg-green-50 border-green-500 text-green-600': copied }"
              @click="copyLink"
            >
              <svg v-if="copied" class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
              <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,21H8V7H19M19,5H8A2,2 0 0,0 6,7V21A2,2 0 0,0 8,23H19A2,2 0 0,0 21,21V7A2,2 0 0,0 19,5M16,1H4A2,2 0 0,0 2,3V17H4V3H16V1Z"/>
              </svg>
            </button>
          </div>
          <p class="text-sm text-gray-500 mt-1">
            This link expires in 7 days
          </p>
        </div>

        <!-- Social Share -->
        <div>
          <label class="label">Share via</label>
          <div class="flex gap-3">
            <button
              class="flex-1 btn bg-[#1877f2] text-white hover:bg-[#166fe5]"
              @click="openSocialShare(socialUrls.facebook)"
            >
              <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2.04C6.5 2.04 2 6.53 2 12.06C2 17.06 5.66 21.21 10.44 21.96V14.96H7.9V12.06H10.44V9.85C10.44 7.34 11.93 5.96 14.22 5.96C15.31 5.96 16.45 6.15 16.45 6.15V8.62H15.19C13.95 8.62 13.56 9.39 13.56 10.18V12.06H16.34L15.89 14.96H13.56V21.96A10 10 0 0 0 22 12.06C22 6.53 17.5 2.04 12 2.04Z"/>
              </svg>
              Facebook
            </button>
            <button
              class="flex-1 btn bg-[#1da1f2] text-white hover:bg-[#1a91da]"
              @click="openSocialShare(socialUrls.twitter)"
            >
              <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M22.46,6C21.69,6.35 20.86,6.58 20,6.69C20.88,6.16 21.56,5.32 21.88,4.31C21.05,4.81 20.13,5.16 19.16,5.36C18.37,4.5 17.26,4 16,4C13.65,4 11.73,5.92 11.73,8.29C11.73,8.63 11.77,8.96 11.84,9.27C8.28,9.09 5.11,7.38 3,4.79C2.63,5.42 2.42,6.16 2.42,6.94C2.42,8.43 3.17,9.75 4.33,10.5C3.62,10.5 2.96,10.3 2.38,10C2.38,10 2.38,10 2.38,10.03C2.38,12.11 3.86,13.85 5.82,14.24C5.46,14.34 5.08,14.39 4.69,14.39C4.42,14.39 4.15,14.36 3.89,14.31C4.43,16 6,17.26 7.89,17.29C6.43,18.45 4.58,19.13 2.56,19.13C2.22,19.13 1.88,19.11 1.54,19.07C3.44,20.29 5.7,21 8.12,21C16,21 20.33,14.46 20.33,8.79C20.33,8.6 20.33,8.42 20.32,8.23C21.16,7.63 21.88,6.87 22.46,6Z"/>
              </svg>
              Twitter
            </button>
          </div>
        </div>

        <!-- Email -->
        <a
          :href="socialUrls.email"
          class="btn-outline w-full justify-center"
        >
          <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z"/>
          </svg>
          Send via Email
        </a>

        <!-- Generate New -->
        <div class="pt-2 border-t border-gray-200 text-center">
          <button
            class="text-sm text-primary-500 hover:text-primary-600"
            @click="generateLink"
            :disabled="loading"
          >
            Generate a new link
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
