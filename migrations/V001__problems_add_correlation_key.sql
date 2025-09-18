-- V001: Add correlation_key to problems
ALTER TABLE problems ADD COLUMN IF NOT EXISTS correlation_key varchar;

-- Rollback (manual consideration):
-- ALTER TABLE problems DROP COLUMN IF EXISTS correlation_key;

