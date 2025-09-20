Status: Draft

# Story
As a change manager and incident commander,
I want an impact analysis panel embedded in Change and Incident UIs,
so that I can see affected CIs/services and risk at a glance.

## Acceptance Criteria
1. Panel shows impact summary (blast radius, criticality, impacted services) and a mini-graph for affected CIs with expand-on-click.
2. For changes, panel appears pre-approval and on updates; shows pass/fail of gate thresholds based on impact size and policy.
3. For incidents, panel updates with current CI context and suggests escalation targets based on dependencies.
4. Real-time refresh when CMDB changes affect the impact set; cache with invalidation; UI shows last updated time.
5. Accessibility and performance: render p95 < 600ms; responsive.

## Tasks / Subtasks
- [ ] Impact panel component with summary and mini-graph (AC: 1)
- [ ] Change UI integration with gate status (AC: 2)
- [ ] Incident UI integration with escalation suggestions (AC: 3)
- [ ] Real-time refresh hooks and cache indicators (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-08/04-impact-assessment-service-integration.md
- PRD: 2.9 impact analysis; 2.3 change approvals; 2.1 incidents
- ADRs: 0005 CMDB impact handshake; 0007 gateway; 0001 events
- APIs: cmdb.yaml (impact), changes.yaml, incidents.yaml

## Testing
- Correctness of summary; gate status display; escalation suggestions
- Real-time updates; performance and a11y checks

## Change Log
| Date       | Version | Description                                           | Author |
|------------|---------|-------------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — impact analysis panel UI (08)         | PO     |

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

