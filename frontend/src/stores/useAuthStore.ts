import { computed, ref } from 'vue'
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  updateProfile,
  sendEmailVerification,
  sendPasswordResetEmail,
  type User as FirebaseUser,
} from 'firebase/auth'
import { auth } from '@/services/firebase'
import { getCurrentUser } from '@/services/authApi'
import type { User } from '@/types/user'

const firebaseUser = ref<FirebaseUser | null>(null)
const user = ref<User | null>(null)
const isLoading = ref(true)
const error = ref<string | null>(null)
const isInitialized = ref(false)

// Detect if user is on a mobile device
function isMobileDevice(): boolean {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

// Initialize auth state listener
function initializeAuth(): Promise<void> {
  return new Promise((resolve) => {
    if (isInitialized.value) {
      resolve()
      return
    }

    // Handle Google redirect result (for mobile sign-in)
    // Always check on mobile - sessionStorage doesn't persist across domain redirects
    if (isMobileDevice()) {
      getRedirectResult(auth)
        .then(async (result) => {
          // result is null if there was no redirect
          if (result?.user) {
            console.log('Google redirect successful, syncing user...')
            try {
              const idToken = await result.user.getIdToken()
              user.value = await getCurrentUser(idToken)
            } catch (syncErr: any) {
              console.error('Failed to sync Google user with backend:', syncErr)
              await signOut(auth)
              error.value = syncErr?.message || 'Failed to create account. Please try again.'
            }
          }
        })
        .catch((err) => {
          console.error('Google redirect error:', err.code, err.message)
          if (err.code) {
            error.value = getAuthErrorMessage(err.code)
          }
        })
    }

    onAuthStateChanged(auth, async (fbUser) => {
      firebaseUser.value = fbUser

      if (fbUser) {
        try {
          const idToken = await fbUser.getIdToken()
          user.value = await getCurrentUser(idToken)
        } catch (err) {
          console.error('Failed to sync user with backend:', err)
          user.value = null
        }
      } else {
        user.value = null
      }

      isLoading.value = false
      isInitialized.value = true
      resolve()
    })
  })
}

async function loginWithEmail(email: string, password: string): Promise<{ ok: boolean; message: string }> {
  isLoading.value = true
  error.value = null

  try {
    const result = await signInWithEmailAndPassword(auth, email, password)

    // Wait for backend sync to complete
    if (result.user) {
      try {
        const idToken = await result.user.getIdToken()
        user.value = await getCurrentUser(idToken)
      } catch (syncErr) {
        console.error('Failed to sync user with backend:', syncErr)
      }
    }

    return { ok: true, message: 'Welcome back!' }
  } catch (err: any) {
    const message = getAuthErrorMessage(err.code)
    error.value = message
    return { ok: false, message }
  } finally {
    isLoading.value = false
  }
}

async function signupWithEmail(
  email: string,
  password: string,
  displayName: string,
  username: string,
  recaptchaToken?: string
): Promise<{ ok: boolean; message: string }> {
  isLoading.value = true
  error.value = null

  try {
    const result = await createUserWithEmailAndPassword(auth, email, password)

    // Update display name in Firebase
    if (displayName) {
      await updateProfile(result.user, { displayName })
    }

    // Wait for backend sync to complete with username and recaptcha
    if (result.user) {
      try {
        const idToken = await result.user.getIdToken()
        user.value = await getCurrentUser(idToken, { username, recaptchaToken })
      } catch (syncErr: any) {
        console.error('Failed to sync user with backend:', syncErr)
        // If backend sync fails (e.g., username taken), delete the Firebase user
        await result.user.delete()
        return { ok: false, message: syncErr.message || 'Failed to create account' }
      }

      // Send verification email (non-blocking)
      sendEmailVerification(result.user).catch((err) => {
        console.error('Failed to send verification email:', err)
      })
    }

    return { ok: true, message: 'Account created! Check your email to verify your account.' }
  } catch (err: any) {
    const message = getAuthErrorMessage(err.code)
    error.value = message
    return { ok: false, message }
  } finally {
    isLoading.value = false
  }
}

async function loginWithGoogle(): Promise<{ ok: boolean; message: string }> {
  isLoading.value = true
  error.value = null

  try {
    const provider = new GoogleAuthProvider()

    // Use redirect for mobile devices (popup doesn't work well on mobile)
    if (isMobileDevice()) {
      await signInWithRedirect(auth, provider)
      // Redirect happens, result handled in initializeAuth via getRedirectResult
      return { ok: true, message: 'Redirecting to Google...' }
    }

    // Use popup for desktop
    const result = await signInWithPopup(auth, provider)

    // Wait for backend sync to complete
    if (result.user) {
      try {
        const idToken = await result.user.getIdToken()
        user.value = await getCurrentUser(idToken)
      } catch (syncErr: any) {
        console.error('Failed to sync user with backend:', syncErr)
        // Sign out of Firebase if backend sync fails
        await signOut(auth)
        const message = syncErr?.message || 'Failed to create account. Please try again.'
        error.value = message
        return { ok: false, message }
      }
    }

    return { ok: true, message: 'Welcome!' }
  } catch (err: any) {
    console.error('Google sign-in error:', err.code, err.message)
    const message = getAuthErrorMessage(err.code)
    error.value = message
    return { ok: false, message }
  } finally {
    isLoading.value = false
  }
}

async function logout(): Promise<void> {
  await signOut(auth)
  user.value = null
}

async function resetPassword(email: string): Promise<{ ok: boolean; message: string }> {
  try {
    await sendPasswordResetEmail(auth, email)
    return { ok: true, message: 'Password reset email sent. Check your inbox.' }
  } catch (err: any) {
    const message = getAuthErrorMessage(err.code)
    return { ok: false, message }
  }
}

async function refreshUser(): Promise<void> {
  if (firebaseUser.value) {
    try {
      const idToken = await firebaseUser.value.getIdToken()
      user.value = await getCurrentUser(idToken)
    } catch (err) {
      console.error('Failed to refresh user data:', err)
    }
  }
}

function updateUserData(updates: Partial<User>): void {
  if (user.value) {
    user.value = { ...user.value, ...updates }
  }
}

async function getIdToken(): Promise<string | null> {
  return firebaseUser.value?.getIdToken() ?? null
}

function getAuthErrorMessage(code: string): string {
  switch (code) {
    case 'auth/email-already-in-use':
      return 'This email is already registered'
    case 'auth/invalid-email':
      return 'Invalid email address'
    case 'auth/operation-not-allowed':
      return 'This sign-in method is not enabled'
    case 'auth/weak-password':
      return 'Password should be at least 6 characters'
    case 'auth/user-disabled':
      return 'This account has been disabled'
    case 'auth/user-not-found':
    case 'auth/wrong-password':
    case 'auth/invalid-credential':
      return 'Invalid email or password'
    case 'auth/too-many-requests':
      return 'Too many attempts. Please try again later'
    case 'auth/popup-closed-by-user':
      return 'Sign-in was cancelled'
    default:
      return 'An error occurred. Please try again'
  }
}

export function useAuthStore() {
  return {
    user: computed(() => user.value),
    firebaseUser: computed(() => firebaseUser.value),
    isAuthenticated: computed(() => !!user.value),
    isAdmin: computed(() => user.value?.isAdmin ?? false),
    isFoundingMember: computed(() => user.value?.isFoundingMember ?? false),
    blockedUserIds: computed(() => user.value?.blockedUserIds ?? []),
    isLoading: computed(() => isLoading.value),
    error: computed(() => error.value),
    isInitialized: computed(() => isInitialized.value),
    initializeAuth,
    loginWithEmail,
    signupWithEmail,
    loginWithGoogle,
    logout,
    resetPassword,
    getIdToken,
    refreshUser,
    updateUserData,
  }
}
