---
id: arch-05-eventing-architecture
title: Eventing Architecture
owner: Architect
status: Draft
last_updated: 2025-09-03
links:
  - ../event-catalog.md
  - ../prd/03-system-architecture-monolithic-approach.md
---

## Patterns
- Outbox (JDBC) with Spring Modulith event publication registry
- @ApplicationModuleListener with REQUIRES_NEW for reliability
- Externalization via Kafka with routing keys and headers

## Contracts
- Versioned events; add fields only, never break consumers
- Change↔CMDB impact assessment handshake for approvals

## SLOs
- <50ms publication latency; <500ms cross‑module propagation (p95)

## Change↔CMDB Impact Assessment (Sequence)
```mermaid
sequenceDiagram
  participant CHG as Change Module
  participant EVT as Event Publication Registry
  participant CMDB as CMDB Module

  CHG->>EVT: Publish ChangeImpactAssessmentRequestedEvent
  EVT-->>CMDB: Deliver event after commit
  CMDB->>EVT: Publish ChangeImpactAssessedEvent
  EVT-->>CHG: Deliver impact result event
```
