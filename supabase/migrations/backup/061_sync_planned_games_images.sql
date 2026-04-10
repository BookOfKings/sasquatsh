-- Update planned_games JSON in events table with images from bgg_games_cache
-- This updates the 'image' field in each planned game object where it's null

UPDATE events e
SET planned_games = (
  SELECT jsonb_agg(
    CASE
      WHEN (game->>'image') IS NULL AND bgc.thumbnail_url IS NOT NULL THEN
        game || jsonb_build_object('image', bgc.thumbnail_url)
      ELSE
        game
    END
  )
  FROM jsonb_array_elements(e.planned_games::jsonb) AS game
  LEFT JOIN bgg_games_cache bgc ON bgc.bgg_id = (game->>'bggId')::integer
)
WHERE e.planned_games IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM jsonb_array_elements(e.planned_games::jsonb) AS game
    WHERE (game->>'image') IS NULL
  );
