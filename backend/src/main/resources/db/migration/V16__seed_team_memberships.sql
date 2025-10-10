-- Migration V16: Seed default team memberships (idempotent)
-- Adds all active users as members of 'General Support' team if present.

DO $$
DECLARE
    gs_id UUID;
BEGIN
    SELECT id INTO gs_id FROM teams WHERE name = 'General Support' LIMIT 1;
    IF gs_id IS NOT NULL THEN
        INSERT INTO team_members (id, team_id, user_id, added_at, version)
        SELECT gen_random_uuid(), gs_id, u.id, now(), 1
        FROM users u
        WHERE u.is_active = true
        ON CONFLICT (team_id, user_id) DO NOTHING;
    END IF;
END $$;

