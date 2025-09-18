-- V002: Add rollback fields to changes
ALTER TABLE changes 
  ADD COLUMN IF NOT EXISTS rollback_plan text,
  ADD COLUMN IF NOT EXISTS rollback_verified boolean DEFAULT false;

-- Rollback:
-- ALTER TABLE changes DROP COLUMN IF EXISTS rollback_verified;
-- ALTER TABLE changes DROP COLUMN IF EXISTS rollback_plan;

