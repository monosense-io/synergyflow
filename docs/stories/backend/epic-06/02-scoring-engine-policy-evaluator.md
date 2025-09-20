Status: Draft

# Story
As a routing service owner,
I want a scoring engine and policy evaluator,
so that tickets are assigned optimally by skills, category, SLA, and availability.

## Acceptance Criteria
1. Implement priority score formula combining Skill Match, Category Match, SLA Performance, Customer Criticality, and Availability with configurable weights.
2. Policy evaluator supports hard constraints (must match skill/cert) and soft weights; returns top-N candidates with scores and rationale.
3. SLA-aware: deprioritize agents/teams at capacity or outside business hours; prefer those with better historical SLA performance.
4. API endpoint accepts routing request payload and returns assignment decision; latency p95 ≤ 2s under nominal load.
5. Emit ticket.routed event on successful assignment including rationale; audit logged; fallbacks documented (safe queue).

## Tasks / Subtasks
- [ ] Define routing request/response schema; validation (AC: 4)
- [ ] Implement scoring functions and weight configuration (AC: 1)
- [ ] Policy evaluator with hard/soft constraints and top‑N (AC: 2)
- [ ] SLA/availability integration hooks (AC: 3)
- [ ] Routing API + ticket.routed event + audit (AC: 4, 5)

## Dev Notes
- PRD: docs/prd/04-multi-team-support-intelligent-routing.md (routing criteria and algorithm)
- ADRs: 0006 event partitioning; 0001 event bus; 0004 event contract versioning
- Architecture: module-architecture.md; observability-reliability.md (latency SLO)
- APIs: teams.yaml, users.yaml, incidents.yaml (consuming module), events.yaml

## Testing
- Unit tests for scoring and constraints; configuration overrides
- Latency tests; top‑N correctness under load
- Event and audit contract tests

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — scoring/policy evaluator     | PO     |

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

