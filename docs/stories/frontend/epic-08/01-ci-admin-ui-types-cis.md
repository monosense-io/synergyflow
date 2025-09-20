Status: Draft

# Story
As a CMDB administrator,
I want a CI admin UI to manage CI types and CIs,
so that type schemas, ownership, and CI records are governed through a clear interface.

## Acceptance Criteria
1. CIType management screens: list/create/edit with schema editor (fields, required, formats) and validation preview.
2. CI management screens: list/filter (type, owner, status), create/edit with type-driven dynamic fields and validation errors.
3. Ownership assignment to users/teams with search; status transitions (active/retired) gated with confirmation and rationale.
4. Audit panel shows recent changes; RBAC hides admin actions from unauthorized users.
5. Accessibility and performance: lists p95 < 800ms; editor responsive; a11y AA.

## Tasks / Subtasks
- [ ] CIType list/editor with schema validation preview (AC: 1)
- [ ] CI list/create/edit with dynamic fields and validation (AC: 2)
- [ ] Ownership/Status controls with rationale (AC: 3)
- [ ] Audit panel and RBAC UI states (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-08/01-ci-model-type-owners.md
- PRD: 2.9 CMDB admin
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: cmdb.yaml; users.yaml; teams.yaml

## Testing
- Schema editor validation; CI create/edit with dynamic validation; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — CI admin UI (08)             | PO     |

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

