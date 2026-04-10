-- Migration: Social Features
-- Looking for Players and Game Invitations

-- Player Requests (Looking for Players)
CREATE TABLE IF NOT EXISTS player_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    game_preferences VARCHAR(500),  -- "Heavy euros, 3+ hours"
    city VARCHAR(100),
    state VARCHAR(50),
    available_days VARCHAR(100),  -- "Weekends, Friday nights"
    player_count_needed INT DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Game Invitations
CREATE TABLE IF NOT EXISTS game_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    invite_code VARCHAR(32) NOT NULL UNIQUE,
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    invited_email VARCHAR(255),  -- For email invites (optional)
    channel VARCHAR(20) CHECK (channel IN ('link', 'email', 'facebook', 'twitter', 'instagram')),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'expired')),
    accepted_by_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_player_requests_user ON player_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_player_requests_location ON player_requests(city, state);
CREATE INDEX IF NOT EXISTS idx_player_requests_active ON player_requests(is_active, expires_at);
CREATE INDEX IF NOT EXISTS idx_invitations_event ON game_invitations(event_id);
CREATE INDEX IF NOT EXISTS idx_invitations_code ON game_invitations(invite_code);

-- RLS Policies for player_requests
ALTER TABLE player_requests ENABLE ROW LEVEL SECURITY;

-- Anyone can view active player requests
CREATE POLICY "Active requests viewable by all" ON player_requests
FOR SELECT USING (is_active = true AND (expires_at IS NULL OR expires_at > NOW()));

-- Users can manage their own requests
CREATE POLICY "Users can manage own requests" ON player_requests
FOR ALL USING (user_id = (
    SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
));

-- RLS Policies for game_invitations
ALTER TABLE game_invitations ENABLE ROW LEVEL SECURITY;

-- Invitations viewable by invite code (for accepting)
CREATE POLICY "Invitations viewable by code" ON game_invitations
FOR SELECT USING (true);

-- Event hosts and inviters can manage invitations
CREATE POLICY "Hosts can manage invitations" ON game_invitations
FOR ALL USING (
    invited_by_user_id = (
        SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
    )
    OR
    event_id IN (
        SELECT id FROM events WHERE host_user_id = (
            SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
        )
    )
);
