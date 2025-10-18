# Coding Standards & Implementation Guide (SynergyFlow)

These rules are mandatory for all contributors and AI agents. They complement ADRs and prevent drift and hallucination. When in doubt, defer to Story Context XML, ADRs, and architecture docs under `docs/architecture/`.

## General
- Language level: Java 21. Avoid returning `null`; prefer `Optional` for repository lookups and nullable results.
- Package naming: `io.monosense.synergyflow.<module>.<layer>`. Examples: `incident.domain`, `incident.application`, `incident.api`, `audit.internal`.
- Module boundaries: No cross‑module imports of internals. Only `api` types are importable across modules.
- DTOs: Immutable (Java records or final fields). Do not expose entities via API. Validate using Jakarta Bean Validation.
- Errors: RFC 7807 `application/problem+json` with stable `code` values. Never leak PII in problem details. Provide `fields[]` (name, message, code) for validation errors.
- Logging: Structured JSON via SLF4J/Logback. Include correlationId and causationId in MDC; include traceId/spanId when available. Never log plaintext PII or secrets.
- Internationalization: Endpoints that honor `Accept-Language` MUST return `Content-Language` and include `Vary: Accept-Language`.
- Observability: Micrometer timers on HTTP handlers; OpenTelemetry spans for major operations (create/search/async jobs). Emit event‑lag and consumer‑error metrics.
- Build system: Gradle (Kotlin DSL). Use Gradle Wrapper. Prefer Version Catalogs; do not add Maven/Groovy DSL builds.

## Java Tooling (Lombok, MapStruct)
- Lombok: Use to reduce boilerplate. Prefer `@RequiredArgsConstructor` for constructor injection; `@Getter` for immutable fields; `@Builder` for complex aggregates/DTOs; `@Slf4j` for logging. Avoid `@Data` on entities; implement equals/hashCode/toString explicitly when needed.
- MapStruct: Standardize DTO/domain mapping. Define mappers per module with `@Mapper(componentModel = "spring")`. Keep mapping declarative; use `@Mapping` for divergent names; isolate conversion helpers via `@Mapper(uses = …)`.
- Package placement: API DTO mappers live next to DTOs in `…api`; persistence mappers live in `…infrastructure`.

## API Design
- Base path: `/api/v1/**`. JSON only.
- Pagination: `page` (0‑based), `size` (1..100, default 20). Sorting: repeated `sort=field,(asc|desc)`.
- Conditional GET: ETag/If‑None‑Match; return 304 when unchanged. Set `Cache-Control` appropriately.
- Vary headers: Always include `Origin`; include `Accept-Language` on localized responses.
- Validation errors: RFC7807 with `fields[]`. Use Jakarta annotations on request records.
- OpenAPI: Keep spec under `docs/openapi/synergyflow.yaml`. Update with any contract changes.

## Security
- Authentication: OAuth2 Resource Server (JWT). Validate and map standard claims; treat tenant/org and roles as first‑class.
- Authorization: Policy‑as‑code (OPA) for coarse rules; method‑level `@PreAuthorize` for fine‑grained checks. Enforce department/clearance on reads/writes where applicable.
- Secrets: Never commit secrets. Load from environment/secret stores at runtime.
- Data handling: Mask/omit PII in logs; do not echo sensitive input in errors.

## Transactions & Reliability
- Boundary: Service layer (`…application`). Annotate with `@Transactional` where needed.
- Idempotency: For POST/PUT that can be retried, support `Idempotency-Key` (scope: tenant+method+path+key). Store request hash with TTL; reply with original response or 409 on conflict.
- Rate limiting: Token‑bucket per user+client; 100 req/min default; return 429 with standard headers.

## MyBatis‑Plus (Repository Layer)
- CRUD: Use `BaseMapper<T>` for simple CRUD; list explicit columns; avoid `select *`.
- Complex queries: Place XML mappers under `apps/backend/src/main/resources/mapper/<module>/…Mapper.xml`. Match interface and XML ids.
- SQL style: Prefer CTEs and PostgreSQL features (JSONB, FTS, `ON CONFLICT`); always bind parameters; never concatenate SQL strings.
- Model split: Keep table‑bound entities in `…infrastructure` when separating from domain models.
- Encryption/indexing: Encrypted columns handled with `TypeHandler` or `<field>_enc`; equality lookups use HMAC `<field>_eq` index. Never log plaintext.
- Paging: Use MyBatis‑Plus `Page` and map to API pagination shape.
- Migrations: All schema changes via Flyway under `apps/backend/src/main/resources/db/migration`.
- Interceptor: Include `mybatis-plus-jsqlparser` and register `PaginationInnerInterceptor(DbType.POSTGRE_SQL)`.

Configuration example
```java
@Configuration
class MybatisPlusConfig {
  @Bean
  MybatisPlusInterceptor interceptor() {
    MybatisPlusInterceptor i = new MybatisPlusInterceptor();
    i.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
    return i;
  }
}
```

## Spring Modulith (Modular Architecture)
- One Java package per module root; annotate and enforce boundaries with Modulith.
- Events: Publish domain events for cross‑module interactions. Do not call other modules’ internals directly.
- Imports: Only `…api` layer types cross module boundaries. No circular deps.
- Architecture tests: Add a test running `ApplicationModules.of(SynergyFlowApplication.class).verify()` to detect cycles and boundary violations.

Example architecture verification
```java
@Test
void architecture_is_sound() {
  ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);
  modules.verify();
}
```

## Testing
- Unit tests: JUnit 5 + AssertJ/Mockito. Focus on service logic and mappers.
- Integration (web/persistence): Prefer Spring slices (e.g., `@DataJdbcTest`, `@MybatisTest`) and full SpringBootTests as needed.
- Databases: Use Testcontainers for PostgreSQL for persistence/integration tests. In‑memory DBs are not allowed for data‑path integration. Exception: very small Modulith event‑registry smoke tests may use H2 for speed; any assertion involving schema/SQL behavior must use PostgreSQL Testcontainers.
- Modulith: Use `spring-modulith-starter-test` to assert published/consumed events and scenarios.
- Performance: Include k6 scripts and thresholds for critical flows where applicable.

## Javadoc Requirements
- Class‑level (REQUIRED): comprehensive description plus tags `@author monosense`, `@since <version-introduced>`, `@version`.
- Method‑level (REQUIRED): purpose plus `@param`, `@return`, and `@throws` where applicable. No author/version tags at method level.
- Usage examples: For complex APIs and services, include a short example in Javadoc demonstrating typical usage and edge cases.

## Frontend (Summary for AI Agents)
- Framework: Next.js 15 (App Router) + TypeScript.
- State/Data: TanStack Query for server state; Zustand for local UI state.
- Tables/Lists: Server‑driven pagination; virtualization where appropriate.
- Forms: react‑hook‑form + Zod; map RFC7807 `fields[]` to form errors.
- Errors: Central handler for RFC7807; surface `title`/`detail` + field issues.
- Auth: OIDC JWT validation at gateway; client reads roles/department/clearance from ID token and enforces route/component guards.
- Observability: Web vitals + OpenTelemetry web for traces.

## Definition of Done (for PRs)
- Adheres to package structure and Modulith boundaries.
- API contracts documented/updated in OpenAPI.
- Error handling returns RFC7807 with stable codes.
- Logs/metrics/traces added where relevant; no PII in logs.
- Tests added/updated; coverage meets CI thresholds.
- Significant changes reference relevant ADRs in the PR description.
- Lombok/MapStruct applied where they meaningfully reduce boilerplate.
- Javadocs present per policy (class‑level tags; method‑level params/returns/throws; examples for complex flows).
- Gradle Kotlin DSL only; build passes with `./gradlew`.

## Repository Layout (Backend)
- `apps/backend/` — Spring Boot + Spring Modulith backend
  - `src/main/java/io/monosense/synergyflow/<module>/<layer>/…`
  - `src/main/resources/db/migration/` — Flyway migrations
  - `src/test/java/…` — unit/integration tests (JUnit 5, Modulith Test, Testcontainers)

## Naming & Packaging Examples
- Module: `incident`
  - API: `io.monosense.synergyflow.incident.api`
  - Application: `io.monosense.synergyflow.incident.application`
  - Domain: `io.monosense.synergyflow.incident.domain`
  - Infrastructure: `io.monosense.synergyflow.incident.infrastructure`

## Observability & Logging Examples
- MDC keys: `correlationId`, `causationId`. Ensure these flow into logs and serialized events.
- Emit event processing lag, retry counts, and consumer error metrics.

---

Authoritative references
- Architecture: `docs/architecture/architecture.md`
- Testing strategy: `docs/architecture/appendix-19-testing-strategy.md`
- ADRs: `docs/architecture-decisions.md` (e.g., ADR‑006 Transactional Outbox)

