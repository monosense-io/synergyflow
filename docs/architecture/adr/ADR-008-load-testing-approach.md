# ADR 0008 — Load Testing Approach (JUnit @Tag("load"), Configurable)

Date: 2025-10-08
Status: Accepted
Owner: Developer Agent
Priority: High
Applies To: Backend services, SSE bridge

## Context
We need repeatable, code‑reviewable load tests aligned with our Gradle/JUnit stack and performance SLOs. External tools (k6/Gatling) may be added later, but we need a baseline that runs in CI and dev laptops.

## Decision
Maintain load tests as JUnit 5 tests annotated with `@Tag("load")`, executed on demand with configurable parameters (users, RPS, duration, ramp, payload size). Defaults live in code; overrides come from system properties/env.

### Conventions
- Source set: run in existing `integrationTest` or a dedicated `loadTest` source set (to be added later if helpful).
- Tag: `@Tag("load")` on classes/methods.
- Invocation examples:
  - Gradle: `./gradlew test -Djunit.jupiter.tags=load -Dload.users=50 -Dload.rps=25 -Dload.duration=120s`
  - Gradle (integration tests): `./gradlew integrationTest -Djunit.jupiter.tags=load …`
- Parameters (system props or env):
  - `load.users` (int, default 25)
  - `load.rps` (int, default 20)
  - `load.duration` (e.g., `120s`)
  - `load.ramp` (e.g., `30s`)
  - `load.payloadBytes` (int, default 512)

### Sketch
```java
@Tag("load")
class TicketApiLoadTest {
  int users = Integer.getInteger("load.users", 25);
  int rps = Integer.getInteger("load.rps", 20);
  Duration duration = Duration.parse("PT" + System.getProperty("load.duration", "120s").replace("s","S"));

  @Test
  void sustainThroughputWithinBudget() throws Exception {
    var executor = Executors.newVirtualThreadPerTaskExecutor(); // JDK 21
    var limiter = RateLimiter.create(rps); // choose impl
    var end = Instant.now().plus(duration);
    var latencies = new LongAdder();
    // … issue requests using HttpClient, record p95/p99, error rate …
  }
}
```

### Budgets & Reporting
- Targets (Phase 1): p95 ≤ 400 ms, p99 ≤ 800 ms, error rate < 1% under specified `users` × `rps`.
- Export CSV/JSON metrics to `build/reports/load/` and emit Micrometer counters during runs for local dashboards.

## Consequences
- Load scenarios live next to code and reviewers; low barrier to run.
- Not a full replacement for tool‑driven tests (k6/Gatling). Those can be layered later; this ADR sets the baseline contract and tagging.

## References
- docs/architecture/performance-model.md
- docs/architecture/gradle-build-config.md
