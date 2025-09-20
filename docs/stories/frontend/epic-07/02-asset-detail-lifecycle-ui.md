Status: Draft

# Story
As an IT asset manager,
I want an asset detail and lifecycle UI,
so that I can view history and perform governed changes.

## Acceptance Criteria
1. Detail view shows core fields, type-specific attributes, ownership, location, and current status.
2. Timeline shows lifecycle events (assigned, transferred, retired) with who/when/rationale; attachments shown where present.
3. Actions: assign/transfer/retire with required rationale and validations; unauthorized users cannot perform actions.
4. Related tickets section lists linked incidents/requests/changes; link through to ticket detail.
5. Accessibility and performance: detail render p95 < 600ms; a11y AA.

## Tasks / Subtasks
- [ ] Detail and timeline views (AC: 1, 2)
- [ ] Action modals with validations and rationale (AC: 3)
- [ ] Related tickets list (AC: 4)
- [ ] a11y and performance checks (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-07/02-asset-model-ownership-lifecycle-tracking.md
- PRD: 2.8 lifecycle and ticket linking
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- APIs: cmdb.yaml/itam module; incidents.yaml; service-requests.yaml; changes.yaml

## Testing
- Timeline correctness; action validations and RBAC; related tickets rendering; performance and a11y

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — asset detail & lifecycle (07) | PO     |

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

