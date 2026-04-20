-- Track shelf scan usage for tier limiting
CREATE TABLE shelf_scans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  image_url TEXT,
  games_detected INTEGER NOT NULL DEFAULT 0,
  games_added INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shelf_scans_user_id ON shelf_scans(user_id);
CREATE INDEX idx_shelf_scans_created_at ON shelf_scans(created_at);

ALTER TABLE shelf_scans ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read shelf scans" ON shelf_scans FOR SELECT USING (true);
