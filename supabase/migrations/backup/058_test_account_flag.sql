-- Add is_test_account flag to users table
-- Test accounts should not be able to enter raffles

-- Add the column
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_test_account BOOLEAN DEFAULT FALSE;

-- Create index for filtering
CREATE INDEX IF NOT EXISTS idx_users_is_test_account ON users(is_test_account) WHERE is_test_account = TRUE;

-- Update the raffle entry multiplier function to exclude test accounts
CREATE OR REPLACE FUNCTION get_raffle_entry_multiplier(user_uuid UUID, entry_type VARCHAR)
RETURNS INTEGER AS $$
DECLARE
  user_tier VARCHAR;
  user_is_test BOOLEAN;
  user_is_founding BOOLEAN;
BEGIN
  -- Get user flags
  SELECT is_founding_member, COALESCE(is_test_account, FALSE)
  INTO user_is_founding, user_is_test
  FROM users
  WHERE id = user_uuid;

  -- Founding members don't participate in raffles (they get free stuff already)
  IF user_is_founding THEN
    RETURN 0;
  END IF;

  -- Test accounts are not eligible for raffles
  IF user_is_test THEN
    RETURN 0;
  END IF;

  -- Get user's subscription tier
  SELECT COALESCE(subscription_override_tier, subscription_tier, 'free')
  INTO user_tier
  FROM users
  WHERE id = user_uuid;

  -- Attending always gives 1 entry
  IF entry_type = 'attend_event' THEN
    RETURN 1;
  END IF;

  -- Hosting/planning: paid tiers get 2 entries, free gets 1
  IF user_tier IN ('basic', 'pro', 'premium') THEN
    RETURN 2;
  ELSE
    RETURN 1;
  END IF;
END;
$$ LANGUAGE plpgsql;

COMMENT ON COLUMN users.is_test_account IS 'Test accounts used for E2E testing - excluded from raffles';
