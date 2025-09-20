Status: Draft

# Story
As a database engineer,
I want a migrations dashboard UI,
so that I can plan, apply, monitor, and roll back migration batches with evidence.

## Acceptance Criteria
1. List MigrationBatches with status, env, checks, dry‑run results, and evidence links; filters and pagination.
2. Apply and rollback actions with preflight summary; locks visualized; events shown (migration.applied, rollback.executed).
3. Evidence panel shows checksums, durations, row counts, and before/after samples; export evidence packet.
4. a11y AA; responsive; performance: updates under 1s; real‑time events wired.
5. Permissions and audit: admin‑only destructive actions; audit panel visible.

## Tasks / Subtasks
- [ ] List/detail views with filters and evidence panel (AC: 1, 3)
- [ ] Apply/rollback flows with preflight (AC: 2)
- [ ] Real‑time updates; export; a11y/perf (AC: 4)
- [ ] Permissions/audit integration (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-16/02-migration-orchestration-evidence.md
- PRD: 20 migrations process
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- Events/APIs: system.yaml (migrations), events (migration.applied, rollback.executed)

## Testing
- Flow correctness; evidence export; real‑time updates; permissions; a11y/perf

## Change Log
| Date       | Version | Description                               | Author |
|------------|---------|-------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — migrations dashboard UI   | PO     |

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

