import type { User } from '@/types/user'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY
const USE_FIREBASE_EMULATOR = import.meta.env.VITE_USE_FIREBASE_EMULATOR === 'true'

export interface UsernameCheckResult {
  available: boolean
  reason?: string
}

// Check if a username is available (no auth required)
export async function checkUsernameAvailable(username: string): Promise<UsernameCheckResult> {
  const response = await fetch(`${FUNCTIONS_URL}/check-username?username=${encodeURIComponent(username)}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
    },
  })

  if (!response.ok) {
    return { available: false, reason: 'Failed to check username' }
  }

  return response.json()
}

export interface SyncUserOptions {
  username?: string
  recaptchaToken?: string
}

export async function getCurrentUser(idToken: string, options?: SyncUserOptions): Promise<User> {
  // In emulator mode, skip the backend call entirely and return a mock user
  // (emulator tokens aren't valid for production backend, and CORS blocks the request)
  if (USE_FIREBASE_EMULATOR) {
    console.log('[Emulator Mode] Skipping auth-sync, using mock user')
    // Decode the Firebase ID token to get basic user info
    // Firebase ID tokens are JWTs - extract the payload
    try {
      const payload = JSON.parse(atob(idToken.split('.')[1] || ''))
      return {
        id: `emulator-${payload.user_id || payload.sub}`,
        email: payload.email || 'emulator@test.com',
        displayName: payload.name || options?.username || 'Emulator User',
        username: options?.username || `emulator_${Date.now()}`,
        avatarUrl: undefined,
        isAdmin: false,
        isFoundingMember: false,
        blockedUserIds: [],
        createdAt: new Date().toISOString(),
        subscriptionTier: 'free',
      }
    } catch (e) {
      console.warn('[Emulator Mode] Failed to decode token, using default mock user')
      return {
        id: `emulator-${Date.now()}`,
        email: 'emulator@test.com',
        displayName: options?.username || 'Emulator User',
        username: options?.username || `emulator_${Date.now()}`,
        avatarUrl: undefined,
        isAdmin: false,
        isFoundingMember: false,
        blockedUserIds: [],
        createdAt: new Date().toISOString(),
        subscriptionTier: 'free',
      }
    }
  }

  const response = await fetch(`${FUNCTIONS_URL}/auth-sync`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': idToken,
      'Content-Type': 'application/json',
    },
    body: options ? JSON.stringify(options) : undefined,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || error.error || 'Failed to get user')
  }

  return response.json()
}

export async function updateUser(
  idToken: string,
  data: { displayName?: string }
): Promise<User> {
  // In emulator mode, return a mock updated user
  if (USE_FIREBASE_EMULATOR) {
    console.log('[Emulator Mode] Skipping auth-sync update, returning mock user')
    try {
      const payload = JSON.parse(atob(idToken.split('.')[1] || ''))
      return {
        id: `emulator-${payload.user_id || payload.sub}`,
        email: payload.email || 'emulator@test.com',
        displayName: data.displayName || payload.name || 'Emulator User',
        username: `emulator_${Date.now()}`,
        avatarUrl: undefined,
        isAdmin: false,
        isFoundingMember: false,
        blockedUserIds: [],
        createdAt: new Date().toISOString(),
        subscriptionTier: 'free',
      }
    } catch {
      return {
        id: `emulator-${Date.now()}`,
        email: 'emulator@test.com',
        displayName: data.displayName || 'Emulator User',
        username: `emulator_${Date.now()}`,
        avatarUrl: undefined,
        isAdmin: false,
        isFoundingMember: false,
        blockedUserIds: [],
        createdAt: new Date().toISOString(),
        subscriptionTier: 'free',
      }
    }
  }

  const response = await fetch(`${FUNCTIONS_URL}/auth-sync`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': idToken,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || error.error || 'Failed to update user')
  }

  return response.json()
}
