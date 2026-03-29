-- Pokemon TCG Decks
-- User deck collection and deck contents

-- Main decks table
CREATE TABLE IF NOT EXISTS pokemon_decks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- Basic info
  name VARCHAR(100) NOT NULL,
  description TEXT,
  format_id VARCHAR(50) REFERENCES pokemon_formats(id),

  -- Featured Pokemon (for display)
  featured_pokemon_tcg_id VARCHAR(50),
  featured_pokemon_name VARCHAR(200),
  featured_pokemon_image TEXT,

  -- Deck archetype (e.g., "Charizard ex", "Lugia VSTAR")
  archetype VARCHAR(100),

  -- Metadata
  is_public BOOLEAN DEFAULT false,

  -- External imports
  pokemon_tcg_live_code VARCHAR(50),  -- PTCGL deck code
  limitless_tcg_id VARCHAR(50),        -- LimitlessTCG deck ID
  import_url TEXT,

  -- Stats (cached for performance)
  card_count INTEGER DEFAULT 0,
  pokemon_count INTEGER DEFAULT 0,
  trainer_count INTEGER DEFAULT 0,
  energy_count INTEGER DEFAULT 0,
  estimated_price_usd DECIMAL(10, 2),

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Deck cards table
CREATE TABLE IF NOT EXISTS pokemon_deck_cards (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  deck_id UUID NOT NULL REFERENCES pokemon_decks(id) ON DELETE CASCADE,
  pokemon_tcg_id VARCHAR(50) NOT NULL,

  quantity SMALLINT NOT NULL DEFAULT 1 CHECK (quantity >= 1 AND quantity <= 60),

  -- Cache card info for quick display
  card_name VARCHAR(200),
  supertype VARCHAR(50),  -- Pokemon, Trainer, Energy

  created_at TIMESTAMPTZ DEFAULT NOW(),

  -- Unique constraint: one entry per card per deck
  CONSTRAINT unique_pokemon_card_per_deck UNIQUE(deck_id, pokemon_tcg_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_pokemon_decks_owner ON pokemon_decks(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_decks_format ON pokemon_decks(format_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_decks_public ON pokemon_decks(is_public) WHERE is_public = true;
CREATE INDEX IF NOT EXISTS idx_pokemon_decks_archetype ON pokemon_decks(archetype) WHERE archetype IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pokemon_deck_cards_deck ON pokemon_deck_cards(deck_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_deck_cards_tcg_id ON pokemon_deck_cards(pokemon_tcg_id);

-- Update triggers
CREATE OR REPLACE FUNCTION update_pokemon_deck_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS pokemon_deck_updated ON pokemon_decks;
CREATE TRIGGER pokemon_deck_updated
  BEFORE UPDATE ON pokemon_decks
  FOR EACH ROW
  EXECUTE FUNCTION update_pokemon_deck_timestamp();

-- Update deck counts when cards change
CREATE OR REPLACE FUNCTION update_pokemon_deck_counts()
RETURNS TRIGGER AS $$
DECLARE
  deck_uuid UUID;
BEGIN
  IF TG_OP = 'DELETE' THEN
    deck_uuid := OLD.deck_id;
  ELSE
    deck_uuid := NEW.deck_id;
  END IF;

  UPDATE pokemon_decks
  SET
    card_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
    ),
    pokemon_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
      AND supertype = 'Pokémon'
    ),
    trainer_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
      AND supertype = 'Trainer'
    ),
    energy_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
      AND supertype = 'Energy'
    ),
    updated_at = NOW()
  WHERE id = deck_uuid;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS pokemon_deck_cards_count_update ON pokemon_deck_cards;
CREATE TRIGGER pokemon_deck_cards_count_update
  AFTER INSERT OR UPDATE OR DELETE ON pokemon_deck_cards
  FOR EACH ROW
  EXECUTE FUNCTION update_pokemon_deck_counts();

-- RLS Policies
ALTER TABLE pokemon_decks ENABLE ROW LEVEL SECURITY;
ALTER TABLE pokemon_deck_cards ENABLE ROW LEVEL SECURITY;

-- Decks: Owner can do everything
CREATE POLICY "pokemon_decks_owner_all" ON pokemon_decks
  FOR ALL
  USING (owner_user_id = auth.uid());

-- Decks: Anyone can view public decks
CREATE POLICY "pokemon_decks_select_public" ON pokemon_decks
  FOR SELECT
  USING (is_public = true);

-- Deck cards: Owner can manage cards in their decks
CREATE POLICY "pokemon_deck_cards_owner_all" ON pokemon_deck_cards
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM pokemon_decks d
      WHERE d.id = pokemon_deck_cards.deck_id
      AND d.owner_user_id = auth.uid()
    )
  );

-- Deck cards: Anyone can view cards in public decks
CREATE POLICY "pokemon_deck_cards_select_public" ON pokemon_deck_cards
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM pokemon_decks d
      WHERE d.id = pokemon_deck_cards.deck_id
      AND d.is_public = true
    )
  );

COMMENT ON TABLE pokemon_decks IS 'User Pokemon TCG deck collection';
COMMENT ON TABLE pokemon_deck_cards IS 'Cards in Pokemon TCG decks';
