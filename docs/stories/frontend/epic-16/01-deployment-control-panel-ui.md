Status: Draft

# Story
As a release manager,
I want a deployment control panel UI with environment promotion and gate visibility,
so that I can coordinate deployments safely.

## Acceptance Criteria
1. Control panel lists deployments by env with status, artifact, approvers, and gate results; filters and search.
2. Promotion action gated by quality/error budget status; dialog shows rationale, evidence links, and required approvals.
3. Rollback action available with confirmation and impact summary; emits rollback request to backend.
4. Real‑time updates via events (deploy.started/completed); a11y AA; responsive; performance: refresh under 1s.
5. Audit panel shows promotion/rollback actions; permissions enforced.

## Tasks / Subtasks
- [ ] Deployments list and status with filters (AC: 1)
- [ ] Promotion dialog with gate/evidence/approvals (AC: 2)
- [ ] Rollback action and impact summary (AC: 3)
- [ ] Real‑time updates; a11y/perf; audit (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-16/01-cicd-workflows-environment-promotion.md
- PRD: 20 deployment & operations
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md; observability-reliability.md
- Events/APIs: system.yaml (deploy), events (deploy.*, rollback.executed)

## Testing
- Gate visibility; promotion/rollback flows; real‑time updates; a11y/perf; permissions/audit

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — deployment control panel    | PO     |

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

