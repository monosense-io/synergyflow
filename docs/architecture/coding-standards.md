---
id: arch-10-coding-standards
title: Coding Standards
owner: Architect
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/03-system-architecture-monolithic-approach.md
  - ../prd/11-testing-strategy-spring-modulith.md
  - ../prd/13-security-compliance-framework.md
  - ../event-catalog.md
  - ../../migrations/
---

## Principles

- Readability over cleverness; minimize cognitive load.
- Small, composable units; fail fast with helpful messages.
- Strong boundaries: respect Spring Modulith module boundaries and ADRs.
- Tests document behavior; production code favors clarity.

## Languages & Versions

- Java 21 for backend (Spring Boot 3.2+). Gradle 8 (Kotlin DSL).
- TypeScript 5 for Next.js frontend. ESLint + Prettier enforced.
- Dart (Flutter 3.x) for mobile. Null‑safety enabled.

## Repository Structure (backend)

- Package by module (bounded context), not by technical layer.
- Each module has clear entry points (services), domain, and adapters.
- Add `package-info.java` with `@ApplicationModule` and optional `@NamedInterface` per PRD.

## Spring Modulith Conventions

- Zero direct dependencies between ITIL modules (see ADR‑0001).
- Only `user` is shared; `team` depends on `user::api` (ADR‑0002).
- Cross‑module interactions via events only; never use `@Autowired` into other modules.
- Event records are immutable and versioned: include `version` and `createdAt/Instant`.
- Event listeners use `@ApplicationModuleListener`; long work runs in `REQUIRES_NEW` with retries.
- Annotate named interfaces with `@NamedInterface("api")` only for external tooling.

## Java Style (backend)

- Package names: `io.monosense.synergyflow.<module>[.<feature>]`.
- Classes and methods: meaningful names; avoid abbreviations.
- Prefer records/DTOs for event payloads and pure data structures.
- Service methods return domain results, not `null`; use Optional only at boundaries.
- Logging: SLF4J; structured context (correlationId, module, eventType). No stacktraces for expected flows.
- Exceptions: domain exceptions are checked or well‑typed runtime; never swallow.
- Validation: Bean Validation (Jakarta) at boundaries; defensive checks in domain.

### Imports

- Do not use fully‑qualified class names (FQCN) in code. Always add explicit imports.
- Use static imports for test assertions and common matchers (e.g., AssertJ, JUnit).
- Avoid wildcard imports (`import x.y.*`); prefer explicit class imports.
- Organize imports: standard libs → third‑party → internal; no unused imports committed.

### Lombok

- Always use Lombok where it improves clarity and reduces boilerplate.
- Use `@Getter/@Setter` sparingly; prefer immutability with `@Value` or final fields with `@RequiredArgsConstructor`.
- Use `@Builder` for complex object construction; keep builders at aggregate or DTO boundaries.
- Include `@Slf4j` for logging where appropriate.

### MapStruct

- Always use MapStruct for DTO ↔ domain ↔ entity mappings.
- Define mappers as Spring components (`componentModel = "spring"`).
- Centralize common mappings; avoid ad‑hoc conversion logic scattered in services.

### Javadoc

- Every public class must have comprehensive Javadoc including:
  - `@author monosense`
  - `@since <yyyy-mm>` (or sprint tag)
  - `@version <semver>`
- Public methods expose purpose, parameters, return values, and exceptions via `@param`, `@return`, `@throws`.

## Transactions

- Command handlers/service methods are transactional (`@Transactional`), publish events after commit.
- Event listeners open new transactions for side effects; idempotent handlers.

## Testing Standards

- Use `@ApplicationModuleTest` for module isolation; do not mock other modules.
- Verify events with `PublishedEvents` / `AssertablePublishedEvents`.
- Cross‑module flows use `Scenario` with timeouts and retries.
- Unit tests: Given/When/Then naming; avoid over‑mocking. Cover happy path and failure modes.
- Minimum coverage: critical modules 90%+, overall 80%+; event paths must be tested.

### Database & Containers

- Avoid H2 and other in‑memory DBs for tests.
- Use Testcontainers exclusively for DB and infra dependencies (PostgreSQL, Redis, MinIO, Kafka, etc.).
- Tests must be self‑contained and parallelizable; container lifecycle managed per
  test suite with reusable containers where possible.

## Database & Migrations

- SQL naming: snake_case tables/columns; singular table names are acceptable when consistent with PRD (e.g., `incidents`).
- Primary keys: string IDs (varchar) as per PRD; consistent length; no DB‑generated business keys.
- Index naming: `idx_<table>_<columns>`; partial indexes for active states.
- Migrations live under `migrations/` as `V###__description.sql`; include rollback notes.
- Avoid destructive changes; use phased rollouts (add/write both → backfill → switch → drop).
- Test migrations against Testcontainers PostgreSQL in CI; never rely on H2 compatibility.

## REST & GraphQL API

- REST: OpenAPI 3 spec, consistent error shape `{ code, message, details, correlationId }`.
- Status codes: 2xx success; 4xx client errors; 5xx server; use 429 for rate limiting.
- Pagination: cursor or page/limit with `total` when needed; document limits.
- GraphQL for complex reads only; no mutations that cross module boundaries directly.
- Idempotency for POST where applicable via `Idempotency-Key`.

## Frontend (Next.js + TypeScript)

- App Router with route groups; RSC where possible; client components only for interactivity.
- State: TanStack Query for server data; local state minimal.
- Forms: React Hook Form + Zod schemas; server validation mirrors Zod.
- UI: shadcn/ui + Tailwind; tokens via Style Dictionary; WCAG 2.1 AA.
- Lint/format: ESLint (recommended + security), Prettier; strict TS config.

## Mobile (Flutter)

- State management: Riverpod; immutable models; Freezed where useful.
- Layering: data → domain → presentation; keep platform channels minimal.
- Offline: SQLite with sync conflict resolution strategies documented.

## Observability

- Micrometer metrics per module; tag with `module`, `eventType`, `result`.
- Log JSON; include `correlationId`, `userId` when available; no PII in logs.
- Tracing via OpenTelemetry; propagate context across async boundaries.

## Security

- Input validation and output encoding; CSP and CSRF protections.
- Secrets via Vault (no secrets in code or config); rotate automatically.
- Access control: method‑level `@PreAuthorize` where appropriate; enforce module boundaries.

## Notifications

- Templates versioned; localized; previewable; link to entities via safe URLs.
- Respect user/team notification preferences; quiet hours; dedupe to prevent floods.

## Git & Reviews

- Conventional Commits; small PRs with clear description and checklist.
- Require one reviewer for non‑critical, two for security/DB changes.
- Block merges on failing tests, coverage regression, or docs validation failures.

## Documentation

- Update related shards upon changes (architecture, data model, security, tests).
- Keep ADRs current when decisions change; prefer new ADR entries over editing history.
