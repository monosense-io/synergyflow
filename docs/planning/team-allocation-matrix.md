# Team Allocation Matrix — SynergyFlow

**Owner:** Product Manager (PM)
**Date:** 2025-10-06
**Version:** 1.0 (Draft)

---

## Teams & Roles

- Team A — ITSM (Sprints 3–6 primary)
  - 2 Backend Engineers
  - 1 Frontend Engineer
  - Focus: Tickets/Incidents/Requests, Agent Console, Routing/SLA.

- Team B — PM (Sprints 3–6 primary)
  - 2 Backend Engineers
  - 1 Frontend Engineer
  - Focus: Issues/Boards/Sprints, Metrics.

- Shared
  - 1 Infrastructure Engineer (K8s, CI/CD, observability)
  - 1 QA Engineer (ATDD/E2E/perf), expands to 2 during S7–S10
  - Architect (modulith boundaries, performance)
  - Product Manager (backlog, scope, stakeholders)

## RACI (High-Level)

- Requirements (PRD & backlog): PM (R), Architect (A), Teams (C), Stakeholders (I)
- Architecture & boundaries: Architect (R/A), Teams (C), PM (I)
- Implementation (Epics 2–5): Teams (R), Architect (C/A for gates), PM (I)
- Testing (unit/integration/E2E/perf): QA (R), Teams (C), Architect (A for perf), PM (I)
- Releases: Infra (R), Architect (A), PM (C), Teams (I)

## Capacity Notes

- Baseline velocity: 40 pts/sprint; expect reduced output in Sprint 6 (holidays).
- Add QA headcount for S7–S10 to meet Beta→GA quality bar.

