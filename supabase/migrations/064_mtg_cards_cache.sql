-- Scryfall card data cache (similar pattern to bgg_games_cache)
-- Stores MTG card data fetched from Scryfall API

CREATE TABLE IF NOT EXISTS scryfall_cards_cache (
    scryfall_id UUID PRIMARY KEY,
    oracle_id UUID,
    name VARCHAR(200) NOT NULL,
    mana_cost VARCHAR(100),
    cmc DECIMAL(4,1),
    type_line VARCHAR(200),
    oracle_text TEXT,
    power VARCHAR(10),
    toughness VARCHAR(10),
    loyalty VARCHAR(10),
    colors TEXT[],
    color_identity TEXT[],
    keywords TEXT[],
    legalities JSONB,  -- {standard: "legal", modern: "legal", commander: "legal", ...}
    set_code VARCHAR(10),
    set_name VARCHAR(100),
    collector_number VARCHAR(20),
    rarity VARCHAR(20),
    image_uri_small VARCHAR(500),
    image_uri_normal VARCHAR(500),
    image_uri_large VARCHAR(500),
    image_uri_art_crop VARCHAR(500),
    image_uri_png VARCHAR(500),
    prices JSONB,  -- {usd: "1.50", usd_foil: "3.00", ...}
    is_double_faced BOOLEAN DEFAULT FALSE,
    card_faces JSONB,  -- For double-faced cards: [{name, mana_cost, type_line, oracle_text, image_uris}, ...]
    layout VARCHAR(30),  -- normal, transform, modal_dfc, adventure, etc.
    cached_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_scryfall_name ON scryfall_cards_cache(name);
CREATE INDEX IF NOT EXISTS idx_scryfall_oracle_id ON scryfall_cards_cache(oracle_id);
CREATE INDEX IF NOT EXISTS idx_scryfall_set_code ON scryfall_cards_cache(set_code);
CREATE INDEX IF NOT EXISTS idx_scryfall_cached_at ON scryfall_cards_cache(cached_at);

-- Full-text search on card name
CREATE INDEX IF NOT EXISTS idx_scryfall_name_trgm ON scryfall_cards_cache USING gin (name gin_trgm_ops);

-- RLS - cards cache is public read
ALTER TABLE scryfall_cards_cache ENABLE ROW LEVEL SECURITY;

CREATE POLICY "scryfall_cache_select" ON scryfall_cards_cache
FOR SELECT USING (true);

-- Only service role can insert/update
CREATE POLICY "scryfall_cache_insert" ON scryfall_cards_cache
FOR INSERT WITH CHECK (false);

CREATE POLICY "scryfall_cache_update" ON scryfall_cards_cache
FOR UPDATE USING (false);
