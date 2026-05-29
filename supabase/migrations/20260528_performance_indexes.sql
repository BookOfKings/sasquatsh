-- Add critical indexes for commonly queried columns

-- firebase_uid is used in every authenticated request for user lookup
CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users (firebase_uid);

-- email lookups for admin search and duplicate checks
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- Unique constraint on referral_rewards to prevent duplicate milestone awards
CREATE UNIQUE INDEX IF NOT EXISTS idx_referral_rewards_user_milestone
  ON referral_rewards (user_id, referral_count_at_reward);
