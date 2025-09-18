---
id: 27-architecture-decision-records-adr
title: 27. Architecture Decision Records (ADR)
version: 8.0
last_updated: 2025-09-03
owner: Architect
status: Draft
---

## 27. Architecture Decision Records (ADR)

### ADR-01: Event-Only Inter-Module Communication (Accepted)

- Context: Direct module dependencies in a monolith increase coupling and hinder modularity, testing, and evolution. Spring Modulith encourages event-driven boundaries and module isolation.
- Decision: All ITIL process modules have zero direct dependencies between each other. Interactions occur via domain events only. The Change↔CMDB impact check uses an explicit event handshake (ChangeImpactAssessmentRequestedEvent → ChangeImpactAssessedEvent). The Integration module remains a pure event listener. The User module is the only shared foundation module; direct use is minimized and not required by ITIL modules.
- Exceptions: None for ITIL modules. CMDB exposes an optional read API only for external tooling; ITIL modules do not call it synchronously.
- Rationale: Reduces coupling, enforces clean boundaries, simplifies module verification (ApplicationModules.verify), improves resilience via async patterns.
- Consequences: Introduces eventual consistency and requires event contract versioning. Adds testing needs (Scenario API). Requires observability for event flows.
- Status: Accepted (Doc v8.0, 2025‑09‑03). Revisit only if a regulated workflow demands strict synchronous checks.

### ADR-02: Only 'user' as Shared Foundation Module (Accepted)

- Context: Some shared module access is necessary for identity and authentication, but widespread shared dependencies increase coupling and risk boundary violations.
- Decision: The 'user' module is the only shared foundation module. No other ITIL modules are exposed as shared dependencies. All other inter-module needs are satisfied via domain events and local read models. The 'team' module depends on 'user' (for identity resolution) but is not a shared dependency for others.
- Exceptions: None. For read needs across modules, prefer event-driven projections. For external consumers, expose dedicated APIs via the integration boundary rather than inter-module calls.
- Rationale: Minimizes coupling, keeps boundaries clean, improves testability and architectural verification.
- Consequences: Requires eventual consistency patterns, projections, and good observability. May add slight latency for data propagation but improves overall modularity.
- Status: Accepted (Doc v8.0, 2025‑09‑03).

## Review Checklist

- Content complete and consistent with PRD
- Acceptance criteria traceable to tests (where applicable)
- Data model references validated (where applicable)
- Event interactions documented (where applicable)
- Security/privacy considerations addressed
- Owner reviewed and status updated

## Traceability

- Features → Data Model: see 18-data-model-overview.md
- Features → Events: see 03-system-architecture-monolithic-approach.md
- Features → Security: see 13-security-compliance-framework.md
- Features → Tests: see 11-testing-strategy-spring-modulith.md
