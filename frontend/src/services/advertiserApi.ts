const FUNCTIONS_URL = import.meta.env.VITE_SUPABASE_FUNCTIONS_URL
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY

export interface AdvertiserAd {
  id: string
  user_id: string
  stripe_subscription_id: string | null
  ad_tier: string
  status: string
  title: string
  description: string
  image_url: string | null
  link_url: string
  target_city: string | null
  target_state: string | null
  impression_count: number
  click_count: number
  started_at: string | null
  expires_at: string | null
  created_at: string
}

export async function getMyAds(token: string): Promise<AdvertiserAd[]> {
  const response = await fetch(`${FUNCTIONS_URL}/ad-checkout?action=my-ads`, {
    headers: {
      'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
      'X-Firebase-Token': token,
    },
  })
  if (!response.ok) throw new Error('Failed to fetch ads')
  const data = await response.json()
  return data.ads
}
