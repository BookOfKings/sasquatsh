-- Add is_implemented flag to admin_notes
ALTER TABLE admin_notes ADD COLUMN IF NOT EXISTS is_implemented BOOLEAN DEFAULT FALSE;
