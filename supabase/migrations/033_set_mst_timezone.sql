-- Set all existing users to Mountain Standard Time (MST)
UPDATE users SET timezone = 'America/Denver';

-- Also update the default for new users
ALTER TABLE users ALTER COLUMN timezone SET DEFAULT 'America/Denver';
