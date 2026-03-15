<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

async function handleSubmit() {
  if (!email.value) {
    errorMessage.value = 'Please enter your email address'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  const result = await auth.resetPassword(email.value)

  if (result.ok) {
    successMessage.value = result.message
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4 bg-stone-50">
    <div class="card p-6 max-w-md w-full">
      <!-- Header -->
      <div class="text-center mb-6">
        <img src="/logo.png" alt="Sasquatsh" class="w-24 h-24 mx-auto" />
        <h1 class="text-xl font-bold text-primary-500 mt-2">Reset Password</h1>
        <p class="text-sm text-gray-500">Enter your email to receive a password reset link</p>
      </div>

      <!-- Success Alert -->
      <div v-if="successMessage" class="alert-success mb-4">
        <svg class="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12,2A10,10 0 0,1 22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2M12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12A8,8 0 0,0 12,4M11,16.5L6.5,12L7.91,10.59L11,13.67L16.59,8.09L18,9.5L11,16.5Z"/>
        </svg>
        <span class="flex-1">{{ successMessage }}</span>
      </div>

      <!-- Error Alert -->
      <div v-if="errorMessage" class="alert-error mb-4">
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

      <!-- Form -->
      <form v-if="!successMessage" @submit.prevent="handleSubmit" class="space-y-4">
        <div>
          <label for="email" class="label">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            class="input"
            placeholder="you@example.com"
            :disabled="loading"
          />
        </div>

        <button type="submit" class="btn-primary w-full" :disabled="loading">
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          Send Reset Link
        </button>
      </form>

      <!-- Back to Login link -->
      <div class="text-center mt-6">
        <button @click="goToLogin" class="text-sm text-primary-500 hover:text-primary-600 font-medium">
          Back to Sign In
        </button>
      </div>
    </div>
  </div>
</template>
