-- Pokemon TCG Event Registrations
-- Links event registrations to decks with snapshots

CREATE TABLE IF NOT EXISTS pokemon_event_registrations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  registration_id UUID NOT NULL REFERENCES event_registrations(id) ON DELETE CASCADE,

  -- Deck selection
  deck_id UUID REFERENCES pokemon_decks(id) ON DELETE SET NULL,

  -- Deck snapshot at submission time (frozen copy)
  deck_snapshot JSONB,

  -- Quick display fields (cached from deck)
  featured_pokemon_name VARCHAR(200),
  featured_pokemon_image TEXT,
  deck_archetype VARCHAR(100),

  -- Age division for official events
  age_division VARCHAR(20) CHECK (age_division IN ('junior', 'senior', 'masters')),

  -- Submission tracking
  submitted_at TIMESTAMPTZ,
  is_confirmed BOOLEAN DEFAULT false,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT one_pokemon_reg_per_registration UNIQUE(registration_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_pokemon_event_reg_registration ON pokemon_event_registrations(registration_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_event_reg_deck ON pokemon_event_registrations(deck_id);

-- Update trigger
CREATE OR REPLACE FUNCTION update_pokemon_event_registration_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS pokemon_event_registration_updated ON pokemon_event_registrations;
CREATE TRIGGER pokemon_event_registration_updated
  BEFORE UPDATE ON pokemon_event_registrations
  FOR EACH ROW
  EXECUTE FUNCTION update_pokemon_event_registration_timestamp();

-- RLS Policies
ALTER TABLE pokemon_event_registrations ENABLE ROW LEVEL SECURITY;

-- Users can view their own Pokemon registrations
CREATE POLICY "pokemon_event_reg_select_own" ON pokemon_event_registrations
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      WHERE er.id = pokemon_event_registrations.registration_id
      AND er.user_id = auth.uid()
    )
  );

-- Users can manage their own Pokemon registrations
CREATE POLICY "pokemon_event_reg_insert_own" ON pokemon_event_registrations
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM event_registrations er
      WHERE er.id = pokemon_event_registrations.registration_id
      AND er.user_id = auth.uid()
    )
  );

CREATE POLICY "pokemon_event_reg_update_own" ON pokemon_event_registrations
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      WHERE er.id = pokemon_event_registrations.registration_id
      AND er.user_id = auth.uid()
    )
  );

-- Event hosts can view registrations for their events
CREATE POLICY "pokemon_event_reg_select_host" ON pokemon_event_registrations
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      JOIN events e ON e.id = er.event_id
      WHERE er.id = pokemon_event_registrations.registration_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Anyone can view Pokemon registrations for public events
CREATE POLICY "pokemon_event_reg_select_public" ON pokemon_event_registrations
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      JOIN events e ON e.id = er.event_id
      WHERE er.id = pokemon_event_registrations.registration_id
      AND e.is_public = true
    )
  );

COMMENT ON TABLE pokemon_event_registrations IS 'Pokemon TCG-specific registration data with deck snapshots';
