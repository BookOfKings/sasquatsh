-- Migration: LFP Enhancements
-- Adds: Event Locations, LFP location details, blocked users, admin flag

-- =====================================================
-- Part 1: Event Locations (shared, with dates)
-- =====================================================
CREATE TABLE IF NOT EXISTS event_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    name_normalized VARCHAR(200) NOT NULL,  -- lowercase for duplicate detection
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    venue VARCHAR(200),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for event_locations
CREATE INDEX IF NOT EXISTS idx_event_locations_normalized ON event_locations(name_normalized, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_event_locations_status ON event_locations(status, end_date);
CREATE INDEX IF NOT EXISTS idx_event_locations_dates ON event_locations(start_date, end_date);

-- =====================================================
-- Part 2: LFP Location Details (modify player_requests)
-- =====================================================
ALTER TABLE player_requests ADD COLUMN IF NOT EXISTS event_location_id UUID REFERENCES event_locations(id) ON DELETE SET NULL;
ALTER TABLE player_requests ADD COLUMN IF NOT EXISTS hall_area VARCHAR(100);
ALTER TABLE player_requests ADD COLUMN IF NOT EXISTS table_number VARCHAR(50);
ALTER TABLE player_requests ADD COLUMN IF NOT EXISTS booth VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_player_requests_event_location ON player_requests(event_location_id);

-- =====================================================
-- Part 3: Admin flag and Blocked Users
-- =====================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_user_ids UUID[] DEFAULT '{}';

-- GIN index for efficient array containment queries
CREATE INDEX IF NOT EXISTS idx_users_blocked ON users USING GIN (blocked_user_ids);

-- =====================================================
-- Part 4: RLS Policies for event_locations
-- =====================================================
ALTER TABLE event_locations ENABLE ROW LEVEL SECURITY;

-- Anyone can view approved, active (not expired) locations
CREATE POLICY "Approved active locations viewable by all" ON event_locations
    FOR SELECT USING (status = 'approved' AND end_date >= CURRENT_DATE);

-- Users can view their own pending/rejected submissions
CREATE POLICY "Users can view own location submissions" ON event_locations
    FOR SELECT USING (
        created_by_user_id IN (
            SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
        )
    );

-- Admins can view all locations (for management)
CREATE POLICY "Admins can view all locations" ON event_locations
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM users
            WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
            AND is_admin = TRUE
        )
    );

-- Authenticated users can insert (creates pending status)
CREATE POLICY "Authenticated users can create locations" ON event_locations
    FOR INSERT WITH CHECK (
        created_by_user_id IN (
            SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
        )
    );

-- Admins can update locations (approve, reject, edit)
CREATE POLICY "Admins can update locations" ON event_locations
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM users
            WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
            AND is_admin = TRUE
        )
    );

-- Admins can delete locations
CREATE POLICY "Admins can delete locations" ON event_locations
    FOR DELETE USING (
        EXISTS (
            SELECT 1 FROM users
            WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
            AND is_admin = TRUE
        )
    );
