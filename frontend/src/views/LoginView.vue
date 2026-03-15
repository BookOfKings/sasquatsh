<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})

const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')

async function handleEmailLogin() {
  if (!form.email || !form.password) {
    errorMessage.value = 'Please fill in all fields'
    return
  }

  loading.value = true
  errorMessage.value = ''

  const result = await auth.loginWithEmail(form.email, form.password)

  if (result.ok) {
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}

async function handleGoogleLogin() {
  loading.value = true
  errorMessage.value = ''

  const result = await auth.loginWithGoogle()

  if (result.ok) {
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}

function goToSignup() {
  router.push('/signup')
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4 bg-stone-50">
    <div class="card p-6 max-w-md w-full">
      <!-- Header -->
      <div class="text-center mb-6">
        <img src="/logo.png" alt="Sasquatsh" class="w-32 h-32 mx-auto" />
        <h1 class="text-xl font-bold text-primary-500 mt-2">Sasquatsh</h1>
        <p class="text-sm text-gray-500">Sign in to your account</p>
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
      <form @submit.prevent="handleEmailLogin" class="space-y-4">
        <div>
          <label for="email" class="label">Email</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            class="input"
            placeholder="you@example.com"
            :disabled="loading"
          />
        </div>

        <div>
          <label for="password" class="label">Password</label>
          <div class="relative">
            <input
              id="password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="input pr-10"
              placeholder="Enter your password"
              :disabled="loading"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              <svg v-if="showPassword" class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M11.83,9L15,12.16C15,12.11 15,12.05 15,12A3,3 0 0,0 12,9C11.94,9 11.89,9 11.83,9M7.53,9.8L9.08,11.35C9.03,11.56 9,11.77 9,12A3,3 0 0,0 12,15C12.22,15 12.44,14.97 12.65,14.92L14.2,16.47C13.53,16.8 12.79,17 12,17A5,5 0 0,1 7,12C7,11.21 7.2,10.47 7.53,9.8M2,4.27L4.28,6.55L4.73,7C3.08,8.3 1.78,10 1,12C2.73,16.39 7,19.5 12,19.5C13.55,19.5 15.03,19.2 16.37,18.66L16.74,19L19.73,22L21,20.73L3.27,3M12,7A5,5 0 0,1 17,12C17,12.64 16.87,13.26 16.64,13.82L19.57,16.75C21.07,15.5 22.27,13.86 23,12C21.27,7.61 17,4.5 12,4.5C10.6,4.5 9.26,4.75 8,5.2L10.17,7.35C10.74,7.13 11.35,7 12,7Z"/>
              </svg>
              <svg v-else class="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12,9A3,3 0 0,0 9,12A3,3 0 0,0 12,15A3,3 0 0,0 15,12A3,3 0 0,0 12,9M12,17A5,5 0 0,1 7,12A5,5 0 0,1 12,7A5,5 0 0,1 17,12A5,5 0 0,1 12,17M12,4.5C7,4.5 2.73,7.61 1,12C2.73,16.39 7,19.5 12,19.5C17,19.5 21.27,16.39 23,12C21.27,7.61 17,4.5 12,4.5Z"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="flex justify-end">
          <router-link to="/forgot-password" class="text-sm text-primary-500 hover:text-primary-600">
            Forgot Password?
          </router-link>
        </div>

        <button type="submit" class="btn-primary w-full" :disabled="loading">
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          Sign In
        </button>
      </form>

      <!-- Divider -->
      <div class="flex items-center my-6">
        <hr class="flex-1 border-gray-200" />
        <span class="mx-3 text-sm text-gray-400">or</span>
        <hr class="flex-1 border-gray-200" />
      </div>

      <!-- Google Login -->
      <button
        @click="handleGoogleLogin"
        class="btn w-full border border-gray-300 bg-white hover:bg-gray-50"
        :disabled="loading"
      >
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        Continue with Google
      </button>

      <!-- Sign up link -->
      <div class="text-center mt-6">
        <span class="text-sm text-gray-500">Don't have an account?</span>
        <button @click="goToSignup" class="text-sm text-primary-500 hover:text-primary-600 font-medium ml-1">
          Sign up
        </button>
      </div>
    </div>
  </div>
</template>
