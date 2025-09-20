Status: Draft

# Story
As a developer and release manager,
I want a Quality Gates dashboard UI,
so that I can see gate statuses, failures, and rationale for each build.

## Acceptance Criteria
1. Dashboard lists builds with gate summary (pass/fail) and breakdown (coverage, contract, security, performance, flakiness) with thresholds.
2. Detail view shows failing gates with rationale and links to artifacts (reports, diffs) and regression.suite.passed events.
3. Filters by service/branch/environment/timeframe; export CSV/PDF; permissions enforced.
4. Real‑time updates for running builds; performance responsive; a11y AA.
5. Manual override flow (if allowed) prompts for rationale and approvals; audit displayed.

## Tasks / Subtasks
- [ ] Builds list with gate badges and filters (AC: 1, 3)
- [ ] Gate detail view with artifacts and events (AC: 2)
- [ ] Real‑time updates; export; a11y/perf (AC: 4)
- [ ] Override flow with audit (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-14/03-quality-gate-evaluation-service.md
- PRD: testing strategy — verification gates
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs/Events: system.yaml (gates), events (regression.suite.passed)

## Testing
- Filters; detail artifacts; real‑time updates; override flow; a11y/perf

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Quality Gates dashboard UI   | PO     |

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

