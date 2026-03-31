-- ============================================================================
-- Warhammer 40,000 Event Configuration
-- ============================================================================

-- Create warhammer40k_event_config table
CREATE TABLE IF NOT EXISTS warhammer40k_event_config (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,

  -- Game setup
  game_type VARCHAR(20) NOT NULL DEFAULT 'matched'
    CHECK (game_type IN ('matched', 'narrative', 'crusade', 'open')),
  points_limit SMALLINT DEFAULT 2000,
  player_mode VARCHAR(10) DEFAULT '1v1'
    CHECK (player_mode IN ('1v1', '2v2', 'group')),

  -- Mission
  mission_pack VARCHAR(50),
  mission_notes TEXT,

  -- Army rules
  battle_ready_required BOOLEAN DEFAULT false,
  wysiwyg_required BOOLEAN DEFAULT false,
  forge_world_allowed BOOLEAN DEFAULT true,
  legends_allowed BOOLEAN DEFAULT false,
  army_rules_notes TEXT,

  -- Terrain & table
  terrain_type VARCHAR(30) DEFAULT 'casual'
    CHECK (terrain_type IN ('tournament', 'casual', 'bring_your_own')),
  table_size VARCHAR(20) DEFAULT '44x60',

  -- Game flow
  time_limit_minutes SMALLINT,

  -- Tournament
  event_type VARCHAR(20) NOT NULL DEFAULT 'casual'
    CHECK (event_type IN ('casual', 'tournament', 'campaign', 'league')),
  tournament_style VARCHAR(30)
    CHECK (tournament_style IN ('swiss', 'single_elimination', 'round_robin')),
  rounds_count SMALLINT,

  -- Prizes
  has_prizes BOOLEAN DEFAULT false,
  prize_structure TEXT,
  entry_fee DECIMAL(10, 2),
  entry_fee_currency VARCHAR(3) DEFAULT 'USD',

  -- Settings
  allow_spectators BOOLEAN DEFAULT true,
  allow_proxies BOOLEAN DEFAULT false,
  proxy_notes TEXT,

  -- Mission selection
  mission_selection VARCHAR(20) CHECK (mission_selection IN ('random', 'pre_selected')),
  pre_selected_missions TEXT[],
  secondary_objectives VARCHAR(20) CHECK (secondary_objectives IN ('tactical', 'fixed', 'custom')),

  -- Army submission
  require_army_list BOOLEAN DEFAULT false,
  army_list_deadline TIMESTAMPTZ,
  army_list_notes TEXT,

  -- Tournament extensions
  round_time_minutes SMALLINT,
  include_top_cut BOOLEAN DEFAULT false,
  scoring_type VARCHAR(20) CHECK (scoring_type IN ('win_loss', 'win_draw_loss', 'battle_points')),

  -- Crusade
  starting_supply_limit SMALLINT,
  starting_crusade_points SMALLINT,
  crusade_progression_notes TEXT,

  -- Timestamps
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT one_warhammer40k_config_per_event UNIQUE(event_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_warhammer40k_event_config_event_id
  ON warhammer40k_event_config(event_id);
CREATE INDEX IF NOT EXISTS idx_warhammer40k_event_config_game_type
  ON warhammer40k_event_config(game_type);
CREATE INDEX IF NOT EXISTS idx_warhammer40k_event_config_event_type
  ON warhammer40k_event_config(event_type);

-- Updated_at trigger
CREATE OR REPLACE FUNCTION update_warhammer40k_event_config_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS warhammer40k_event_config_updated ON warhammer40k_event_config;
CREATE TRIGGER warhammer40k_event_config_updated
  BEFORE UPDATE ON warhammer40k_event_config
  FOR EACH ROW
  EXECUTE FUNCTION update_warhammer40k_event_config_timestamp();

-- ============================================================================
-- Row Level Security
-- ============================================================================

ALTER TABLE warhammer40k_event_config ENABLE ROW LEVEL SECURITY;

-- Public can read config for public events
CREATE POLICY warhammer40k_event_config_select_public
  ON warhammer40k_event_config FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = warhammer40k_event_config.event_id
      AND e.is_public = true
    )
  );

-- Host can read their own event configs
CREATE POLICY warhammer40k_event_config_select_host
  ON warhammer40k_event_config FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = warhammer40k_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Host can insert config for their events
CREATE POLICY warhammer40k_event_config_insert_host
  ON warhammer40k_event_config FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = warhammer40k_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Host can update config for their events
CREATE POLICY warhammer40k_event_config_update_host
  ON warhammer40k_event_config FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = warhammer40k_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Host can delete config for their events
CREATE POLICY warhammer40k_event_config_delete_host
  ON warhammer40k_event_config FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM events e
      WHERE e.id = warhammer40k_event_config.event_id
      AND e.host_user_id = auth.uid()
    )
  );

-- Group admins can manage config for group events
CREATE POLICY warhammer40k_event_config_manage_group_admin
  ON warhammer40k_event_config FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM events e
      JOIN group_members gm ON gm.group_id = e.group_id
      WHERE e.id = warhammer40k_event_config.event_id
      AND gm.user_id = auth.uid()
      AND gm.role IN ('admin', 'owner')
    )
  );

-- ============================================================================
-- Update events game_system CHECK constraint
-- ============================================================================

ALTER TABLE events DROP CONSTRAINT IF EXISTS events_game_system_check;
ALTER TABLE events ADD CONSTRAINT events_game_system_check
  CHECK (game_system IN ('board_game', 'mtg', 'pokemon_tcg', 'yugioh', 'warhammer40k'));
