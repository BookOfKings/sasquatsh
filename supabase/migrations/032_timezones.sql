-- Add timezone support to users and event_locations tables

-- Add timezone column to users table (defaults to America/New_York if not set)
ALTER TABLE users ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'America/New_York';

COMMENT ON COLUMN users.timezone IS 'User''s preferred timezone (IANA timezone name, e.g. America/New_York)';

-- Add timezone column to event_locations table
ALTER TABLE event_locations ADD COLUMN IF NOT EXISTS timezone VARCHAR(50);

COMMENT ON COLUMN event_locations.timezone IS 'Venue timezone (IANA timezone name). Used when creating events at this venue.';

-- Add timezone column to events table (stores the timezone the event was created in)
ALTER TABLE events ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'America/New_York';

COMMENT ON COLUMN events.timezone IS 'Timezone the event time is specified in (IANA timezone name)';

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_timezone ON users(timezone);
CREATE INDEX IF NOT EXISTS idx_events_timezone ON events(timezone);
