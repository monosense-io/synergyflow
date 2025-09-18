---
adr: 0012
title: Workflow Orchestration Strategy — Adopt Flowable 7.x
status: Accepted
date: 2025-09-18
owner: Architect
links:
  - ../architecture/module-architecture.md
  - ../architecture/api-architecture.md
  - ../architecture/deployment-architecture.md
  - ../prd/02-itil-v4-core-features-specification.md
  - ../prd/03-system-architecture-monolithic-approach.md
---

Context

- SynergyFlow requires configurable workflows for ITIL processes (incidents, requests,
  changes, knowledge approvals) with human tasks, timers, and approvals.
- Administrative users must be able to author and update workflows without code changes.
- Prior consideration included Camunda and code‑first engines; we want an open‑source,
  embeddable BPMN engine with on‑prem friendly footprint and mature modeling tools.

Decision

- Adopt Flowable 7.x as the workflow engine.
- Embed the Flowable engine in the `workflow` module; expose process APIs internally.
- Provide Flowable Modeler/IDM apps for administrative users to author BPMN/DMN models
  (SSO‑protected, admin‑only), with versioned deployment to the engine.

Rationale

- Standards‑based (BPMN 2.0, DMN, CMMN) with mature modeling UI and REST APIs.
- Lightweight, embeddable in Spring Boot; fits Spring Modulith boundaries.
- On‑prem deployment suitability and OSS licensing.

Consequences

- Introduces Flowable dependencies and ACT_* tables in PostgreSQL.
- Requires modeling/deployment lifecycle (dev → staging → prod) for BPMN artifacts.
- Monitoring and operations need to include process instance metrics and job executor health.
- Ditch Camunda; do not introduce additional external workflow engines.

Implementation Notes

- The `workflow` module listens to domain events and starts/advances BPMN processes.
- Use timers and asynchronous jobs for SLAs and escalations.
- Use Flowable forms and task assignments for approvals (e.g., CAB) and service requests.
- Secure Flowable Modeler behind SSO; limit to admin roles; store models in Git‑backed repo.

Testing & Observability

- Use Flowable’s Spring test support to validate process definitions and task lifecycles.
- Emit Micrometer metrics for process starts, completions, SLA timers, and failures.
