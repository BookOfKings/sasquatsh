-- Enable pg_cron and pg_net extensions (Supabase provides these)
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Schedule recurring games generator to run twice daily (6 AM and 6 PM UTC)
-- The function is idempotent so running multiple times is safe.
-- It generates new planning sessions/events and auto-finalizes expired sessions.
SELECT cron.schedule(
  'recurring-games-generator',
  '0 6,18 * * *',
  $$
  SELECT net.http_post(
    url := 'https://yyfukoddeyiaxiufztdx.supabase.co/functions/v1/recurring-games-generator',
    headers := jsonb_build_object(
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl5ZnVrb2RkZXlpYXhpdWZ6dGR4Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTkxMDE4MiwiZXhwIjoyMDg3NDg2MTgyfQ.KJR2maxnJvBlEcQHMQHyiFlaCQwWS4HhU2akg2wI_cA',
      'Content-Type', 'application/json'
    ),
    body := '{}'::jsonb
  );
  $$
);
