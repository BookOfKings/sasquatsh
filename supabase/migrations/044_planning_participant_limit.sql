-- Add participant limit feature to planning sessions
-- Allows hosts to limit how many members can participate (first-come-first-served)

-- Add max_participants column to planning_sessions
ALTER TABLE planning_sessions
  ADD COLUMN IF NOT EXISTS max_participants INTEGER DEFAULT NULL;

-- Ensure max_participants is positive if set
ALTER TABLE planning_sessions
  ADD CONSTRAINT check_max_participants_positive
  CHECK (max_participants IS NULL OR max_participants > 0);

-- Track if user secured a participation slot
ALTER TABLE planning_invitees
  ADD COLUMN IF NOT EXISTS has_slot BOOLEAN DEFAULT FALSE;

-- Index for efficient slot counting
CREATE INDEX IF NOT EXISTS idx_planning_invitees_slots
  ON planning_invitees(session_id) WHERE has_slot = TRUE;

-- Give existing invitees who have responded a slot (for backward compatibility)
UPDATE planning_invitees
SET has_slot = TRUE
WHERE has_responded = TRUE;
