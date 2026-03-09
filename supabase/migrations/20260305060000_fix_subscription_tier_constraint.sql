-- Drop and recreate the subscription_tier check constraint to allow all valid tiers
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_subscription_tier_check;

-- Add the correct constraint with all valid subscription tiers
ALTER TABLE users ADD CONSTRAINT users_subscription_tier_check 
  CHECK (subscription_tier IN ('free', 'basic', 'pro', 'premium'));
