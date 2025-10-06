# Project Plan — SynergyFlow (Enterprise ITSM & PM Platform)

**Owner:** Product Manager (PM)
**Date:** 2025-10-06
**Version:** 1.0 (Draft)
**Horizon:** 20 weeks (10 sprints × 2 weeks)

---

## Objectives (Outcome-Oriented)

- Deliver functional ITSM (incidents, requests, agent console, SLA) and PM (issues, boards, sprints) modules aligned to PRD v3.0 (`docs/product/prd.md`).
- Hit performance budgets: queue/list p95 <200ms; side‑panel hydrate p95 <300ms; event E2E ≤2s p95.
- Achieve 95%+ SLA compliance in pilot; maintain sprint velocity variance <15%.
- Ship GA by end of Sprint 10 with basic reporting and audit trail enabled.

## Scope

In scope:
- Epic 1 (Foundation & Infrastructure), Epic 2 (Core ITSM), Epic 3 (Core PM), Epic 4 (Workflow Engine), Epic 5 (Unified Dashboard & Realtime), Epic 6 (SLA Reporting).
- Kubernetes deployment with Envoy Gateway; PostgreSQL, Redis Streams, OpenSearch, Keycloak.

Out of scope (Phase 2+):
- Advanced analytics, multi-tenant admin portal, marketplace integrations.

## Deliverables (by epic)

- Tech specs: `docs/epics/epic-1-foundation-tech-spec.md`, `docs/epics/epic-2-itsm-tech-spec.md`, `docs/epics/epic-3-pm-tech-spec.md`, `docs/epics/epic-4-workflow-tech-spec.md`.
- OpenAPI contracts (ITSM/PM/Workflow/SSE) — to be added under `docs/api/`.
- CI/CD with quality gates; Flyway migrations; event outbox; SLA timers; SSE gateway.

## Timeline Summary

- Execution window: 2025-10-06 → 2026-02-27 (10 two‑week sprints; holiday-aware).
- Detailed schedule and sprint goals: `docs/planning/sprint-plan.md`.

## Milestones

- M1: Foundational stack up (end Sprint 2, 2025-10-31).
- M2: ITSM + PM alpha (end Sprint 6, 2025-12-26) — holiday risk, feature-light.
- M3: Workflow + Unified dashboard beta (end Sprint 8, 2026-01-30).
- M4: RC performance gate (end Sprint 9, 2026-02-13).
- M5: GA + handover (end Sprint 10, 2026-02-27).

## Success Criteria & Gates

- Authenticated health OK; migrations applied; 250 concurrent user perf model holds.
- Queue/list p95 <200ms, side‑panel p95 <300ms in staging; event lag ≤2s p95.
- UAT pass rate ≥95%; Sev‑1/2 defects ≤3 open at GA.

## Communication & Cadence

- Standup (daily), Sprint planning (bi‑weekly), Sprint review/demo (bi‑weekly), Retro (bi‑weekly), Architecture council (weekly), Release triage (weekly during S8–S10).

## Change Control

- PRD change proposals logged against `docs/product/prd.md` with impact notes; triaged in planning; epics re‑baselined only at sprint boundaries unless Sev‑1 risk.
