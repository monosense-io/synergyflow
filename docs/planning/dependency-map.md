# Dependency Map — SynergyFlow

**Owner:** Product Manager (PM)
**Date:** 2025-10-06
**Version:** 1.0 (Draft)

---

## Epic Sequencing (Critical Path)

1) Epic 1 — Foundation →
2) Epic 2 — ITSM (parallel with Epic 3) →
3) Epic 3 — PM (parallel with Epic 2) →
4) Epic 4 — Workflow Engine →
5) Epic 5 — Unified Dashboard & Realtime →
6) Epic 6 — SLA Reporting → GA

## Internal Dependencies

- Modulith boundaries and outbox (Epic 1) required before read‑model heavy features (Epics 2–3).
- Timer service (Epic 1) required before SLA UI/countdowns (Epic 2) and workflow escalations (Epic 4).
- SSE gateway (Epic 1) needed for unified dashboard (Epic 5).

## External/Platform Dependencies

- Keycloak realm/clients → JWT validation, roles, batch auth.
- PostgreSQL 16+ → OLTP + read models; Flyway migrations.
- Redis 7.x → Streams for fan‑out; rate‑limit policies.
- OpenSearch → Correlation/search; optional until S8–S9.

## Gates

- Gate G1 (end S2): foundation ready
- Gate G2 (end S6): alpha perf + stability
- Gate G3 (end S9): RC perf budgets

