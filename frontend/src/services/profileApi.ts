import type { UserProfile, PublicProfile, UpdateProfileInput, BlockedUser } from '@/types/profile'
import { compressAvatar } from '@/utils/imageUtils'

const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

// Helper to make authenticated requests
async function authenticatedRequest<T>(
  path: string,
  token: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${FUNCTIONS_URL}${path}`, {
    ...options,
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
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
  const response = await fetch(`${FUNCTIONS_URL}/profile?id=${userId}`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'Content-Type': 'application/json',
    },
  })

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

// ============ Blocked Users ============

// Get list of blocked users with details
export async function getBlockedUsers(token: string): Promise<BlockedUser[]> {
  return authenticatedRequest<BlockedUser[]>('/profile?include=blocked', token)
}

// Block a user
export async function blockUser(
  token: string,
  userId: string
): Promise<{ message: string; blockedUserIds: string[] }> {
  return authenticatedRequest<{ message: string; blockedUserIds: string[] }>(
    `/profile?action=block&userId=${userId}`,
    token,
    { method: 'POST' }
  )
}

// Unblock a user
export async function unblockUser(
  token: string,
  userId: string
): Promise<{ message: string; blockedUserIds: string[] }> {
  return authenticatedRequest<{ message: string; blockedUserIds: string[] }>(
    `/profile?action=unblock&userId=${userId}`,
    token,
    { method: 'POST' }
  )
}

// ============ Avatar Management ============

// Upload avatar image (with automatic compression)
export async function uploadAvatar(
  token: string,
  file: File
): Promise<{ message: string; avatarUrl: string; user: UserProfile }> {
  // Compress image before upload for better performance
  let fileToUpload = file

  try {
    fileToUpload = await compressAvatar(file)
    if (fileToUpload !== file) {
      console.log(`Compressed avatar from ${(file.size / 1024).toFixed(0)}KB to ${(fileToUpload.size / 1024).toFixed(0)}KB`)
    }
  } catch (err) {
    console.warn('Avatar compression failed, uploading original:', err)
  }

  const formData = new FormData()
  formData.append('avatar', fileToUpload)

  const response = await fetch(`${FUNCTIONS_URL}/profile?action=upload-avatar`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
      // Don't set Content-Type - browser will set it with boundary for multipart
    },
    body: formData,
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

  return response.json()
}

// Delete avatar image
export async function deleteAvatar(
  token: string
): Promise<{ message: string; user: UserProfile }> {
  return authenticatedRequest<{ message: string; user: UserProfile }>(
    '/profile?action=delete-avatar',
    token,
    { method: 'POST' }
  )
}
