import { computed, ref } from 'vue'
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  updateProfile,
  sendEmailVerification,
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

// Initialize auth state listener
function initializeAuth(): Promise<void> {
  return new Promise((resolve) => {
    if (isInitialized.value) {
      resolve()
      return
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
    blockedUserIds: computed(() => user.value?.blockedUserIds ?? []),
    isLoading: computed(() => isLoading.value),
    error: computed(() => error.value),
    isInitialized: computed(() => isInitialized.value),
    initializeAuth,
    loginWithEmail,
    signupWithEmail,
    loginWithGoogle,
    logout,
    getIdToken,
  }
}
