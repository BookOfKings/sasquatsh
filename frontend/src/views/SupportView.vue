<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'

const auth = useAuthStore()

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

const form = ref({
  name: auth.user.value?.displayName || '',
  email: auth.user.value?.email || '',
  platform: '',
  type: 'bug',
  subject: '',
  message: '',
})

const submitted = ref(false)
const loading = ref(false)
const error = ref('')

async function handleSubmit() {
  if (!form.value.name.trim() || !form.value.email.trim() || !form.value.subject.trim() || !form.value.message.trim()) {
    error.value = 'Please fill in all required fields'
    return
  }
  if (!form.value.platform) {
    error.value = 'Please select which platform you are using'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await fetch(`${FUNCTIONS_URL}/contact`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: form.value.name.trim(),
        email: form.value.email.trim(),
        subject: `[${form.value.type.toUpperCase()}] [${form.value.platform}] ${form.value.subject.trim()}`,
        message: `Platform: ${form.value.platform}\nType: ${form.value.type}\n\n${form.value.message.trim()}`,
        recaptchaToken: 'support-form',
      }),
    })

    if (!response.ok) throw new Error('Failed to send')
    submitted.value = true
  } catch {
    error.value = 'Failed to send your request. Please try again or email us at support@sasquatsh.com'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = {
    name: auth.user.value?.displayName || '',
    email: auth.user.value?.email || '',
    platform: '',
    type: 'bug',
    subject: '',
    message: '',
  }
  submitted.value = false
  error.value = ''
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-12">
    <div class="max-w-2xl mx-auto px-4">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">Support</h1>
        <p class="text-gray-600">Report a bug, request a feature, or get help with Sasquatsh.</p>
      </div>

      <!-- Success -->
      <div v-if="submitted" class="card p-8 text-center">
        <svg class="w-16 h-16 mx-auto text-green-500 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <h2 class="text-xl font-bold text-gray-900 mb-2">Thanks for reaching out!</h2>
        <p class="text-gray-600 mb-6">We've received your message and will get back to you as soon as possible.</p>
        <button @click="resetForm" class="btn-outline">Submit Another Request</button>
      </div>

      <!-- Form -->
      <div v-else class="card p-8">
        <div v-if="error" class="alert-error mb-4">{{ error }}</div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <!-- Platform -->
          <div>
            <label class="label">Which platform are you using? *</label>
            <div class="flex gap-3">
              <button
                type="button"
                class="flex-1 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors"
                :class="form.platform === 'website' ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                @click="form.platform = 'website'"
              >
                <svg class="w-5 h-5 mx-auto mb-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M4,6H20V16H4M20,18A2,2 0 0,0 22,16V6C22,4.89 21.1,4 20,4H4C2.89,4 2,4.89 2,6V16A2,2 0 0,0 4,18H0V20H24V18H20Z"/>
                </svg>
                Website
              </button>
              <button
                type="button"
                class="flex-1 px-4 py-3 rounded-lg border-2 text-sm font-medium transition-colors"
                :class="form.platform === 'ios' ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                @click="form.platform = 'ios'"
              >
                <svg class="w-5 h-5 mx-auto mb-1" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.48-3.24 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.53 4.09zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
                </svg>
                iOS App
              </button>
            </div>
          </div>

          <!-- Type -->
          <div>
            <label class="label">What can we help with? *</label>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="opt in [
                  { value: 'bug', label: 'Report a Bug', icon: 'M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z' },
                  { value: 'feature', label: 'Feature Request', icon: 'M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z' },
                  { value: 'help', label: 'Need Help', icon: 'M15.07,11.25L14.17,12.17C13.45,12.89 13,13.5 13,15H11V14.5C11,13.39 11.45,12.39 12.17,11.67L13.41,10.41C13.78,10.05 14,9.55 14,9C14,7.89 13.1,7 12,7A2,2 0 0,0 10,9H8A4,4 0 0,1 12,5A4,4 0 0,1 16,9C16,9.88 15.64,10.67 15.07,11.25M13,19H11V17H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z' },
                  { value: 'feedback', label: 'General Feedback', icon: 'M20,2H4A2,2 0 0,0 2,4V22L6,18H20A2,2 0 0,0 22,16V4A2,2 0 0,0 20,2M20,16H5.17L4,17.17V4H20V16Z' },
                ]"
                :key="opt.value"
                type="button"
                class="px-3 py-2 rounded-lg border text-sm transition-colors flex items-center gap-1.5"
                :class="form.type === opt.value ? 'border-primary-500 bg-primary-50 text-primary-700' : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                @click="form.type = opt.value"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor"><path :d="opt.icon"/></svg>
                {{ opt.label }}
              </button>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="label">Name *</label>
              <input v-model="form.name" type="text" class="input" required />
            </div>
            <div>
              <label class="label">Email *</label>
              <input v-model="form.email" type="email" class="input" required />
            </div>
          </div>

          <div>
            <label class="label">Subject *</label>
            <input v-model="form.subject" type="text" class="input" placeholder="Brief summary of your issue or request" required />
          </div>

          <div>
            <label class="label">Details *</label>
            <textarea v-model="form.message" class="input" rows="5" placeholder="Please describe the issue, feature, or feedback in detail. For bugs, include steps to reproduce if possible." required></textarea>
          </div>

          <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
            <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
            Send Request
          </button>
        </form>
      </div>

      <!-- FAQ / Quick Help -->
      <div class="mt-8 card p-6">
        <h2 class="font-bold text-gray-900 mb-4">Common Questions</h2>
        <div class="space-y-3 text-sm">
          <details class="group">
            <summary class="font-medium text-gray-700 cursor-pointer hover:text-primary-600">How do I create a game night?</summary>
            <p class="mt-1 text-gray-600 pl-4">Tap "Host a Game" on your dashboard or the Games tab. Fill in the details and publish your event.</p>
          </details>
          <details class="group">
            <summary class="font-medium text-gray-700 cursor-pointer hover:text-primary-600">How do I invite people to my group?</summary>
            <p class="mt-1 text-gray-600 pl-4">Go to your group page and tap the share/link icon to copy an invite link. Anyone with the link can join.</p>
          </details>
          <details class="group">
            <summary class="font-medium text-gray-700 cursor-pointer hover:text-primary-600">How do I add games to my collection?</summary>
            <p class="mt-1 text-gray-600 pl-4">Go to My Collection from the menu. You can search BoardGameGeek, browse the Top 50, or scan barcodes to add games.</p>
          </details>
          <details class="group">
            <summary class="font-medium text-gray-700 cursor-pointer hover:text-primary-600">How do I cancel my subscription?</summary>
            <p class="mt-1 text-gray-600 pl-4">For website subscriptions, go to Profile and click Manage Subscription. For iOS subscriptions, go to Settings > Apple ID > Subscriptions on your device.</p>
          </details>
        </div>
      </div>
    </div>
  </div>
</template>
