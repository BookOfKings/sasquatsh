-- Add accepted_at timestamp to track acceptance separate from response
-- This enables a two-step flow: Accept invitation -> Submit availability

ALTER TABLE planning_invitees
  ADD COLUMN accepted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL;

-- Index for efficient filtering of accepted invitations
CREATE INDEX idx_planning_invitees_accepted
  ON planning_invitees(session_id, accepted_at) WHERE accepted_at IS NOT NULL;

-- For existing responded invitees, backfill accepted_at to match responded_at
-- This ensures they show in the correct state
UPDATE planning_invitees
SET accepted_at = responded_at
WHERE has_responded = TRUE AND accepted_at IS NULL;
