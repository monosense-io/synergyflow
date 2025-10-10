-- Migration V14: Routing rules team reference
-- Story 2.4 AC-10 requests adding a `team_id` FK to `routing_rules`.
-- The codebase already uses `target_team_id` with an FK (added in earlier migrations),
-- which fulfills the same requirement. This migration is a documented no-op to
-- keep versioning consistent with the Story plan without duplicating columns.

-- Verification (no-op): ensure the existing column is present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'routing_rules' AND column_name = 'target_team_id'
    ) THEN
        RAISE EXCEPTION 'Expected column routing_rules.target_team_id not found';
    END IF;
END $$;

