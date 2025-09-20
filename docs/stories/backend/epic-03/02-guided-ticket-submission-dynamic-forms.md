Status: Draft

# Story
As an end user,
I want guided ticket submission with dynamic forms and attachments,
so that I can submit complete requests/incidents efficiently.

## Acceptance Criteria
1. API returns form schemas (JSON Schema) per service/request type with conditional fields and validation rules.
2. Submitting a request/incident validates the payload against the schema; attachments are accepted and stored securely.
3. Created ticket includes SLA information and initial status; emits request.created (or incident.created) event.
4. Endpoint returns helpful validation errors and field-level messages for UI display.
5. p95 request submission handler latency < 500ms excluding file upload.

## Tasks / Subtasks
- [ ] Form schema service and retrieval endpoints (AC: 1)
- [ ] Submission API with JSON Schema validation and error mapping (AC: 2, 4)
- [ ] Attachment handling with size/type limits and secure storage (AC: 2)
- [ ] SLA calculation on creation; initial status set (AC: 3)
- [ ] Event emission request.created/incident.created (AC: 3)
- [ ] Performance tuning and metrics (AC: 5)

## Dev Notes
- PRD: 2.5 Portal (guided submission, templates)
- Dependencies: Service Catalog (Epic 04) for per-service schemas and SLAs
- Architecture: api-architecture.md; data-architecture.md for attachments; observability-reliability.md for latency
- ADRs: 0001 event bus; 0004 contract versioning; 0007 gateway; 0009 DB migration policy
- APIs: service-requests.yaml, incidents.yaml (if incident creation supported), users.yaml

## Testing
- Schema validation (conditional fields, required, formats) and error mapping
- Attachment upload tests (limits, security)
- Event contract tests and SLA presence in response
- Performance tests for handler latency excluding upload

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 03 seed #2           | PO     |

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

