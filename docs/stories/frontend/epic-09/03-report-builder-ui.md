Status: Draft

# Story
As a reporting analyst,
I want a report builder UI with filters, grouping, aggregations, and charts,
so that I can build and run reports within guardrails.

## Acceptance Criteria
1. Dataset/field picker constrained to allowlisted items; UI enforces guardrails (max rows, required filters, time ranges).
2. Builder supports filters, grouping, aggregations, sorting, and chart selection; previews results with pagination.
3. Row/field‑level security enforced; UI shows permission errors gracefully without leaking data.
4. Export options (CSV/XLSX/PDF) available per policy; watermark on sensitive exports.
5. Run history tab shows prior runs with parameters and status; re‑run option.

## Tasks / Subtasks
- [ ] Dataset/field picker and guardrail validations (AC: 1)
- [ ] Query builder and preview with pagination (AC: 2)
- [ ] Permission error handling UX (AC: 3)
- [ ] Export flows and watermarking (AC: 4)
- [ ] Run history tab and re‑run (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-09/03-report-builder-guardrails.md
- PRD: 2.11 Reports
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (reports & history)

## Testing
- Guardrail UX; preview correctness; exports; permissions; run history behaviors

## Change Log
| Date       | Version | Description                             | Author |
|------------|---------|-----------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Report builder UI       | PO     |

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

