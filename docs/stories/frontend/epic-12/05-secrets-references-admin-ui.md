Status: Draft

# Story
As a platform engineer,
I want a secrets references admin UI,
so that services can reference external secrets securely and transparently.

## Acceptance Criteria
1. List SecretRefs with path/engine (masked), owner, last rotated, consumers (services); filters and pagination.
2. Create/update SecretRef entries without exposing secret values; validate accessibility with a test action that returns only status, never values.
3. Rotate action triggers rotation workflow with confirmation; shows downstream impact list; progress tracked.
4. RBAC: admin‑only writes; read‑only for others; audit all actions; copy‑once download for bootstrap tokens when required.
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] SecretRef list and detail views (AC: 1)
- [ ] Create/update flows with validation and masking (AC: 2)
- [ ] Rotate action with impact preview and progress (AC: 3)
- [ ] RBAC/audit and copy‑once downloads (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-12/04-secrets-management-integration.md
- PRD: 13 security — secrets management
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (secrets references)

## Testing
- Validation/masking; rotate flows; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — secrets references admin UI | PO     |

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

