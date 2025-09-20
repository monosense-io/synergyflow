Status: Draft

# Story
As a platform owner,
I want a degradation controls admin UI,
so that I can view and toggle graceful degradation modes during incidents.

## Acceptance Criteria
1. Panel shows current health signals and degradation states per service; indicators for read‑only/limited/cached modes.
2. Authorized users can toggle degradation modes with confirmation and rationale; changes audited; rollback easy.
3. Safety checks prevent conflicting states; banner text for UIs is editable and localized.
4. Real‑time updates via events; performance responsive; a11y AA; responsive layout.
5. Permissions enforced; actions logged to audit pipeline.

## Tasks / Subtasks
- [ ] Health/degradation status panel (AC: 1)
- [ ] Toggle actions with confirmation/audit (AC: 2)
- [ ] Safety checks and banner management (AC: 3)
- [ ] Real‑time updates and a11y/perf (AC: 4)
- [ ] Permissions/audit integration (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-13/05-ha-posture-degradation-health-signals.md
- PRD: HA posture & degradation
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs/Events: system.yaml (degradation), events (health.signal, degradation.*)

## Testing
- Toggle flows; safety checks; banner updates; real‑time events; permissions/audit; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — degradation controls admin UI | PO     |

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

