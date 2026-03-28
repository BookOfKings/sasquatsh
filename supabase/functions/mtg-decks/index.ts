import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verifyFirebaseToken, createResponders, getCorsHeaders, getFirebaseToken } from '../_shared/firebase.ts'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

interface DeckCard {
  scryfallId: string
  quantity: number
  board: 'main' | 'sideboard' | 'maybeboard' | 'commander'
  cardName?: string
}

interface CreateDeckInput {
  name: string
  formatId?: string
  description?: string
  powerLevel?: number
  isPublic?: boolean
  commanderScryfallId?: string
  partnerCommanderScryfallId?: string
  cards?: DeckCard[]
}

interface UpdateDeckInput {
  name?: string
  formatId?: string
  description?: string
  powerLevel?: number
  isPublic?: boolean
  commanderScryfallId?: string
  partnerCommanderScryfallId?: string
}

interface ImportDeckInput {
  source: 'moxfield' | 'archidekt' | 'text'
  url?: string
  deckList?: string
  name?: string
  formatId?: string
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: getCorsHeaders(req) })
  }

  const { json: jsonResponse, error: errorResponse } = createResponders(req)

  const token = getFirebaseToken(req)
  if (!token) {
    return errorResponse('Missing Firebase token', 401)
  }

  const firebaseUser = await verifyFirebaseToken(token)
  if (!firebaseUser) {
    return errorResponse('Invalid Firebase token', 401)
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey)

  // Get user from database
  const { data: user, error: userError } = await supabase
    .from('users')
    .select('id')
    .eq('firebase_uid', firebaseUser.uid)
    .single()

  if (userError || !user) {
    return errorResponse('User not found', 404)
  }

  const url = new URL(req.url)
  const deckId = url.searchParams.get('id')
  const action = url.searchParams.get('action')

  // GET - List decks or get single deck
  if (req.method === 'GET') {
    if (deckId) {
      // Get single deck with cards
      const { data: deck, error } = await supabase
        .from('mtg_decks')
        .select(`
          *,
          format:mtg_formats(*),
          cards:mtg_deck_cards(
            id, scryfall_id, quantity, board, card_name
          )
        `)
        .eq('id', deckId)
        .single()

      if (error) {
        if (error.code === 'PGRST116') {
          return errorResponse('Deck not found', 404)
        }
        return errorResponse(error.message, 500)
      }

      // Check access: owner or public
      if (deck.owner_user_id !== user.id && !deck.is_public) {
        return errorResponse('Access denied', 403)
      }

      // Fetch card data from cache
      const scryfallIds = deck.cards.map((c: { scryfall_id: string }) => c.scryfall_id)
      let cardData: Record<string, unknown> = {}

      if (scryfallIds.length > 0) {
        const { data: cards } = await supabase
          .from('scryfall_cards_cache')
          .select('*')
          .in('scryfall_id', scryfallIds)

        if (cards) {
          cardData = Object.fromEntries(cards.map(c => [c.scryfall_id, c]))
        }
      }

      // Enrich cards with cached data
      const enrichedCards = deck.cards.map((c: { scryfall_id: string }) => ({
        ...c,
        card: cardData[c.scryfall_id] || null
      }))

      return jsonResponse({
        ...deck,
        cards: enrichedCards
      })
    }

    // List user's decks
    const formatId = url.searchParams.get('format')
    const publicOnly = url.searchParams.get('public') === 'true'

    let query = supabase
      .from('mtg_decks')
      .select(`
        id, name, description, format_id, commander_scryfall_id, partner_commander_scryfall_id,
        power_level, is_public, card_count, colors, created_at, updated_at,
        format:mtg_formats(id, name)
      `)
      .order('updated_at', { ascending: false })

    if (publicOnly) {
      query = query.eq('is_public', true)
    } else {
      query = query.eq('owner_user_id', user.id)
    }

    if (formatId) {
      query = query.eq('format_id', formatId)
    }

    const { data: decks, error } = await query

    if (error) {
      return errorResponse(error.message, 500)
    }

    // Get commander card data for display
    const commanderIds = decks
      .flatMap(d => [d.commander_scryfall_id, d.partner_commander_scryfall_id])
      .filter(Boolean)

    let commanderData: Record<string, unknown> = {}
    if (commanderIds.length > 0) {
      const { data: commanders } = await supabase
        .from('scryfall_cards_cache')
        .select('scryfall_id, name, image_small, image_normal, colors, color_identity')
        .in('scryfall_id', commanderIds)

      if (commanders) {
        commanderData = Object.fromEntries(commanders.map(c => [c.scryfall_id, c]))
      }
    }

    // Enrich decks with commander data
    const enrichedDecks = decks.map(d => ({
      ...d,
      commander: d.commander_scryfall_id ? commanderData[d.commander_scryfall_id] || null : null,
      partnerCommander: d.partner_commander_scryfall_id ? commanderData[d.partner_commander_scryfall_id] || null : null
    }))

    return jsonResponse({ decks: enrichedDecks })
  }

  // POST - Create deck or import
  if (req.method === 'POST') {
    const body = await req.json()

    // Import deck
    if (action === 'import') {
      const input = body as ImportDeckInput

      if (input.source === 'text' && input.deckList) {
        // Parse deck list
        const cards = parseDeckList(input.deckList)

        // Create deck
        const { data: deck, error: deckError } = await supabase
          .from('mtg_decks')
          .insert({
            owner_user_id: user.id,
            name: input.name || 'Imported Deck',
            format_id: input.formatId || null,
          })
          .select()
          .single()

        if (deckError) {
          return errorResponse(deckError.message, 500)
        }

        // Add cards
        if (cards.length > 0) {
          const cardRows = cards.map(c => ({
            deck_id: deck.id,
            scryfall_id: c.scryfallId || `pending:${c.name}`,
            quantity: c.quantity,
            board: c.board || 'main',
            card_name: c.name
          }))

          await supabase
            .from('mtg_deck_cards')
            .insert(cardRows)
        }

        return jsonResponse({ deck, imported: cards.length })
      }

      if (input.source === 'moxfield' && input.url) {
        // TODO: Implement Moxfield import
        return errorResponse('Moxfield import not yet implemented', 501)
      }

      if (input.source === 'archidekt' && input.url) {
        // TODO: Implement Archidekt import
        return errorResponse('Archidekt import not yet implemented', 501)
      }

      return errorResponse('Invalid import source or missing data', 400)
    }

    // Create new deck
    const input = body as CreateDeckInput

    if (!input.name) {
      return errorResponse('Deck name is required', 400)
    }

    const { data: deck, error: deckError } = await supabase
      .from('mtg_decks')
      .insert({
        owner_user_id: user.id,
        name: input.name,
        format_id: input.formatId || null,
        description: input.description || null,
        power_level: input.powerLevel || null,
        is_public: input.isPublic ?? false,
        commander_scryfall_id: input.commanderScryfallId || null,
        partner_commander_scryfall_id: input.partnerCommanderScryfallId || null,
      })
      .select()
      .single()

    if (deckError) {
      return errorResponse(deckError.message, 500)
    }

    // Add initial cards if provided
    if (input.cards && input.cards.length > 0) {
      const cardRows = input.cards.map(c => ({
        deck_id: deck.id,
        scryfall_id: c.scryfallId,
        quantity: c.quantity,
        board: c.board || 'main',
        card_name: c.cardName || null
      }))

      await supabase
        .from('mtg_deck_cards')
        .insert(cardRows)
    }

    return jsonResponse({ deck }, 201)
  }

  // PUT - Update deck
  if (req.method === 'PUT') {
    if (!deckId) {
      return errorResponse('Deck ID required', 400)
    }

    // Verify ownership
    const { data: existing, error: existingError } = await supabase
      .from('mtg_decks')
      .select('owner_user_id')
      .eq('id', deckId)
      .single()

    if (existingError || !existing) {
      return errorResponse('Deck not found', 404)
    }

    if (existing.owner_user_id !== user.id) {
      return errorResponse('Access denied', 403)
    }

    const body = await req.json() as UpdateDeckInput

    const updates: Record<string, unknown> = {}
    if (body.name !== undefined) updates.name = body.name
    if (body.formatId !== undefined) updates.format_id = body.formatId
    if (body.description !== undefined) updates.description = body.description
    if (body.powerLevel !== undefined) updates.power_level = body.powerLevel
    if (body.isPublic !== undefined) updates.is_public = body.isPublic
    if (body.commanderScryfallId !== undefined) updates.commander_scryfall_id = body.commanderScryfallId
    if (body.partnerCommanderScryfallId !== undefined) updates.partner_commander_scryfall_id = body.partnerCommanderScryfallId

    const { data: deck, error: updateError } = await supabase
      .from('mtg_decks')
      .update(updates)
      .eq('id', deckId)
      .select()
      .single()

    if (updateError) {
      return errorResponse(updateError.message, 500)
    }

    return jsonResponse({ deck })
  }

  // PATCH - Add/update/remove cards
  if (req.method === 'PATCH') {
    if (!deckId) {
      return errorResponse('Deck ID required', 400)
    }

    // Verify ownership
    const { data: existing, error: existingError } = await supabase
      .from('mtg_decks')
      .select('owner_user_id')
      .eq('id', deckId)
      .single()

    if (existingError || !existing) {
      return errorResponse('Deck not found', 404)
    }

    if (existing.owner_user_id !== user.id) {
      return errorResponse('Access denied', 403)
    }

    const body = await req.json()

    // Add cards
    if (body.add && Array.isArray(body.add)) {
      const cardRows = body.add.map((c: DeckCard) => ({
        deck_id: deckId,
        scryfall_id: c.scryfallId,
        quantity: c.quantity,
        board: c.board || 'main',
        card_name: c.cardName || null
      }))

      // Use upsert to handle duplicates
      const { error } = await supabase
        .from('mtg_deck_cards')
        .upsert(cardRows, {
          onConflict: 'deck_id,scryfall_id,board',
          ignoreDuplicates: false
        })

      if (error) {
        return errorResponse(error.message, 500)
      }
    }

    // Update card quantities
    if (body.update && Array.isArray(body.update)) {
      for (const update of body.update) {
        await supabase
          .from('mtg_deck_cards')
          .update({ quantity: update.quantity })
          .eq('deck_id', deckId)
          .eq('scryfall_id', update.scryfallId)
          .eq('board', update.board || 'main')
      }
    }

    // Remove cards
    if (body.remove && Array.isArray(body.remove)) {
      for (const remove of body.remove) {
        await supabase
          .from('mtg_deck_cards')
          .delete()
          .eq('deck_id', deckId)
          .eq('scryfall_id', remove.scryfallId)
          .eq('board', remove.board || 'main')
      }
    }

    // Get updated deck
    const { data: deck, error: deckError } = await supabase
      .from('mtg_decks')
      .select(`
        *,
        cards:mtg_deck_cards(id, scryfall_id, quantity, board, card_name)
      `)
      .eq('id', deckId)
      .single()

    if (deckError) {
      return errorResponse(deckError.message, 500)
    }

    return jsonResponse({ deck })
  }

  // DELETE - Delete deck
  if (req.method === 'DELETE') {
    if (!deckId) {
      return errorResponse('Deck ID required', 400)
    }

    // Verify ownership
    const { data: existing, error: existingError } = await supabase
      .from('mtg_decks')
      .select('owner_user_id')
      .eq('id', deckId)
      .single()

    if (existingError || !existing) {
      return errorResponse('Deck not found', 404)
    }

    if (existing.owner_user_id !== user.id) {
      return errorResponse('Access denied', 403)
    }

    const { error: deleteError } = await supabase
      .from('mtg_decks')
      .delete()
      .eq('id', deckId)

    if (deleteError) {
      return errorResponse(deleteError.message, 500)
    }

    return jsonResponse({ success: true })
  }

  return errorResponse('Method not allowed', 405)
})

// Parse deck list text into card entries
function parseDeckList(deckList: string): Array<{ quantity: number; name: string; board: string; scryfallId?: string }> {
  const lines = deckList.split('\n').filter(line => line.trim())
  const entries: Array<{ quantity: number; name: string; board: string; scryfallId?: string }> = []
  let currentBoard = 'main'

  for (const line of lines) {
    const trimmed = line.trim()

    // Skip comments
    if (trimmed.startsWith('//') || trimmed.startsWith('#')) continue

    // Detect board sections
    if (trimmed.toLowerCase().startsWith('sideboard')) {
      currentBoard = 'sideboard'
      continue
    }
    if (trimmed.toLowerCase().startsWith('commander')) {
      currentBoard = 'commander'
      continue
    }
    if (trimmed.toLowerCase().startsWith('maybeboard')) {
      currentBoard = 'maybeboard'
      continue
    }

    // Parse card line: "4 Lightning Bolt" or "4x Lightning Bolt"
    let match = trimmed.match(/^(\d+)x?\s+(.+?)(?:\s+\([^)]+\))?$/i)
    if (!match) {
      // Try "Card Name x4" format
      match = trimmed.match(/^(.+?)\s+x?(\d+)$/i)
      if (match) {
        match = [match[0], match[2], match[1]]
      }
    }

    if (match) {
      const quantity = parseInt(match[1], 10)
      const name = match[2].trim()
      if (quantity > 0 && name) {
        entries.push({ quantity, name, board: currentBoard })
      }
    } else if (trimmed && !trimmed.match(/^\d+$/)) {
      // Single card with no quantity
      entries.push({ quantity: 1, name: trimmed, board: currentBoard })
    }
  }

  return entries
}
