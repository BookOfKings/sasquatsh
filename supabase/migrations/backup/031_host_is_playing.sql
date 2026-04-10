-- Add host_is_playing field to events table
-- This indicates whether the host is playing in the game or just organizing

ALTER TABLE events ADD COLUMN IF NOT EXISTS host_is_playing BOOLEAN DEFAULT TRUE;

-- Add comment explaining the field
COMMENT ON COLUMN events.host_is_playing IS 'Whether the host is participating as a player. If true, they count toward max_players.';
