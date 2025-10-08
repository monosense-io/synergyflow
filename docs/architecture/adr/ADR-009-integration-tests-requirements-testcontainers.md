# ADR 0009 — Integration Test Requirements (Testcontainers Mandatory)

Date: 2025-10-08
Status: Accepted
Owner: Developer Agent
Priority: High
Applies To: Integration tests for DB, Redis, SSE, OAuth

## Context
Deterministic integration tests require realistic infrastructure (PostgreSQL, Redis for Streams, Keycloak, etc.). In‑memory substitutes (H2, embedded Redis) diverge from production and hide issues in schema, networking, and timing.

## Decision
All integration tests MUST provision dependencies via Testcontainers. No in‑memory stand‑ins for DB/Redis/OAuth. Tests configure Spring via `@DynamicPropertySource` from container endpoints. Containers run per test class or per suite depending on stability.

### Minimums
- PostgreSQL: `PostgreSQLContainer` with init SQL or Flyway.
- Redis Streams: `GenericContainer("redis:7.4")` (expose 6379). Optionally pin minor version in CI.
- OAuth: `GenericContainer("quay.io/keycloak/keycloak:24")` or WireMock stubs for token introspection if needed.
- Optional: `Network.newNetwork()` to isolate cross‑container traffic; use network aliases (`redis`, `db`).

### Spring/JUnit 5 Sketch
```java
@Testcontainers
@SpringBootTest
class ItTicketApi {
  static Network net = Network.newNetwork();

  @Container static PostgreSQLContainer<?> db = new PostgreSQLContainer<>("postgres:16").withNetwork(net).withNetworkAliases("db");
  @Container static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
      .withExposedPorts(6379).withNetwork(net).withNetworkAliases("redis");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", db::getJdbcUrl);
    r.add("spring.datasource.username", db::getUsername);
    r.add("spring.datasource.password", db::getPassword);
    r.add("spring.data.redis.host", redis::getHost);
    r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }
}
```

### Execution
- Local: container reuse enabled for speed (`~/.testcontainers.properties`: `testcontainers.reuse.enable=true`).
- CI: reuse disabled; images pulled/pinned; parallelism balanced to avoid port contention.

## Consequences
- Higher fidelity and fewer production surprises.
- Slightly slower than in‑memory tests; mitigated by reuse and class‑scoped containers.

## Alternatives Considered
- Embedded DB/Redis: faster but non‑representative; rejects features we rely on (JSONB, Streams semantics, auth flows).

## References
- docs/architecture/gradle-build-config.md
- docs/architecture/eventing-and-timers.md
