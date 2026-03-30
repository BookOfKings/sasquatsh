-- Pokemon TCG Event Configuration
-- Stores Pokemon TCG-specific settings for events

CREATE TABLE IF NOT EXISTS pokemon_event_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,

  -- Format
  format_id VARCHAR(50) REFERENCES pokemon_formats(id),
  custom_format_name VARCHAR(100),

  -- Event type
  event_type VARCHAR(30) NOT NULL DEFAULT 'casual'
    CHECK (event_type IN (
      'casual',           -- Casual play
      'league',           -- Pokemon League play
      'league_cup',       -- League Cup tournament
      'league_challenge', -- League Challenge
      'regional',         -- Regional Championship
      'international',    -- International Championship
      'worlds',           -- World Championship
      'prerelease',       -- Prerelease event
      'draft'             -- Booster draft
    )),

  -- Tournament structure
  tournament_style VARCHAR(20) CHECK (tournament_style IN ('swiss', 'single_elim', 'double_elim')),
  rounds_count SMALLINT,
  round_time_minutes SMALLINT DEFAULT 50,
  best_of SMALLINT DEFAULT 1 CHECK (best_of IN (1, 3)),
  top_cut SMALLINT,

  -- Deck rules
  allow_proxies BOOLEAN DEFAULT false,
  proxy_limit SMALLINT,
  require_deck_registration BOOLEAN DEFAULT false,
  deck_submission_deadline TIMESTAMPTZ,
  allow_deck_changes BOOLEAN DEFAULT false,

  -- Prize support
  has_prizes BOOLEAN DEFAULT false,
  prize_structure TEXT,
  entry_fee DECIMAL(10, 2),
  entry_fee_currency VARCHAR(3) DEFAULT 'USD',

  -- Play settings
  use_play_points BOOLEAN DEFAULT false,  -- Official Play! Pokemon points

  -- Age divisions (for official events)
  has_junior_division BOOLEAN DEFAULT false,
  has_senior_division BOOLEAN DEFAULT false,
  has_masters_division BOOLEAN DEFAULT true,

  -- Spectators
  allow_spectators BOOLEAN DEFAULT true,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT one_pokemon_config_per_event UNIQUE(event_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_pokemon_event_config_event_id ON pokemon_event_config(event_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_event_config_format_id ON pokemon_event_config(format_id);

-- Update trigger
CREATE OR REPLACE FUNCTION update_pokemon_event_config_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS pokemon_event_config_updated ON pokemon_event_config;
CREATE TRIGGER pokemon_event_config_updated
  BEFORE UPDATE ON pokemon_event_config
  FOR EACH ROW
  EXECUTE FUNCTION update_pokemon_event_config_timestamp();

-- RLS Policies
ALTER TABLE pokemon_event_config ENABLE ROW LEVEL SECURITY;

-- Anyone can view Pokemon config for public events
CREATE POLICY "pokemon_event_config_select_public" ON pokemon_event_config
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = pokemon_event_config.event_id
      AND e.is_public = true
    )
  );

-- Event host can view/manage their event's Pokemon config
CREATE POLICY "pokemon_event_config_select_host" ON pokemon_event_config
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = pokemon_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "pokemon_event_config_insert_host" ON pokemon_event_config
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = pokemon_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "pokemon_event_config_update_host" ON pokemon_event_config
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = pokemon_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "pokemon_event_config_delete_host" ON pokemon_event_config
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = pokemon_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Group admins can manage Pokemon config for group events
CREATE POLICY "pokemon_event_config_manage_group_admin" ON pokemon_event_config
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM events e
      JOIN group_memberships gm ON gm.group_id = e.group_id
      WHERE e.id = pokemon_event_config.event_id
      AND gm.user_id = auth.uid()
      AND gm.role IN ('owner', 'admin')
    )
  );

COMMENT ON TABLE pokemon_event_config IS 'Pokemon TCG-specific configuration for events';
