import { computed, ref } from 'vue'
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  GoogleAuthProvider,
  EmailAuthProvider,
  signOut,
  onAuthStateChanged,
  updateProfile,
  updatePassword,
  reauthenticateWithCredential,
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
const redirectHandled = ref(false) // Track if we've already handled redirect result
const signupInProgress = ref(false) // Prevent onAuthStateChanged from interfering during signup

// Detect if user is on a mobile device
function isMobileDevice(): boolean {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

// Initialize auth state listener
async function initializeAuth(): Promise<void> {
  if (isInitialized.value) {
    return
  }

  // Always check for redirect result on mobile (sessionStorage can be unreliable)
  // This handles the case where user returns from Google OAuth redirect
  if (isMobileDevice()) {
    console.log('[Auth] Mobile device detected, checking for redirect result...')
    try {
      const result = await getRedirectResult(auth)
      console.log('[Auth] getRedirectResult:', result ? 'user found' : 'no result')
      if (result?.user) {
        console.log('[Auth] Google redirect successful, syncing user...')
        sessionStorage.removeItem('pendingGoogleRedirect')
        redirectHandled.value = true
        try {
          const idToken = await result.user.getIdToken()
          user.value = await getCurrentUser(idToken)
          firebaseUser.value = result.user
          isLoading.value = false
          isInitialized.value = true
          console.log('[Auth] User synced successfully from redirect')
          return // User is synced, we're done
        } catch (syncErr: any) {
          console.error('[Auth] Failed to sync Google user with backend:', syncErr)
          await signOut(auth)
          error.value = syncErr?.message || 'Failed to create account. Please try again.'
        }
      } else {
        console.log('[Auth] No redirect result, will check onAuthStateChanged')
      }
    } catch (err: any) {
      console.error('[Auth] Google redirect error:', err.code, err.message)
      sessionStorage.removeItem('pendingGoogleRedirect')
      if (err.code) {
        error.value = getAuthErrorMessage(err.code)
      } else {
        error.value = err?.message || 'Sign-in failed. Please try again.'
      }
    }
  } else {
    // Desktop: only check if we have the pending flag (for popup fallback)
    const pendingRedirect = sessionStorage.getItem('pendingGoogleRedirect')
    if (pendingRedirect) {
      sessionStorage.removeItem('pendingGoogleRedirect')
      try {
        const result = await getRedirectResult(auth)
        if (result?.user) {
          console.log('Google redirect successful, syncing user...')
          redirectHandled.value = true
          try {
            const idToken = await result.user.getIdToken()
            user.value = await getCurrentUser(idToken)
            firebaseUser.value = result.user
            isLoading.value = false
            isInitialized.value = true
            return // User is synced, we're done
          } catch (syncErr: any) {
            console.error('Failed to sync Google user with backend:', syncErr)
            await signOut(auth)
            error.value = syncErr?.message || 'Failed to create account. Please try again.'
          }
        }
      } catch (err: any) {
        console.error('Google redirect error:', err.code, err.message)
        if (err.code) {
          error.value = getAuthErrorMessage(err.code)
        }
      }
    }
  }

  // Set up auth state listener for normal auth flow
  console.log('[Auth] Setting up onAuthStateChanged listener')
  return new Promise((resolve) => {
    onAuthStateChanged(auth, async (fbUser) => {
      console.log('[Auth] onAuthStateChanged fired:', fbUser ? `user ${fbUser.email}` : 'no user')
      firebaseUser.value = fbUser

      if (fbUser) {
        // Skip if we already synced this user (from redirect handling above)
        if (redirectHandled.value && user.value) {
          console.log('[Auth] User already synced from redirect, skipping')
          isLoading.value = false
          isInitialized.value = true
          resolve()
          return
        }

        // Skip if signup is in progress - signup handles its own sync
        if (signupInProgress.value) {
          console.log('[Auth] Signup in progress, skipping onAuthStateChanged sync')
          firebaseUser.value = fbUser
          isLoading.value = false
          isInitialized.value = true
          resolve()
          return
        }

        console.log('[Auth] Syncing user with backend...')
        try {
          const idToken = await fbUser.getIdToken()
          user.value = await getCurrentUser(idToken)
          console.log('[Auth] User synced successfully')
        } catch (err: any) {
          console.error('[Auth] Failed to sync user with backend:', err)
          // Show error to user instead of silently failing
          error.value = err?.message || 'Failed to sync account. Please try again.'
          user.value = null
          // Sign out to prevent stuck state
          console.log('[Auth] Signing out due to sync failure')
          await signOut(auth).catch(() => {})
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
  signupInProgress.value = true

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
        firebaseUser.value = result.user
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
    signupInProgress.value = false
    isLoading.value = false
  }
}

async function loginWithGoogle(): Promise<{ ok: boolean; message: string }> {
  isLoading.value = true
  error.value = null

  try {
    const provider = new GoogleAuthProvider()

    // On mobile, always use redirect (popups are unreliable on mobile browsers)
    // On desktop, use popup for better UX
    if (isMobileDevice()) {
      console.log('Mobile detected, using redirect for Google sign-in')
      sessionStorage.setItem('pendingGoogleRedirect', 'true')
      await signInWithRedirect(auth, provider)
      // Page will redirect, so this return is just for TypeScript
      return { ok: true, message: 'Redirecting to Google...' }
    }

    // Desktop: use popup
    let result
    try {
      result = await signInWithPopup(auth, provider)
    } catch (popupErr: any) {
      // If popup was blocked or closed on desktop, fall back to redirect
      if (popupErr.code === 'auth/popup-blocked' || popupErr.code === 'auth/popup-closed-by-user') {
        console.log('Popup blocked/closed, falling back to redirect')
        sessionStorage.setItem('pendingGoogleRedirect', 'true')
        await signInWithRedirect(auth, provider)
        return { ok: true, message: 'Redirecting to Google...' }
      }
      throw popupErr
    }

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
    // Use ActionCodeSettings to redirect to our domain after password reset
    const actionCodeSettings = {
      url: 'https://sasquatsh.com/login?passwordReset=success',
      handleCodeInApp: false,
    }
    await sendPasswordResetEmail(auth, email, actionCodeSettings)
    return { ok: true, message: 'Password reset email sent. Check your inbox.' }
  } catch (err: any) {
    const message = getAuthErrorMessage(err.code)
    return { ok: false, message }
  }
}

async function changePassword(currentPassword: string, newPassword: string): Promise<{ ok: boolean; message: string }> {
  if (!firebaseUser.value || !firebaseUser.value.email) {
    return { ok: false, message: 'You must be logged in to change your password' }
  }

  try {
    // Re-authenticate with current password first
    const credential = EmailAuthProvider.credential(firebaseUser.value.email, currentPassword)
    await reauthenticateWithCredential(firebaseUser.value, credential)

    // Now update the password
    await updatePassword(firebaseUser.value, newPassword)
    return { ok: true, message: 'Password changed successfully!' }
  } catch (err: any) {
    const message = getAuthErrorMessage(err.code)
    return { ok: false, message }
  }
}

async function deleteAccount(deleteAccountFn: (token: string) => Promise<void>): Promise<{ ok: boolean; message: string }> {
  if (!firebaseUser.value) {
    return { ok: false, message: 'You must be logged in to delete your account' }
  }

  try {
    // Get token FIRST before any deletion
    const token = await firebaseUser.value.getIdToken()

    // Try to delete Firebase account FIRST
    // This checks if re-auth is needed BEFORE we touch backend data
    try {
      await firebaseUser.value.delete()
    } catch (firebaseErr: any) {
      if (firebaseErr.code === 'auth/requires-recent-login') {
        return { ok: false, message: 'For security, please sign out and sign back in before deleting your account' }
      }
      throw firebaseErr
    }

    // Firebase account deleted successfully
    // Now clean up backend data using the token we already obtained
    try {
      await deleteAccountFn(token)
    } catch (backendErr) {
      // Backend cleanup failed, but Firebase account is already gone
      // Log this but don't fail - user account is effectively deleted
      console.error('Backend cleanup failed after Firebase deletion:', backendErr)
    }

    // Clear local state
    user.value = null
    firebaseUser.value = null

    return { ok: true, message: 'Account deleted successfully' }
  } catch (err: any) {
    const message = err.message || 'Failed to delete account'
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
    changePassword,
    deleteAccount,
    getIdToken,
    refreshUser,
    updateUserData,
  }
}
