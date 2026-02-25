-- BGG Integration & Multiple Games per Event Migration
-- Run this in your Supabase SQL Editor

-- Cache table for BoardGameGeek game data
CREATE TABLE IF NOT EXISTS bgg_games_cache (
    bgg_id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    year_published INT,
    thumbnail_url VARCHAR(500),
    image_url VARCHAR(500),
    min_players INT,
    max_players INT,
    min_playtime INT,
    max_playtime INT,
    playing_time INT,
    weight DECIMAL(3,2),  -- Complexity rating (1-5)
    description TEXT,
    categories TEXT[],  -- Array of category names
    mechanics TEXT[],   -- Array of mechanic names
    cached_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Game night can have multiple board games
CREATE TABLE IF NOT EXISTS event_games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    bgg_id INT REFERENCES bgg_games_cache(bgg_id),
    game_name VARCHAR(160) NOT NULL,
    thumbnail_url VARCHAR(500),
    min_players INT,
    max_players INT,
    playing_time INT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_alternative BOOLEAN NOT NULL DEFAULT FALSE,
    added_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_bgg_cache_name ON bgg_games_cache(name);
CREATE INDEX IF NOT EXISTS idx_event_games_event ON event_games(event_id);
CREATE INDEX IF NOT EXISTS idx_event_games_bgg ON event_games(bgg_id);

-- Enable RLS
ALTER TABLE bgg_games_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_games ENABLE ROW LEVEL SECURITY;

-- BGG cache is readable by everyone (it's public game data)
CREATE POLICY "BGG cache readable by everyone" ON bgg_games_cache
FOR SELECT USING (true);

-- Event games readable if event is public or user is registered
CREATE POLICY "Event games readable for public events" ON event_games
FOR SELECT USING (
    EXISTS (SELECT 1 FROM events WHERE id = event_id AND is_public = true)
);

CREATE POLICY "Event games readable for registered users" ON event_games
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM event_registrations er
        JOIN users u ON er.user_id = u.id
        WHERE er.event_id = event_games.event_id
        AND u.firebase_uid = auth.uid()::text
    )
);

-- Function to clean old cache entries (older than 7 days)
CREATE OR REPLACE FUNCTION clean_bgg_cache()
RETURNS void AS $$
BEGIN
    DELETE FROM bgg_games_cache
    WHERE cached_at < NOW() - INTERVAL '7 days'
    AND bgg_id NOT IN (SELECT DISTINCT bgg_id FROM event_games WHERE bgg_id IS NOT NULL);
END;
$$ LANGUAGE plpgsql;
