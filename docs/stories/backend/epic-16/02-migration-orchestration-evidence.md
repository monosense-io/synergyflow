Status: Draft

# Story
As a database engineer,
I want migration orchestration with evidence collection and rollback,
so that schema and data changes are safe and auditable.

## Acceptance Criteria
1. MigrationBatch model groups ordered migrations with preflight checks, dry‑run, and dependency validation; idempotent and reversible.
2. Orchestrator applies batches per environment with locks; emits migration.applied and rollback.executed on failure/revert.
3. Evidence captured: checksums, durations, row counts, before/after samples; evidence attached to batch record.
4. Health checks run post‑migration; gate with rollback if SLO signals degrade beyond thresholds.
5. APIs to list/apply/rollback batches; RBAC enforced; audit all operations.

## Tasks / Subtasks
- [ ] MigrationBatch schema and runner with locks (AC: 1, 2)
- [ ] Preflight/dry‑run and dependency validation (AC: 1)
- [ ] Evidence capture and storage (AC: 3)
- [ ] Post‑migration health checks and gates (AC: 4)
- [ ] APIs + RBAC + audit + events (AC: 5)

## Dev Notes
- PRD: docs/prd/20-deployment-operations.md; ADR 0009 DB migration policy
- Architecture: migrations.md; observability-reliability.md
- Events/APIs: system.yaml (migrations), events (migration.applied, rollback.executed)

## Testing
- Dry‑run vs apply flows; idempotency; rollback; evidence integrity; health gates

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — migration orchestration (16.02)| PO     |

## Dev Agent Record

### Agent Model Used
<record at implementation time>

### Debug Log References
<links at implementation time>

### Completion Notes List
<notes at implementation time>

### File List
<files at implementation time>

## QA Results
<QA to fill>

