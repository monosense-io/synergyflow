Status: Draft

# Story
As an integration engineer,
I want a contract test report viewer UI,
so that I can inspect API/event contract diffs and resolve issues quickly.

## Acceptance Criteria
1. Report list shows recent contract test runs with status, impacted services, and links to artifacts.
2. Diff viewer highlights breaking vs non‑breaking changes for OpenAPI and event schemas; shows version policy notes.
3. Filters by service/module/timeframe; export diff as HTML/PDF; permissions enforced.
4. a11y AA; performance: diff render p95 < 800ms for typical specs.
5. Links to failing PRs/builds and guidance to resolve.

## Tasks / Subtasks
- [ ] Report list and filters (AC: 1, 3)
- [ ] Diff viewer for OpenAPI/event schemas with policy notes (AC: 2)
- [ ] Export and permissions (AC: 3)
- [ ] a11y/perf checks (AC: 4)
- [ ] PR/build links and guidance panel (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-14/02-contract-and-event-schema-tests.md
- PRD: testing strategy — contract tests & gates
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs/Events: system.yaml (contract reports)

## Testing
- Diff accuracy; exports; permissions; a11y/perf; links integrity

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — contract test report viewer UI | PO     |

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

