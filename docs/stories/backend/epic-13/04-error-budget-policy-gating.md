Status: Draft

# Story
As a product owner and SRE,
I want error budget policies and gating integrated with delivery pipelines,
so that promotions are blocked when reliability is below target.

## Acceptance Criteria
1. Define ErrorBudget policies per service/env with gate rules (e.g., if burn rate > X over Y, block promotions except emergency fixes).
2. Pipeline integration (Epic 16) queries SLO/error budget status; returns pass/fail with rationale and links to dashboards.
3. Manual override requires justification, risk acceptance, and approval workflow; all actions audited.
4. Events emitted for gate decisions (gate.passed/failed) with reliability context; Notifications sent to stakeholders.
5. Reporting API lists recent gate decisions and overrides; supports export and permission controls.

## Tasks / Subtasks
- [ ] ErrorBudget policy model and evaluator (AC: 1)
- [ ] Pipeline gate endpoint and rationale payload (AC: 2)
- [ ] Override workflow + audit + notifications (AC: 3, 4)
- [ ] Reporting API and exports (AC: 5)

## Dev Notes
- PRD: reliability requirements — error budgets & gates
- Dependencies: Epic 16 deployment pipeline; Epic 09 dashboards; Epic 11 notifications
- Architecture: observability-reliability.md
- ADRs: 0001 event bus; 0011 cache (optional for status)

## Testing
- Gate evaluator scenarios; pipeline integration mocks; override approvals
- Reporting correctness; events and notifications

## Change Log
| Date       | Version | Description                               | Author |
|------------|---------|-------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — error budget gates        | PO     |

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

