-- Migration V13: Align teams schema with Story 2.4 and seed defaults
-- This migration adapts the existing shared `teams` table (created in V3) to the
-- Story 2.4 expectations by normalizing column names and adding timestamps.
-- It is written to be idempotent when re-run.

-- 1) Rename columns if needed to match Story 2.4 naming
DO $$
BEGIN
    -- team_name -> name
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teams' AND column_name = 'name'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teams' AND column_name = 'team_name'
    ) THEN
        ALTER TABLE teams RENAME COLUMN team_name TO name;
    END IF;

    -- is_active -> enabled
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teams' AND column_name = 'enabled'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'teams' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE teams RENAME COLUMN is_active TO enabled;
    END IF;
END $$;

-- 2) Add updated_at timestamp if missing and backfill from created_at
ALTER TABLE teams ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE teams SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE teams ALTER COLUMN updated_at SET NOT NULL;

-- 3) Ensure unique index on teams(name)
DROP INDEX IF EXISTS idx_teams_name;
CREATE UNIQUE INDEX idx_teams_name ON teams(name);

-- 4) Seed default teams required by Story 2.4 (idempotent via ON CONFLICT)
INSERT INTO teams (id, name, description, enabled, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Network Team',  'Handles network infrastructure, VPN, firewall, and connectivity issues', true, NOW(), NOW()),
    (gen_random_uuid(), 'Software Team', 'Handles application issues, software installations, and license management', true, NOW(), NOW()),
    (gen_random_uuid(), 'Hardware Team', 'Handles hardware failures, device provisioning, and physical asset management', true, NOW(), NOW()),
    (gen_random_uuid(), 'General Support','Default team for tickets not matching other routing rules', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Note: We intentionally keep existing extra columns (e.g., team_lead_id, version)
-- from earlier migrations for backward compatibility with other modules.

