# Solution Architecture — SynergyFlow

Date: 2025-10-06
Owner: Architect
Status: Proposed v1 (consolidated from PRD + architecture package)

## Executive Summary
SynergyFlow uses a Spring Modulith core with three companion deployments (Event Worker, Timer/SLA, Search Indexer). Hot reads are served from denormalized read models; writes emit transactional outbox events fanned out via Redis Streams; the HTTP app pushes idempotent deltas to clients through SSE. Keycloak provides OIDC; PostgreSQL is the OLTP store; OpenSearch handles discovery/correlation (SQL search acceptable for MVP; promote to OpenSearch when needed).

## Architecture Views
- C4 System/Container/Deployment: see `docs/architecture/architecture-blueprint.md`
- Module Boundaries and Named Interfaces: see `docs/architecture/module-boundaries.md`
- Runtime Views (queue, side‑panel, outbox→SSE): see `docs/architecture/architecture-blueprint.md`

## Components
- Core App (HTTP + SSE) with modules: ITSM, PM, Workflow, Security, Eventing, SLA, SSE, Audit
- Companions: Event Worker, Timer/SLA Service, Search Indexer
- Stores: PostgreSQL (OLTP + read models), Redis Streams, OpenSearch

## Assumptions & Scope
- Scale baseline: 250 concurrent users sustained; <10k tickets/issues at MVP.
- Phase 1 emphasizes iteration speed and operational simplicity over maximum throughput.
- Search may start with SQL (GIN/ILIKE) and evolve to OpenSearch as volume grows.
- Out of scope for MVP: mobile app, external integrations beyond IdP, ML models (beyond basic heuristics).
- Target Audience: Intermediate-to-expert backend engineers (Spring Boot, CQRS/eventing, Postgres); frontend engineers familiar with React + Ant Design.

## Cross‑cutting Concerns
- Security & Identity: OIDC/JWT via Keycloak; server‑side RBAC filters and batch authorization for composite reads.
- Observability: Tracing, RED metrics, structured logs; see `performance-model.md` for signals and gates.
- Performance Budgets (p95): queue <200ms; side‑panel <300ms; CRUD <400ms; E2E event propagation ≤2s; SSE reconnect catch‑up <1s.
- Cost/complexity: Prefer single repo, profiles per companion, boring infra defaults.

## Data Architecture
- OLTP schema for tickets/issues/approvals + audit/outbox (Flyway V1).
- Read models: `ticket_card`, `queue_row`, `issue_card`, `board_view`, `sprint_summary`.
- Primary keys: UUIDv7 (Hibernate 7 `@UuidGenerator(style = VERSION_7)` or custom fallback); time‑ordered for index locality.

## Eventing & Timers
- Transactional Outbox envelope: `{id, aggregateId, type, version, occurredAt, payload}`; unique on `(aggregateId, version)`.
- Streams: single Redis Stream with consumer groups (`app-sse`, `read-model`, `indexer`); at‑least‑once; idempotent reducers drop stale versions.
- Timers: business‑calendar aware durable timers with O(1) mutate/cancel; drift <1s; escalation handlers idempotent.

## API Contracts
- ITSM, PM, Workflow, SSE OpenAPI stubs under `docs/api`; cursor/replay semantics defined for SSE.

## Technology and Library Decision Table

| Category | Technology | Version | Rationale | Alternatives Considered |
|---|---|---|---|---|
| Core Runtime | Java (Temurin) | 21.0.8+9 LTS | Current LTS; longevity and security updates | Java 17 LTS (older), Java 22 (non‑LTS) |
| Core Framework | Spring Boot | 3.5.0 | Mature ecosystem; aligns with Modulith 1.4.x | Quarkus/Micronaut (different ecosystem) |
| Modularity | Spring Modulith | 1.4.2 | Module boundaries + tests; companion friendly | Plain packages + ArchUnit only |
| ORM | Hibernate ORM | 7.0.10.Final | UUIDv7 generator, time‑ordered PKs | Boot‑managed ORM (override if incompatible) |
| Data Access | Spring Data JPA | Boot‑managed (via 3.5.0 BOM) | Integration with JPA, repositories | MyBatis (not needed) |
| Database | PostgreSQL Server | 16.10 | Stable, feature rich | 15.x (older), 17 (newer) |
| JDBC Driver | pgJDBC | 42.7.8 | Latest stable driver for PG 16 | 42.6.x (older) |
| Migrations | Flyway | 10.17.1 | Simple, reliable schema versioning | Liquibase |
| Caching/Streams | Redis OSS | 7.2.x | Streams for fan‑out; simple ops | Redis 8 (newest), Kafka (heavier) |
| Redis Client | Redisson | 3.52.0 | Robust client, streams support | Lettuce (viable) |
| Search | OpenSearch | 2.19.2 | OSS alternative, stable 2.x | Elasticsearch (licensing) |
| Security | Keycloak | 26.1.0 | OIDC provider; standard JWT | Auth0/Okta (SaaS) |
| Observability | OpenTelemetry Java Agent | 2.14.0 | Standardized traces/metrics/logs | Micrometer‑only (reduced scope) |
| Testing | Testcontainers (Java) | 1.21.3 | Realistic infra in tests | Embedded DBs only |
| Performance | Gatling | 3.14.x | Load and latency testing | k6/Locust |
| Frontend | Node.js (LTS) | 22.x (≥22.12) | Modern LTS; Vite 7 requirement | Node 20 (maintenance) |
| Frontend | React | 18.2.0 | Stable; enterprise ecosystem | React 19 (phase‑2 upgrade) |
| Frontend | Vite | 7.x | Fast dev/build; Node ≥22.12 | Webpack/Rspack |
| Frontend | Ant Design | 5.19.x | Enterprise UI components | MUI (alt) |
| Frontend | TypeScript | 5.6.x | Modern TS features | 5.3.x (older) |

Notes:
- Backend library patch levels are pinned via the Spring Boot 3.5.0 BOM unless explicitly overridden above.
- If Hibernate 7 conflicts with the BOM, fall back to Boot‑managed ORM and use the documented UUIDv7 custom generator until upgrade.

## Availability & Resilience
- Target SLOs: API p95/p99 as above; error budget policy applied per sprint.
- Failure isolation: companions crash must not degrade HTTP p95; outbox lag tolerated up to 2s p95.
- Backpressure: event worker autoscale on outbox lag; SSE fan‑out bounded by stream lag and client cursors.
- Retry strategy: exponential backoff with caps; idempotent writes guarded by version and unique keys.

## Security & Compliance
- AuthZ model: role‑based (Admin, ITSM Agent, Developer, Manager, Employee) with scope filters for queries.
- Data classification: tickets/comments may contain PII; restrict exposure in read models; mask internal notes for unauthorized roles.
- Secrets & config: managed via Kubernetes Secrets + sealed‑secrets or equivalent; rotate quarterly; no secrets in images.
- Audit: all state‑changing actions produce audit events; queryable by aggregate and time.

## Tenancy & Isolation
- Phase 1: single‑tenant per environment with org scoping at application layer.
- Phase 2 options: schema‑per‑tenant (Postgres) vs row‑level isolation; revisit at >3 tenants or regulatory need.

## Backup & DR
- PostgreSQL: PITR with WAL archiving; daily full snapshot; RPO ≤15 minutes; RTO ≤4 hours.
- Redis: persistence enabled (AOF) for stream continuity; acceptable to rebuild read models from outbox if lost.
- OpenSearch: snapshot to object storage nightly; rebuild allowed from events if snapshot unavailable.

## Environments & Promotion
- Environments: Dev/CI → Staging → Prod; same manifests with overlays (Kustomize/Helm).
- Migrations: Flyway gated in CI; blocked on failing dry‑run; rolling updates with readiness gates.
- CI/CD: build, unit/integration tests, modularity tests, OpenAPI lint, perf smoke (Gatling), SAST/DAST, SBOM publish.

## Proposed Source Tree

```text
repo-root/
├── backend/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── src/main/java/io/monosense/synergyflow/
│   │   ├── itsm/
│   │   │   ├── api/
│   │   │   ├── spi/           # @NamedInterface public contracts
│   │   │   └── internal/      # Implementation details
│   │   ├── pm/
│   │   ├── workflow/
│   │   ├── security/
│   │   ├── eventing/
│   │   ├── sla/
│   │   ├── sse/
│   │   └── audit/
│   ├── companions/
│   │   ├── event-worker/      # Outbox poller + Redis Streams publisher
│   │   ├── timer-service/     # SLA escalation scheduler
│   │   └── search-indexer/    # OpenSearch indexing worker
│   └── src/main/resources/db/migration/  # Flyway scripts
├── frontend/
│   ├── itsm-ui/
│   ├── pm-ui/
│   └── shared-ui/
├── infrastructure/
│   ├── k8s/
│   └── envoy/
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── epics/
│   └── product/
└── testing/
    ├── integration/
    ├── e2e/
    └── performance/
```

## Capacity & Sizing (MVP Baseline)
- Core App: 5 replicas, 1 vCPU/2GB requests (2/4 limits); ~50 active users per replica.
- Event Worker: 2 replicas, 0.5 vCPU/1GB (1/2 limits); scale on `outbox_rows_behind`.
- Timer/SLA: 2 replicas, 0.5 vCPU/1GB (1/2 limits); shard by timer buckets if needed.
- Postgres: 4 vCPU/16GB; streaming replica for HA; pool size 50.

## Risks & Tripwires
- Sustained API p95 >400ms, consumer lag >2s, or timer drift >1s → trigger scale‑out or architecture revisit (partitioning, Kafka adoption).
- Module boundary violations → fail modularity tests; fix before merge.
- Over‑eager search rollout → defer OpenSearch until query volume/latency require it.

## Roadmap & Gates
- Alpha (S6): perf budgets met in staging; SSE reconnect <1s; timer drift <1s.
- RC (S9): p99 <800ms; 2h soak stable; zero missed timers; zero duplicate reducer applies.
- GA (S10): ops runbooks, backup/restore drills, security review, on‑call readiness.

## Decision Log
- ADR 0001: Modulith + Companions
- ADR 0002: UUIDv7
- ADR 0003: Redis Streams → revisit Kafka at scale
- ADR 0004: SSE vs WebSocket

## Alternatives Considered (Summary)
- Microservices (Spring Cloud): Higher ops and consistency cost; premature for 250 users. Prefer Modulith + companions with clear tripwires to split.
- Serverless (Lambda): Concurrency/tail‑latency risks for timers and streams; complex local dev.
- CQRS + Event Sourcing (Kafka): Strong history and audit, but heavier infra and write complexity; defer until scale demands.
See ADR 0001 for detailed analysis.

## Architecture Selection Process
- Input sources: PRD v3.0, performance budgets (performance-model.md), Modulith guidance, prior art.
- Shortlist: Modulith + companions, Microservices (Spring Cloud), Serverless (FaaS), CQRS+ES (Kafka‑centric).
- Evaluation criteria: team size (≤10), user scale (≤250 concurrent), iteration speed, tail latency risk, ops complexity, testability.
- Comparison outcome: Modulith + companions maximizes iteration speed and keeps physics loops off the hot path; ops remains simple.
- Decision checkpoint: Recorded in ADR 0001; alternatives summarized above.

## Open Questions
- When to switch to OpenSearch (thresholds beyond SQL+GIN)?
- Tenant isolation choice for Phase 2?
- Batch authorization scaling targets at >250 concurrent users?

## Specialist Sections
- DevOps Architecture: Environments, promotion, K8s overlays, backup/DR (see sections above). Expand to a dedicated doc if complexity increases.
- Security Architecture: OIDC, RBAC, PII handling, audit (see Security & Compliance). Expand with data‑flow diagrams if external integrations appear.
- Test Architecture: Unit/integration/perf plans and gates (see `performance-model.md` and epic tech specs). Add component‑level contract tests as APIs stabilize.

## Appendix: Project‑Type Questionnaire (Executed)
- See `docs/architecture/project-type-questions.md` (executed 2025-10-06). Covers ITSM/PM specifics: ITIL alignment, SLA policies, routing, board WIP, approvals scope, PII handling, SLOs.
