-- Add active location fields for users (when traveling to conventions, etc.)
-- This is separate from home_city/home_state which is their permanent location

ALTER TABLE users ADD COLUMN IF NOT EXISTS active_city VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active_state VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active_location_expires_at TIMESTAMPTZ;

-- Index for active location queries
CREATE INDEX IF NOT EXISTS idx_users_active_location ON users(active_city, active_state)
WHERE active_city IS NOT NULL;

COMMENT ON COLUMN users.active_city IS 'Temporary active location city (e.g., when at a convention)';
COMMENT ON COLUMN users.active_state IS 'Temporary active location state';
COMMENT ON COLUMN users.active_location_expires_at IS 'When the active location expires (optional, for auto-clearing)';
