Status: Draft

# Story
As an API governance lead,
I want OpenAPI governance tooling and CI checks,
so that APIs are consistent, versioned, and non‑breaking changes are caught early.

## Acceptance Criteria
1. Linting ruleset enforced (naming, descriptions, error shapes, pagination, IDs) for openapi.yaml and module specs; CI fails on violations.
2. Backward‑compat checks compare PR specs to mainline; breaking changes fail with human‑readable diffs and guidance.
3. Versioning policy enforced (semver) with changelog generation from diffs; deprecation workflow documented and flagged.
4. Publish step validates and bundles specs for portal distribution; signed artifact stored.
5. Contract test generation scaffolded for services consuming the specs (ties to Epic 14 contract tests).

## Tasks / Subtasks
- [ ] Lint ruleset and CI job (AC: 1)
- [ ] Backward‑compat checker and diff reporter (AC: 2)
- [ ] Versioning policy enforcement + changelog tool (AC: 3)
- [ ] Publish/bundle pipeline with signature (AC: 4)
- [ ] Contract test generator scaffolding (AC: 5)

## Dev Notes
- PRD: docs/prd/05-integration-architecture.md (API standards)
- Architecture: docs/architecture/api-architecture.md
- ADRs: 0004 event contract versioning (principles apply to API versioning)
- APIs: openapi.yaml; module specs in docs/api/modules/*.yaml

## Testing
- Lint violation samples; breaking/non‑breaking change cases; changelog output; signed artifact verification

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — OpenAPI governance & CI      | PO     |

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

