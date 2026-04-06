import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createResponders, getCorsHeaders, getFirebaseToken, verifyFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

interface RaffleRow {
  id: string
  title: string
  description: string | null
  prize_name: string
  prize_description: string | null
  prize_image_url: string | null
  prize_bgg_id: number | null
  prize_value_cents: number | null
  start_date: string
  end_date: string
  terms_conditions: string | null
  mail_in_instructions: string | null
  status: 'draft' | 'active' | 'ended' | 'cancelled'
  winner_user_id: string | null
  winner_selected_at: string | null
  winner_notified_at: string | null
  winner_claimed_at: string | null
  banner_image_url: string | null
  created_at: string
  updated_at: string
}

interface RaffleEntryRow {
  id: string
  raffle_id: string
  user_id: string
  entry_type: 'host_event' | 'plan_session' | 'attend_event' | 'mail_in'
  source_id: string | null
  entry_count: number
  mail_in_name: string | null
  mail_in_address: string | null
  mail_in_verified: boolean
  created_at: string
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)
  const supabase = createClient(supabaseUrl, supabaseServiceKey)
  const url = new URL(req.url)

  const action = url.searchParams.get('action')
  const raffleId = url.searchParams.get('id')
  const adminMode = url.searchParams.get('admin')

  // Get authenticated user
  const token = getFirebaseToken(req)
  let userId: string | null = null
  let isAdmin = false

  if (token) {
    const firebaseUser = await verifyFirebaseToken(token)
    if (firebaseUser) {
      const { data: user } = await supabase
        .from('users')
        .select('id, is_admin')
        .eq('firebase_uid', firebaseUser.uid)
        .single()

      if (user) {
        userId = user.id
        isAdmin = user.is_admin
      }
    }
  }

  // ============================================
  // GET - Fetch raffles
  // ============================================
  if (req.method === 'GET') {
    // Admin: List all raffles
    if (adminMode === 'list') {
      if (!isAdmin) {
        return errorResponse('Admin access required', 403)
      }

      const { data: raffles, error } = await supabase
        .from('raffles')
        .select(`
          *,
          winner:users!winner_user_id(id, display_name, avatar_url),
          created_by:users!created_by_user_id(id, display_name)
        `)
        .order('created_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      // Get entry counts for each raffle
      const raffleIds = raffles.map((r: RaffleRow) => r.id)
      const { data: entryCounts } = await supabase
        .from('raffle_entries')
        .select('raffle_id, entry_count')
        .in('raffle_id', raffleIds)

      const countByRaffle: Record<string, { entries: number; users: number }> = {}
      if (entryCounts) {
        for (const e of entryCounts) {
          if (!countByRaffle[e.raffle_id]) {
            countByRaffle[e.raffle_id] = { entries: 0, users: 0 }
          }
          countByRaffle[e.raffle_id].entries += e.entry_count
          countByRaffle[e.raffle_id].users += 1
        }
      }

      return jsonResponse({
        raffles: raffles.map((r: RaffleRow & { winner: unknown; created_by: unknown }) => ({
          ...transformRaffle(r),
          winner: r.winner,
          createdBy: r.created_by,
          stats: countByRaffle[r.id] || { entries: 0, users: 0 },
        })),
      })
    }

    // Admin: Get raffle entries
    if (adminMode === 'entries' && raffleId) {
      if (!isAdmin) {
        return errorResponse('Admin access required', 403)
      }

      const { data: entries, error } = await supabase
        .from('raffle_entries')
        .select(`
          *,
          user:users(id, display_name, avatar_url, subscription_tier)
        `)
        .eq('raffle_id', raffleId)
        .order('created_at', { ascending: false })

      if (error) {
        return errorResponse(error.message, 500)
      }

      return jsonResponse({
        entries: entries.map((e: RaffleEntryRow & { user: unknown }) => ({
          id: e.id,
          raffleId: e.raffle_id,
          userId: e.user_id,
          entryType: e.entry_type,
          sourceId: e.source_id,
          entryCount: e.entry_count,
          mailInName: e.mail_in_name,
          mailInAddress: e.mail_in_address,
          mailInVerified: e.mail_in_verified,
          createdAt: e.created_at,
          user: e.user ? {
            id: (e.user as Record<string, unknown>).id,
            displayName: (e.user as Record<string, unknown>).display_name,
            avatarUrl: (e.user as Record<string, unknown>).avatar_url,
            subscriptionTier: (e.user as Record<string, unknown>).subscription_tier,
          } : null,
        })),
      })
    }

    // Get specific raffle by ID
    if (raffleId) {
      const query = supabase
        .from('raffles')
        .select(`
          *,
          winner:users!winner_user_id(id, display_name, avatar_url)
        `)
        .eq('id', raffleId)
        .single()

      const { data: raffle, error } = await query

      if (error) {
        return errorResponse('Raffle not found', 404)
      }

      // Non-admin users can only see active/ended raffles
      if (!isAdmin && !['active', 'ended'].includes(raffle.status)) {
        return errorResponse('Raffle not found', 404)
      }

      const result = transformRaffle(raffle)

      // If user is authenticated, include their entries
      if (userId) {
        const { data: userEntries } = await supabase
          .from('raffle_entries')
          .select('*')
          .eq('raffle_id', raffleId)
          .eq('user_id', userId)

        const totalEntries = userEntries?.reduce((sum, e) => sum + e.entry_count, 0) || 0
        Object.assign(result, {
          userEntries: userEntries?.map(transformEntry) || [],
          userTotalEntries: totalEntries,
        })
      }

      // Include total entry counts
      const { data: allEntries } = await supabase
        .from('raffle_entries')
        .select('entry_count')
        .eq('raffle_id', raffleId)

      const totalAllEntries = allEntries?.reduce((sum, e) => sum + e.entry_count, 0) || 0
      const uniqueParticipants = allEntries?.length || 0

      return jsonResponse({
        raffle: {
          ...result,
          winner: raffle.winner,
          stats: {
            totalEntries: totalAllEntries,
            uniqueParticipants,
          },
        },
      })
    }

    // Get active raffle (default)
    const now = new Date().toISOString()

    const { data: activeRaffle, error } = await supabase
      .from('raffles')
      .select(`
        *,
        winner:users!winner_user_id(id, display_name, avatar_url)
      `)
      .eq('status', 'active')
      .lte('start_date', now)
      .gt('end_date', now)
      .order('start_date', { ascending: false })
      .limit(1)
      .maybeSingle()

    if (error) {
      return errorResponse(error.message, 500)
    }

    if (!activeRaffle) {
      return jsonResponse({ raffle: null })
    }

    const result = transformRaffle(activeRaffle)

    // If user is authenticated, include their entries
    if (userId) {
      const { data: userEntries } = await supabase
        .from('raffle_entries')
        .select('*')
        .eq('raffle_id', activeRaffle.id)
        .eq('user_id', userId)

      const totalEntries = userEntries?.reduce((sum, e) => sum + e.entry_count, 0) || 0
      Object.assign(result, {
        userEntries: userEntries?.map(transformEntry) || [],
        userTotalEntries: totalEntries,
      })
    }

    // Include total entry counts
    const { data: allEntries } = await supabase
      .from('raffle_entries')
      .select('entry_count')
      .eq('raffle_id', activeRaffle.id)

    const totalAllEntries = allEntries?.reduce((sum, e) => sum + e.entry_count, 0) || 0
    const uniqueParticipants = allEntries?.length || 0

    return jsonResponse({
      raffle: {
        ...result,
        stats: {
          totalEntries: totalAllEntries,
          uniqueParticipants,
        },
      },
    })
  }

  // ============================================
  // POST - Create raffle or submit mail-in entry
  // ============================================
  if (req.method === 'POST') {
    // Submit mail-in entry
    if (action === 'mail-in') {
      const body = await req.json()
      const { name, address, raffleId: mailInRaffleId } = body

      if (!name || !address) {
        return errorResponse('Name and address are required for mail-in entry', 400)
      }

      // Get active raffle
      const targetRaffleId = mailInRaffleId || null
      let raffle

      if (targetRaffleId) {
        const { data, error } = await supabase
          .from('raffles')
          .select('id, status, start_date, end_date, mail_in_instructions')
          .eq('id', targetRaffleId)
          .eq('status', 'active')
          .single()

        if (error || !data) {
          return errorResponse('Raffle not found or not active', 404)
        }
        raffle = data
      } else {
        const now = new Date().toISOString()
        const { data, error } = await supabase
          .from('raffles')
          .select('id, status, start_date, end_date, mail_in_instructions')
          .eq('status', 'active')
          .lte('start_date', now)
          .gt('end_date', now)
          .limit(1)
          .maybeSingle()

        if (error || !data) {
          return errorResponse('No active raffle found', 404)
        }
        raffle = data
      }

      if (!raffle.mail_in_instructions) {
        return errorResponse('This raffle does not accept mail-in entries', 400)
      }

      // Create mail-in entry
      const { data: entry, error: insertError } = await supabase
        .from('raffle_entries')
        .insert({
          raffle_id: raffle.id,
          user_id: userId || null, // Can be null for anonymous mail-in
          entry_type: 'mail_in',
          entry_count: 1,
          mail_in_name: name,
          mail_in_address: address,
          mail_in_verified: false,
        })
        .select()
        .single()

      if (insertError) {
        return errorResponse(insertError.message, 500)
      }

      return jsonResponse({
        success: true,
        entryId: entry.id,
        message: 'Mail-in entry submitted. It will be verified before being included in the drawing.',
      })
    }

    // Admin: Select winner
    if (action === 'select-winner') {
      if (!isAdmin) {
        return errorResponse('Admin access required', 403)
      }

      if (!raffleId) {
        return errorResponse('Raffle ID required', 400)
      }

      // Verify raffle is ready for winner selection
      const { data: raffle, error: raffleError } = await supabase
        .from('raffles')
        .select('*')
        .eq('id', raffleId)
        .single()

      if (raffleError || !raffle) {
        return errorResponse('Raffle not found', 404)
      }

      if (raffle.status !== 'active') {
        return errorResponse('Raffle must be active to select winner', 400)
      }

      if (new Date(raffle.end_date) > new Date()) {
        return errorResponse('Raffle has not ended yet', 400)
      }

      // Get all entries with weighted random selection
      const { data: entries, error: entriesError } = await supabase
        .from('raffle_entries')
        .select('user_id, entry_count')
        .eq('raffle_id', raffleId)

      if (entriesError || !entries || entries.length === 0) {
        return errorResponse('No entries found for this raffle', 400)
      }

      // Aggregate entries by user
      const userEntries: Record<string, number> = {}
      for (const e of entries) {
        if (e.user_id) {
          userEntries[e.user_id] = (userEntries[e.user_id] || 0) + e.entry_count
        }
      }

      const userIds = Object.keys(userEntries)
      if (userIds.length === 0) {
        return errorResponse('No valid entries found', 400)
      }

      // Weighted random selection
      const totalWeight = Object.values(userEntries).reduce((a, b) => a + b, 0)
      let random = Math.random() * totalWeight
      let winnerId: string | null = null

      for (const [uid, weight] of Object.entries(userEntries)) {
        random -= weight
        if (random <= 0) {
          winnerId = uid
          break
        }
      }

      if (!winnerId) {
        winnerId = userIds[0] // Fallback
      }

      // Update raffle with winner
      const { error: updateError } = await supabase
        .from('raffles')
        .update({
          winner_user_id: winnerId,
          winner_selected_at: new Date().toISOString(),
          status: 'ended',
          updated_at: new Date().toISOString(),
        })
        .eq('id', raffleId)

      if (updateError) {
        return errorResponse(updateError.message, 500)
      }

      // Get winner details
      const { data: winner } = await supabase
        .from('users')
        .select('id, display_name, email, avatar_url')
        .eq('id', winnerId)
        .single()

      return jsonResponse({
        success: true,
        winner,
        totalEntries: totalWeight,
        winnerEntries: userEntries[winnerId],
      })
    }

    // Admin: Create new raffle
    if (!isAdmin) {
      return errorResponse('Admin access required', 403)
    }

    const body = await req.json()
    const {
      title,
      description,
      prizeName,
      prizeDescription,
      prizeImageUrl,
      prizeBggId,
      prizeValueCents,
      startDate,
      endDate,
      termsConditions,
      mailInInstructions,
      bannerImageUrl,
      status,
    } = body

    if (!title || !prizeName || !startDate || !endDate) {
      return errorResponse('Title, prize name, start date, and end date are required', 400)
    }

    const { data: raffle, error: insertError } = await supabase
      .from('raffles')
      .insert({
        title,
        description,
        prize_name: prizeName,
        prize_description: prizeDescription,
        prize_image_url: prizeImageUrl,
        prize_bgg_id: prizeBggId,
        prize_value_cents: prizeValueCents,
        start_date: startDate,
        end_date: endDate,
        terms_conditions: termsConditions,
        mail_in_instructions: mailInInstructions,
        banner_image_url: bannerImageUrl,
        status: status || 'draft',
        created_by_user_id: userId,
      })
      .select()
      .single()

    if (insertError) {
      return errorResponse(insertError.message, 500)
    }

    return jsonResponse({
      success: true,
      raffle: transformRaffle(raffle),
    })
  }

  // ============================================
  // PUT - Update raffle
  // ============================================
  if (req.method === 'PUT') {
    if (!isAdmin) {
      return errorResponse('Admin access required', 403)
    }

    if (!raffleId) {
      return errorResponse('Raffle ID required', 400)
    }

    const body = await req.json()
    const updateData: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    }

    // Map camelCase to snake_case
    const fieldMap: Record<string, string> = {
      title: 'title',
      description: 'description',
      prizeName: 'prize_name',
      prizeDescription: 'prize_description',
      prizeImageUrl: 'prize_image_url',
      prizeBggId: 'prize_bgg_id',
      prizeValueCents: 'prize_value_cents',
      startDate: 'start_date',
      endDate: 'end_date',
      termsConditions: 'terms_conditions',
      mailInInstructions: 'mail_in_instructions',
      bannerImageUrl: 'banner_image_url',
      status: 'status',
      winnerNotifiedAt: 'winner_notified_at',
      winnerClaimedAt: 'winner_claimed_at',
    }

    for (const [camel, snake] of Object.entries(fieldMap)) {
      if (body[camel] !== undefined) {
        updateData[snake] = body[camel]
      }
    }

    const { data: raffle, error } = await supabase
      .from('raffles')
      .update(updateData)
      .eq('id', raffleId)
      .select()
      .single()

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse({
      success: true,
      raffle: transformRaffle(raffle),
    })
  }

  // ============================================
  // DELETE - Delete raffle (admin only)
  // ============================================
  if (req.method === 'DELETE') {
    if (!isAdmin) {
      return errorResponse('Admin access required', 403)
    }

    if (!raffleId) {
      return errorResponse('Raffle ID required', 400)
    }

    // Check if raffle can be deleted (only draft or cancelled)
    const { data: raffle } = await supabase
      .from('raffles')
      .select('status')
      .eq('id', raffleId)
      .single()

    if (!raffle) {
      return errorResponse('Raffle not found', 404)
    }

    if (!['draft', 'cancelled'].includes(raffle.status)) {
      return errorResponse('Can only delete draft or cancelled raffles', 400)
    }

    const { error } = await supabase
      .from('raffles')
      .delete()
      .eq('id', raffleId)

    if (error) {
      return errorResponse(error.message, 500)
    }

    return jsonResponse({ success: true })
  }

  return errorResponse('Method not allowed', 405)
})

// Transform database row to API response format
function transformRaffle(row: RaffleRow) {
  return {
    id: row.id,
    title: row.title,
    description: row.description,
    prizeName: row.prize_name,
    prizeDescription: row.prize_description,
    prizeImageUrl: row.prize_image_url,
    prizeBggId: row.prize_bgg_id,
    prizeValueCents: row.prize_value_cents,
    startDate: row.start_date,
    endDate: row.end_date,
    termsConditions: row.terms_conditions,
    mailInInstructions: row.mail_in_instructions,
    status: row.status,
    winnerUserId: row.winner_user_id,
    winnerSelectedAt: row.winner_selected_at,
    winnerNotifiedAt: row.winner_notified_at,
    winnerClaimedAt: row.winner_claimed_at,
    bannerImageUrl: row.banner_image_url,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  }
}

function transformEntry(row: RaffleEntryRow) {
  return {
    id: row.id,
    raffleId: row.raffle_id,
    userId: row.user_id,
    entryType: row.entry_type,
    sourceId: row.source_id,
    entryCount: row.entry_count,
    createdAt: row.created_at,
  }
}
