-- Migration: 041_direct_user_invitations.sql
-- Description: Add support for inviting specific users directly (in-app invitations)

-- Add invited_user_id column for direct user invitations
ALTER TABLE group_invitations
ADD COLUMN IF NOT EXISTS invited_user_id UUID REFERENCES users(id) ON DELETE CASCADE;

-- Add status column to track invitation state (pending, accepted, declined)
ALTER TABLE group_invitations
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'pending'
CHECK (status IN ('pending', 'accepted', 'declined', 'expired'));

-- Index for querying user's pending invitations
CREATE INDEX IF NOT EXISTS idx_grp_invitations_invited_user ON group_invitations(invited_user_id) WHERE invited_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_grp_invitations_status ON group_invitations(status);

-- Add unique constraint to prevent duplicate direct invitations to same user for same group
CREATE UNIQUE INDEX IF NOT EXISTS idx_grp_invitations_user_group_unique
ON group_invitations(group_id, invited_user_id)
WHERE invited_user_id IS NOT NULL AND status = 'pending';
