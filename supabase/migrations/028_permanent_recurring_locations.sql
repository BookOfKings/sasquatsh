-- Migration: Support permanent and recurring locations
-- 1. Make start_date/end_date nullable (permanent locations have no dates)
-- 2. Add is_permanent flag
-- 3. Add recurring_days for day-of-week recurrence (e.g., "every Saturday")

-- =====================================================
-- Part 1: Add new columns to event_locations
-- =====================================================

-- Add is_permanent flag (permanent locations don't expire)
ALTER TABLE event_locations ADD COLUMN IF NOT EXISTS is_permanent BOOLEAN NOT NULL DEFAULT FALSE;

-- Add recurring_days as an array of integers (0=Sunday, 1=Monday, ..., 6=Saturday)
ALTER TABLE event_locations ADD COLUMN IF NOT EXISTS recurring_days INTEGER[];

-- Make start_date nullable (permanent locations don't need dates)
ALTER TABLE event_locations ALTER COLUMN start_date DROP NOT NULL;

-- Make end_date nullable
ALTER TABLE event_locations ALTER COLUMN end_date DROP NOT NULL;

COMMENT ON COLUMN event_locations.is_permanent IS 'Permanent locations like game stores that never expire';
COMMENT ON COLUMN event_locations.recurring_days IS 'Array of day numbers (0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat) when location is active';

-- =====================================================
-- Part 2: Update RLS policies to handle permanent locations
-- =====================================================

-- Drop existing policies
DROP POLICY IF EXISTS "Approved active locations viewable by all" ON event_locations;
DROP POLICY IF EXISTS "Users can view own location submissions" ON event_locations;

-- Create new policy that handles permanent, dated, and recurring locations
CREATE POLICY "Approved active locations viewable by all" ON event_locations
    FOR SELECT USING (
        status = 'approved' AND (
            -- Permanent locations are always active
            is_permanent = TRUE
            -- Dated locations: end_date must be today or later
            OR (end_date IS NOT NULL AND end_date >= CURRENT_DATE)
            -- If no dates set, show anyway (safety net)
            OR (start_date IS NULL AND end_date IS NULL AND is_permanent = FALSE)
        )
    );

CREATE POLICY "Users can view own location submissions" ON event_locations
    FOR SELECT USING (
        created_by_user_id IN (
            SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
        )
    );

-- =====================================================
-- Part 3: Update hot_locations view
-- =====================================================

-- Drop and recreate the view because column structure changed
DROP VIEW IF EXISTS hot_locations;

CREATE VIEW hot_locations AS
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
AND (
    -- Permanent locations are always hot
    el.is_permanent = TRUE
    -- Dated locations: active now or starting within 30 days
    OR (
        el.end_date IS NOT NULL
        AND el.end_date >= CURRENT_DATE
        AND (el.start_date IS NULL OR el.start_date <= CURRENT_DATE + INTERVAL '30 days')
    )
    -- Recurring locations with no end date are always active
    OR (el.recurring_days IS NOT NULL AND array_length(el.recurring_days, 1) > 0)
)
ORDER BY
    -- Prioritize locations with activity
    (COALESCE(event_counts.count, 0) + COALESCE(user_counts.count, 0)) DESC,
    -- Then permanent locations
    el.is_permanent DESC,
    -- Then by start date
    el.start_date ASC NULLS LAST;

COMMENT ON VIEW hot_locations IS 'Active approved venues (permanent, recurring, or date-based) ranked by popularity';

-- =====================================================
-- Part 4: Create index for recurring days queries
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_event_locations_recurring ON event_locations USING GIN (recurring_days)
WHERE recurring_days IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_event_locations_permanent ON event_locations(is_permanent)
WHERE is_permanent = TRUE;
