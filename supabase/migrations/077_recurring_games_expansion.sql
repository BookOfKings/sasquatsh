-- ============================================================================
-- Recurring Games Expansion
-- Adds scheduling flexibility, location, game system, and generation tracking
-- ============================================================================

-- Expand recurring_games table
ALTER TABLE recurring_games
  ADD COLUMN IF NOT EXISTS frequency VARCHAR(20) NOT NULL DEFAULT 'weekly'
    CHECK (frequency IN ('weekly', 'biweekly', 'monthly')),
  ADD COLUMN IF NOT EXISTS monthly_week SMALLINT
    CHECK (monthly_week IS NULL OR (monthly_week >= -1 AND monthly_week <= 4 AND monthly_week != 0)),
  ADD COLUMN IF NOT EXISTS host_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS event_location_id UUID REFERENCES event_locations(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(200),
  ADD COLUMN IF NOT EXISTS city VARCHAR(100),
  ADD COLUMN IF NOT EXISTS state VARCHAR(50),
  ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
  ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'America/New_York',
  ADD COLUMN IF NOT EXISTS game_system VARCHAR(20) DEFAULT 'board_game',
  ADD COLUMN IF NOT EXISTS game_title VARCHAR(160),
  ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT true,
  ADD COLUMN IF NOT EXISTS host_is_playing BOOLEAN DEFAULT true,
  ADD COLUMN IF NOT EXISTS next_occurrence_date DATE,
  ADD COLUMN IF NOT EXISTS last_generated_date DATE;

-- Add from_recurring_game_id to events table
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS from_recurring_game_id UUID REFERENCES recurring_games(id) ON DELETE SET NULL;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_recurring_games_next_occurrence
  ON recurring_games(next_occurrence_date) WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_recurring_games_group_active
  ON recurring_games(group_id) WHERE is_active = true;

CREATE INDEX IF NOT EXISTS idx_events_recurring_game
  ON events(from_recurring_game_id) WHERE from_recurring_game_id IS NOT NULL;
