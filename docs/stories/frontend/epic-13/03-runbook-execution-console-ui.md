Status: Draft

# Story
As an on-call engineer,
I want a runbook execution console UI,
so that I can follow, check off, and record steps during incidents and drills.

## Acceptance Criteria
1. Runbook viewer with step checklist, owners, and estimated durations; supports links to tools/scripts; offline printable.
2. Execution mode records timestamps per step, notes, and attachments; produces a completion report and stores for audits.
3. Suggest next steps based on signals (health/degradation); show embedded KPIs to judge progress.
4. Permissions restrict editing vs executing; audit all actions; a11y AA; responsive.
5. Performance: UI remains responsive under frequent updates; autosave notes.

## Tasks / Subtasks
- [ ] Viewer and execution modes; checklist and notes (AC: 1, 2)
- [ ] Completion report generation and storage (AC: 2)
- [ ] Signal integration (health/degradation) and KPIs (AC: 3)
- [ ] Permissions/audit; a11y/responsive; autosave (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-13/03-chaos-scenarios-runbooks.md and 05‑ha‑posture‑degradation‑health‑signals.md
- PRD: reliability runbooks
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (runbooks, reports), events (health.signal, degradation.*)

## Testing
- Checklist flows; completion reports; autosave; a11y/perf; permissions/audit

## Change Log
| Date       | Version | Description                               | Author |
|------------|---------|-------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — runbook execution console | PO     |

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

