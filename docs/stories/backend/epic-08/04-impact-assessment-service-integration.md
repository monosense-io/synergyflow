Status: Draft

# Story
As a change manager and incident commander,
I want automated impact assessment integrated into change approvals and incident handling,
so that risk is visible and decisions are informed.

## Acceptance Criteria
1. Impact service evaluates changes by traversing affected CIs/relationships and computes risk metrics (blast radius, criticality, dependencies).
2. For change submissions, provide pre-approval impact summary to the Change module; block or require additional approvals based on policy.
3. For incidents, provide impact analysis (upstream/downstream affected) to inform triage and escalation.
4. Emit change.impact.assessed events; cache assessments with invalidation on CMDB changes.
5. APIs expose impact reports (JSON + human-readable summary) and support links to snapshots for before/after comparisons.

## Tasks / Subtasks
- [ ] Impact traversal logic and metrics computation (AC: 1)
- [ ] Change module integration per ADR 0005 handshake (AC: 2)
- [ ] Incident integration and report endpoints (AC: 3, 5)
- [ ] Event emission and caching strategy (AC: 4)
- [ ] Policy hooks for gate decisions (AC: 2)

## Dev Notes
- PRD: 2.9 impact analysis; 2.3 change approvals references
- ADRs: 0005 CMDB impact assessment handshake; 0001 event bus; 0004 versioning; 0011 cache (optional)
- Architecture: data-architecture.md; module-architecture.md
- APIs: cmdb.yaml (impact), changes.yaml (integration), incidents.yaml

## Testing
- Impact metrics correctness on sample graphs; policy gate behavior
- Integration tests with Change and Incident modules; event contracts
- Cache invalidation on CMDB updates

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — impact assessment integration | PO     |

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

