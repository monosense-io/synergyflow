Status: Draft

# Story
As a knowledge engineer and agent,
I want a Known Error repository linked to incidents and problems,
so that workarounds can be applied quickly to reduce MTTR.

## Acceptance Criteria
1. Create/search Known Error records; each includes description, workaround steps, affected CIs/services, and validity status.
2. Agents can attach a Known Error to incidents; users see applied workaround in history.
3. Problems can reference Known Errors; linkage is visible and audit logged.
4. Related article suggestions display during ticket update based on category/keywords.
5. Events: known-error.published, known-error.applied.to.ticket

## Tasks / Subtasks
- [ ] Known Error entity and search APIs (AC: 1)
- [ ] Incident linkage and history update (AC: 2)
- [ ] Problem linkage and audit (AC: 3)
- [ ] Suggestion service integration with search (AC: 4)
- [ ] Event emissions for publish/apply (AC: 5)

## Dev Notes
- PRD 2.2 (Known Errors) and 2.7 Knowledge Management for search/suggestion patterns
- ADRs: 0001 events, 0004 versioning, 0011 cache store for suggestion perf
- APIs: problems.yaml, incidents.yaml, knowledge.yaml, search.yaml

## Testing
- Search relevance tests and response times
- Linkage behavior and audit trail tests
- Suggestion results correct for categories/keywords
- Event contracts for publish/apply

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 01 seed #5           | PO     |

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

