Status: Draft

# Story
As a notifications admin,
I want a template editor UI with localization and preview,
so that I can author and manage channel‑specific templates safely.

## Acceptance Criteria
1. Editor supports per‑channel parts (email subject/body, push title/body, in‑app text) with variable linting and locale tabs.
2. Preview renders with sample context and locale; missing variables highlighted with diagnostics; diff view between versions.
3. Versioning controls: save draft, publish with confirmation, rollback with rationale; audit panel shows history.
4. RBAC: only admins can edit/publish; others read‑only; changes localized with fallback indicators.
5. Accessibility and performance standards met; large templates remain responsive.

## Tasks / Subtasks
- [ ] Template editor with variable linting and locale tabs (AC: 1)
- [ ] Preview and diagnostics; version diff (AC: 2)
- [ ] Publish/rollback with audit; RBAC UI states (AC: 3, 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-11/01-template-localization-system.md
- PRD: 07 notifications — templates & localization
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (templates)

## Testing
- Linting and diagnostics; preview correctness; versioning flows; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — template editor & i18n UI    | PO     |

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

