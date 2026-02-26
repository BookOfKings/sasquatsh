-- Migration: Add usernames to users table
-- Usernames are unique (case-insensitive), required, and follow format rules

-- 1. Add username column (nullable initially)
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(30);

-- 2. Auto-generate usernames for existing users (user1, user2, etc.)
WITH numbered_users AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) as num
  FROM users WHERE username IS NULL
)
UPDATE users SET username = 'user' || numbered_users.num
FROM numbered_users WHERE users.id = numbered_users.id;

-- 3. Create unique index (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_lower
ON users (LOWER(username));

-- 4. Add check constraint for valid username format
-- Rules: 3-30 chars, starts with letter, alphanumeric + underscores only
ALTER TABLE users ADD CONSTRAINT chk_username_format
CHECK (username ~ '^[a-zA-Z][a-zA-Z0-9_]{2,29}$');

-- 5. Make username NOT NULL after populating existing users
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
