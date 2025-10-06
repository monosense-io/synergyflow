# Sprint Plan — SynergyFlow
**Owner:** Product Manager (PM)
**Date:** 2025-10-06
**Version:** 1.0 (Draft)
**Velocity Baseline:** 40 pts/sprint (team baseline); parallel tracks begin Sprint 3

---

## Sprint Schedule (10 × 2 weeks)

- Sprint 1 — 2025-10-06 (Mon) → 2025-10-17 (Fri)
  - Goals: Repo bootstrap, CI/CD skeleton, dev docker-compose, health endpoint, Flyway baseline, Keycloak stub config.
  - Exit: /actuator/health OK with JWT; initial DB up.

- Sprint 2 — 2025-10-20 (Mon) → 2025-10-31 (Fri)
  - Goals: Outbox → Redis Stream, SSE gateway skeleton, Timer/SLA service scaffold, Modulith boundaries, basic read models.
  - Exit: Event fan-out visible; timer fires demo; read model populated.

- Sprint 3 — 2025-11-03 (Mon) → 2025-11-14 (Fri)
  - Track A (ITSM): Ticket domain + CRUD; queue card read model; Agent Console shell.
  - Track B (PM): Issue domain + CRUD; board shell.
  - Exit: Create/read/update tickets/issues; queue/board render demo data.

- Sprint 4 — 2025-11-17 (Mon) → 2025-11-28 (Fri) [US Thanksgiving 11/27]
  - ITSM: Composite side-panel API; routing rules engine MVP; SLA countdown UI.
  - PM: Sprint model; board moves; basic metrics.
  - Exit: End-to-end incident flow with auto-assignment; board moves persisted.

- Sprint 5 — 2025-12-01 (Mon) → 2025-12-12 (Fri)
  - ITSM: Suggested Resolution MVP; metrics dashboard cards (MTTR/FRT/SLA%).
  - PM: Burndown; backlog view; card intelligence hooks.
  - Exit: Demo: “Server Down” scenario with resolution + metrics update.

- Sprint 6 — 2025-12-15 (Mon) → 2025-12-26 (Fri) [Holiday impact]
  - Hardening/Light features: Perf passes on queue/side-panel; auth batch checks; accessibility pass; bugfix.
  - Exit: Alpha tag; freeze heavy scope due to holidays.

- Sprint 7 — 2026-01-05 (Mon) → 2026-01-16 (Fri)
  - Workflow Engine: approvals UI + rules; timers integration; audit trail.
  - Unified Dashboard: live tiles across ITSM/PM; SSE resiliency.
  - Exit: Approve/reject flows; unified view updates in real-time.

- Sprint 8 — 2026-01-19 (Mon) → 2026-01-30 (Fri)
  - Workflow: approval bundles, simulation basics; policy × budget × inventory strip (stub data).
  - Realtime: reconnect cursor + idempotent reducers; perf tuning.
  - Exit: Beta tag; UAT cohort onboarded.

- Sprint 9 — 2026-02-02 (Mon) → 2026-02-13 (Fri)
  - Reporting: SLA reporting; search/related incidents; stability.
  - Perf/Scale: p95/p99 target validation; soak testing.
  - Exit: Release Candidate; perf gates pass in staging.

- Sprint 10 — 2026-02-16 (Mon) → 2026-02-27 (Fri) [US Presidents Day 2/16]
  - GA prep: docs, runbooks, backup/restore drills, observability, security review.
  - Exit: GA release + handover.

## Milestone Mapping

- M1 (S2 end): Foundation up
- M2 (S6 end): Alpha
- M3 (S8 end): Beta
- M4 (S9 end): RC
- M5 (S10 end): GA

## Notes

- Holiday risks: Thanksgiving (Nov 27, 2025), winter holidays (late Dec), Presidents Day (Feb 16, 2026).
- Adjust velocity/scope accordingly; keep S6 as hardening-heavy.

