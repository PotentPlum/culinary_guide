CREATE TABLE IF NOT EXISTS user_attempts (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id       uuid NOT NULL,
  source_type   text NOT NULL CHECK (source_type IN ('challenge', 'recipe')),
  source_id     uuid,
  attempted_at  timestamptz NOT NULL DEFAULT now(),
  score         integer CHECK (score BETWEEN 1 AND 10),
  techniques    text[]  NOT NULL DEFAULT '{}',
  cuisines      text[]  NOT NULL DEFAULT '{}',
  ingredients   text[]  NOT NULL DEFAULT '{}',
  taste_score   integer CHECK (taste_score BETWEEN 1 AND 10),
  notes         text
);

-- RLS: anon full access (matches existing tables)
ALTER TABLE user_attempts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "anon full access" ON user_attempts FOR ALL TO anon USING (true) WITH CHECK (true);
