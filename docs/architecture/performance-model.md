# Performance Model — SynergyFlow

Date: 2025-10-06
Owner: Architect

---

## SLOs and Budgets

- API p95: queue/list <200ms; side‑panel hydrate <300ms; CRUD <400ms; p99 <800ms.
- Realtime: E2E event propagation ≤2s p95; SSE reconnect catch‑up <1s.
- Timer precision: drift <1s; update/cancel p95 <50ms.
- Search: query p95 <300ms; index lag p95 <2s.
- Auth overhead: <10% request time; batch check 100 IDs p95 <3ms.

## Test Plan (Summary)

- Unit + integration baselines in CI; focus perf in S6 and S9.
- Gatling scenarios:
  - Queue list 50 cards with cursor pagination.
  - Side‑panel hydrate under mixed load.
  - CRUD write bursts + outbox fan‑out; consumer lag measurement.
  - SSE reconnect with replay cursor; idempotent reducer drop‑stale verification.
  - Timer churn: create/update/cancel cadence with SLA policy changes.

## Acceptance Gates

- Gate G2 (Alpha, end S6): all p95 budgets met in staging; SSE reconnect <1s; timer drift <1s.
- Gate G3 (RC, end S9): p99 <800ms; soak test 2h stable; zero missed timers; zero duplicate reducer applies.

## Observability Signals

- Traces: queue, side‑panel, CRUD, SSE push.
- Metrics: p50/95/99 latencies; SSE reconnects; reducer stale‑drop count; outbox lag; stream consumer lag; timer drift.
- Logs: state transitions, WIP violations, auth batch anomalies.

