CREATE TABLE IF NOT EXISTS public.user_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  source_type text NOT NULL CHECK (source_type IN ('challenge', 'recipe')),
  source_id uuid,
  attempted_at timestamptz NOT NULL DEFAULT now(),
  score integer CHECK (score BETWEEN 1 AND 10),
  techniques text[] NOT NULL DEFAULT '{}',
  cuisines text[] NOT NULL DEFAULT '{}',
  ingredients text[] NOT NULL DEFAULT '{}',
  taste_score integer CHECK (taste_score BETWEEN 1 AND 10),
  notes text
);

ALTER TABLE public.user_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own attempts"
  ON public.user_attempts
  FOR ALL
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);
