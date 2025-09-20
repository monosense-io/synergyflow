Status: Draft

# Story
As a service designer,
I want a form schema editor UI with preview and validation,
so that I can define dynamic request forms with confidence.

## Acceptance Criteria
1. Editor supports JSON Schema with helpers for conditional logic; inline validation with error highlighting.
2. Live preview renders the form with current schema and sample data; shows conditional behavior.
3. Versioning UI to compare schemas (diff) and restore previous versions with confirmation.
4. Save/publish workflow with RBAC and audit of changes (who/when/what).
5. Accessibility and performance: editor interactions responsive; large schemas handled smoothly.

## Tasks / Subtasks
- [ ] Schema editor with linting and conditional helpers (AC: 1)
- [ ] Live form preview with sample data controls (AC: 2)
- [ ] Version history, diff, and restore (AC: 3)
- [ ] Save/publish flows with RBAC and audit (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-04/02-dynamic-form-schema-engine.md
- PRD: 2.6 dynamic forms
- Architecture/UX: docs/ux/README.md; gateway-envoy.md (admin routes)
- ADRs: 0007 gateway
- APIs: service-requests.yaml (schema endpoints)

## Testing
- Editor validation; preview correctness; diff/restore flows; role checks

## Change Log
| Date       | Version | Description                                | Author |
|------------|---------|--------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Form schema editor UI (04) | PO     |

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

