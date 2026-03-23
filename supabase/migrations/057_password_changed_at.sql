-- Add password_changed_at column to track when users last changed their password
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;

-- Add index for potential queries
CREATE INDEX IF NOT EXISTS idx_users_password_changed_at ON users(password_changed_at);
