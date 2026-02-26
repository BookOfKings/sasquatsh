-- Redesign player_requests table for "Host needs players" use case
-- Previously: Users posting that they're looking for games to join
-- Now: Event hosts posting urgent requests for fill-in players (someone bailed)

-- Add new columns
ALTER TABLE player_requests
ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES events(id) ON DELETE CASCADE,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'open' CHECK (status IN ('open', 'filled', 'cancelled'));

-- Drop columns that are no longer needed (event has this info)
ALTER TABLE player_requests
DROP COLUMN IF EXISTS title,
DROP COLUMN IF EXISTS game_preferences,
DROP COLUMN IF EXISTS city,
DROP COLUMN IF EXISTS state,
DROP COLUMN IF EXISTS available_days,
DROP COLUMN IF EXISTS event_location_id,
DROP COLUMN IF EXISTS hall_area,
DROP COLUMN IF EXISTS table_number,
DROP COLUMN IF EXISTS booth;

-- Clear existing data since the model has fundamentally changed
-- (Old posts were "looking for games", new posts are "need players for my event")
TRUNCATE player_requests;

-- Make event_id required after clearing data
ALTER TABLE player_requests ALTER COLUMN event_id SET NOT NULL;

-- Update indexes
DROP INDEX IF EXISTS idx_player_requests_location;
DROP INDEX IF EXISTS idx_player_requests_event_location;
CREATE INDEX IF NOT EXISTS idx_player_requests_event ON player_requests(event_id);
CREATE INDEX IF NOT EXISTS idx_player_requests_status ON player_requests(status) WHERE status = 'open';
CREATE INDEX IF NOT EXISTS idx_player_requests_expires ON player_requests(expires_at) WHERE status = 'open';

-- Add comment
COMMENT ON TABLE player_requests IS 'Urgent requests from event hosts needing fill-in players (max 15 min duration)';
