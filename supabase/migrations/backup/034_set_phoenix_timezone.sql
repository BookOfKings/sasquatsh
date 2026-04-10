-- Set all existing users to Arizona time (MST year-round, no DST)
UPDATE users SET timezone = 'America/Phoenix';

-- Also update the default for new users
ALTER TABLE users ALTER COLUMN timezone SET DEFAULT 'America/Phoenix';
