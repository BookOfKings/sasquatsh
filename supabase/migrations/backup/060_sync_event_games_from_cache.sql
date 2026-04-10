-- Sync event_games thumbnail and player data from bgg_games_cache
-- This updates any event_games that have a bgg_id with data from the cache

UPDATE event_games eg
SET
  thumbnail_url = bgc.thumbnail_url,
  min_players = bgc.min_players,
  max_players = bgc.max_players,
  playing_time = bgc.playing_time
FROM bgg_games_cache bgc
WHERE eg.bgg_id = bgc.bgg_id
  AND bgc.thumbnail_url IS NOT NULL;

-- Also sync planning_game_suggestions
UPDATE planning_game_suggestions pgs
SET
  thumbnail_url = bgc.thumbnail_url,
  min_players = bgc.min_players,
  max_players = bgc.max_players,
  playing_time = bgc.playing_time
FROM bgg_games_cache bgc
WHERE pgs.bgg_id = bgc.bgg_id
  AND bgc.thumbnail_url IS NOT NULL;
