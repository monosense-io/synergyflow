Status: Draft

# Story
As a developer and partner engineer,
I want an event catalog and schema viewer UI,
so that I can browse event contracts, versions, and examples.

## Acceptance Criteria
1. Catalog lists events with name, version, topic, and status (stable/deprecated); filters and search.
2. Schema viewer renders JSON schema with examples and headers (id, type, version, timestamp, correlationId); copy code snippets.
3. Version history and diff between versions; deprecation timelines shown; download schema.
4. Permissions enforced for internal‑only events; public ones accessible via portal.
5. Accessibility and performance standards met; a11y AA.

## Tasks / Subtasks
- [ ] Catalog list, filters, and search (AC: 1)
- [ ] Schema viewer and examples/snippets (AC: 2)
- [ ] Version history/diff and downloads (AC: 3)
- [ ] Permissions and public portal mode (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-15/04-event-publishing-standards-sdks.md
- PRD: 5 integration — events catalog
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0004 event versioning
- APIs: system.yaml (event contracts)

## Testing
- Catalog accuracy; history/diff; permissions; a11y/perf

## Change Log
| Date       | Version | Description                                        | Author |
|------------|---------|----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — event catalog/schema viewer UI   | PO     |

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

