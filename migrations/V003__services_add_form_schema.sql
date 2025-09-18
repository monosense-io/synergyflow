-- V003: Add form_schema to services
ALTER TABLE services ADD COLUMN IF NOT EXISTS form_schema jsonb;

-- Rollback:
-- ALTER TABLE services DROP COLUMN IF EXISTS form_schema;

