ALTER TABLE challenges
  ADD COLUMN IF NOT EXISTS techniques   text[]  NOT NULL DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS cuisines     text[]  NOT NULL DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS ingredients  text[]  NOT NULL DEFAULT '{}';
