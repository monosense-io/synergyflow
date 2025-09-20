Status: Draft

# Story
As a routing administrator,
I want a routing policy editor UI for weights and constraints,
so that I can tune assignment behavior and explain decisions.

## Acceptance Criteria
1. UI to configure scoring weights for Skill/Category/SLA/Criticality/Availability; show validation and effect preview.
2. Define hard constraints (must‑have skills/certs) and soft preferences; version history and rollback.
3. Test panel: paste sample ticket context and preview top‑N candidates with scores and rationale.
4. Publish policies with RBAC and audit; changes propagate to backend.
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] Weights editor with validation and save/rollback (AC: 1, 2)
- [ ] Constraint builder UI (AC: 2)
- [ ] Test panel consuming backend preview API (AC: 3)
- [ ] Publish flow with RBAC and audit (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-06/02-scoring-engine-policy-evaluator.md
- PRD: routing criteria and algorithm
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway; 0004 versioning
- APIs: teams.yaml/system.yaml for policy endpoints

## Testing
- Editor validation; preview correctness vs backend; RBAC/audit

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — routing policy editor UI    | PO     |

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

