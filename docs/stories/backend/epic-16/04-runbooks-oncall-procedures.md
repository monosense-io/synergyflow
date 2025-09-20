Status: Draft

# Story
As an on-call lead,
I want runbooks and on-call procedures standardized and integrated,
so that incident response is fast, consistent, and auditable.

## Acceptance Criteria
1. Runbook model with versioning, owners, steps, and links to tools; published runbooks immutable; draft→publish workflow with audit.
2. On-call schedules and alert routes defined per team/service; handoff rules and escalation paths documented and enforced.
3. Incident opened flows attach relevant runbooks automatically based on service; responders receive notifications with deep links.
4. Post-incident action items captured and linked to ReliabilityReviews (Epic 13); completion tracked.
5. APIs for runbooks, schedules, and routes; permissions enforced; export runbooks as PDF/Markdown.

## Tasks / Subtasks
- [ ] Runbook schema, versioning, and publish workflow (AC: 1)
- [ ] On-call schedules + alert routes and enforcement (AC: 2)
- [ ] Incident linkage and notifications (AC: 3)
- [ ] Post-incident actions and review linkage (AC: 4)
- [ ] APIs + permissions + export (AC: 5)

## Dev Notes
- PRD: docs/prd/20-deployment-operations.md; link to Epic 11 for notifications
- Architecture: observability-reliability.md (alerts); gateway-envoy.md; system.yaml (runbooks, schedules)
- Events: incident.opened

## Testing
- Publish workflow; schedule enforcement; incident linkage; export correctness

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — runbooks & on-call (16.04)     | PO     |

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

