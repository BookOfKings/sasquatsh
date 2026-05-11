-- Referral system

-- Track referrals
CREATE TABLE referrals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  referrer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  referred_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status VARCHAR(20) NOT NULL DEFAULT 'credited' CHECK (status IN ('credited', 'revoked')),
  credited_at TIMESTAMPTZ DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(referred_user_id),
  CHECK (referrer_user_id != referred_user_id)
);

CREATE INDEX idx_referrals_referrer ON referrals(referrer_user_id);
CREATE INDEX idx_referrals_referred ON referrals(referred_user_id);

-- Track referral reward milestones
CREATE TABLE referral_rewards (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  referral_count_at_reward INTEGER NOT NULL,
  pro_expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_referral_rewards_user ON referral_rewards(user_id);

-- Add referred_by to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS referred_by_user_id UUID REFERENCES users(id);

-- Add subscription override expiry
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_override_expires_at TIMESTAMPTZ;

-- Add 'referral' to raffle entry types
ALTER TABLE raffle_entries DROP CONSTRAINT IF EXISTS valid_entry_type;
ALTER TABLE raffle_entries ADD CONSTRAINT valid_entry_type
  CHECK (entry_type IN ('host_event', 'plan_session', 'attend_event', 'mail_in', 'referral'));

-- Enable RLS
ALTER TABLE referrals ENABLE ROW LEVEL SECURITY;
ALTER TABLE referral_rewards ENABLE ROW LEVEL SECURITY;

-- RLS policies (service role bypasses, these are for completeness)
CREATE POLICY "Users can view their own referrals" ON referrals
  FOR SELECT USING (referrer_user_id = auth.uid() OR referred_user_id = auth.uid());

CREATE POLICY "Users can view their own rewards" ON referral_rewards
  FOR SELECT USING (user_id = auth.uid());
