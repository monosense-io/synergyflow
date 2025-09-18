---
id: arch-02-module-architecture
title: Module Architecture (Spring Modulith)
owner: Architect
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/03-system-architecture-monolithic-approach.md
  - ../event-catalog.md
  - ../prd/24-spring-modulith-architecture-compliance-summary.md
---

## Principles
- Event-only inter-module communication (no direct deps between ITIL modules).
- Only 'user' is a shared foundation module; 'team' depends on 'user' only.
- Named interfaces limited to external tooling; internal flow via events.

## Modules (Bounded Contexts)
- ITIL: incident, problem, change, knowledge, cmdb
- Core: user, team
- Infra: notification, workflow, integration

## Allowed Dependencies
- user: none
- team: user::api
- others: zero direct module dependencies (events only)

## Event Handshakes
- Change↔CMDB: ChangeImpactAssessmentRequestedEvent → ChangeImpactAssessedEvent (approval gating)

## Compliance
- ApplicationModules.verify() must pass in CI.
- Modulith actuator enabled for runtime inspection.

## Module Dependencies (Mermaid)
```mermaid
flowchart LR
  user[user]
  team[team] -->|api| user
  incident[incident]
  problem[problem]
  change[change]
  knowledge[knowledge]
  cmdb[cmdb]
  notification[notification]
  workflow[workflow]
  integration[integration]

  %% Event-only communication; edges omitted intentionally for synchronous deps
  %% Use Event Catalog for cross-module flows
```
