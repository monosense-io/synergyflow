-- V004: Add form_data and approved_by to service_requests
ALTER TABLE service_requests 
  ADD COLUMN IF NOT EXISTS form_data jsonb,
  ADD COLUMN IF NOT EXISTS approved_by varchar;

-- Rollback:
-- ALTER TABLE service_requests DROP COLUMN IF EXISTS approved_by;
-- ALTER TABLE service_requests DROP COLUMN IF EXISTS form_data;

