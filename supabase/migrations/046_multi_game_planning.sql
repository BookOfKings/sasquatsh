-- Multi-game planning feature
-- Allows planning sessions to have multiple games and multi-vote support

-- 1. Add games array to events table for multi-game events
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS planned_games JSONB DEFAULT NULL;
-- Format: [{"bggId": 123, "name": "Catan", "image": "url", "interestedCount": 3}, ...]

-- 2. Add max_games column to planning_sessions (tier-based limit)
ALTER TABLE planning_sessions
  ADD COLUMN IF NOT EXISTS max_games INTEGER DEFAULT 5;

-- Note: planning_game_votes already has UNIQUE(suggestion_id, user_id) constraint
-- which already supports multi-voting (one vote per suggestion per user is allowed,
-- and users can vote for multiple suggestions/games)

COMMENT ON COLUMN events.planned_games IS 'Array of games selected from planning session with 2+ interested players';
COMMENT ON COLUMN planning_sessions.max_games IS 'Maximum number of game suggestions allowed (based on host tier)';
