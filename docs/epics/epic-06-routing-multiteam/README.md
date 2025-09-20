---
id: epic-06-routing-multiteam
title: Epic 06 — Intelligent Routing & Multi‑Team Support
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 06 — Intelligent Routing & Multi‑Team Support

## Epic Goal
Provide skill-, category-, SLA- and availability-aware routing across global teams with governed escalation, aligned to PRD 4.x.

## Scope
- In scope: Team hierarchy, skills matrices, workload balancing, priority scoring, timezone alignment, escalation rules.
- Out of scope: Non-ticket workloads.

## Key Requirements
- PRD 4: Hierarchy (Org→Dept→Team→Agent), skill and workload-based routing, SLA optimization, business hours/timezone awareness, escalation tiers and triggers.

Refs: docs/prd/04-multi-team-support-intelligent-routing.md

## Architecture & ADR References
- Module boundaries: docs/architecture/module-architecture.md
- Event partitioning for scale: docs/adrs/0006-event-publication-partitioning-retention.md
- Shared foundations: docs/adrs/0002-user-as-only-shared-foundation-module.md
- Event bus: docs/adrs/0001-event-only-inter-module-communication.md

## API & Events Impact
- APIs: docs/api/modules/teams.yaml, docs/api/modules/users.yaml, docs/api/modules/incidents.yaml
- Events: ticket.routed, ticket.escalated, capacity.updated

## Data Model Impact
- Entities: Team, AgentProfile, Skill, Capacity, RoutingPolicy, EscalationRule.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 01 (Incidents) and Epic 04 (Catalog) as routing consumers; Epic 11 (Notifications); Epic 12 (Security) for role checks.

## Risks & Mitigation
- Routing bias → continuous evaluation with SLA/CSAT feedback loop.
- Capacity misestimation → real-time signals with hysteresis and caps.

## Acceptance Criteria (Epic)
- Priority scoring implemented; routing within SLA and capacity constraints; escalations trigger correctly.

## Story Seeds
1) Data model for teams, skills, capacity.
2) Scoring engine and policy evaluator.
3) Escalation tiers and triggers.
4) Telemetry feedback loop from SLA/CSAT.

## Verification & Done
- Simulation tests for routing; SLA conformance reports.

