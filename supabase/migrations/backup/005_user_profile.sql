-- Migration: User Profile Enhancements
-- Adds location and travel preferences to user profiles

-- Add profile fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS max_travel_miles INT DEFAULT 25;
ALTER TABLE users ADD COLUMN IF NOT EXISTS home_city VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS home_state VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS home_postal_code VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS favorite_games TEXT[]; -- Array of favorite game names
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_game_types TEXT[]; -- Array of preferred categories

-- Create index for location-based queries
CREATE INDEX IF NOT EXISTS idx_users_location ON users(home_city, home_state);

-- RLS: Users can view other users' public profile info
CREATE POLICY "Users can view public profiles" ON users
FOR SELECT USING (true);

-- RLS: Users can update their own profile
CREATE POLICY "Users can update own profile" ON users
FOR UPDATE USING (
    firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
);
