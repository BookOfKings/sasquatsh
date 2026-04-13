-- Add open_to_group flag to planning_sessions
ALTER TABLE planning_sessions
ADD COLUMN IF NOT EXISTS open_to_group BOOLEAN NOT NULL DEFAULT false;

-- Add comment
COMMENT ON COLUMN planning_sessions.open_to_group IS 'When true, any group member can join without explicit invitation';
