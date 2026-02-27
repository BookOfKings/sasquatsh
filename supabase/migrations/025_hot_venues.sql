-- Migration: Hot Venues Feature
-- Revert to approval workflow, add venue fields to users and events

-- =====================================================
-- Part 1: Revert event_locations to require approval
-- =====================================================

-- Change default status back to pending
ALTER TABLE event_locations ALTER COLUMN status SET DEFAULT 'pending';

-- Drop the simplified policy
DROP POLICY IF EXISTS "Active locations viewable by all" ON event_locations;

-- Recreate approval-based policies (drop first in case they exist)
DROP POLICY IF EXISTS "Approved active locations viewable by all" ON event_locations;
DROP POLICY IF EXISTS "Users can view own location submissions" ON event_locations;

CREATE POLICY "Approved active locations viewable by all" ON event_locations
    FOR SELECT USING (status = 'approved' AND end_date >= CURRENT_DATE);

CREATE POLICY "Users can view own location submissions" ON event_locations
    FOR SELECT USING (
        created_by_user_id IN (
            SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
        )
    );

-- =====================================================
-- Part 2: Add venue fields to users (for active/traveling location)
-- =====================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS active_event_location_id UUID REFERENCES event_locations(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS active_location_hall VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active_location_room VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active_location_table VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_users_active_event_location ON users(active_event_location_id)
WHERE active_event_location_id IS NOT NULL;

COMMENT ON COLUMN users.active_event_location_id IS 'Reference to venue/convention the user is currently at';
COMMENT ON COLUMN users.active_location_hall IS 'Hall or area at the venue';
COMMENT ON COLUMN users.active_location_room IS 'Room name/number at the venue';
COMMENT ON COLUMN users.active_location_table IS 'Table number at the venue';

-- =====================================================
-- Part 3: Add venue fields to events
-- =====================================================

ALTER TABLE events ADD COLUMN IF NOT EXISTS event_location_id UUID REFERENCES event_locations(id) ON DELETE SET NULL;
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue_hall VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue_room VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS venue_table VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_events_event_location ON events(event_location_id)
WHERE event_location_id IS NOT NULL;

COMMENT ON COLUMN events.event_location_id IS 'Reference to approved venue/convention';
COMMENT ON COLUMN events.venue_hall IS 'Hall or area at the venue';
COMMENT ON COLUMN events.venue_room IS 'Room name/number at the venue';
COMMENT ON COLUMN events.venue_table IS 'Table number at the venue';

-- =====================================================
-- Part 4: Create hot_locations view for ranking venues
-- =====================================================

CREATE OR REPLACE VIEW hot_locations AS
SELECT
    el.*,
    COALESCE(event_counts.count, 0) as event_count,
    COALESCE(user_counts.count, 0) as user_count
FROM event_locations el
LEFT JOIN (
    SELECT event_location_id, COUNT(*) as count
    FROM events
    WHERE event_location_id IS NOT NULL
    AND event_date >= CURRENT_DATE
    GROUP BY event_location_id
) event_counts ON event_counts.event_location_id = el.id
LEFT JOIN (
    SELECT active_event_location_id, COUNT(*) as count
    FROM users
    WHERE active_event_location_id IS NOT NULL
    GROUP BY active_event_location_id
) user_counts ON user_counts.active_event_location_id = el.id
WHERE el.status = 'approved'
AND el.start_date <= CURRENT_DATE + INTERVAL '30 days'  -- Include upcoming events within 30 days
AND el.end_date >= CURRENT_DATE
ORDER BY (COALESCE(event_counts.count, 0) + COALESCE(user_counts.count, 0)) DESC, el.start_date ASC;

COMMENT ON VIEW hot_locations IS 'Active approved venues ranked by popularity (event + user count)';
