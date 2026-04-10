-- Migration: Multi-Table Game Session Scheduling
-- Adds support for scheduling multiple games at multiple tables during planning sessions

-- Add table_count to planning_sessions
ALTER TABLE planning_sessions
  ADD COLUMN IF NOT EXISTS table_count INTEGER DEFAULT NULL;

-- Add host_session_preferences for storing host's game picks before finalize
ALTER TABLE planning_sessions
  ADD COLUMN IF NOT EXISTS host_session_preferences JSONB DEFAULT NULL;

-- Add scheduled_sessions for storing the game schedule before finalize
ALTER TABLE planning_sessions
  ADD COLUMN IF NOT EXISTS scheduled_sessions JSONB DEFAULT NULL;

-- Add multi-table flag to events
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS is_multi_table BOOLEAN DEFAULT FALSE;

-- Physical tables at the event
CREATE TABLE IF NOT EXISTS event_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    table_number INTEGER NOT NULL,
    table_name VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, table_number)
);

-- Game sessions scheduled at tables
CREATE TABLE IF NOT EXISTS event_game_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    table_id UUID NOT NULL REFERENCES event_tables(id) ON DELETE CASCADE,
    bgg_id INTEGER,
    game_name VARCHAR(200) NOT NULL,
    thumbnail_url TEXT,
    min_players INTEGER,
    max_players INTEGER,
    slot_index INTEGER NOT NULL DEFAULT 0,
    start_time TIME,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(20) DEFAULT 'scheduled',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(event_id, table_id, slot_index)
);

-- Player registrations for sessions
CREATE TABLE IF NOT EXISTS game_session_registrations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES event_game_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_host_reserved BOOLEAN DEFAULT FALSE,
    UNIQUE(session_id, user_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_event_tables_event ON event_tables(event_id);
CREATE INDEX IF NOT EXISTS idx_event_game_sessions_event ON event_game_sessions(event_id);
CREATE INDEX IF NOT EXISTS idx_event_game_sessions_table ON event_game_sessions(table_id);
CREATE INDEX IF NOT EXISTS idx_game_session_registrations_session ON game_session_registrations(session_id);
CREATE INDEX IF NOT EXISTS idx_game_session_registrations_user ON game_session_registrations(user_id);

-- Enable RLS on new tables
ALTER TABLE event_tables ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_game_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE game_session_registrations ENABLE ROW LEVEL SECURITY;

-- RLS Policies for event_tables
CREATE POLICY "event_tables_select" ON event_tables
    FOR SELECT USING (true);

CREATE POLICY "event_tables_insert" ON event_tables
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM events
            WHERE events.id = event_id
            AND events.host_user_id = auth.uid()
        )
    );

CREATE POLICY "event_tables_delete" ON event_tables
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM events
            WHERE events.id = event_id
            AND events.host_user_id = auth.uid()
        )
    );

-- RLS Policies for event_game_sessions
CREATE POLICY "event_game_sessions_select" ON event_game_sessions
    FOR SELECT USING (true);

CREATE POLICY "event_game_sessions_insert" ON event_game_sessions
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM events
            WHERE events.id = event_id
            AND events.host_user_id = auth.uid()
        )
    );

CREATE POLICY "event_game_sessions_update" ON event_game_sessions
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM events
            WHERE events.id = event_id
            AND events.host_user_id = auth.uid()
        )
    );

CREATE POLICY "event_game_sessions_delete" ON event_game_sessions
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM events
            WHERE events.id = event_id
            AND events.host_user_id = auth.uid()
        )
    );

-- RLS Policies for game_session_registrations
CREATE POLICY "game_session_registrations_select" ON game_session_registrations
    FOR SELECT USING (true);

CREATE POLICY "game_session_registrations_insert" ON game_session_registrations
    FOR INSERT WITH CHECK (
        user_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM event_game_sessions egs
            JOIN events e ON e.id = egs.event_id
            WHERE egs.id = session_id
            AND e.host_user_id = auth.uid()
        )
    );

CREATE POLICY "game_session_registrations_delete" ON game_session_registrations
    FOR DELETE USING (
        user_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM event_game_sessions egs
            JOIN events e ON e.id = egs.event_id
            WHERE egs.id = session_id
            AND e.host_user_id = auth.uid()
        )
    );
