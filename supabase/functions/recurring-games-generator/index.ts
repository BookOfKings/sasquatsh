import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { getCorsHeaders } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

// ── Utility: compute next occurrence date ──────────────────────────────
function computeNextOccurrence(
  frequency: string,
  dayOfWeek: number,
  monthlyWeek: number | null,
  afterDate: Date
): string {
  if (frequency === 'weekly') {
    // Next matching dayOfWeek on or after afterDate
    const d = new Date(afterDate)
    const diff = (dayOfWeek - d.getUTCDay() + 7) % 7
    d.setUTCDate(d.getUTCDate() + (diff === 0 ? 0 : diff))
    return d.toISOString().slice(0, 10)
  }

  if (frequency === 'biweekly') {
    // Next matching dayOfWeek at least 14 days after afterDate
    const d = new Date(afterDate)
    d.setUTCDate(d.getUTCDate() + 14)
    const diff = (dayOfWeek - d.getUTCDay() + 7) % 7
    d.setUTCDate(d.getUTCDate() + diff)
    return d.toISOString().slice(0, 10)
  }

  if (frequency === 'monthly') {
    // Nth dayOfWeek of next month (monthlyWeek=1 means 1st, -1 means last)
    const d = new Date(afterDate)
    let year = d.getUTCFullYear()
    let month = d.getUTCMonth() + 1
    if (month > 11) {
      month = 0
      year++
    }

    if (monthlyWeek === -1) {
      // Last occurrence of dayOfWeek in the month
      const lastDay = new Date(Date.UTC(year, month + 1, 0))
      const diff = (lastDay.getUTCDay() - dayOfWeek + 7) % 7
      lastDay.setUTCDate(lastDay.getUTCDate() - diff)
      return lastDay.toISOString().slice(0, 10)
    }

    // Nth occurrence (1-4)
    const week = monthlyWeek ?? 1
    const firstOfMonth = new Date(Date.UTC(year, month, 1))
    const firstDayDiff = (dayOfWeek - firstOfMonth.getUTCDay() + 7) % 7
    const nthDate = 1 + firstDayDiff + (week - 1) * 7
    const result = new Date(Date.UTC(year, month, nthDate))
    return result.toISOString().slice(0, 10)
  }

  // Fallback: weekly
  return computeNextOccurrence('weekly', dayOfWeek, null, afterDate)
}

Deno.serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const corsHeaders = getCorsHeaders(req)

  // This function runs as a cron job with service role — no auth required
  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  const generated: string[] = []
  let skipped = 0
  const errors: string[] = []

  try {
    // Find all active recurring games whose next occurrence is within 14 days
    const { data: recurringGames, error: fetchError } = await supabase
      .from('recurring_games')
      .select('*')
      .eq('is_active', true)
      .lte('next_occurrence_date', new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10))

    if (fetchError) {
      return new Response(
        JSON.stringify({ error: `Failed to fetch recurring games: ${fetchError.message}` }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    if (!recurringGames || recurringGames.length === 0) {
      return new Response(
        JSON.stringify({ generated: 0, skipped: 0, errors: [] }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    for (const rg of recurringGames) {
      try {
        const nextDate = rg.next_occurrence_date

        // Check for duplicate: has an event already been created for this date?
        const { data: existing, error: dupError } = await supabase
          .from('events')
          .select('id')
          .eq('from_recurring_game_id', rg.id)
          .eq('event_date', nextDate)
          .maybeSingle()

        if (dupError) {
          errors.push(`Error checking duplicate for recurring game ${rg.id}: ${dupError.message}`)
          continue
        }

        if (existing) {
          // Event already exists for this date, skip but advance the next_occurrence_date
          skipped++

          // Compute new next_occurrence_date based on the current one
          const afterDate = new Date(nextDate)
          afterDate.setUTCDate(afterDate.getUTCDate() + 1) // day after current occurrence
          const newNextDate = computeNextOccurrence(
            rg.frequency,
            rg.day_of_week,
            rg.monthly_week,
            afterDate
          )

          await supabase
            .from('recurring_games')
            .update({
              last_generated_date: nextDate,
              next_occurrence_date: newNextDate,
            })
            .eq('id', rg.id)

          continue
        }

        // Insert new event
        const { error: insertError } = await supabase
          .from('events')
          .insert({
            host_user_id: rg.host_user_id,
            group_id: rg.group_id,
            title: rg.title,
            description: rg.description,
            game_system: rg.game_system ?? 'board_game',
            game_title: rg.game_title,
            event_date: nextDate,
            start_time: rg.start_time,
            timezone: rg.timezone,
            duration_minutes: rg.duration_minutes ?? 120,
            max_players: rg.max_players ?? 4,
            host_is_playing: rg.host_is_playing ?? true,
            is_public: rg.is_public ?? true,
            status: 'published',
            event_location_id: rg.event_location_id,
            address_line1: rg.address_line1,
            city: rg.city,
            state: rg.state,
            postal_code: rg.postal_code,
            location_details: rg.location_details,
            from_recurring_game_id: rg.id,
          })

        if (insertError) {
          errors.push(`Failed to create event for recurring game ${rg.id}: ${insertError.message}`)
          continue
        }

        generated.push(rg.id)

        // Update recurring game: set last_generated_date and compute new next_occurrence_date
        const afterDate = new Date(nextDate)
        afterDate.setUTCDate(afterDate.getUTCDate() + 1) // day after current occurrence
        const newNextDate = computeNextOccurrence(
          rg.frequency,
          rg.day_of_week,
          rg.monthly_week,
          afterDate
        )

        const { error: updateError } = await supabase
          .from('recurring_games')
          .update({
            last_generated_date: nextDate,
            next_occurrence_date: newNextDate,
          })
          .eq('id', rg.id)

        if (updateError) {
          errors.push(`Event created but failed to update recurring game ${rg.id}: ${updateError.message}`)
        }
      } catch (err) {
        errors.push(`Unexpected error for recurring game ${rg.id}: ${(err as Error).message}`)
      }
    }
  } catch (err) {
    return new Response(
      JSON.stringify({ error: `Unexpected error: ${(err as Error).message}` }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  return new Response(
    JSON.stringify({
      generated: generated.length,
      skipped,
      errors,
    }),
    { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
  )
})
