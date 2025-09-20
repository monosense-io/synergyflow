Status: Draft

# Story
As a service designer,
I want a dynamic form schema engine with conditional logic,
so that request forms adapt to context and validate inputs reliably.

## Acceptance Criteria
1. Define JSON Schema per service/offering with conditional fields (if/then/else), enums, formats, and custom validators.
2. Retrieve schema via API for a given service/offering/version; include UI hints (labels, placeholders) as extensions.
3. Validate submissions server-side against the schema; return field-level errors in a structured format.
4. Version schemas and maintain backward compatibility window; provide diff endpoint between versions.
5. Enforce attachment rules (types/sizes) declared in schema; store attachments securely.

## Tasks / Subtasks
- [ ] Schema storage and versioning model (AC: 1, 4)
- [ ] Retrieval API including UI hints (AC: 2)
- [ ] Validation middleware and error mapper (AC: 3)
- [ ] Diff endpoint and compatibility checks (AC: 4)
- [ ] Attachment policy enforcement and storage (AC: 5)

## Dev Notes
- PRD: 2.6 Service Catalog (dynamic forms)
- Architecture: data-architecture.md for schema storage; api-architecture.md for endpoints
- ADRs: 0009 DB migration; 0007 gateway; 0001 events (optional form.schema.updated)
- APIs: service-requests.yaml

## Testing
- Conditional logic scenarios; custom validators; error message mapping
- Versioning/diff tests; attachment policy enforcement

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 04 seed #2           | PO     |

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

