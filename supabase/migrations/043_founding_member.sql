-- Add founding member flag to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_founding_member BOOLEAN DEFAULT FALSE;

-- Index for efficient lookups
CREATE INDEX IF NOT EXISTS idx_users_founding_member ON users(is_founding_member) WHERE is_founding_member = TRUE;

COMMENT ON COLUMN users.is_founding_member IS 'Flag for users who joined during the founding period - displays special badge';
