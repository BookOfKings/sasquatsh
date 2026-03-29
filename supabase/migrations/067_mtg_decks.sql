-- MTG Decks
-- User deck collection and deck contents

-- Main decks table
CREATE TABLE IF NOT EXISTS mtg_decks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- Basic info
  name VARCHAR(100) NOT NULL,
  description TEXT,
  format_id VARCHAR(50) REFERENCES mtg_formats(id),

  -- Commander info (for Commander/Brawl/Oathbreaker formats)
  commander_scryfall_id VARCHAR(50),
  partner_commander_scryfall_id VARCHAR(50),

  -- Metadata
  power_level SMALLINT CHECK (power_level >= 1 AND power_level <= 10),
  is_public BOOLEAN DEFAULT false,

  -- External imports
  moxfield_id VARCHAR(50),
  archidekt_id VARCHAR(50),
  import_url TEXT,

  -- Stats (cached for performance)
  card_count INTEGER DEFAULT 0,
  colors VARCHAR(5)[] DEFAULT '{}',  -- Array of W, U, B, R, G
  estimated_price_usd DECIMAL(10, 2),

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Deck cards table
CREATE TABLE IF NOT EXISTS mtg_deck_cards (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  deck_id UUID NOT NULL REFERENCES mtg_decks(id) ON DELETE CASCADE,
  scryfall_id VARCHAR(50) NOT NULL,

  quantity SMALLINT NOT NULL DEFAULT 1 CHECK (quantity >= 1 AND quantity <= 99),
  board VARCHAR(20) NOT NULL DEFAULT 'main'
    CHECK (board IN ('main', 'sideboard', 'maybeboard', 'commander')),

  -- Cache card name for quick display
  card_name VARCHAR(200),

  created_at TIMESTAMPTZ DEFAULT NOW(),

  -- Unique constraint: one entry per card per board per deck
  CONSTRAINT unique_card_per_board UNIQUE(deck_id, scryfall_id, board)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_mtg_decks_owner ON mtg_decks(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_mtg_decks_format ON mtg_decks(format_id);
CREATE INDEX IF NOT EXISTS idx_mtg_decks_public ON mtg_decks(is_public) WHERE is_public = true;
CREATE INDEX IF NOT EXISTS idx_mtg_decks_commander ON mtg_decks(commander_scryfall_id) WHERE commander_scryfall_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_mtg_deck_cards_deck ON mtg_deck_cards(deck_id);
CREATE INDEX IF NOT EXISTS idx_mtg_deck_cards_scryfall ON mtg_deck_cards(scryfall_id);

-- Update triggers
CREATE OR REPLACE FUNCTION update_mtg_deck_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS mtg_deck_updated ON mtg_decks;
CREATE TRIGGER mtg_deck_updated
  BEFORE UPDATE ON mtg_decks
  FOR EACH ROW
  EXECUTE FUNCTION update_mtg_deck_timestamp();

-- Update deck card count when cards change
CREATE OR REPLACE FUNCTION update_deck_card_count()
RETURNS TRIGGER AS $$
DECLARE
  deck_uuid UUID;
BEGIN
  IF TG_OP = 'DELETE' THEN
    deck_uuid := OLD.deck_id;
  ELSE
    deck_uuid := NEW.deck_id;
  END IF;

  UPDATE mtg_decks
  SET card_count = (
    SELECT COALESCE(SUM(quantity), 0)
    FROM mtg_deck_cards
    WHERE deck_id = deck_uuid
    AND board IN ('main', 'commander')
  ),
  updated_at = NOW()
  WHERE id = deck_uuid;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS deck_cards_count_update ON mtg_deck_cards;
CREATE TRIGGER deck_cards_count_update
  AFTER INSERT OR UPDATE OR DELETE ON mtg_deck_cards
  FOR EACH ROW
  EXECUTE FUNCTION update_deck_card_count();

-- RLS Policies
ALTER TABLE mtg_decks ENABLE ROW LEVEL SECURITY;
ALTER TABLE mtg_deck_cards ENABLE ROW LEVEL SECURITY;

-- Decks: Owner can do everything
CREATE POLICY "mtg_decks_owner_all" ON mtg_decks
  FOR ALL
  USING (owner_user_id = auth.uid());

-- Decks: Anyone can view public decks
CREATE POLICY "mtg_decks_select_public" ON mtg_decks
  FOR SELECT
  USING (is_public = true);

-- Deck cards: Owner can manage cards in their decks
CREATE POLICY "mtg_deck_cards_owner_all" ON mtg_deck_cards
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM mtg_decks d
      WHERE d.id = mtg_deck_cards.deck_id
      AND d.owner_user_id = auth.uid()
    )
  );

-- Deck cards: Anyone can view cards in public decks
CREATE POLICY "mtg_deck_cards_select_public" ON mtg_deck_cards
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM mtg_decks d
      WHERE d.id = mtg_deck_cards.deck_id
      AND d.is_public = true
    )
  );

COMMENT ON TABLE mtg_decks IS 'User MTG deck collection';
COMMENT ON TABLE mtg_deck_cards IS 'Cards in MTG decks';
