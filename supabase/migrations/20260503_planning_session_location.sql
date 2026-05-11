-- Add location fields to planning sessions
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS event_location_id UUID REFERENCES event_locations(id);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS venue_hall VARCHAR(100);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS venue_room VARCHAR(100);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS venue_table VARCHAR(100);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(200);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS state VARCHAR(50);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE planning_sessions ADD COLUMN IF NOT EXISTS location_details TEXT;
