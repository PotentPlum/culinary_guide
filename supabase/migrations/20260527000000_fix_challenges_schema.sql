ALTER TABLE challenges
  DROP CONSTRAINT IF EXISTS challenges_difficulty_check;

ALTER TABLE challenges
  ADD CONSTRAINT challenges_difficulty_check
  CHECK (difficulty IN ('Beginner', 'Intermediate', 'Advanced'));

ALTER TABLE challenges ADD COLUMN IF NOT EXISTS skill         TEXT;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS time_estimate INTEGER;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS why           TEXT;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS output_type   TEXT;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS recipe        JSONB;
ALTER TABLE challenges ADD COLUMN IF NOT EXISTS params        JSONB;
