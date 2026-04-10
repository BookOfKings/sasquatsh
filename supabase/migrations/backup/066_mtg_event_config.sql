-- MTG Event Configuration
-- Stores MTG-specific settings for events

CREATE TABLE IF NOT EXISTS mtg_event_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,

  -- Format
  format_id VARCHAR(50) REFERENCES mtg_formats(id),
  custom_format_name VARCHAR(100),

  -- Event structure
  event_type VARCHAR(20) NOT NULL DEFAULT 'casual'
    CHECK (event_type IN ('casual', 'swiss', 'single_elim', 'double_elim', 'round_robin', 'pods')),
  rounds_count SMALLINT,
  round_time_minutes SMALLINT DEFAULT 50,
  pods_size SMALLINT DEFAULT 4,

  -- Deck rules
  allow_proxies BOOLEAN DEFAULT false,
  proxy_limit SMALLINT,
  power_level_min SMALLINT CHECK (power_level_min >= 1 AND power_level_min <= 10),
  power_level_max SMALLINT CHECK (power_level_max >= 1 AND power_level_max <= 10),
  banned_cards TEXT[] DEFAULT '{}',

  -- Limited/Draft specific
  packs_per_player SMALLINT,
  draft_style VARCHAR(20) CHECK (draft_style IN ('standard', 'rochester', 'winston', 'grid')),
  cube_id UUID,

  -- Prize support
  has_prizes BOOLEAN DEFAULT false,
  prize_structure TEXT,
  entry_fee DECIMAL(10, 2),
  entry_fee_currency VARCHAR(3) DEFAULT 'USD',

  -- Deck registration
  require_deck_registration BOOLEAN DEFAULT false,
  deck_submission_deadline TIMESTAMPTZ,

  -- Spectators
  allow_spectators BOOLEAN DEFAULT true,

  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT valid_power_level_range CHECK (
    power_level_min IS NULL OR power_level_max IS NULL OR power_level_min <= power_level_max
  ),
  CONSTRAINT one_config_per_event UNIQUE(event_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_mtg_event_config_event_id ON mtg_event_config(event_id);
CREATE INDEX IF NOT EXISTS idx_mtg_event_config_format_id ON mtg_event_config(format_id);

-- Update trigger
CREATE OR REPLACE FUNCTION update_mtg_event_config_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS mtg_event_config_updated ON mtg_event_config;
CREATE TRIGGER mtg_event_config_updated
  BEFORE UPDATE ON mtg_event_config
  FOR EACH ROW
  EXECUTE FUNCTION update_mtg_event_config_timestamp();

-- RLS Policies
ALTER TABLE mtg_event_config ENABLE ROW LEVEL SECURITY;

-- Anyone can view MTG config for public events
CREATE POLICY "mtg_event_config_select_public" ON mtg_event_config
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = mtg_event_config.event_id
      AND e.is_public = true
    )
  );

-- Event host can view/manage their event's MTG config
CREATE POLICY "mtg_event_config_select_host" ON mtg_event_config
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = mtg_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "mtg_event_config_insert_host" ON mtg_event_config
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = mtg_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "mtg_event_config_update_host" ON mtg_event_config
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = mtg_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

CREATE POLICY "mtg_event_config_delete_host" ON mtg_event_config
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = mtg_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Group admins can manage MTG config for group events
CREATE POLICY "mtg_event_config_manage_group_admin" ON mtg_event_config
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM events e
      JOIN group_memberships gm ON gm.group_id = e.group_id
      WHERE e.id = mtg_event_config.event_id
      AND gm.user_id = auth.uid()
      AND gm.role IN ('owner', 'admin')
    )
  );

COMMENT ON TABLE mtg_event_config IS 'MTG-specific configuration for events';
