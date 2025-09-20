Status: Draft

# Story
As a release manager,
I want a release management UI to assemble releases, view gate readiness, and promote safely,
so that I can coordinate end-to-end releases with confidence.

## Acceptance Criteria
1. UI to create a release and add related changes/problems/incidents; shows scope, owner, schedule.
2. Gates view shows required checks and current status (pass/fail/pending) with evidence links.
3. Promotion action disabled until gates pass; promotion dialog summarizes impacts and notifications.
4. Real-time updates via events; status timeline with timestamps.
5. Accessibility and performance: p95 API→UI latency < 800ms for gate refresh; a11y AA.

## Tasks / Subtasks
- [ ] Release creation/edit forms; linkage pickers (AC: 1)
- [ ] Gates dashboard with evidence links and status icons (AC: 2)
- [ ] Promotion flow with confirmation and summary (AC: 3)
- [ ] Event-driven updates (SSE/WebSocket) for statuses (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend dependencies: docs/stories/backend/epic-02/04-release-assembly-gates-promotion.md
- PRD 2.4 Release Management
- ADRs: 0001 events; 0007 gateway; 0004 versioning
- Architecture: testing-architecture.md for e2e tests; gateway-envoy.md for SSE/WebSocket policy
- APIs: changes.yaml (release endpoints), system.yaml (gates)

## Testing
- UI flows for release assembly, gate status refresh, promotion
- Real-time updates via mocked events
- a11y and performance validations

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — release UI for Epic 02      | PO     |

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

