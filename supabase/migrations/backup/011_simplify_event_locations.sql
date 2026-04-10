-- Simplify event locations - no approval workflow needed
-- All locations are immediately available

-- Update existing locations to approved status
UPDATE event_locations SET status = 'approved' WHERE status = 'pending';

-- Change default status to approved
ALTER TABLE event_locations ALTER COLUMN status SET DEFAULT 'approved';

-- Drop the old RLS policies
DROP POLICY IF EXISTS "Approved active locations viewable by all" ON event_locations;
DROP POLICY IF EXISTS "Users can view own submissions" ON event_locations;

-- Create simple policy - all active locations are viewable
CREATE POLICY "Active locations viewable by all" ON event_locations
    FOR SELECT USING (end_date >= CURRENT_DATE);
