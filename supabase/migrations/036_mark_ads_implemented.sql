-- Mark the Ads note as implemented
UPDATE admin_notes
SET is_implemented = true, updated_at = NOW()
WHERE title ILIKE '%ads%' OR title ILIKE '%advertising%';
