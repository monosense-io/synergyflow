Status: Draft

# Story
As a knowledge reviewer,
I want an approval queue UI with review actions and audit visibility,
so that I can efficiently review and publish articles.

## Acceptance Criteria
1. Queue lists pending reviews with filters (tag, owner, age); shows diff from previous version.
2. Reviewers can approve or request changes with rationale; actions reflected immediately; notifications sent.
3. Publish button available when requirements met; disabled otherwise with explanation.
4. Audit panel shows history of review actions for the article/version.
5. Accessibility and performance: list p95 < 800ms; interactions responsive.

## Tasks / Subtasks
- [ ] Queue list and filters; diff preview (AC: 1)
- [ ] Approve/request changes actions; rationale capture (AC: 2)
- [ ] Publish control with state and explanations (AC: 3)
- [ ] Audit panel (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-05/02-knowledge-approval-workflow-role-policies.md
- PRD: 2.7 approvals
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: knowledge.yaml; system.yaml (notifications)

## Testing
- Filter behaviors; action flows; state transitions and publish gating
- a11y and performance tests

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — approval queue UI (Epic 05)      | PO     |

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

