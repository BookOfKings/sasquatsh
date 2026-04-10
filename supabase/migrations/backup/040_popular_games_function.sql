-- Create function to get popular games from events
CREATE OR REPLACE FUNCTION get_popular_games(limit_count INTEGER DEFAULT 10)
RETURNS TABLE (
  game_name TEXT,
  bgg_id BIGINT,
  usage_count BIGINT,
  thumbnail_url TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    eg.game_name::TEXT,
    eg.bgg_id,
    COUNT(*)::BIGINT as usage_count,
    MAX(eg.thumbnail_url)::TEXT as thumbnail_url
  FROM event_games eg
  WHERE eg.game_name IS NOT NULL AND eg.game_name != ''
  GROUP BY eg.game_name, eg.bgg_id
  ORDER BY usage_count DESC, eg.game_name ASC
  LIMIT limit_count;
END;
$$ LANGUAGE plpgsql STABLE;
