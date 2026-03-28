-- MTG format definitions reference table
-- Can be updated as formats change or new formats are added

CREATE TABLE IF NOT EXISTS mtg_formats (
    id VARCHAR(50) PRIMARY KEY,  -- 'commander', 'standard', 'modern', etc.
    name VARCHAR(100) NOT NULL,
    description TEXT,
    min_deck_size INT,
    max_deck_size INT,
    max_copies INT DEFAULT 4,  -- NULL for singleton formats
    has_commander BOOLEAN DEFAULT FALSE,
    has_sideboard BOOLEAN DEFAULT TRUE,
    sideboard_size INT DEFAULT 15,
    is_constructed BOOLEAN DEFAULT TRUE,  -- false for draft/sealed
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0
);

-- RLS - formats are public read
ALTER TABLE mtg_formats ENABLE ROW LEVEL SECURITY;

CREATE POLICY "mtg_formats_select" ON mtg_formats
FOR SELECT USING (true);

-- Seed common formats
INSERT INTO mtg_formats (id, name, description, min_deck_size, max_deck_size, max_copies, has_commander, has_sideboard, sideboard_size, is_constructed, sort_order) VALUES
('commander', 'Commander (EDH)', '100-card singleton format with a legendary creature as commander', 100, 100, 1, true, false, 0, true, 1),
('standard', 'Standard', 'Rotating format with recent sets', 60, NULL, 4, false, true, 15, true, 2),
('modern', 'Modern', 'Non-rotating format from 8th Edition forward', 60, NULL, 4, false, true, 15, true, 3),
('pioneer', 'Pioneer', 'Non-rotating format from Return to Ravnica forward', 60, NULL, 4, false, true, 15, true, 4),
('legacy', 'Legacy', 'Eternal format with restricted list', 60, NULL, 4, false, true, 15, true, 5),
('vintage', 'Vintage', 'Eternal format allowing most powerful cards', 60, NULL, 4, false, true, 15, true, 6),
('pauper', 'Pauper', 'Commons-only format', 60, NULL, 4, false, true, 15, true, 7),
('oathbreaker', 'Oathbreaker', '60-card singleton with Planeswalker commander', 60, 60, 1, true, false, 0, true, 8),
('brawl', 'Brawl', 'Standard-legal Commander variant', 60, 60, 1, true, false, 0, true, 9),
('draft', 'Booster Draft', 'Limited format - draft cards from booster packs', 40, NULL, NULL, false, false, 0, false, 10),
('sealed', 'Sealed Deck', 'Limited format - build from sealed booster packs', 40, NULL, NULL, false, false, 0, false, 11),
('cube', 'Cube Draft', 'Draft from a curated card pool', 40, NULL, NULL, false, false, 0, false, 12),
('casual', 'Casual / Kitchen Table', 'No format restrictions - house rules apply', 60, NULL, 4, false, false, 0, true, 20),
('custom', 'Custom Format', 'Custom format with specific house rules', NULL, NULL, NULL, false, false, 0, true, 99)
ON CONFLICT (id) DO NOTHING;
