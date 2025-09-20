Status: Draft

# Story
As an IT asset manager,
I want an Asset data model with ownership, location, and lifecycle tracking,
so that we maintain a complete, auditable inventory history.

## Acceptance Criteria
1. Asset entity includes type-specific attributes (by AssetType), identifiers (serial, MAC, hostname, license key), owner, location, and status.
2. Ownership and location changes recorded as LifecycleEvent entries (assigned, transferred, retired) with user, timestamp, and rationale.
3. CRUD APIs enforce validation by AssetType; updates create audit entries; soft-delete for retired assets.
4. Search/list APIs with filters (type, owner, status, location) and pagination; export CSV.
5. Link Asset to tickets (incidents/requests/changes) by ID; referenced context appears in ticket detail APIs.

## Tasks / Subtasks
- [ ] Asset, AssetType, Ownership, Location, LifecycleEvent schemas and migrations (AC: 1, 2)
- [ ] CRUD + validation; type-specific attribute handling (AC: 1, 3)
- [ ] Audit/LifecycleEvent creation on updates (AC: 2, 3)
- [ ] Search/list/export endpoints (AC: 4)
- [ ] Ticket linkage APIs and projections (AC: 5)

## Dev Notes
- PRD: 2.8 ITAM (ownership, location, lifecycle; ticket linkage)
- Dependencies: ticket modules (incidents/service-requests/changes) for linkage
- Architecture: data-architecture.md; api-architecture.md
- ADRs: 0009 DB migrations; 0001 events (optional asset.updated)
- APIs: cmdb.yaml (or itam module), incidents.yaml, service-requests.yaml, changes.yaml

## Testing
- Type validation; lifecycle events creation; audit immutability
- Search/filter correctness; export format; ticket linkage in projections

## Change Log
| Date       | Version | Description                                        | Author |
|------------|---------|----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 07 seed #2                 | PO     |

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

