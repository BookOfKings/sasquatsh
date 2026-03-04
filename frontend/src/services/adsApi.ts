const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface Ad {
  id: string
  name: string
  advertiserName: string | null
  adType: string
  placement: string
  imageUrl: string | null
  title: string | null
  description: string | null
  linkUrl: string
  targetCity: string | null
  targetState: string | null
  startDate: string | null
  endDate: string | null
  isActive: boolean
  isHouseAd: boolean
  priority: number
  createdAt: string
  updatedAt: string
}

export interface AdStats {
  id: string
  name: string
  advertiserName: string | null
  isHouseAd: boolean
  isActive: boolean
  startDate: string | null
  endDate: string | null
  impressionCount: number
  clickCount: number
  ctrPercent: number
}

export interface CreateAdInput {
  name: string
  advertiserName?: string
  adType?: string
  placement?: string
  imageUrl?: string
  title?: string
  description?: string
  linkUrl: string
  targetCity?: string
  targetState?: string
  startDate?: string
  endDate?: string
  isActive?: boolean
  isHouseAd?: boolean
  priority?: number
}

export interface UpdateAdInput extends Partial<CreateAdInput> {}

// Helper to make authenticated admin requests
async function adminRequest<T>(
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

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

// Get all ads (admin)
export async function getAllAds(token: string): Promise<Ad[]> {
  const response = await adminRequest<{ ads: Ad[] }>('/admin-stats?action=ads', token)
  return response.ads || []
}

// Get ad stats (admin)
export async function getAdStats(token: string): Promise<AdStats[]> {
  const response = await adminRequest<{ stats: AdStats[] }>('/admin-stats?action=ad-stats', token)
  return response.stats || []
}

// Create a new ad (admin)
export async function createAd(token: string, data: CreateAdInput): Promise<Ad> {
  const response = await adminRequest<{ ad: Ad }>('/admin-stats?action=create-ad', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
  return response.ad
}

// Update an ad (admin)
export async function updateAd(token: string, id: string, data: UpdateAdInput): Promise<Ad> {
  const response = await adminRequest<{ ad: Ad }>('/admin-stats?action=update-ad', token, {
    method: 'POST',
    body: JSON.stringify({ adId: id, ...data }),
  })
  return response.ad
}

// Delete an ad (admin)
export async function deleteAd(token: string, id: string): Promise<void> {
  await adminRequest<{ message: string }>('/admin-stats?action=delete-ad', token, {
    method: 'POST',
    body: JSON.stringify({ adId: id }),
  })
}

// Toggle ad active status (admin)
export async function toggleAdActive(token: string, id: string, isActive: boolean): Promise<Ad> {
  return updateAd(token, id, { isActive })
}
