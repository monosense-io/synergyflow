Status: Draft

# Story
As a support operations lead,
I want an escalation policy editor UI,
so that I can define tiers, triggers, and notifications with guardrails.

## Acceptance Criteria
1. Configure tiers (L1/L2/L3), triggers (SLA threshold, severity change, customer escalation), and actions (reassign, notify).
2. Visual timeline shows when triggers fire relative to SLA; simulator previews outcome for a sample ticket.
3. RBAC‑restricted publish/rollback with audit; policy versions visible.
4. Accessibility and performance standards met.
5. Policies include hysteresis/backoff options to avoid oscillations.

## Tasks / Subtasks
- [ ] Tiers/triggers/actions editor (AC: 1)
- [ ] Timeline and simulator (AC: 2)
- [ ] Publish/rollback with RBAC and audit (AC: 3)
- [ ] a11y/perf checks (AC: 4)

## Dev Notes
- Backend: docs/stories/backend/epic-06/03-escalation-tiers-triggers.md
- PRD: escalation rules
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: teams.yaml/system.yaml (policy endpoints)

## Testing
- Editor flows; simulator accuracy; publish/rollback behavior; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — escalation policy editor UI   | PO     |

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

