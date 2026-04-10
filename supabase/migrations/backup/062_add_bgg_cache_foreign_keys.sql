-- Add foreign key relationships from event_games and planning_game_suggestions to bgg_games_cache
-- This allows Supabase PostgREST to do JOINs, so we don't need to copy game data

-- First, insert placeholder entries for any bgg_ids referenced but not in cache
INSERT INTO bgg_games_cache (bgg_id, name)
SELECT DISTINCT eg.bgg_id, eg.game_name
FROM event_games eg
WHERE eg.bgg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM bgg_games_cache bgc WHERE bgc.bgg_id = eg.bgg_id)
ON CONFLICT (bgg_id) DO NOTHING;

INSERT INTO bgg_games_cache (bgg_id, name)
SELECT DISTINCT pgs.bgg_id, pgs.game_name
FROM planning_game_suggestions pgs
WHERE pgs.bgg_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM bgg_games_cache bgc WHERE bgc.bgg_id = pgs.bgg_id)
ON CONFLICT (bgg_id) DO NOTHING;

-- Add foreign key constraints (drop first if they exist from a partial migration)
ALTER TABLE event_games DROP CONSTRAINT IF EXISTS event_games_bgg_id_fkey;
ALTER TABLE event_games
ADD CONSTRAINT event_games_bgg_id_fkey
FOREIGN KEY (bgg_id) REFERENCES bgg_games_cache(bgg_id) ON DELETE SET NULL;

ALTER TABLE planning_game_suggestions DROP CONSTRAINT IF EXISTS planning_game_suggestions_bgg_id_fkey;
ALTER TABLE planning_game_suggestions
ADD CONSTRAINT planning_game_suggestions_bgg_id_fkey
FOREIGN KEY (bgg_id) REFERENCES bgg_games_cache(bgg_id) ON DELETE SET NULL;
