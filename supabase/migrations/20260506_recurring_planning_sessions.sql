-- Recurring Planning Sessions

-- Add planning session mode to recurring games
ALTER TABLE recurring_games ADD COLUMN IF NOT EXISTS generates_planning_session BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE recurring_games ADD COLUMN IF NOT EXISTS deadline_day_offset INTEGER NOT NULL DEFAULT 1;
ALTER TABLE recurring_games ADD COLUMN IF NOT EXISTS table_count INTEGER;

-- Link planning sessions back to recurring games
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS from_recurring_game_id UUID REFERENCES recurring_games(id) ON DELETE SET NULL;
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS target_event_date DATE;

-- Idempotency index: prevent duplicate planning sessions for the same recurring game + date
CREATE UNIQUE INDEX IF NOT EXISTS idx_planning_sessions_recurring_date
  ON planning_sessions(from_recurring_game_id, target_event_date)
  WHERE from_recurring_game_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_planning_sessions_recurring_game
  ON planning_sessions(from_recurring_game_id)
  WHERE from_recurring_game_id IS NOT NULL;

-- Allow share links to target a specific recurring game
ALTER TABLE shareable_invite_links ADD COLUMN IF NOT EXISTS recurring_game_id UUID REFERENCES recurring_games(id) ON DELETE CASCADE;
