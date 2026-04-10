-- Add age requirements to events and birth date to users

-- Add minimum age to events
ALTER TABLE events ADD COLUMN IF NOT EXISTS min_age INTEGER CHECK (min_age >= 0 AND min_age <= 100);

-- Add birth date to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_date DATE;

-- Index for filtering events by age
CREATE INDEX IF NOT EXISTS idx_events_min_age ON events(min_age) WHERE min_age IS NOT NULL;
