Status: Draft

# Story
As a release engineer,
I want a rollout management UI for blue/green and canary,
so that I can start, pause, monitor, and abort rollouts with confidence.

## Acceptance Criteria
1. Start blue/green cutover or canary rollout with policy selection; show thresholds (latency/errors/SLOs) and windows.
2. Live KPI panel shows rollout metrics and pass/fail evaluations; actions to pause/abort/promote enabled by policy and RBAC.
3. Timeline shows events (deploy.started, canary step, promote, rollback) and links to logs/dashboards.
4. a11y AA; responsive; performance: KPI refresh < 1s; real‑time events wired.
5. Audit of actions and outcomes; export rollout report (PDF/CSV).

## Tasks / Subtasks
- [ ] Start rollout flow with policy selection (AC: 1)
- [ ] KPI panel and actions (pause/abort/promote) (AC: 2)
- [ ] Timeline and links (AC: 3)
- [ ] Real‑time updates; export; a11y/perf; audit (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-16/03-rollout-strategy-toolkit-bluegreen-canary.md
- PRD: 20 rollout strategies
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- Events/APIs: system.yaml (rollout), events (deploy.*, rollback.executed)

## Testing
- Start/pause/abort/promote flows; KPI correctness; timeline; export; a11y/perf

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — rollout management UI       | PO     |

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

