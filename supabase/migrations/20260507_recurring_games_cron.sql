-- Enable pg_cron and pg_net extensions (Supabase provides these)
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Schedule recurring games generator to run twice daily (6 AM and 6 PM UTC)
-- The function is idempotent so running multiple times is safe.
-- It generates new planning sessions/events and auto-finalizes expired sessions.
-- Note: Uses service_role key from Supabase config (not hardcoded).
SELECT cron.schedule(
  'recurring-games-generator',
  '0 6,18 * * *',
  $$
  SELECT net.http_post(
    url := 'https://yyfukoddeyiaxiufztdx.supabase.co/functions/v1/recurring-games-generator',
    headers := jsonb_build_object(
      'Authorization', 'Bearer ' || current_setting('app.settings.service_role_key', true),
      'Content-Type', 'application/json'
    ),
    body := '{}'::jsonb
  );
  $$
);
