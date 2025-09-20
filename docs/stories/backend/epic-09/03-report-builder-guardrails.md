Status: Draft

# Story
As a reporting analyst,
I want a report builder engine with query guardrails,
so that users can create useful reports without harming performance or leaking data.

## Acceptance Criteria
1. ReportDefinition supports selecting datasets, fields, filters, grouping, aggregations, and chart types; validated against an allowlist per dataset.
2. Guardrails enforce limits: max rows, time ranges, disallow Cartesian products, require indexed filters; pre‑aggregation available for heavy queries.
3. Row/field‑level security enforced via central policy; query rewritten to include predicates.
4. Execution API returns data with metadata (query plan, execution time); supports pagination and export (CSV/XLSX/PDF with watermark for sensitive reports).
5. Run history recorded with parameters and status; errors logged with safe messages.

## Tasks / Subtasks
- [ ] Dataset catalog and allowlists (AC: 1)
- [ ] Query builder and validator (AC: 1, 2)
- [ ] Policy enforcement and predicate injection (AC: 3)
- [ ] Execution API with pagination/export and metadata (AC: 4)
- [ ] Run history storage and error handling (AC: 5)

## Dev Notes
- PRD: 2.11 Reports (builder, scheduling, export)
- Architecture: data-architecture.md; security-architecture.md (policy); observability-reliability.md (metrics)
- ADRs: 0011 cache (optional pre‑aggregation); 0007 gateway; 0001 events (report run start/finish optional)
- APIs: system.yaml (reports)

## Testing
- Guardrail validations; policy predicate injection; export correctness
- Performance on typical datasets; run history logging; error messages sanitized

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Report builder guardrails   | PO     |

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

