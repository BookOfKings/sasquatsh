-- Game Night Planning Feature
-- Collaborative planning for group game nights with date voting and game suggestions

-- Planning Sessions (one per planned game night)
CREATE TABLE planning_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    response_deadline TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'open' CHECK (status IN ('open', 'finalized', 'cancelled')),
    finalized_date DATE,
    finalized_game_id UUID,
    created_event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Invited Members (subset of group members)
CREATE TABLE planning_invitees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES planning_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    has_responded BOOLEAN DEFAULT FALSE,
    responded_at TIMESTAMP,
    cannot_attend_any BOOLEAN DEFAULT FALSE,
    UNIQUE(session_id, user_id)
);

-- Proposed Dates (host proposes, members vote)
CREATE TABLE planning_dates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES planning_sessions(id) ON DELETE CASCADE,
    proposed_date DATE NOT NULL,
    start_time TIME,
    UNIQUE(session_id, proposed_date)
);

-- Date Availability Votes
CREATE TABLE planning_date_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date_id UUID NOT NULL REFERENCES planning_dates(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_available BOOLEAN NOT NULL,
    UNIQUE(date_id, user_id)
);

-- Game Suggestions (BGG integration)
CREATE TABLE planning_game_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES planning_sessions(id) ON DELETE CASCADE,
    suggested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bgg_id INTEGER,
    game_name VARCHAR(200) NOT NULL,
    thumbnail_url TEXT,
    min_players INTEGER,
    max_players INTEGER,
    playing_time INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(session_id, bgg_id)
);

-- Game Votes (upvote system)
CREATE TABLE planning_game_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    suggestion_id UUID NOT NULL REFERENCES planning_game_suggestions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(suggestion_id, user_id)
);

-- Indexes
CREATE INDEX idx_planning_sessions_group ON planning_sessions(group_id);
CREATE INDEX idx_planning_sessions_status ON planning_sessions(status, response_deadline);
CREATE INDEX idx_planning_sessions_creator ON planning_sessions(created_by_user_id);
CREATE INDEX idx_planning_invitees_session ON planning_invitees(session_id);
CREATE INDEX idx_planning_invitees_user ON planning_invitees(user_id);
CREATE INDEX idx_planning_dates_session ON planning_dates(session_id);
CREATE INDEX idx_planning_date_votes_date ON planning_date_votes(date_id);
CREATE INDEX idx_planning_game_suggestions_session ON planning_game_suggestions(session_id);
CREATE INDEX idx_planning_game_votes_suggestion ON planning_game_votes(suggestion_id);

-- Add foreign key for finalized_game_id after table exists
ALTER TABLE planning_sessions
ADD CONSTRAINT fk_finalized_game
FOREIGN KEY (finalized_game_id) REFERENCES planning_game_suggestions(id) ON DELETE SET NULL;
