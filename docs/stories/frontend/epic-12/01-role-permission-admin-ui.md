Status: Draft

# Story
As a security admin,
I want a roles, permissions, and policy admin UI,
so that I can manage RBAC/ABAC definitions with audit and safety.

## Acceptance Criteria
1. Roles/permissions list and editors with search and pagination; policy editor for ABAC rules with validation and test inputs.
2. Explainability panel: given a user and resource, show allow/deny decision rationale and matching rules.
3. Versioning UI: draft→publish→rollback with rationale; audit panel shows who/when/what changed.
4. RBAC: only security admins can edit/publish; others read‑only; error states for invalid policies.
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] Role/permission lists and editors (AC: 1)
- [ ] Policy editor + test harness (AC: 1, 2)
- [ ] Explainability panel (AC: 2)
- [ ] Versioning/publish/rollback with audit (AC: 3)
- [ ] RBAC UI + a11y/perf checks (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-12/01-authz-service-rbac-abac-policies.md
- PRD: 13 security & compliance — access controls
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md (admin routes)
- ADRs: 0007 gateway
- APIs: system.yaml (authz admin)

## Testing
- Editor validations; explainability accuracy; versioning flows; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                               | Author |
|------------|---------|-------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — roles/permissions admin UI | PO     |

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

