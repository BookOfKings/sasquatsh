-- Yu-Gi-Oh! TCG Event Configuration
-- Stores Yu-Gi-Oh! TCG-specific settings for events

CREATE TABLE IF NOT EXISTS yugioh_event_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,

  -- Format
  format_id VARCHAR(50),  -- 'advanced', 'traditional', 'speed_duel', 'time_wizard', 'casual'
  custom_format_name VARCHAR(100),

  -- Event type
  event_type VARCHAR(30) NOT NULL DEFAULT 'casual'
    CHECK (event_type IN (
      'casual',     -- Casual play
      'locals',     -- Local store tournament
      'ots',        -- Official Tournament Store event
      'regional',   -- Regional Championship
      'ycs',        -- Yu-Gi-Oh! Championship Series
      'nationals',  -- National Championship
      'worlds'      -- World Championship
    )),

  -- Tournament structure
  tournament_style VARCHAR(30) CHECK (tournament_style IN ('swiss', 'single_elimination', 'double_elimination')),
  rounds_count SMALLINT,
  round_time_minutes SMALLINT DEFAULT 40,
  best_of SMALLINT DEFAULT 3 CHECK (best_of IN (1, 3)),
  top_cut SMALLINT,

  -- Deck rules
  allow_proxies BOOLEAN DEFAULT false,
  proxy_limit SMALLINT,
  require_deck_registration BOOLEAN DEFAULT false,
  deck_submission_deadline TIMESTAMPTZ,
  allow_side_deck BOOLEAN DEFAULT true,
  enforce_format_legality BOOLEAN DEFAULT true,
  house_rules_notes TEXT,

  -- Prize support
  has_prizes BOOLEAN DEFAULT false,
  prize_structure TEXT,
  entry_fee DECIMAL(10, 2),
  entry_fee_currency VARCHAR(3) DEFAULT 'USD',

  -- Official play
  is_official_event BOOLEAN DEFAULT false,
  awards_ots_points BOOLEAN DEFAULT false,

  -- Spectators
  allow_spectators BOOLEAN DEFAULT true,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT one_yugioh_config_per_event UNIQUE(event_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_yugioh_event_config_event_id ON yugioh_event_config(event_id);
CREATE INDEX IF NOT EXISTS idx_yugioh_event_config_format_id ON yugioh_event_config(format_id);
CREATE INDEX IF NOT EXISTS idx_yugioh_event_config_event_type ON yugioh_event_config(event_type);

-- Update trigger
CREATE OR REPLACE FUNCTION update_yugioh_event_config_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS yugioh_event_config_updated ON yugioh_event_config;
CREATE TRIGGER yugioh_event_config_updated
  BEFORE UPDATE ON yugioh_event_config
  FOR EACH ROW
  EXECUTE FUNCTION update_yugioh_event_config_timestamp();

-- RLS Policies
ALTER TABLE yugioh_event_config ENABLE ROW LEVEL SECURITY;

-- Anyone can view Yu-Gi-Oh! config for public events
CREATE POLICY "yugioh_event_config_select_public" ON yugioh_event_config
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = yugioh_event_config.event_id
      AND e.is_public = true
    )
  );

-- Event host can view/manage their event's Yu-Gi-Oh! config
CREATE POLICY "yugioh_event_config_select_host" ON yugioh_event_config
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = yugioh_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "yugioh_event_config_insert_host" ON yugioh_event_config
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = yugioh_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "yugioh_event_config_update_host" ON yugioh_event_config
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = yugioh_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "yugioh_event_config_delete_host" ON yugioh_event_config
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = yugioh_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Group admins can manage Yu-Gi-Oh! config for group events
CREATE POLICY "yugioh_event_config_manage_group_admin" ON yugioh_event_config
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM events e
      JOIN group_memberships gm ON gm.group_id = e.group_id
      WHERE e.id = yugioh_event_config.event_id
      AND gm.user_id = auth.uid()
      AND gm.role IN ('owner', 'admin')
    )
  );

-- Update events table game_system CHECK constraint to include 'yugioh'
-- First drop the old constraint, then add the new one
ALTER TABLE events DROP CONSTRAINT IF EXISTS events_game_system_check;
ALTER TABLE events ADD CONSTRAINT events_game_system_check
  CHECK (game_system IN ('board_game', 'mtg', 'pokemon_tcg', 'yugioh'));

COMMENT ON TABLE yugioh_event_config IS 'Yu-Gi-Oh! TCG-specific configuration for events';
