-- Pokemon TCG Cards Cache
-- Caches card data from pokemontcg.io API

CREATE TABLE IF NOT EXISTS pokemon_cards_cache (
  pokemon_tcg_id VARCHAR(50) PRIMARY KEY,  -- e.g., "base1-4", "sv1-125"

  -- Basic card info
  name VARCHAR(200) NOT NULL,
  supertype VARCHAR(50) NOT NULL,  -- Pokemon, Trainer, Energy
  subtypes VARCHAR(50)[] DEFAULT '{}',  -- Stage 1, Stage 2, VMAX, Item, etc.
  hp VARCHAR(10),
  types VARCHAR(20)[] DEFAULT '{}',  -- Fire, Water, etc.

  -- Evolution chain
  evolves_from VARCHAR(100),
  evolves_to VARCHAR(100)[] DEFAULT '{}',

  -- Abilities and attacks (stored as JSONB)
  abilities JSONB DEFAULT '[]',
  attacks JSONB DEFAULT '[]',
  weaknesses JSONB DEFAULT '[]',
  resistances JSONB DEFAULT '[]',
  retreat_cost VARCHAR(20)[] DEFAULT '{}',

  -- Set info
  set_id VARCHAR(50) NOT NULL,
  set_name VARCHAR(200) NOT NULL,
  set_series VARCHAR(100),
  card_number VARCHAR(20) NOT NULL,

  -- Metadata
  artist VARCHAR(200),
  rarity VARCHAR(50),
  flavor_text TEXT,
  national_pokedex_numbers INTEGER[] DEFAULT '{}',

  -- Legalities
  legalities JSONB DEFAULT '{}',  -- {standard: 'Legal', expanded: 'Legal', unlimited: 'Legal'}
  regulation_mark VARCHAR(10),  -- F, G, H, etc.

  -- Images
  image_small TEXT NOT NULL,
  image_large TEXT NOT NULL,

  -- Pricing
  tcgplayer_url TEXT,
  tcgplayer_prices JSONB,  -- {normal: {low, mid, high, market}, holofoil: {...}, etc}
  cardmarket_url TEXT,
  cardmarket_prices JSONB,

  -- Cache management
  cached_at TIMESTAMPTZ DEFAULT NOW(),
  stale_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours'),

  -- Full-text search (populated via trigger)
  search_vector TSVECTOR
);

-- Trigger to update search vector
CREATE OR REPLACE FUNCTION pokemon_cards_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('simple', COALESCE(NEW.name, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(NEW.set_name, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(array_to_string(NEW.subtypes, ' '), '')), 'C');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS pokemon_cards_search_vector_trigger ON pokemon_cards_cache;
CREATE TRIGGER pokemon_cards_search_vector_trigger
  BEFORE INSERT OR UPDATE ON pokemon_cards_cache
  FOR EACH ROW
  EXECUTE FUNCTION pokemon_cards_search_vector_update();

-- Indexes
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_name ON pokemon_cards_cache(name);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_name_lower ON pokemon_cards_cache(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_supertype ON pokemon_cards_cache(supertype);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_set_id ON pokemon_cards_cache(set_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_types ON pokemon_cards_cache USING GIN(types);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_stale_at ON pokemon_cards_cache(stale_at);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_search ON pokemon_cards_cache USING GIN(search_vector);

-- Pokemon Sets Cache
CREATE TABLE IF NOT EXISTS pokemon_sets_cache (
  set_id VARCHAR(50) PRIMARY KEY,  -- e.g., "base1", "sv1"

  -- Basic info
  name VARCHAR(200) NOT NULL,
  series VARCHAR(100) NOT NULL,
  printed_total INTEGER NOT NULL,
  total INTEGER NOT NULL,

  -- Codes
  ptcgo_code VARCHAR(20),  -- Pokemon TCG Online code

  -- Dates
  release_date VARCHAR(20),  -- YYYY/MM/DD format

  -- Legalities
  legalities JSONB DEFAULT '{}',

  -- Images
  symbol_url TEXT NOT NULL,
  logo_url TEXT NOT NULL,

  -- Cache management
  cached_at TIMESTAMPTZ DEFAULT NOW(),
  stale_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '24 hours')
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_pokemon_sets_name ON pokemon_sets_cache(name);
CREATE INDEX IF NOT EXISTS idx_pokemon_sets_series ON pokemon_sets_cache(series);
CREATE INDEX IF NOT EXISTS idx_pokemon_sets_release_date ON pokemon_sets_cache(release_date DESC);
CREATE INDEX IF NOT EXISTS idx_pokemon_sets_stale_at ON pokemon_sets_cache(stale_at);

-- Note: game_system is a VARCHAR column with CHECK constraint in events table,
-- not an enum. Pokemon TCG support is already included ('pokemon_tcg').

COMMENT ON TABLE pokemon_cards_cache IS 'Cache of Pokemon TCG card data from pokemontcg.io API';
COMMENT ON TABLE pokemon_sets_cache IS 'Cache of Pokemon TCG set data from pokemontcg.io API';
