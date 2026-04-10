-- Backfill thumbnail_url in event_games from bgg_games_cache
-- For any event_games with a bgg_id but missing thumbnail, pull from cache

UPDATE event_games eg
SET thumbnail_url = bgc.thumbnail_url
FROM bgg_games_cache bgc
WHERE eg.bgg_id = bgc.bgg_id
  AND eg.thumbnail_url IS NULL
  AND bgc.thumbnail_url IS NOT NULL;
