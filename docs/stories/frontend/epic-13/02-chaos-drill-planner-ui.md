Status: Draft

# Story
As a reliability lead,
I want a chaos drill planner UI,
so that I can schedule, scope, approve, and review drills with evidence.

## Acceptance Criteria
1. Planner creates drills with scenario, scope, window, environment, approvers; validations and conflict checks.
2. Approval workflow UI shows required approvers; status changes with audit; prod drills require additional approvals.
3. Execution view streams evidence (metrics/logs links) and shows pass/fail criteria; upload runbook references.
4. Review page captures outcomes, remediation items, and ReliabilityReview record creation.
5. Accessibility and performance standards met; a11y AA.

## Tasks / Subtasks
- [ ] Planner form and conflict checks (AC: 1)
- [ ] Approvals UI with audit (AC: 2)
- [ ] Execution evidence view and runbook linking (AC: 3)
- [ ] Review outcomes and remediation capture (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-13/03-chaos-scenarios-runbooks.md
- PRD: reliability operations — chaos drills
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (drills/reviews)

## Testing
- Planner validations; approvals; evidence streaming; review flows; a11y/perf

## Change Log
| Date       | Version | Description                             | Author |
|------------|---------|-----------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — chaos drill planner UI  | PO     |

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

