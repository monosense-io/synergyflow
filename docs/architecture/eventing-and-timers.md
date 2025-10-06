# Eventing & Timers — SynergyFlow

Date: 2025-10-06
Owner: Architect

---

## Outbox Schema & Semantics

Table: `outbox`

- `aggregate_id` (UUID), `aggregate_type` (e.g., Ticket, Issue)
- `event_type` (e.g., TicketCreated, IssueStateChanged)
- `version` (monotonic per aggregate)
- `occurred_at` (event time), `payload` (JSONB), `processed_at`

Payload envelope:
```json
{
  "id": "<aggregateId>",
  "type": "TicketStateChanged",
  "version": 12,
  "occurredAt": "2025-10-06T17:12:34Z",
  "payload": { "status": "In Progress", "assigneeId": "..." }
}
```

Delivery: at‑least‑once. Consumers must reduce idempotently and drop stale `version`.

## Stream Topology

1) Backend writes domain + outbox within one transaction.
2) Event Worker polls `outbox` (visibility window + batching) → appends to Redis Stream `sf.events` with message key `{aggregateId}:{version}`.
3) Consumers:
   - Backend SSE component → push deltas to clients (filter by scope).
   - Read model updaters → project into `ticket_card`, `issue_card`, `sprint_summary`.
   - Search Indexer → update OpenSearch documents.

Redis Streams: one stream per domain (`sf.events`), consumer groups `app-sse`, `read-model`, `indexer`.

## Idempotency & Versioning

- Each client reducer maintains `{id -> version}`; ignores events with `version <= current`.
- Server endpoints support `If-Match: <version>` for optimistic writes.

## SLA Timer Model

Entities:
- `SlaPolicy(priority, response_due, resolve_due, calendar)`
- `BusinessCalendar(workdays, holidays, working_hours)`
- `TimerInstance(id, subjectId, dueAt, status, pauseWindows[])`

Operations (O(1)):
- `createOrUpdate(subjectId, newDueAt)`
- `pause(subjectId)` / `resume(subjectId)` (calendar‑aware)
- `cancel(subjectId)`

Precision: drift <1s; missed timers = 0 (use durable queue + clock skew guards).
