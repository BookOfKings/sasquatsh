-- Resolve fixed bugs
UPDATE public.admin_bugs
SET status = 'resolved', resolved_at = NOW(), updated_at = NOW()
WHERE id IN (
  'ecb0835e-ac97-47f1-88a2-32310d517324',  -- Contacts page fails to send
  '4921dee6-ab5a-4ff9-9b9e-5aa8e3a98eab',  -- Games search not populating location
  'f7f7f74a-253e-4a16-a3f2-666931589c96'   -- Request to join group 403
);

-- Email/password signup is a Firebase Console config issue, not code
UPDATE public.admin_bugs
SET status = 'wont_fix', updated_at = NOW()
WHERE id = '7d744170-1462-4b95-9b5f-dc6a48f728f7';
