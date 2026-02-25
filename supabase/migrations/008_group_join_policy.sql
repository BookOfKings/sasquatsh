-- Migration: Replace is_public boolean with join_policy enum
-- join_policy values: 'open' (anyone can join), 'request' (ask to join), 'invite_only' (invitation required)
-- All groups are now searchable regardless of join policy

-- First drop the old RLS policies that depend on is_public
DROP POLICY IF EXISTS "Public groups viewable by everyone" ON groups;
DROP POLICY IF EXISTS "Members can view private groups" ON groups;
DROP POLICY IF EXISTS "Memberships viewable for public groups" ON group_memberships;
DROP POLICY IF EXISTS "Members can view private group memberships" ON group_memberships;
DROP POLICY IF EXISTS "Recurring games viewable for public groups" ON recurring_games;
DROP POLICY IF EXISTS "Members can view private group recurring games" ON recurring_games;

-- Add new join_policy column
ALTER TABLE groups ADD COLUMN IF NOT EXISTS join_policy VARCHAR(20);

-- Migrate existing data
UPDATE groups SET join_policy = CASE
    WHEN is_public = true THEN 'open'
    ELSE 'request'
END WHERE join_policy IS NULL;

-- Set default and constraint
ALTER TABLE groups ALTER COLUMN join_policy SET NOT NULL;
ALTER TABLE groups ALTER COLUMN join_policy SET DEFAULT 'open';
ALTER TABLE groups ADD CONSTRAINT groups_join_policy_check
    CHECK (join_policy IN ('open', 'request', 'invite_only'));

-- Drop old is_public column and its index
DROP INDEX IF EXISTS idx_groups_public;
ALTER TABLE groups DROP COLUMN IF EXISTS is_public;

-- Add index for join_policy
CREATE INDEX IF NOT EXISTS idx_groups_join_policy ON groups(join_policy);

-- Update RLS policies - all groups are now viewable by everyone
DROP POLICY IF EXISTS "Public groups viewable by everyone" ON groups;
DROP POLICY IF EXISTS "Members can view private groups" ON groups;

-- All groups are searchable/viewable
CREATE POLICY "All groups viewable by everyone" ON groups
FOR SELECT USING (true);

-- Update memberships policies
DROP POLICY IF EXISTS "Memberships viewable for public groups" ON group_memberships;
DROP POLICY IF EXISTS "Members can view private group memberships" ON group_memberships;

-- Memberships viewable for all groups (needed for member count, etc.)
CREATE POLICY "Memberships viewable by everyone" ON group_memberships
FOR SELECT USING (true);

-- Update recurring games policies
DROP POLICY IF EXISTS "Recurring games viewable for public groups" ON recurring_games;
DROP POLICY IF EXISTS "Members can view private group recurring games" ON recurring_games;

CREATE POLICY "Recurring games viewable by everyone" ON recurring_games
FOR SELECT USING (true);
