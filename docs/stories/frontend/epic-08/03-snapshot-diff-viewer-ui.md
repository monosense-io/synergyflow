Status: Draft

# Story
As a change approver,
I want a snapshot/baseline diff viewer UI,
so that I can compare CMDB states before and after changes.

## Acceptance Criteria
1. UI selects two snapshots/baselines or a release-linked pair; displays added/removed/changed CIs and relationships.
2. Diff items show field-level changes for CI attributes and edge changes; filters for type/status/owner; pagination for large diffs.
3. Export diff as CSV/JSON; link to impacted CIs; permissions respected.
4. Performance: initial diff render p95 < 1s for ≤ 10k changes with virtualization.
5. Accessibility and responsive layout.

## Tasks / Subtasks
- [ ] Snapshot selection UI and diff results rendering with virtualization (AC: 1, 4)
- [ ] Filters and pagination (AC: 2)
- [ ] Export and permission handling (AC: 3)
- [ ] a11y/responsive checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-08/03-baseline-snapshot-diff-management.md
- PRD: 2.9 diff view
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- APIs: cmdb.yaml (diff endpoints)

## Testing
- Diff accuracy display vs backend; performance; a11y; export correctness

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — snapshot diff viewer (08)    | PO     |

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

