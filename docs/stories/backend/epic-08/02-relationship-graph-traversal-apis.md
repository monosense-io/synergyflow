Status: Draft

# Story
As a reliability engineer,
I want relationship graph storage and traversal APIs,
so that upstream/downstream impacts can be analyzed efficiently.

## Acceptance Criteria
1. Relationship model supports typed edges (depends_on, hosts, connects_to) with direction and attributes (confidence, latency, capacity).
2. Store relationships in a graph-friendly structure with indexes; traversal APIs expose upstream/downstream queries with depth and filters.
3. Update APIs maintain referential integrity; prevent cycles for certain edge types where disallowed; emit ci.relationship.updated events.
4. Performance: traversal p95 < 300ms for depth≤3 on mid-size graphs; caching available for hot queries.
5. Access control: only authorized roles can mutate relationships; read visibility follows CI permissions.

## Tasks / Subtasks
- [ ] Relationship schema and indexes (AC: 1, 2)
- [ ] Traversal APIs with filters/depth controls (AC: 2)
- [ ] Update endpoints with integrity checks and event emission (AC: 3)
- [ ] Performance tuning and caching (AC: 4)
- [ ] RBAC and permission filters (AC: 5)

## Dev Notes
- PRD: 2.9 CMDB — relationship graph and impact traversal
- ADRs: 0001 event bus; 0004 event versioning; 0011 cache store (optional)
- Architecture: data-architecture.md; module-architecture.md
- APIs: cmdb.yaml

## Testing
- Traversal correctness; integrity checks; performance under load
- Event contracts for ci.relationship.updated; permission filtering

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — relationship graph & traversal | PO     |

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

