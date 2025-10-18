# Architecture Decision Records

Project: synergyflow
Date: 2025-10-18
Author: Eko Purwanto

---

## Overview

This document captures architectural decisions made during the Solution Architecture workflow. Each entry lists context, options considered, the decision, rationale, and consequences.

---

## Decisions

### ADR-001: Architecture Style — Modular Monolith with Spring Modulith

Date: 2025-10-18
Status: Accepted
Decider: Collaborative (PM + Architect)

Context:
We need a structure that supports clear module boundaries, event-driven integration, and rapid delivery for an MVP-scale platform (≤1,000 users) without microservice overhead.

Options Considered:
1. Microservices (per module) — Pros: independent scaling; Cons: high ops complexity, premature distribution
2. Modular Monolith (Spring Modulith) — Pros: strong boundaries, simple ops; Cons: single deployable
3. Classic Monolith — Pros: simplest; Cons: weak boundaries, coupling risk

Decision:
Use Modular Monolith with Spring Boot 3.5.6 + Spring Modulith 1.4.2.

Rationale:
Enforces module boundaries/events with far lower operational complexity than microservices at MVP scale.

Consequences:
- Positive: Faster delivery; easier debugging; strong cohesion
- Negative: Single deployment artifact; scale via replicas
- Neutral: Migration path to services if needed later

---

### ADR-002: Real-Time Updates — Server-Sent Events (SSE) First

Date: 2025-10-18
Status: Accepted
Decider: Collaborative

Context:
Dashboards and ticket views require live updates and freshness badges.

Options Considered:
1. WebSockets — Pros: bidirectional; Cons: more infra complexity
2. Server-Sent Events (SSE) — Pros: simple, HTTP-friendly; Cons: one-way stream
3. Polling — Pros: trivial; Cons: inefficient, stale

Decision:
Adopt SSE for MVP; keep gateway/app WebSocket-ready for future.

Rationale:
Meets real-time needs with minimal complexity.

Consequences:
- Positive: Simple implementation; works with HTTP infra
- Negative: One-way only; fall back to WS if needed

---

### ADR-003: Authorization — OPA Sidecar with Decision Receipts

Date: 2025-10-18
Status: Accepted
Decider: Collaborative

Context:
We need explainable, auditable authorization across modules.

Options Considered:
1. Spring Security ACL/RBAC only — Pros: simple; Cons: limited explainability
2. Custom ABAC — Pros: flexible; Cons: bespoke, hard to audit
3. OPA sidecar + receipts — Pros: policy-as-code, explainable; Cons: extra component

Decision:
Use OPA sidecar for authz with mandatory decision receipts and shadow→canary gates.

Rationale:
Delivers governance and auditability aligned with PRD.

Consequences:
- Positive: Clear policies; audit-grade receipts
- Negative: Sidecar management; performance tuning required

---

### ADR-004: Data Layer — PostgreSQL 16 + FTS (MVP)

Date: 2025-10-18
Status: Accepted
Decider: Collaborative

Context:
Relational data with event outbox and initial knowledge search.

Options Considered:
1. PostgreSQL + FTS — Pros: single system; Cons: basic relevance
2. Elasticsearch for search — Pros: advanced; Cons: extra infra
3. Polyglot persistence — Pros: best-of-breed; Cons: complexity

Decision:
PostgreSQL 16 (CloudNative‑PG HA) with FTS for MVP; reassess vector/ES later.

Rationale:
Keeps infra minimal while meeting MVP needs.

Consequences:
- Positive: Simple ops; fewer moving parts
- Negative: Less powerful search initially

---

### ADR-005: Frontend — Next.js 15 (SSR) + Query/Zustand

Date: 2025-10-18
Status: Accepted
Decider: Collaborative

Context:
Need fast first paint, robust data fetching, and simple local state.

Options Considered:
1. SPA (CSR) — Pros: simple hosting; Cons: slower FCP
2. SSR with Next.js — Pros: fast, SEO; Cons: server complexity
3. Hybrid SSR/ISR — Pros: caching; Cons: extra nuance

Decision:
Next.js 15 (App Router, SSR) with TanStack Query (server state) and Zustand (UI state).

Rationale:
Balances performance and developer productivity.

Consequences:
- Positive: Great UX; predictable state
- Negative: SSR operational nuances

---

### ADR-006: Integration Pattern — Transactional Outbox for Events

Date: 2025-10-18
Status: Accepted
Decider: Collaborative

Context:
We must publish reliable domain events without dual-write issues.

Options Considered:
1. Direct publish in same tx — Pros: simple; Cons: dual-write risk
2. Polling outbox table — Pros: reliable; Cons: added table/process
3. External broker upfront — Pros: decoupling; Cons: extra infra, complexity

Decision:
Transactional outbox table `event_publication` with idempotent consumers.

Rationale:
Reliable delivery with minimal infra at MVP scale.

Consequences:
- Positive: Strong consistency guarantees; retryable delivery
- Negative: Poller/cleanup process to operate

---

## Decision Index

| ID    | Title                                         | Status   | Date       | Decider      |
| ----- | --------------------------------------------- | -------- | ---------- | ------------ |
| 001   | Architecture Style — Modular Monolith         | Accepted | 2025-10-18 | Collaborative|
| 002   | Real-Time Updates — SSE First                 | Accepted | 2025-10-18 | Collaborative|
| 003   | Authorization — OPA Sidecar + Receipts        | Accepted | 2025-10-18 | Collaborative|
| 004   | Data Layer — PostgreSQL 16 + FTS              | Accepted | 2025-10-18 | Collaborative|
| 005   | Frontend — Next.js 15 + Query/Zustand         | Accepted | 2025-10-18 | Collaborative|
| 006   | Integration — Transactional Outbox            | Accepted | 2025-10-18 | Collaborative|

---

_Generated during Solution Architecture (Phase 3) on 2025-10-18_

