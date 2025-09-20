Status: Draft

# Story
As an ITAM engineer,
I want import and discovery pipelines with normalization and merge rules,
so that asset inventory is accurate and deduplicated across data sources.

## Acceptance Criteria
1. Support CSV import and discovery adapters (extensible) that produce a normalized Asset DTO with source metadata and confidence.
2. Normalization rules standardize vendor/model/serial, hardware/software attributes, and locations.
3. Merge and dedupe rules consolidate records by key (serial, MAC, hostname, license key) with conflict resolution and audit of merges.
4. asset.discovered and asset.updated events emitted with source and confidence; idempotent ingestion (no duplicates).
5. Error handling with retry/backoff; failures logged with trace IDs; observability metrics exposed (ingestion rates, errors).

## Tasks / Subtasks
- [ ] Define normalized Asset DTO and mapping for common sources (AC: 1, 2)
- [ ] Implement import (CSV) and discovery adapter interface + one reference adapter (AC: 1)
- [ ] Merge/dedupe service with conflict policies and audit (AC: 3)
- [ ] Event emission and idempotency keys (AC: 4)
- [ ] Observability: metrics/logs/traces; retry/backoff strategy (AC: 5)

## Dev Notes
- PRD: docs/prd/02-itil-v4-core-features-specification.md (2.8 ITAM — discovery/import; normalization)
- Architecture: docs/architecture/data-architecture.md; docs/architecture/data-access-mybatis-plus.md
- ADRs: 0009 DB migration schema evolution; 0001 event bus; 0006 partitioning (if high-throughput);
- APIs/Events: docs/api/modules/cmdb.yaml (if shared), events.yaml (asset.*)

## Testing
- Import/discovery happy-path and error scenarios; idempotency tests
- Normalization/merge correctness with conflict fixtures
- Event contract tests for asset.discovered/asset.updated; metrics assertions

## Change Log
| Date       | Version | Description                                        | Author |
|------------|---------|----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 07 seed #1                 | PO     |

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

