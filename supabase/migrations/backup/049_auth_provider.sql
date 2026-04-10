-- Add auth_provider column to track how users signed up
-- Values: 'password', 'google.com', 'facebook.com', etc.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS auth_provider TEXT DEFAULT 'password';

-- Add comment for documentation
COMMENT ON COLUMN users.auth_provider IS 'Firebase sign-in provider: password, google.com, facebook.com, etc.';
