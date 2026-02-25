-- Add full-text search capability to BGG cache for fast local searching

-- Add a tsvector column for full-text search
ALTER TABLE bgg_games_cache ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS idx_bgg_cache_search ON bgg_games_cache USING GIN(search_vector);

-- Add trigram extension for fuzzy matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add trigram index for fuzzy name matching
CREATE INDEX IF NOT EXISTS idx_bgg_cache_name_trgm ON bgg_games_cache USING GIN(name gin_trgm_ops);

-- Function to update search vector
CREATE OR REPLACE FUNCTION update_bgg_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.categories, ' '), '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.mechanics, ' '), '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search vector on insert/update
DROP TRIGGER IF EXISTS bgg_cache_search_update ON bgg_games_cache;
CREATE TRIGGER bgg_cache_search_update
    BEFORE INSERT OR UPDATE ON bgg_games_cache
    FOR EACH ROW
    EXECUTE FUNCTION update_bgg_search_vector();

-- Update existing rows to populate search vector
UPDATE bgg_games_cache SET search_vector =
    setweight(to_tsvector('english', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'C') ||
    setweight(to_tsvector('english', COALESCE(array_to_string(categories, ' '), '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(array_to_string(mechanics, ' '), '')), 'B');

-- Add popularity/rank column for sorting results
ALTER TABLE bgg_games_cache ADD COLUMN IF NOT EXISTS bgg_rank INT;
ALTER TABLE bgg_games_cache ADD COLUMN IF NOT EXISTS num_ratings INT DEFAULT 0;
ALTER TABLE bgg_games_cache ADD COLUMN IF NOT EXISTS average_rating DECIMAL(4,2);

-- Index for sorting by popularity
CREATE INDEX IF NOT EXISTS idx_bgg_cache_rank ON bgg_games_cache(bgg_rank) WHERE bgg_rank IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bgg_cache_ratings ON bgg_games_cache(num_ratings DESC);

-- Function to search games with ranking
CREATE OR REPLACE FUNCTION search_bgg_cache(
    search_query TEXT,
    result_limit INT DEFAULT 20
)
RETURNS TABLE (
    bgg_id INT,
    name VARCHAR(255),
    year_published INT,
    thumbnail_url VARCHAR(500),
    min_players INT,
    max_players INT,
    playing_time INT,
    bgg_rank INT,
    average_rating DECIMAL(4,2),
    relevance REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.bgg_id,
        c.name,
        c.year_published,
        c.thumbnail_url,
        c.min_players,
        c.max_players,
        c.playing_time,
        c.bgg_rank,
        c.average_rating,
        (
            -- Combine full-text rank with name similarity
            ts_rank(c.search_vector, plainto_tsquery('english', search_query)) * 2 +
            similarity(c.name, search_query) * 3 +
            CASE WHEN c.bgg_rank IS NOT NULL THEN 1.0 / (c.bgg_rank + 1) ELSE 0 END
        )::REAL AS relevance
    FROM bgg_games_cache c
    WHERE
        c.search_vector @@ plainto_tsquery('english', search_query)
        OR c.name ILIKE '%' || search_query || '%'
        OR similarity(c.name, search_query) > 0.3
    ORDER BY relevance DESC, c.num_ratings DESC NULLS LAST
    LIMIT result_limit;
END;
$$ LANGUAGE plpgsql;
