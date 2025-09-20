Status: Draft

# Story
As a release manager,
I want to assemble releases with stage gates and controlled promotion,
so that only ready changes progress through environments with clear evidence.

## Acceptance Criteria
1. Create a release that aggregates related changes/problems/incidents with metadata (scope, owner, schedule).
2. Define stage gates (e.g., tests passed, approvals complete, risk checks) and evaluate them prior to promotion.
3. Promotion is blocked until all required gates pass; gate results are recorded and auditable.
4. Emit events: release.created, release.gate.passed/failed, release.promoted.
5. Notifications sent to stakeholders on gate outcomes and promotions.

## Tasks / Subtasks
- [ ] Release model and linkage to changes/problems/incidents (AC: 1)
- [ ] Gate definition language and evaluator (AC: 2)
- [ ] Promotion service enforcing gate pass (AC: 3)
- [ ] Event emissions and notifications (AC: 4, 5)
- [ ] Readiness evidence ingestion (e.g., test reports) (AC: 2)

## Dev Notes
- PRD 2.4 Release Management (stage gates, linkage, notifications)
- Dependencies: Testing & Quality Gates (docs/epics/epic-14-testing-quality/README.md)
- ADRs: 0001 events, 0004 versioning
- APIs: changes.yaml (linkage), system.yaml (gates), problems.yaml, incidents.yaml
- Data: Release, ReleaseChangeLink, ReleaseGate, GateResult

## Testing
- Gate evaluation correctness with mocked evidence
- Promotion block on failing gates
- Event contracts for release lifecycle
- Notification delivery tests

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 02 seed #4          | PO     |

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

