<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'

const auth = useAuthStore()

// Get reCAPTCHA token
async function getRecaptchaToken(): Promise<string | undefined> {
  try {
    // @ts-expect-error - grecaptcha is loaded from external script
    if (typeof grecaptcha === 'undefined') {
      console.warn('reCAPTCHA not loaded')
      return undefined
    }
    // Wait for grecaptcha to be ready, then execute
    return new Promise((resolve) => {
      // @ts-expect-error - grecaptcha is loaded from external script
      grecaptcha.ready(async () => {
        try {
          // @ts-expect-error - grecaptcha is loaded from external script
          const token = await grecaptcha.execute('6LfZ-HcsAAAAALniN1xOkc_I5t443MorPE66H0CK', { action: 'contact' })
          resolve(token)
        } catch (err) {
          console.error('Failed to execute reCAPTCHA:', err)
          resolve(undefined)
        }
      })
    })
  } catch (err) {
    console.error('Failed to get reCAPTCHA token:', err)
    return undefined
  }
}

const loading = ref(false)
const submitted = ref(false)
const errorMessage = ref('')

const form = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})

// Pre-fill email if user is logged in
if (auth.user.value?.email) {
  form.email = auth.user.value.email
}
if (auth.user.value?.displayName) {
  form.name = auth.user.value.displayName
}

const subjectOptions = [
  { value: 'general', label: 'General Inquiry' },
  { value: 'advertising', label: 'Advertising Inquiry' },
  { value: 'bug', label: 'Report a Bug' },
  { value: 'feature', label: 'Feature Request' },
  { value: 'account', label: 'Account Issue' },
  { value: 'feedback', label: 'Feedback' },
  { value: 'other', label: 'Other' },
]

async function handleSubmit() {
  errorMessage.value = ''

  // Basic validation
  if (!form.name.trim()) {
    errorMessage.value = 'Please enter your name'
    return
  }
  if (!form.email.trim()) {
    errorMessage.value = 'Please enter your email'
    return
  }
  if (!form.subject) {
    errorMessage.value = 'Please select a subject'
    return
  }
  if (!form.message.trim()) {
    errorMessage.value = 'Please enter your message'
    return
  }
  if (form.message.trim().length < 10) {
    errorMessage.value = 'Message must be at least 10 characters'
    return
  }

  loading.value = true

  try {
    // Get reCAPTCHA token
    const recaptchaToken = await getRecaptchaToken()

    const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
    const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

    const response = await fetch(`${FUNCTIONS_URL}/contact`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: form.name.trim(),
        email: form.email.trim(),
        subject: form.subject,
        message: form.message.trim(),
        userId: auth.user.value?.id || null,
        recaptchaToken,
      }),
    })

    if (!response.ok) {
      const data = await response.json().catch(() => ({}))
      throw new Error(data.error || data.message || 'Failed to send message')
    }

    submitted.value = true
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Failed to send message. Please try again.'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = auth.user.value?.displayName || ''
  form.email = auth.user.value?.email || ''
  form.subject = ''
  form.message = ''
  submitted.value = false
  errorMessage.value = ''
}
</script>

<template>
  <div class="container-narrow py-8">
    <div class="card p-6">
      <!-- Header -->
      <div class="text-center mb-6">
        <svg class="w-12 h-12 mx-auto text-primary-500 mb-3" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z"/>
        </svg>
        <h1 class="text-2xl font-bold text-gray-900">Contact Us</h1>
        <p class="text-gray-500 mt-1">We'd love to hear from you</p>
      </div>

      <!-- Success State -->
      <div v-if="submitted" class="text-center py-8">
        <svg class="w-16 h-16 mx-auto text-green-500 mb-4" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <h2 class="text-xl font-semibold text-gray-900 mb-2">Message Sent!</h2>
        <p class="text-gray-600 mb-6">
          Thank you for reaching out. We'll get back to you as soon as possible.
        </p>
        <button @click="resetForm" class="btn-primary">
          Send Another Message
        </button>
      </div>

      <!-- Form -->
      <form v-else @submit.prevent="handleSubmit" class="space-y-6">
        <!-- Error Alert -->
        <div v-if="errorMessage" class="alert-error">
          <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
            <path d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z"/>
          </svg>
          <span class="flex-1">{{ errorMessage }}</span>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label for="name" class="label">Your Name *</label>
            <input
              id="name"
              v-model="form.name"
              type="text"
              class="input"
              placeholder="John Doe"
              :disabled="loading"
            />
          </div>

          <div>
            <label for="email" class="label">Email Address *</label>
            <input
              id="email"
              v-model="form.email"
              type="email"
              class="input"
              placeholder="john@example.com"
              :disabled="loading"
            />
          </div>
        </div>

        <div>
          <label for="subject" class="label">Subject *</label>
          <select
            id="subject"
            v-model="form.subject"
            class="input"
            :disabled="loading"
          >
            <option value="" disabled>Select a subject...</option>
            <option
              v-for="option in subjectOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </div>

        <div>
          <label for="message" class="label">Message *</label>
          <textarea
            id="message"
            v-model="form.message"
            rows="5"
            class="input resize-none"
            placeholder="Tell us what's on your mind..."
            :disabled="loading"
          ></textarea>
          <p class="text-sm text-gray-500 mt-1">
            {{ form.message.length }}/1000 characters
          </p>
        </div>

        <button
          type="submit"
          class="btn-primary w-full"
          :disabled="loading"
        >
          <svg v-if="loading" class="w-5 h-5 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          <svg v-else class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
            <path d="M2,21L23,12L2,3V10L17,12L2,14V21Z"/>
          </svg>
          {{ loading ? 'Sending...' : 'Send Message' }}
        </button>
      </form>

    </div>
  </div>
</template>
