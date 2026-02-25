import type { UserProfile, PublicProfile, UpdateProfileInput } from '@/types/profile'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  if (!response.ok) {
    let message = response.statusText
    try {
      const data = await response.json()
      if (data?.error) message = data.error
      if (data?.message) message = data.message
    } catch {
      // no JSON body
    }
    throw new Error(message)
  }

  return response.json() as Promise<T>
}

// Get current user's profile (authenticated)
export async function getMyProfile(token: string): Promise<UserProfile> {
  return authenticatedRequest<UserProfile>('/profile', token)
}

// Get public profile by user ID
export async function getPublicProfile(userId: string): Promise<PublicProfile> {
  const response = await fetch(`${FUNCTIONS_URL}/profile?id=${userId}`)

  if (!response.ok) {
    throw new Error('Failed to fetch profile')
  }

  return response.json() as Promise<PublicProfile>
}

// Update current user's profile
export async function updateProfile(
  token: string,
  data: UpdateProfileInput
): Promise<UserProfile> {
  return authenticatedRequest<UserProfile>('/profile', token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}
