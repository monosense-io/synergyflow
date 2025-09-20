Status: Draft

# Story
As a BI administrator,
I want BI connectivity with enforced row/field‑level security,
so that analysts access exactly the data they are authorized to see.

## Acceptance Criteria
1. Connectivity profiles for BI tools (e.g., Power BI/Tableau) with auth method (service principal/OAuth) and connection metadata.
2. Row/field‑level security policies defined centrally and applied to dataset exports or views; policies map to users/roles/teams.
3. Connectivity test endpoint validates credentials and sample queries; returns masked results per policy and a pass/fail report.
4. Audit: log connections (who, when, dataset), queries, and policy evaluations; expose summaries for compliance.
5. Documentation: per‑tool connection instructions generated from profiles; downloadable by authorized users.

## Tasks / Subtasks
- [ ] Connectivity profile model and storage (AC: 1, 5)
- [ ] Policy engine for row/field‑level security (AC: 2)
- [ ] Test endpoint executing sample queries with policy evaluation (AC: 3)
- [ ] Audit logging for connections and queries (AC: 4)
- [ ] Doc generator for connection instructions (AC: 5)

## Dev Notes
- PRD: 2.12 BI connectivity and governance
- Architecture: data-architecture.md; security-architecture.md (policy)
- ADRs: 0007 gateway for secure access; 0001 event bus (optional audit streaming)
- APIs: system.yaml (bi connectivity)

## Testing
- Policy evaluation tests and masking behavior
- Connectivity smoke tests per tool profile
- Audit content and permission checks

## Change Log
| Date       | Version | Description                                        | Author |
|------------|---------|----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — BI connectivity & RLS             | PO     |

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

