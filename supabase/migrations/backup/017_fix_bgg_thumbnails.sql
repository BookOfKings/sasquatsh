-- Clear broken thumbnail URLs - placeholder icons will show instead
-- Real thumbnails will be fetched when BGG API token is configured
UPDATE bgg_games_cache SET thumbnail_url = NULL, image_url = NULL;
