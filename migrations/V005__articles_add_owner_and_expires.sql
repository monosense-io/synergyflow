-- V005: Add owner_id and expires_at to articles
ALTER TABLE articles 
  ADD COLUMN IF NOT EXISTS owner_id varchar,
  ADD COLUMN IF NOT EXISTS expires_at timestamp;

-- Rollback:
-- ALTER TABLE articles DROP COLUMN IF EXISTS expires_at;
-- ALTER TABLE articles DROP COLUMN IF EXISTS owner_id;

