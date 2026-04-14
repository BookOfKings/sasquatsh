-- Add collection visibility preference to users table
ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS collection_visibility VARCHAR(10) DEFAULT 'private' NOT NULL
  CONSTRAINT chk_collection_visibility CHECK (collection_visibility IN ('public', 'private'));
