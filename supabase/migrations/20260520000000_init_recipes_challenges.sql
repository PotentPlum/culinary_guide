CREATE TABLE recipes (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name        text NOT NULL,
  ingredients text[] NOT NULL DEFAULT '{}',
  instructions text NOT NULL,
  source_url  text NOT NULL DEFAULT '',
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE challenges (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  ingredient     text NOT NULL,
  challenge_text text NOT NULL,
  difficulty     text CHECK (difficulty IN ('easy', 'medium', 'hard')),
  created_at     timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE recipes   ENABLE ROW LEVEL SECURITY;
ALTER TABLE challenges ENABLE ROW LEVEL SECURITY;

CREATE POLICY "anon full access" ON recipes   FOR ALL TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon full access" ON challenges FOR ALL TO anon USING (true) WITH CHECK (true);
