ALTER TABLE challenges ADD COLUMN IF NOT EXISTS user_id uuid;
CREATE INDEX IF NOT EXISTS challenges_user_id_idx ON challenges (user_id);

-- Replace the blanket anon-full-access policy with a user-scoped one so rows
-- are isolated at the DB level, not just by application-side .eq() filters.
DROP POLICY IF EXISTS "anon full access" ON challenges;
CREATE POLICY "user access" ON challenges
  FOR ALL
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);
