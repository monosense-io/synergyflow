Status: Draft

# Story
As a compliance officer,
I want an audit log explorer UI,
so that I can search, filter, and export signed audit events.

## Acceptance Criteria
1. Search filters: actor, action, resource, result, timeframe; pagination; export with PII redaction.
2. Detail view shows normalized event fields, signature/hash chain status, and correlation ID; verify button re‑checks integrity.
3. Saved queries and scheduled exports for audits (admin‑only); permissions enforced.
4. Performance: filter changes p95 < 800ms on typical ranges; background export for large sets.
5. Accessibility and responsive layout.

## Tasks / Subtasks
- [ ] Search UI and filters; pagination and export (AC: 1)
- [ ] Detail view with signature status and verify action (AC: 2)
- [ ] Saved queries and scheduled exports (AC: 3)
- [ ] a11y/perf checks (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-12/03-centralized-audit-pipeline-signed-logs.md
- PRD: 13 auditability
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (audit query/export)

## Testing
- Filters; export redaction; integrity verification; saved queries; a11y/perf

## Change Log
| Date       | Version | Description                             | Author |
|------------|---------|-----------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — audit log explorer UI   | PO     |

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

