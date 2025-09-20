Status: Draft

# Story
As an on-call coordinator,
I want a runbook library and on-call rota UI,
so that responders can quickly find procedures and see who is on duty.

## Acceptance Criteria
1. Runbook library searchable by service/tag; versioned; publish/badge indicators; export as PDF/Markdown.
2. On-call rota shows schedules per team with current/next on-call, handoff timers, and escalation routes.
3. Incident view links relevant runbooks automatically; deep links into runbook execution console (Epic 13.03 UI).
4. a11y AA; responsive; performance responsive; permissions for admin vs viewer actions.
5. Audit of publish/handoff actions; notifications on rota changes.

## Tasks / Subtasks
- [ ] Library search/list/detail and export (AC: 1)
- [ ] Rota visualization and handoff timers (AC: 2)
- [ ] Incident links and deep links (AC: 3)
- [ ] a11y/perf; permissions; audit/notifications (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-16/04-runbooks-oncall-procedures.md
- PRD: 20 operations — runbooks & on-call
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (runbooks, schedules)

## Testing
- Search/export; rota correctness; incident links; a11y/perf; audit/notifications

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — runbook library & rota UI (16)   | PO     |

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

