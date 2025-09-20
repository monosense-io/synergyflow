Status: Approved

# Story
As a test architect,
I want standardized test architecture scaffolding and conventions,
so that all modules share a consistent, reliable testing approach.

## Acceptance Criteria
1. Provide project‑wide testing conventions for unit, integration, contract, and E2E tests (naming, structure, fixtures, data seeding) per testing‑architecture doc.
2. Create base test utilities: deterministic seeds, testcontainers/embedded DB harness, event bus test harness, and API client helpers.
3. Define module test templates for Spring Modulith boundaries with example tests and CI tasks wired.
4. Document Given‑When‑Then authoring guidelines and mapping to TraceLink entities; add lint/check script to CI.
5. Introduce flakiness mitigation patterns: isolation, time control, retries with diagnostics; CI reports include flaky test detection.

Note: CI gates and thresholds must be explicit and enforceable (see Dev Notes → CI Integration) without changing scope.

## Tasks / Subtasks
- [ ] Conventions document + module templates for test types (AC: 1, 3)
- [ ] Base utilities: seeds, containers, event harness, API helpers (AC: 2)
- [ ] Gherkin/GWT guidance and TraceLink mapping (AC: 4)
- [ ] CI integration: scripts, tasks, and reports; flaky detection (AC: 5)

Course-correction additions (implementation clarity):
- [ ] File structure and locations documented and created (AC: 1, 2, 3)
  - `synergyflow-backend/src/test/java/io/monosense/synergyflow/testsupport/**`
  - `synergyflow-backend/src/testFixtures/java/io/monosense/synergyflow/**` (if Gradle test fixtures used)
  - Example modulith tests per module: `**/*ApplicationModuleTest.java` and `**/*ArchitectureTests.java`
- [ ] CI tasks and gates wired (AC: 3, 5)
  - Gradle tasks: `test`, `integrationTest`, `contractTest`, `architectureTest`
  - Coverage gates via JaCoCo: overall ≥ 80%, critical modules ≥ 90% (per Coding Standards)
  - Reports exposed: `build/test-results/*`, `build/reports/tests/*`, `build/reports/jacoco/*`
- [ ] Traceability lint entrypoint added (AC: 4)
  - Script `scripts/trace-lint.sh` validates Given/When/Then → TraceLink mapping
  - TraceLink storage: `docs/qa/tracelinks/*.yml` (created by QA tasks)
- [ ] Flakiness mitigation configured (AC: 5)
  - One retry for failed tests; collect into `build/reports/tests/flaky/*`
  - Build fails if new flaky tests are detected or flake count increases vs baseline
  - Pre-pull required container images in CI; use reusable containers and a dedicated Docker network to reduce startup races
- [ ] Implement P0 scenarios from test design (AC: 1-5)
  - Use `docs/qa/assessments/14.01-test-design-20250918.md` as the authoritative matrix
  - Ensure trace links exist for P0 scenarios and linter passes

## Dev Notes
- PRD: docs/prd/11-testing-strategy-spring-modulith.md
- Architecture: docs/architecture/testing-architecture.md
- ADRs: 0004 event contract versioning; 0009 DB migration policy
- APIs/Events: openapi.yaml for contract tests

### File Structure & Locations (backend)
- Tests follow source tree guidance (docs/architecture/source-tree.md):
  - Test utilities: `synergyflow-backend/src/test/java/io/monosense/synergyflow/testsupport/**`
    - `.../containers/` reusable Testcontainers (PostgreSQL, DragonflyDB, MinIO, Kafka)
    - `.../events/` event bus test harness using Spring Modulith `PublishedEvents`/`AssertablePublishedEvents`
    - `.../api/` REST API client helpers for test usage
    - `.../time/` deterministic clock/time control utilities
  - Optional Gradle test fixtures: `synergyflow-backend/src/testFixtures/java/io/monosense/synergyflow/**`
  - Example per-module suites:
    - Isolation: `**/*ApplicationModuleTest.java`
    - Architecture: `**/*ArchitectureTests.java` (includes `ApplicationModules.verify()`)

### CI Integration & Gates
- Gradle tasks expected and wired in CI: `test`, `integrationTest`, `contractTest`, `architectureTest`.
- Coverage thresholds (from Coding Standards): overall ≥ 80%, critical modules ≥ 90%; event paths must be exercised.
- Reports must be produced and published:
  - Unit/IT: `build/test-results/*`, `build/reports/tests/*`
  - Coverage: `build/reports/jacoco/*`
  - Architecture verification output (if any) archived as artifacts
- Contract checks: schema validation against `docs/api/openapi.yaml` is in scope for scaffolding (full scenarios are covered in Epic 14 Story 02).
 - Container stability (risk mitigation): pre-pull images; configure reusable containers and a dedicated Docker network; avoid port conflicts.

### Traceability (GWT → TraceLink)
- Provide a lint/check entrypoint `scripts/trace-lint.sh` to validate Given/When/Then test names or annotations map to TraceLink entities.
- TraceLink artifacts are stored under `docs/qa/tracelinks/*.yml` (generated/maintained by QA workflows) and referenced by the linter.

### Flakiness Mitigation
- Enable a single automatic retry for failed tests; collect retry outcomes into a dedicated report directory `build/reports/tests/flaky/*`.
- CI gate fails if new flaky tests are detected or flake counts increase compared to the last successful baseline; include diagnostics in reports.
 - Track and publish flake counts; fail on increase (see Risk Profile OPS-001).

### Seeds & Test Data
- Deterministic seeds and fixed clocks for time-dependent behavior.
- Testcontainers for DB/infra; avoid in-memory DBs; containers reused per suite when possible.

## Testing
- Run sample module tests; verify utilities; CI job sim; linting passes; flaky detection reports generated

Additional validation specifics:
- Verify CI tasks produce artifacts and enforce gates (coverage thresholds and flakiness policy).
- Run traceability linter and confirm mappings resolve to `docs/qa/tracelinks`.
- Confirm `ApplicationModules.verify()` executes in CI and fails on boundary violations.

### Risk Profile & Test Design References
- Risk profile: `docs/qa/assessments/14.01-risk-20250918.md` — Totals: critical 1, high 6, medium 4, low 3; highest OPS-001 (CI flakiness).
- Test design: `docs/qa/assessments/14.01-test-design-20250918.md` — 18 scenarios (unit 6, integration 8, e2e 4); P0 7, P1 8, P2 3.
- Quality gate inputs: use `risk_summary` and `test_design` YAML blocks from these assessments in the gate file for this story.

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — test architecture scaffolding    | PO     |
| 2025-09-18 | 0.2     | PO validation — Approved (Ready for Dev)         | PO     |

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
