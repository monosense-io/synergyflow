# ADR 0001 — Modulith + Companions

Date: 2025-10-06
Status: Accepted

## Context
We need cohesive domain refactoring and fast iteration, but physics-heavy loops (events, timers, indexing) threaten HTTP tail latency and deployment cadence.

## Decision
Adopt a Spring Modulith core application with explicit module boundaries and three companion deployments in the same repo: Event Worker, Timer/SLA Service, Search Indexer.

## Consequences
- Pros: Cohesive domain, explicit boundaries, isolated physics loops, one repo.
- Cons: Operational complexity (multiple profiles/deployments). Requires strong module enforcement.

## Alternatives Considered
- Pure Microservices (Spring Cloud): Clear isolation and independent scaling but higher operational overhead, cross‑service consistency challenges, and premature complexity at MVP scale (250 concurrent users).
- Serverless (Functions/Lambda): Attractive cost model, but timer precision and event stream fan‑out introduce cold‑start and orchestration risks; local development friction.
- Monolithic Modulith Only: Simplest ops, but physics‑heavy loops (outbox processing, timers, indexing) can impact HTTP tail latencies and release cadence.
- CQRS + Event Sourcing (Kafka): Strong audit/history guarantees, but heavier infra and write complexity; defer until scale or durability demands exceed Redis Streams comfort zone.

## Tripwires to Revisit
- API p95 sustained >400ms or p99 >800ms despite horizontal scaling of the core app.
- Redis stream consumer lag sustained >2s or frequent replay gaps.
- Timer drift >1s or missed escalations observed in production.
- Search index lag p95 >2s under normal write rates.
- Team velocity slowed by companion coupling → consider extraction or pattern change.

