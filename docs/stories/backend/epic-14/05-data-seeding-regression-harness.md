Status: Draft

# Story
As a test engineer,
I want deterministic data seeding and a regression harness,
so that suites run reliably and produce comparable results across environments.

## Acceptance Criteria
1. Provide a seeded dataset per module with deterministic IDs/timestamps and factories; seeds reset DB state between suites.
2. Regression harness orchestrates module sequences, seeds, and environment provisioning (containers) and collects unified reports.
3. Performance/security test hooks included (smoke level) and can be toggled for nightly builds.
4. Local developer command runs a representative subset quickly; full regression runs in CI with artifacts stored.
5. Flaky test quarantine mechanism with auto‑unquarantine after stability; reporting tracks flaky rates.

## Tasks / Subtasks
- [ ] Seed factories and reset utilities (AC: 1)
- [ ] Harness orchestration + containers + unified report (AC: 2)
- [ ] Perf/security smoke hooks (AC: 3)
- [ ] Local/CI commands and artifacts storage (AC: 4)
- [ ] Flaky quarantine management and metrics (AC: 5)

## Dev Notes
- PRD: testing strategy — data seeding and regression
- Architecture: testing-architecture.md; data-architecture.md
- ADRs: 0009 DB migration policy
- APIs/Events: regression.suite.passed

## Testing
- Determinism checks; harness e2e; artifacts presence; flaky mechanism behavior

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — data seeding & regression     | PO     |

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

