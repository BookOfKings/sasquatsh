-- Add postal_code column to event_locations table
ALTER TABLE event_locations ADD COLUMN IF NOT EXISTS postal_code TEXT;

-- Add index for postal_code lookups
CREATE INDEX IF NOT EXISTS idx_event_locations_postal_code ON event_locations(postal_code);

-- Update existing venue with postal code (Dice Tower West at Westgate Las Vegas)
UPDATE event_locations 
SET postal_code = '89109' 
WHERE id = 'eb8d4232-ac81-4970-a50a-1693ee37e54e';
