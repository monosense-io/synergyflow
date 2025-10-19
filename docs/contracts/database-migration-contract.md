# Database Migration Contract

**Version:** 1.0.0
**Status:** Active
**Last Updated:** 2025-10-18

## Overview

This document defines the database schema migration contract for SynergyFlow, ensuring safe, versioned, and reversible database schema changes using **Flyway** as the migration tool.

## Migration Tool: Flyway

- **Version:** Flyway 10.x (latest stable)
- **Migration Location:** `backend/src/main/resources/db/migration/`
- **Execution:** Automatically on application startup (Spring Boot auto-configuration)
- **Validation:** Flyway validates checksum of applied migrations on startup

## Schema Organization

SynergyFlow uses **7 PostgreSQL schemas** within the `synergyflow` database:

| Schema | Purpose | Owner Module |
|--------|---------|--------------|
| `synergyflow_users` | User, Team, Role, Permission tables | User Module |
| `synergyflow_incidents` | Incident, Problem, Comment, Worklog tables | Incident Module |
| `synergyflow_changes` | Change, Release, Deployment tables | Change Module |
| `synergyflow_knowledge` | KnowledgeArticle, Tag, Version tables | Knowledge Module |
| `synergyflow_tasks` | Project, Epic, Story, Task, Sprint tables | Task Module |
| `synergyflow_audit` | AuditLog, DecisionReceipt tables | Audit Module |
| `synergyflow_workflows` | Flowable tables (ACT_RU_*, ACT_HI_*) | Workflow Module |
| `public` | Spring Modulith event_publication table | Shared |

## Migration Naming Convention

### Format

```
V{VERSION}__{DESCRIPTION}.sql
```

- **V**: Prefix indicating versioned migration (uppercase V)
- **VERSION**: Version number in format `YYYY.MM.DD.HH.MM` (timestamp-based)
- **__**: Double underscore separator
- **DESCRIPTION**: Underscore-separated description (snake_case)
- **.sql**: SQL file extension

### Version Number Strategy

**Primary Strategy: Timestamp-based (YYYY.MM.DD.HH.MM)**

- ✅ **Pros:** Prevents merge conflicts, natural chronological ordering
- ✅ **Used for:** All schema changes in active development
- ⚠️ **Format:** `YYYY.MM.DD.HH.MM` (e.g., `2025.10.18.14.30`)

**Alternative: Sequential (for releases)**

- Format: `1.0.0_001`, `1.0.0_002`, etc.
- Used only for release-branch stabilization

### Examples

```
V2025.10.18.14.30__create_incidents_schema.sql
V2025.10.18.14.31__create_incidents_table.sql
V2025.10.18.14.32__add_sla_deadline_to_incidents.sql
V2025.10.18.15.00__create_changes_schema.sql
V2025.10.19.09.00__add_version_to_incidents.sql
V2025.10.19.09.15__create_event_publication_table.sql
```

## Migration File Structure

### Directory Layout

```
backend/src/main/resources/db/migration/
├── V2025.10.18.14.30__create_incidents_schema.sql
├── V2025.10.18.14.31__create_incidents_table.sql
├── V2025.10.18.14.32__create_incidents_indexes.sql
├── V2025.10.18.15.00__create_changes_schema.sql
├── V2025.10.18.15.01__create_changes_table.sql
├── V2025.10.19.09.00__create_tasks_schema.sql
├── V2025.10.19.09.01__create_tasks_table.sql
├── R__refresh_incident_summary_view.sql  # Repeatable migration (views/functions)
└── README.md  # Migration documentation
```

### Repeatable Migrations

Prefix: `R__` (uppercase R, double underscore)

- **Purpose:** Views, stored procedures, functions that can be recreated
- **Execution:** Re-run every time checksum changes
- **Example:** `R__refresh_incident_summary_view.sql`

## Migration Templates

### 1. Create Schema Migration

```sql
-- V2025.10.18.14.30__create_incidents_schema.sql
-- Description: Create synergyflow_incidents schema for incident management module
-- Author: Architecture Team
-- Date: 2025-10-18

CREATE SCHEMA IF NOT EXISTS synergyflow_incidents;

-- Grant permissions
GRANT USAGE ON SCHEMA synergyflow_incidents TO synergyflow_app;
GRANT CREATE ON SCHEMA synergyflow_incidents TO synergyflow_app;

-- Add schema comment
COMMENT ON SCHEMA synergyflow_incidents IS 'Incident management tables (incidents, worklogs, comments)';
```

### 2. Create Table Migration

```sql
-- V2025.10.18.14.31__create_incidents_table.sql
-- Description: Create incidents table with optimistic locking
-- Author: Architecture Team
-- Date: 2025-10-18
-- Related: Story-001 (Incident Management)

CREATE TABLE synergyflow_incidents.incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('S1', 'S2', 'S3', 'S4')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    assigned_to UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    sla_deadline TIMESTAMP NOT NULL,
    resolution TEXT,
    version INTEGER NOT NULL DEFAULT 0, -- Optimistic locking

    -- Constraints
    CONSTRAINT incidents_resolved_at_check CHECK (
        (status IN ('RESOLVED', 'CLOSED') AND resolved_at IS NOT NULL) OR
        (status NOT IN ('RESOLVED', 'CLOSED') AND resolved_at IS NULL)
    )
);

-- Table comment
COMMENT ON TABLE synergyflow_incidents.incidents IS 'Incident tracking table with SLA management';

-- Column comments
COMMENT ON COLUMN synergyflow_incidents.incidents.version IS 'Optimistic locking version (JPA @Version)';
COMMENT ON COLUMN synergyflow_incidents.incidents.sla_deadline IS 'Calculated SLA deadline based on priority';
```

### 3. Add Column Migration

```sql
-- V2025.10.19.10.00__add_priority_override_to_incidents.sql
-- Description: Add priority_override column to support manual priority escalation
-- Author: Architecture Team
-- Date: 2025-10-19
-- Related: Story-042 (Manual Priority Override)

ALTER TABLE synergyflow_incidents.incidents
    ADD COLUMN priority_override BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN synergyflow_incidents.incidents.priority_override
    IS 'True if priority manually overridden (not auto-calculated)';
```

### 4. Add Index Migration

```sql
-- V2025.10.18.14.32__create_incidents_indexes.sql
-- Description: Create performance indexes for incidents table
-- Author: Architecture Team
-- Date: 2025-10-18

CREATE INDEX idx_incidents_status
    ON synergyflow_incidents.incidents(status);

CREATE INDEX idx_incidents_assigned_to
    ON synergyflow_incidents.incidents(assigned_to)
    WHERE assigned_to IS NOT NULL;  -- Partial index for assigned incidents

CREATE INDEX idx_incidents_created_at
    ON synergyflow_incidents.incidents(created_at DESC);

CREATE INDEX idx_incidents_sla_deadline
    ON synergyflow_incidents.incidents(sla_deadline)
    WHERE status NOT IN ('RESOLVED', 'CLOSED');  -- Partial index for open incidents

CREATE INDEX idx_incidents_priority_status
    ON synergyflow_incidents.incidents(priority, status);  -- Composite index for filtering

-- Index comments
COMMENT ON INDEX idx_incidents_sla_deadline IS 'Partial index for SLA monitoring (open incidents only)';
```

### 5. Add Foreign Key Migration

```sql
-- V2025.10.19.11.00__add_incidents_foreign_keys.sql
-- Description: Add foreign key constraints to incidents table
-- Author: Architecture Team
-- Date: 2025-10-19
-- IMPORTANT: Run after users table created

ALTER TABLE synergyflow_incidents.incidents
    ADD CONSTRAINT fk_incidents_assigned_to
    FOREIGN KEY (assigned_to)
    REFERENCES synergyflow_users.users(id)
    ON DELETE SET NULL;  -- If user deleted, set assigned_to to NULL

ALTER TABLE synergyflow_incidents.incidents
    ADD CONSTRAINT fk_incidents_created_by
    FOREIGN KEY (created_by)
    REFERENCES synergyflow_users.users(id)
    ON DELETE RESTRICT;  -- Cannot delete user who created incidents

-- Add indexes for FK performance
CREATE INDEX idx_incidents_fk_assigned_to ON synergyflow_incidents.incidents(assigned_to);
CREATE INDEX idx_incidents_fk_created_by ON synergyflow_incidents.incidents(created_by);
```

### 6. Data Migration

```sql
-- V2025.10.20.09.00__migrate_incident_priority_values.sql
-- Description: Migrate old 3-tier priority to new 4-tier priority system
-- Author: Architecture Team
-- Date: 2025-10-20
-- Related: Story-050 (Priority System Enhancement)

-- Update existing data
UPDATE synergyflow_incidents.incidents
SET priority = 'CRITICAL'
WHERE priority = 'HIGH' AND severity = 'S1';

UPDATE synergyflow_incidents.incidents
SET priority = 'HIGH'
WHERE priority = 'HIGH' AND severity IN ('S2', 'S3');

-- Verify migration
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM synergyflow_incidents.incidents
    WHERE priority NOT IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');

    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'Priority migration failed: % invalid records', invalid_count;
    END IF;
END $$;
```

### 7. Rollback Migration (Commented Template)

```sql
-- V2025.10.21.14.00__remove_deprecated_status_column.sql
-- Description: Remove deprecated status_old column (no longer used after status migration)
-- Author: Architecture Team
-- Date: 2025-10-21
-- Related: Story-060 (Status Cleanup)
-- ROLLBACK NOTES:
--   If rollback needed, manually run:
--   ALTER TABLE synergyflow_incidents.incidents ADD COLUMN status_old VARCHAR(20);

ALTER TABLE synergyflow_incidents.incidents
    DROP COLUMN IF EXISTS status_old;
```

## Migration Guidelines

### DO's ✅

1. **Always add comments** - Include description, author, date, related story
2. **Use idempotent operations** - Use `IF NOT EXISTS`, `IF EXISTS` where possible
3. **Add constraints gradually** - Create table → Add indexes → Add foreign keys
4. **Test migrations locally** - Run against local PostgreSQL before committing
5. **Version control migrations** - Never modify applied migrations (checksum validation)
6. **Document rollback steps** - Add rollback notes in comments
7. **Use transactions** - Flyway wraps each migration in transaction (PostgreSQL supports DDL transactions)
8. **Validate data migrations** - Add verification queries after data changes

### DON'Ts ❌

1. **Never modify applied migrations** - Creates checksum mismatch (Flyway will fail)
2. **Don't create cyclic dependencies** - Order migrations by dependency (users → incidents)
3. **Avoid breaking changes** - Use backward-compatible changes (add column with default)
4. **Don't mix DDL and DML** - Separate schema changes from data migrations
5. **Avoid long-running migrations** - Large data migrations should be batched
6. **Don't hardcode values** - Use parameterized scripts where possible

## Versioning Strategy

### Schema Versions

Track schema version separately from application version:

| Application Version | Schema Version | Notes |
|---------------------|----------------|-------|
| 1.0.0 | 2025.10.18 | Initial schema |
| 1.1.0 | 2025.11.05 | Add worklog mirroring |
| 1.2.0 | 2025.12.10 | Add decision receipts |

### Backward Compatibility

**Backward-Compatible Changes** (safe to deploy):
- ✅ Add new table
- ✅ Add new column with default value
- ✅ Add new index
- ✅ Add new constraint (non-breaking)

**Breaking Changes** (require blue-green deployment):
- ⚠️ Rename column (use expand-contract pattern)
- ⚠️ Change column type (add new column, migrate data, drop old)
- ⚠️ Drop column (deprecate first, drop later)
- ⚠️ Change constraint (may fail existing data)

### Expand-Contract Pattern (for breaking changes)

**Phase 1: Expand** (Add new column)
```sql
-- V2025.11.01.10.00__add_incident_state_new.sql
ALTER TABLE synergyflow_incidents.incidents
    ADD COLUMN state VARCHAR(30);

-- Backfill data
UPDATE synergyflow_incidents.incidents
SET state = status;
```

**Phase 2: Dual-Write** (Application writes to both columns)
- Deploy application version that writes to both `status` and `state`

**Phase 3: Contract** (Remove old column after all clients migrated)
```sql
-- V2025.11.15.10.00__drop_incident_status_old.sql
ALTER TABLE synergyflow_incidents.incidents
    DROP COLUMN status;

ALTER TABLE synergyflow_incidents.incidents
    RENAME COLUMN state TO status;
```

## Testing Migrations

### Local Testing

```bash
# Clean database
./gradlew flywayClean

# Run migrations
./gradlew flywayMigrate

# Validate migrations
./gradlew flywayValidate

# Show migration info
./gradlew flywayInfo
```

### Staging Testing

- **Run migrations against staging database** before production
- **Test rollback scenarios** (manual rollback scripts)
- **Validate data integrity** after migration

### Production Deployment

1. **Backup database** before migration
2. **Run Flyway migrate** on production
3. **Validate migration success** (check `flyway_schema_history` table)
4. **Monitor application health** after migration
5. **Rollback if needed** (manual rollback + application rollback)

## Flyway Configuration

### application.yml

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true  # Baseline existing database
    baseline-version: 0
    validate-on-migrate: true  # Validate checksum
    out-of-order: false        # Disallow out-of-order migrations
    schemas:
      - synergyflow_users
      - synergyflow_incidents
      - synergyflow_changes
      - synergyflow_knowledge
      - synergyflow_tasks
      - synergyflow_audit
      - synergyflow_workflows
      - public
```

## Migration Checklist

Before committing migration:

- [ ] Migration file named correctly (`V{YYYY.MM.DD.HH.MM}__{description}.sql`)
- [ ] Header comments added (description, author, date, related story)
- [ ] Idempotent operations used (`IF NOT EXISTS`, `IF EXISTS`)
- [ ] Constraints validated (CHECK constraints, foreign keys)
- [ ] Indexes created for foreign keys and query patterns
- [ ] Table/column comments added
- [ ] Tested locally (`flywayMigrate`, `flywayValidate`)
- [ ] Rollback notes documented (if breaking change)
- [ ] Code review completed
- [ ] Staging deployment tested

## Troubleshooting

### Checksum Mismatch Error

**Error:** `Migration checksum mismatch for migration version X`

**Cause:** Applied migration file was modified

**Solution:**
```sql
-- Option 1: Repair Flyway (updates checksum in flyway_schema_history)
./gradlew flywayRepair

-- Option 2: Manual fix (update checksum in database)
UPDATE flyway_schema_history
SET checksum = <new_checksum>
WHERE version = 'X';
```

**Prevention:** Never modify applied migrations

### Out-of-Order Migration

**Error:** `Detected resolved migration not applied to database: X`

**Cause:** New migration has version older than latest applied migration

**Solution:**
```yaml
# Allow out-of-order migrations (use cautiously)
spring.flyway.out-of-order: true
```

**Prevention:** Use timestamp-based versioning

### Failed Migration

**Error:** Migration fails mid-execution

**Recovery:**
1. Check `flyway_schema_history` table for failed migration
2. Manually rollback changes (if transaction failed)
3. Fix migration SQL
4. Re-run migration

## Appendix: Spring Modulith Event Publication Table

### Auto-Created by Spring Modulith

```sql
-- Created automatically by Spring Modulith on first run
CREATE TABLE event_publication (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_data JSONB NOT NULL,
    published_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    listener_id VARCHAR(255),
    retry_count INTEGER NOT NULL DEFAULT 0,
    correlation_id UUID NOT NULL,
    causation_id UUID
);

CREATE INDEX idx_event_publication_aggregate_id ON event_publication(aggregate_id);
CREATE INDEX idx_event_publication_event_type ON event_publication(event_type);
CREATE INDEX idx_event_publication_completed_at ON event_publication(completed_at);
```

**Note:** This table is managed by Spring Modulith and should NOT be modified via Flyway migrations.

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL DDL Best Practices](https://www.postgresql.org/docs/current/ddl.html)
- [Database Migration Best Practices (Martin Fowler)](https://martinfowler.com/articles/evodb.html)
