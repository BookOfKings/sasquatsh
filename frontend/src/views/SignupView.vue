<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { checkUsernameAvailable } from '@/services/authApi'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({
  displayName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  ageConfirmed: false,
})

// Pre-fill email from query param (e.g., from invitation link)
onMounted(() => {
  if (route.query.email && typeof route.query.email === 'string') {
    form.email = route.query.email
  }
})

const showPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')

// Username validation state
const usernameChecking = ref(false)
const usernameAvailable = ref<boolean | null>(null)
const usernameError = ref('')

// Debounce timer for username check
let usernameCheckTimer: ReturnType<typeof setTimeout> | null = null

// Username format regex (matches backend)
const USERNAME_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{2,29}$/

// Watch username changes and check availability
watch(() => form.username, (newUsername) => {
  // Reset state
  usernameAvailable.value = null
  usernameError.value = ''

  // Clear previous timer
  if (usernameCheckTimer) {
    clearTimeout(usernameCheckTimer)
  }

  if (!newUsername) {
    return
  }

  // Validate format first
  if (!USERNAME_REGEX.test(newUsername)) {
    if (newUsername.length < 3) {
      usernameError.value = 'Username must be at least 3 characters'
    } else if (newUsername.length > 30) {
      usernameError.value = 'Username must be at most 30 characters'
    } else if (!/^[a-zA-Z]/.test(newUsername)) {
      usernameError.value = 'Username must start with a letter'
    } else {
      usernameError.value = 'Only letters, numbers, and underscores allowed'
    }
    return
  }

  // Debounce the availability check
  usernameCheckTimer = setTimeout(async () => {
    usernameChecking.value = true
    try {
      const result = await checkUsernameAvailable(newUsername)
      usernameAvailable.value = result.available
      if (!result.available && result.reason) {
        usernameError.value = result.reason
      }
    } catch (err) {
      console.error('Failed to check username:', err)
    } finally {
      usernameChecking.value = false
    }
  }, 500)
})

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
          const token = await grecaptcha.execute('6LfZ-HcsAAAAALniN1xOkc_I5t443MorPE66H0CK', { action: 'signup' })
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

async function handleEmailSignup() {
  if (!form.displayName || !form.username || !form.email || !form.password) {
    errorMessage.value = 'Please fill in all fields'
    return
  }

  if (!form.ageConfirmed) {
    errorMessage.value = 'You must confirm you are at least 13 years old'
    return
  }

  // Validate username format
  if (!USERNAME_REGEX.test(form.username)) {
    errorMessage.value = 'Please enter a valid username'
    return
  }

  // Check if username is available
  if (usernameAvailable.value === false) {
    errorMessage.value = 'Please choose a different username'
    return
  }

  if (form.password !== form.confirmPassword) {
    errorMessage.value = 'Passwords do not match'
    return
  }

  if (form.password.length < 6) {
    errorMessage.value = 'Password must be at least 6 characters'
    return
  }

  loading.value = true
  errorMessage.value = ''

  // Get reCAPTCHA token
  const recaptchaToken = await getRecaptchaToken()

  const result = await auth.signupWithEmail(
    form.email,
    form.password,
    form.displayName,
    form.username,
    recaptchaToken
  )

  if (result.ok) {
    // Redirect to original destination or dashboard
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } else {
    errorMessage.value = result.message
  }

  loading.value = false
}

async function handleGoogleSignup() {
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

async function handleAppleSignup() {
  loading.value = true
  errorMessage.value = ''

  const result = await auth.loginWithApple()

  if (result.ok) {
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
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
        <img src="/logo.png" alt="Sasquatsh" class="w-32 h-32 mx-auto" />
        <h1 class="text-xl font-bold text-primary-500 mt-2">Join Sasquatsh</h1>
        <p class="text-sm text-gray-500">Start planning epic games</p>
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
      <form @submit.prevent="handleEmailSignup" class="space-y-4">
        <div>
          <label for="displayName" class="label">Display Name</label>
          <input
            id="displayName"
            v-model="form.displayName"
            type="text"
            class="input"
            placeholder="Your name"
            :disabled="loading"
          />
          <p class="text-xs text-gray-500 mt-1">This is how you'll appear to group members and event hosts</p>
        </div>

        <div>
          <label for="username" class="label">Username</label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">@</span>
            <input
              id="username"
              v-model="form.username"
              type="text"
              class="input pl-7"
              :class="{
                'border-green-500 focus:ring-green-500': usernameAvailable === true,
                'border-red-500 focus:ring-red-500': usernameAvailable === false || usernameError
              }"
              placeholder="choose_a_username"
              :disabled="loading"
            />
            <!-- Loading indicator -->
            <div v-if="usernameChecking" class="absolute right-3 top-1/2 -translate-y-1/2">
              <svg class="animate-spin h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
              </svg>
            </div>
            <!-- Available checkmark -->
            <div v-else-if="usernameAvailable === true" class="absolute right-3 top-1/2 -translate-y-1/2">
              <svg class="h-5 w-5 text-green-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21,7L9,19L3.5,13.5L4.91,12.09L9,16.17L19.59,5.59L21,7Z"/>
              </svg>
            </div>
            <!-- Unavailable X -->
            <div v-else-if="usernameAvailable === false || usernameError" class="absolute right-3 top-1/2 -translate-y-1/2">
              <svg class="h-5 w-5 text-red-500" viewBox="0 0 24 24" fill="currentColor">
                <path d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
              </svg>
            </div>
          </div>
          <p v-if="usernameError" class="text-xs text-red-500 mt-1">{{ usernameError }}</p>
          <p v-else-if="usernameAvailable === true" class="text-xs text-green-600 mt-1">Username is available!</p>
          <p v-else class="text-xs text-gray-500 mt-1">3-30 characters, letters, numbers, and underscores only</p>
        </div>

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
              placeholder="At least 6 characters"
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

        <div>
          <label for="confirmPassword" class="label">Confirm Password</label>
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            :type="showPassword ? 'text' : 'password'"
            class="input"
            placeholder="Confirm your password"
            :disabled="loading"
          />
        </div>

        <div class="flex items-start gap-2">
          <input
            id="ageConfirmed"
            v-model="form.ageConfirmed"
            type="checkbox"
            class="mt-1 h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            :disabled="loading"
          />
          <label for="ageConfirmed" class="text-sm text-gray-600">
            I confirm that I am at least 13 years old and agree to the
            <router-link to="/terms" class="text-primary-500 hover:text-primary-600">Terms of Service</router-link>
          </label>
        </div>

        <button type="submit" class="btn-primary w-full" :disabled="loading || !form.ageConfirmed">
          <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
          </svg>
          Create Account
        </button>

        <!-- reCAPTCHA notice -->
        <p class="text-xs text-gray-400 text-center">
          This site is protected by reCAPTCHA and the Google
          <a href="https://policies.google.com/privacy" target="_blank" class="underline">Privacy Policy</a> and
          <a href="https://policies.google.com/terms" target="_blank" class="underline">Terms of Service</a> apply.
        </p>
      </form>

      <!-- Divider -->
      <div class="flex items-center my-6">
        <hr class="flex-1 border-gray-200" />
        <span class="mx-3 text-sm text-gray-400">or</span>
        <hr class="flex-1 border-gray-200" />
      </div>

      <!-- Google Signup -->
      <button
        @click="handleGoogleSignup"
        class="btn w-full border border-gray-300 bg-white hover:bg-gray-50"
        :disabled="loading"
      >
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        Sign up with Google
      </button>

      <!-- Apple Signup -->
      <button
        @click="handleAppleSignup"
        class="btn w-full border border-gray-300 bg-black hover:bg-gray-900 text-white mt-3"
        :disabled="loading"
      >
        <svg class="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
          <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.48-3.24 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.53 4.09zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
        </svg>
        Sign up with Apple
      </button>
      <p class="text-xs text-gray-400 text-center mt-2">Google and Apple users get an auto-generated username that can be changed later</p>

      <!-- Login link -->
      <div class="text-center mt-6">
        <span class="text-sm text-gray-500">Already have an account?</span>
        <button @click="goToLogin" class="text-sm text-primary-500 hover:text-primary-600 font-medium ml-1">
          Sign in
        </button>
      </div>
    </div>
  </div>
</template>
