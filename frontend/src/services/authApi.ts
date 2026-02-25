import type { User } from '@/types/user'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export async function getCurrentUser(idToken: string): Promise<User> {
  const response = await fetch(`${FUNCTIONS_URL}/auth-sync`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': idToken,
      'Content-Type': 'application/json',
    },
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
