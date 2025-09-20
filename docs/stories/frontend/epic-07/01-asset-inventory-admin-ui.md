Status: Draft

# Story
As an ITAM administrator,
I want an asset inventory admin UI for searching, filtering, and managing assets,
so that I can maintain accurate records and governance.

## Acceptance Criteria
1. List view with filters (type, owner, status, location) and pagination; CSV export of current filter results.
2. Create/Edit forms with type-specific fields; validations; lifecycle actions (assign, transfer, retire) with rationale.
3. Audit preview shows recent lifecycle events for selected asset.
4. RBAC: admin-only create/edit/retire; others read-only; unauthorized actions hidden/disabled.
5. Performance and accessibility: list p95 < 800ms; a11y AA compliance.

## Tasks / Subtasks
- [ ] List/filter/sort with pagination and export (AC: 1)
- [ ] Create/Edit forms with type-specific fields and validations (AC: 2)
- [ ] Lifecycle action flows with rationale (AC: 2)
- [ ] Audit preview panel (AC: 3)
- [ ] RBAC UI states and a11y/perf checks (AC: 4, 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-07/02-asset-model-ownership-lifecycle-tracking.md
- PRD: 2.8 ITAM inventory; lifecycle
- Architecture/UX: docs/ux/README.md; gateway-envoy.md for admin routes
- ADRs: 0007 gateway
- APIs: cmdb.yaml/itam module; users.yaml

## Testing
- Filter/search; form validation; lifecycle flows; export; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — asset inventory admin UI (07) | PO     |

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

