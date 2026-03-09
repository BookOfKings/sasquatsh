-- Clear all test Stripe data so live customers can be created
UPDATE users
SET
  stripe_customer_id = NULL,
  stripe_subscription_id = NULL,
  subscription_tier = 'free',
  subscription_status = NULL,
  subscription_expires_at = NULL
WHERE stripe_customer_id IS NOT NULL;
