Status: Draft

# Story
As a platform owner,
I want an operations timeline/status UI,
so that stakeholders can see deployment, rollout, migration, and incident events in one place.

## Acceptance Criteria
1. Timeline aggregates events (deploy.started/completed, rollback.executed, migration.applied, incident.opened) with filtering by service/env/timeframe.
2. Status page view summarizes current state by environment (healthy/degraded/incident) with banners and links.
3. Deep links to relevant dashboards, logs, and runbooks; export timeline as CSV/PDF for postmortems.
4. Real‑time updates via events; a11y AA; responsive; performance responsive under event load.
5. Permissions: internal vs public status view; audit of admin changes to banners.

## Tasks / Subtasks
- [ ] Timeline aggregation and filters (AC: 1)
- [ ] Status page view with banners (AC: 2)
- [ ] Deep links and export (AC: 3)
- [ ] Real‑time updates; a11y/perf (AC: 4)
- [ ] Permissions and banner audit (AC: 5)

## Dev Notes
- Backend: events from Epic 16/13; degradation banners API
- PRD: 20 operations — status and communications
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs/Events: system.yaml (status), events (deploy.*, migration.*, incident.opened, degradation.*)

## Testing
- Filter correctness; export; real‑time; a11y/perf; permissions and banner audit

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — operations timeline/status UI | PO     |

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

