ALTER TABLE challenges ADD COLUMN IF NOT EXISTS user_id uuid;
CREATE INDEX IF NOT EXISTS challenges_user_id_idx ON challenges (user_id);
