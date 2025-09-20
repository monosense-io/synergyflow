Status: Draft

# Story
As a change approver,
I want CMDB baselines and snapshots with diffing,
so that I can compare states over time to assess risk and drift.

## Acceptance Criteria
1. Create Baseline (named) and Snapshot (time-based) for a defined CI scope (single CI or collection via query); store immutable records.
2. Diff API computes changes between two snapshots/baselines (added/removed/changed CIs and relationships) with human-readable and machine formats.
3. Retention policy for snapshots with configurable TTL; baselines retained until deleted by authorized roles.
4. Performance: diff requests p95 < 1s for collections ≤ 10k CIs; pagination for large diffs.
5. Audit: who/when/why for baseline creation and deletions; RBAC restricts access.

## Tasks / Subtasks
- [ ] Baseline/Snapshot schemas and storage (AC: 1)
- [ ] Scope selection (by IDs or saved query) (AC: 1)
- [ ] Diff engine and APIs (AC: 2, 4)
- [ ] Retention policy job and controls (AC: 3)
- [ ] RBAC and audit logging (AC: 5)

## Dev Notes
- PRD: 2.9 baselines/snapshots with diff view
- ADRs: 0009 DB migration policy; 0011 cache (optional for diffs)
- Architecture: data-architecture.md
- APIs: cmdb.yaml (baseline/snapshot endpoints)

## Testing
- Snapshot/baseline creation; diff correctness; retention enforcement
- RBAC and audit tests; performance of diff API

## Change Log
| Date       | Version | Description                                | Author |
|------------|---------|--------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — baseline/snapshot & diff   | PO     |

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

