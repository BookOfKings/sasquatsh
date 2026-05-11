-- Add BGG username to user profiles
ALTER TABLE users ADD COLUMN IF NOT EXISTS bgg_username TEXT;
