Status: Draft

# Story
As a catalog administrator,
I want Service and Offering CRUD with lifecycle management,
so that services can be published, updated, or deprecated with proper governance.

## Acceptance Criteria
1. Create/read/update/deactivate Service and Offering with fields: name, owner, category, SLA, visibility, version, status (Draft/Published/Deprecated), and formSchemaRef.
2. Lifecycle rules enforce valid transitions (Draft→Published→Deprecated) with validation and audit logging.
3. Deactivated/Deprecated services are hidden from non-admin listings but remain queryable by ID for admins.
4. List APIs support filtering (category, owner, status) and pagination; sorting by name/updated_at.
5. RBAC ensures only admins can create/publish/deprecate; audit trail captures who/when/what changed.

## Tasks / Subtasks
- [ ] Data model for Service and Offering with versioning (AC: 1)
- [ ] CRUD endpoints with validation (AC: 1)
- [ ] Lifecycle transition policies and audit hooks (AC: 2, 5)
- [ ] List/search with filters, pagination, sorting (AC: 4)
- [ ] Visibility enforcement for deactivated/deprecated (AC: 3)
- [ ] RBAC checks and tests (AC: 5)

## Dev Notes
- PRD: docs/prd/02-itil-v4-core-features-specification.md (2.6 Service Catalog)
- Architecture: docs/architecture/api-architecture.md, docs/architecture/data-architecture.md
- ADRs: 0009 DB migration schema evolution; 0001 event bus (optional events service.created/updated); 0004 event contract versioning; 0007 gateway
- APIs: docs/api/modules/service-requests.yaml (extend with service/admin endpoints); users.yaml for owner references

## Testing
- CRUD and lifecycle transition tests; visibility rules
- Filter/pagination/sort tests; RBAC; audit immutability

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 04 seed #1           | PO     |

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

