Status: Draft

# Story
As a problem manager,
I want to create and manage problem records linked to related incidents,
so that root causes are identified and recurring issues are addressed.

## Acceptance Criteria
1. Create problem records from one or more incidents; maintain bidirectional links with consistency checks.
2. Problem lifecycle enforces statuses and approvals (Draft → Analysis → Resolved → Closed) with audit trail.
3. RCA fields (cause, contributing factors, corrective/preventive actions) are required at problem closure.
4. Associated incidents display problem linkage and can attach workarounds from Known Errors.
5. Events emitted: problem.created, problem.linked, problem.closed with RCA summary.

## Tasks / Subtasks
- [ ] Problem entity and lifecycle with approvals (AC: 2)
- [ ] Linking service and consistency checks (AC: 1)
- [ ] RCA fields enforcement and validation (AC: 3)
- [ ] Incident view integration for linkage and workarounds (AC: 4)
- [ ] Event emissions for problem lifecycle (AC: 5)

## Dev Notes
- PRD 2.2 Problem Management requirements
- ADRs: 0001 event bus, 0004 event contract versioning, 0009 DB migration policy
- APIs: problems.yaml, incidents.yaml, events.yaml
- Data: Problem, ProblemIncidentLink, RCA fields; ensure transactional integrity when linking multiple incidents.

## Testing
- Lifecycle and approval policy tests
- Bidirectional linking tests and referential integrity
- RCA field validation on closure
- Event contract tests

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 01 seed #4           | PO     |

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

