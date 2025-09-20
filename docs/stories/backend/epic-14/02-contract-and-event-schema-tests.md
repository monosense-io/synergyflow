Status: Draft

# Story
As an integration engineer,
I want contract and event schema tests,
so that API and event changes are validated against versioned contracts.

## Acceptance Criteria
1. Generate and lock OpenAPI contract tests for all public endpoints; breaking changes fail CI with clear diffs.
2. Event schema validation for all published/consumed events; versioned contracts per ADR‑0004; include backward‑compat windows.
3. event.contract.tested emitted with results (pass/fail, impacted services); report artifacts stored.
4. Add negative tests for removed/renamed fields; ensure optional→required changes are flagged.
5. Provide local developer commands to run contract/event tests quickly with snapshots.

## Tasks / Subtasks
- [ ] OpenAPI contract test harness + snapshots (AC: 1, 5)
- [ ] Event schema registry + validators + version policy (AC: 2)
- [ ] Emit event.contract.tested + store artifacts (AC: 3)
- [ ] Negative change test scenarios (AC: 4)

## Dev Notes
- PRD: testing strategy — verification gates
- ADRs: 0004 event contract versioning; 0009 DB migration policy (for contract artifacts storage)
- Architecture: testing-architecture.md
- APIs/Events: openapi.yaml; events catalog

## Testing
- Simulate breaking and non‑breaking changes; verify CI outcomes; event emitted; artifacts available

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — contract & event tests      | PO     |

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

