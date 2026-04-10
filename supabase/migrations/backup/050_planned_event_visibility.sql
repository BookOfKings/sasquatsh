-- Migration: Track events that originated from planning sessions
-- This enables group members to see and join events after planning is finalized

-- Add column to track if event came from a planning session
ALTER TABLE events
ADD COLUMN IF NOT EXISTS from_planning_session_id UUID REFERENCES planning_sessions(id) ON DELETE SET NULL;

-- Add index for efficient queries on group's planned events
CREATE INDEX IF NOT EXISTS idx_events_group_planning
ON events(group_id, from_planning_session_id) WHERE from_planning_session_id IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN events.from_planning_session_id IS 'References the planning session this event was created from. NULL for manually created events.';
