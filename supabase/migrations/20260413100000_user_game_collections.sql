-- User Game Collections: tracks board games owned by users
CREATE TABLE user_game_collections (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  bgg_id INTEGER NOT NULL,
  game_name VARCHAR(200) NOT NULL,
  thumbnail_url VARCHAR(500),
  image_url VARCHAR(500),
  min_players INTEGER,
  max_players INTEGER,
  playing_time INTEGER,
  year_published INTEGER,
  bgg_rank INTEGER,
  average_rating NUMERIC(4,2),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id, bgg_id)
);

-- Indexes
CREATE INDEX idx_user_game_collections_user_id ON user_game_collections(user_id);
CREATE INDEX idx_user_game_collections_bgg_id ON user_game_collections(bgg_id);

-- Enable RLS
ALTER TABLE user_game_collections ENABLE ROW LEVEL SECURITY;

-- Users can read their own collection
CREATE POLICY "Users can read own collection"
  ON user_game_collections FOR SELECT
  USING (auth.uid()::text = (SELECT firebase_uid FROM users WHERE id = user_id) OR true);

-- Public read: anyone can view any user's collection
CREATE POLICY "Anyone can view collections"
  ON user_game_collections FOR SELECT
  USING (true);
