-- Add game_system column to events table to support different game types
-- Values: 'board_game' (default), 'mtg', 'pokemon_tcg', 'yugioh'

ALTER TABLE events
ADD COLUMN IF NOT EXISTS game_system VARCHAR(20) DEFAULT 'board_game'
CHECK (game_system IN ('board_game', 'mtg', 'pokemon_tcg', 'yugioh'));

-- Index for filtering by game system
CREATE INDEX IF NOT EXISTS idx_events_game_system ON events(game_system);

-- Update existing events to have the default value
UPDATE events SET game_system = 'board_game' WHERE game_system IS NULL;
