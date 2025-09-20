Status: Draft

# Story
As a CMDB steward,
I want a data quality dashboard UI,
so that I can track quality scores and remediate violations.

## Acceptance Criteria
1. Dashboard shows quality scores by CIType/team with trend charts; filters by type/team/date range.
2. Violations table lists issues (missing owner, invalid attribute, relationship constraint); supports bulk assign/close with rationale.
3. Links to CI detail and remediation tasks; notifications can be triggered for owners.
4. Export CSV; permissions enforced; audit of actions.
5. Accessibility and performance: updates p95 < 1s with caching; a11y AA.

## Tasks / Subtasks
- [ ] Widgets and filters; trend charts (AC: 1)
- [ ] Violations table with bulk actions (AC: 2)
- [ ] Remediation task linking and notifications (AC: 3)
- [ ] Export and permissions/audit (AC: 4)
- [ ] Caching and a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-08/05-data-quality-governance-validation.md
- PRD: 2.9 governance and data quality
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway; 0011 cache store
- APIs: cmdb.yaml (quality endpoints); system.yaml (notifications)

## Testing
- Score/violation accuracy; bulk actions; notifications; export; a11y/perf

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — data quality dashboard UI (08)  | PO     |

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

