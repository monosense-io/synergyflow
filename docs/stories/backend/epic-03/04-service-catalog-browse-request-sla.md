Status: Draft

# Story
As an end user,
I want to browse the service catalog and submit requests with clear SLAs,
so that I can understand expectations and initiate the right fulfillment flows.

## Acceptance Criteria
1. Catalog listing API returns services/offerings with owner, category, SLA summary, and availability status; supports filtering and pagination.
2. Detail API returns per-service form schema and approval workflow summary (read-only) to inform users.
3. Request submission endpoint creates a service request linked to the selected service; emits request.created; SLA attached.
4. Endpoints respect role visibility; deactivated services hidden from non-admins.
5. p95 for catalog listing < 500ms with pagination.

## Tasks / Subtasks
- [ ] Catalog listing endpoint with filters/pagination (AC: 1)
- [ ] Service detail endpoint with form schema and SLA (AC: 2)
- [ ] Request creation linking to service (AC: 3)
- [ ] Visibility rules and RBAC (AC: 4)
- [ ] Performance tuning and indexes (AC: 5)

## Dev Notes
- PRD: 2.5 Portal (catalog browsing); 2.6 Service Catalog (for schema/SLA sources)
- Dependencies: Epic 04 Service Catalog
- ADRs: 0001 event bus; 0004 versioning; 0011 cache for list performance
- Architecture: api-architecture.md; data-architecture.md
- APIs: service-requests.yaml

## Testing
- Listing filters/pagination correctness; role-based visibility
- Detail endpoint accuracy (schema/SLA)
- Request creation happy-path and failures; event contracts
- Performance for listing

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 03 seed #4           | PO     |

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

