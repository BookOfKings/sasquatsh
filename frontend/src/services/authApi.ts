import type { User } from '@/types/user'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:5191'

export async function getCurrentUser(idToken: string): Promise<User> {
  const response = await fetch(`${BASE_URL}/api/auth/me`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${idToken}`,
      'Content-Type': 'application/json',
    },
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || 'Failed to get user')
  }

  return response.json()
}

export async function updateUser(
  idToken: string,
  data: { displayName?: string }
): Promise<User> {
  const response = await fetch(`${BASE_URL}/api/auth/me`, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${idToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || 'Failed to update user')
  }

  return response.json()
}
