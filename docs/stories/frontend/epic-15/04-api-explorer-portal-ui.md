Status: Draft

# Story
As a developer (internal/partner),
I want an API explorer portal UI,
so that I can browse OpenAPI docs, try endpoints, and manage tokens.

## Acceptance Criteria
1. API catalog lists services and endpoints from published OpenAPI bundles with search and tags; Swagger UI embedded for try‑it (sandbox).
2. Token management panel for API keys (copy‑once) and OAuth tokens (where applicable); scopes shown; permissions enforced.
3. Version selector and changelog display; deprecation badges; link to governance rules.
4. Rate limit indicators and usage for current client; warnings on nearing limits.
5. Accessibility and performance: load p95 < 1s; a11y AA.

## Tasks / Subtasks
- [ ] Catalog and Swagger UI integration (AC: 1)
- [ ] Token panel with copy‑once and scopes (AC: 2)
- [ ] Version selector and changelog (AC: 3)
- [ ] Rate limit indicators (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: OpenAPI governance pipeline and API client/keys stories
- PRD: 5 integration — API portal
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: openapi.yaml bundles; system.yaml (clients/keys)

## Testing
- Try‑it flows (sandbox); token handling; version/changelog; rate limits; a11y/perf

## Change Log
| Date       | Version | Description                              | Author |
|------------|---------|------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — API explorer portal UI   | PO     |

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

