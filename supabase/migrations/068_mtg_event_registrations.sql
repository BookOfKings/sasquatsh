-- MTG Event Registrations
-- Links event registrations to decks with snapshots

CREATE TABLE IF NOT EXISTS mtg_event_registrations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  registration_id UUID NOT NULL REFERENCES event_registrations(id) ON DELETE CASCADE,

  -- Deck selection
  deck_id UUID REFERENCES mtg_decks(id) ON DELETE SET NULL,

  -- Deck snapshot at submission time (frozen copy)
  deck_snapshot JSONB,

  -- Quick display fields (cached from deck/commander)
  commander_name VARCHAR(200),
  commander_image_url TEXT,
  partner_commander_name VARCHAR(200),
  partner_commander_image_url TEXT,
  deck_colors VARCHAR(5)[] DEFAULT '{}',

  -- Submission tracking
  submitted_at TIMESTAMPTZ,
  is_confirmed BOOLEAN DEFAULT false,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT one_mtg_reg_per_registration UNIQUE(registration_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_mtg_event_reg_registration ON mtg_event_registrations(registration_id);
CREATE INDEX IF NOT EXISTS idx_mtg_event_reg_deck ON mtg_event_registrations(deck_id);

-- Update trigger
CREATE OR REPLACE FUNCTION update_mtg_event_registration_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS mtg_event_registration_updated ON mtg_event_registrations;
CREATE TRIGGER mtg_event_registration_updated
  BEFORE UPDATE ON mtg_event_registrations
  FOR EACH ROW
  EXECUTE FUNCTION update_mtg_event_registration_timestamp();

-- RLS Policies
ALTER TABLE mtg_event_registrations ENABLE ROW LEVEL SECURITY;

-- Users can view their own MTG registrations
CREATE POLICY "mtg_event_reg_select_own" ON mtg_event_registrations
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      WHERE er.id = mtg_event_registrations.registration_id
      AND er.user_id = auth.uid()
    )
  );

-- Users can manage their own MTG registrations
CREATE POLICY "mtg_event_reg_insert_own" ON mtg_event_registrations
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM event_registrations er
      WHERE er.id = mtg_event_registrations.registration_id
      AND er.user_id = auth.uid()
    )
  );

CREATE POLICY "mtg_event_reg_update_own" ON mtg_event_registrations
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      WHERE er.id = mtg_event_registrations.registration_id
      AND er.user_id = auth.uid()
    )
  );

-- Event hosts can view registrations for their events
CREATE POLICY "mtg_event_reg_select_host" ON mtg_event_registrations
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      JOIN events e ON e.id = er.event_id
      WHERE er.id = mtg_event_registrations.registration_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Anyone can view MTG registrations for public events (commander display)
CREATE POLICY "mtg_event_reg_select_public" ON mtg_event_registrations
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_registrations er
      JOIN events e ON e.id = er.event_id
      WHERE er.id = mtg_event_registrations.registration_id
      AND e.is_public = true
    )
  );

COMMENT ON TABLE mtg_event_registrations IS 'MTG-specific registration data with deck snapshots';
