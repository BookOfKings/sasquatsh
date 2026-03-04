/**
 * Seed script to populate the zip_codes table with US zip code data
 *
 * Usage:
 *   npx ts-node supabase/scripts/seed-zip-codes.ts
 *
 * Or if running from project root:
 *   npx tsx supabase/scripts/seed-zip-codes.ts
 *
 * Requires SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY environment variables
 */

import { createClient } from '@supabase/supabase-js'

const SUPABASE_URL = process.env.SUPABASE_URL || process.env.VITE_SUPABASE_URL
const SUPABASE_SERVICE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY

if (!SUPABASE_URL || !SUPABASE_SERVICE_KEY) {
  console.error('Missing required environment variables:')
  console.error('  SUPABASE_URL (or VITE_SUPABASE_URL)')
  console.error('  SUPABASE_SERVICE_ROLE_KEY')
  process.exit(1)
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY)

// Free zip code data source - using a publicly available dataset
const ZIP_DATA_URL = 'https://raw.githubusercontent.com/millbj92/US-Zip-Codes-JSON/master/USCities.json'

interface ZipCodeEntry {
  zip_code: string
  latitude: number
  longitude: number
  city: string
  state: string
  timezone?: string
}

async function fetchZipCodes(): Promise<ZipCodeEntry[]> {
  console.log('Fetching zip code data...')

  const response = await fetch(ZIP_DATA_URL)
  if (!response.ok) {
    throw new Error(`Failed to fetch zip codes: ${response.statusText}`)
  }

  const data = await response.json()

  // The dataset format is an array of objects with these fields
  return data.map((entry: Record<string, unknown>) => ({
    zip_code: String(entry.zip_code).padStart(5, '0'),
    latitude: Number(entry.latitude),
    longitude: Number(entry.longitude),
    city: String(entry.city),
    state: String(entry.state),
    timezone: entry.timezone as string | undefined,
  }))
}

async function seedZipCodes() {
  try {
    const zipCodes = await fetchZipCodes()
    console.log(`Fetched ${zipCodes.length} zip codes`)

    // Insert in batches of 1000
    const BATCH_SIZE = 1000
    let inserted = 0

    for (let i = 0; i < zipCodes.length; i += BATCH_SIZE) {
      const batch = zipCodes.slice(i, i + BATCH_SIZE).map(z => ({
        zip: z.zip_code,
        city: z.city,
        state: z.state,
        latitude: z.latitude,
        longitude: z.longitude,
        timezone: z.timezone || null,
      }))

      const { error } = await supabase
        .from('zip_codes')
        .upsert(batch, { onConflict: 'zip' })

      if (error) {
        console.error(`Error inserting batch at ${i}:`, error.message)
      } else {
        inserted += batch.length
        console.log(`Inserted ${inserted}/${zipCodes.length} zip codes`)
      }
    }

    console.log('Done! Zip codes seeded successfully.')

    // Verify count
    const { count } = await supabase
      .from('zip_codes')
      .select('*', { count: 'exact', head: true })

    console.log(`Total zip codes in database: ${count}`)

  } catch (error) {
    console.error('Failed to seed zip codes:', error)
    process.exit(1)
  }
}

seedZipCodes()
