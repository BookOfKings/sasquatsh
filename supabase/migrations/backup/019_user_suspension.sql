-- Add is_suspended field to users table for admin moderation
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_suspended BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_by_user_id UUID REFERENCES users(id);

-- Index for quick lookup of suspended users
CREATE INDEX IF NOT EXISTS idx_users_is_suspended ON users(is_suspended) WHERE is_suspended = TRUE;

COMMENT ON COLUMN users.is_suspended IS 'Whether the user account is suspended';
COMMENT ON COLUMN users.suspension_reason IS 'Admin notes about why the user was suspended';
COMMENT ON COLUMN users.suspended_at IS 'When the user was suspended';
COMMENT ON COLUMN users.suspended_by_user_id IS 'Which admin suspended the user';
