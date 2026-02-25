-- Migration: 007_group_management.sql
-- Description: Add tables for group member management (join requests, invitations)

-- Join requests for private groups
CREATE TABLE IF NOT EXISTS group_join_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'approved', 'rejected')),
    reviewed_by_user_id UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

-- Group invitations
CREATE TABLE IF NOT EXISTS group_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    invite_code VARCHAR(32) NOT NULL UNIQUE,
    invited_email VARCHAR(255),
    max_uses INT DEFAULT 1,
    uses_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Track who used which invitation
CREATE TABLE IF NOT EXISTS group_invitation_uses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID NOT NULL REFERENCES group_invitations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(invitation_id, user_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_join_requests_group ON group_join_requests(group_id);
CREATE INDEX IF NOT EXISTS idx_join_requests_user ON group_join_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_join_requests_status ON group_join_requests(group_id, status);
CREATE INDEX IF NOT EXISTS idx_grp_invitations_group ON group_invitations(group_id);
CREATE INDEX IF NOT EXISTS idx_grp_invitations_code ON group_invitations(invite_code);
CREATE INDEX IF NOT EXISTS idx_invitation_uses_invitation ON group_invitation_uses(invitation_id);
CREATE INDEX IF NOT EXISTS idx_invitation_uses_user ON group_invitation_uses(user_id);
