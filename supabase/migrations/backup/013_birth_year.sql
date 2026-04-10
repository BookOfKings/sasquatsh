-- Change birth_date to birth_year (just the year, more privacy-friendly)

-- Drop the old column and add new one
ALTER TABLE users DROP COLUMN IF EXISTS birth_date;
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_year INTEGER CHECK (birth_year >= 1900 AND birth_year <= 2100);
