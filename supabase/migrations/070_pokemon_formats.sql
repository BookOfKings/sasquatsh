-- Pokemon TCG format definitions reference table
-- Can be updated as formats change or new formats are added

CREATE TABLE IF NOT EXISTS pokemon_formats (
  id VARCHAR(50) PRIMARY KEY,  -- 'standard', 'expanded', 'unlimited', etc.
  name VARCHAR(100) NOT NULL,
  description TEXT,
  min_deck_size INT DEFAULT 60,
  max_deck_size INT DEFAULT 60,
  max_copies INT DEFAULT 4,  -- Except basic energy
  is_rotating BOOLEAN DEFAULT FALSE,  -- Whether format rotates
  is_active BOOLEAN DEFAULT TRUE,
  sort_order INT DEFAULT 0
);

-- RLS - formats are public read
ALTER TABLE pokemon_formats ENABLE ROW LEVEL SECURITY;

CREATE POLICY "pokemon_formats_select" ON pokemon_formats
FOR SELECT USING (true);

-- Seed Pokemon TCG formats
INSERT INTO pokemon_formats (id, name, description, min_deck_size, max_deck_size, max_copies, is_rotating, sort_order) VALUES
('standard', 'Standard', 'The main competitive format with the most recent sets. Legal cards rotate yearly.', 60, 60, 4, true, 1),
('expanded', 'Expanded', 'Includes cards from Black & White era onwards. Larger card pool.', 60, 60, 4, false, 2),
('unlimited', 'Unlimited', 'All cards ever printed are legal. No rotation.', 60, 60, 4, false, 3),
('theme', 'Theme Deck', 'Pre-constructed theme decks only. Great for beginners.', 60, 60, 4, false, 4),
('gym_leader_challenge', 'Gym Leader Challenge', 'Single-type singleton format. Community-created format.', 60, 60, 1, false, 5),
('retro', 'Retro', 'Classic format using older card pools (Base Set through Neo era).', 60, 60, 4, false, 6),
('casual', 'Casual', 'Kitchen table play. House rules welcome.', 60, 60, 4, false, 10)
ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE pokemon_formats IS 'Pokemon TCG format definitions';
