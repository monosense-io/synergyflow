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

Payload envelope (lowercase_with_underscores keys):
```json
{
  "aggregate_id": "<uuid>",
  "aggregate_type": "TICKET",
  "event_type": "TicketStateChanged",
  "version": 12,
  "occurred_at": "2025-10-06T17:12:34Z",
  "schema_version": 1,
  "payload": { "status": "IN_PROGRESS", "assignee_id": "<uuid>" }
}
```

DB mapping:
- Envelope `payload` -> column `event_payload` (JSONB)
- Worker selection index: `idx_outbox_unprocessed` (partial, `processed_at IS NULL`)

Delivery: at‑least‑once. Consumers must reduce idempotently and drop stale `version`.

## Stream Topology

1) Backend writes domain + outbox within one transaction.
2) Event Worker polls `outbox` (visibility window + batching) → appends to Redis Stream `sf.events` with message key `{aggregateId}:{version}`.
3) Consumers:
   - Backend SSE component → push deltas to clients (filter by scope).
   - Read model updaters → project into `ticket_card`, `issue_card`, `sprint_summary`.
   - Search Indexer → update OpenSearch documents.

Redis Streams: one stream per domain (`sf.events`), consumer groups `app-sse`, `read-model`, `indexer`.

## Metrics (Publisher)

Micrometer meters emitted by the outbox write-path (implemented in Story 1.5):
- Counter `outbox_published_total` tags: `aggregate_type`, `event_type`, `result` (success/duplicate/error)
- Counter `outbox_publish_errors_total` tags: `aggregate_type`, `event_type`, `result` (duplicate/data_integrity/exception type)
- Timer `outbox_publish_latency_ms` tags: `aggregate_type`, `event_type`

Logging keys (structured): `aggregate_id`, `aggregate_type`, `event_type`, `version`, `latency_ms`

Implementation: `EventPublisherImpl` (backend/src/main/java/io/monosense/synergyflow/eventing/internal/)

## Idempotency & Versioning

- Each client reducer maintains `{id -> version}`; ignores events with `version <= current`.
- Server endpoints support `If-Match: <version>` for optimistic writes.

### Consumer Guidance (Cross‑Module)

- Reducers MUST persist the last processed `{aggregate_id -> version}` and drop any event with `version <= current` for that aggregate.
- Consumers SHOULD be idempotent when applying state transitions; all updaters must tolerate duplicate deliveries.
- If a consumer cannot find the target aggregate, it SHOULD buffer/skip and retry rather than error‑fail the pipeline.

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

## Data Classification (Outbox Payloads)

- Do not place secrets or sensitive PII in `payload` unless strictly required.
- Prefer identifiers over full records (e.g., `user_id` instead of name/email).
- If PII is unavoidable, document the fields and apply field‑level encryption at rest (DB column encryption) and minimize retention.
- Payloads should be additive‑only to preserve backward compatibility; use `schema_version` for evolution.

## Publisher Retry Policy

- Duplicate events are detected via SQLState `23505` (unique violation) and surfaced as `DuplicateEventException` without retry.
- Transient DB errors (SQLState `08xxx`, `40001`, `40P01`) are retried up to 2 times with short backoff inside the publish operation.
- All other data integrity issues are not retried and are logged with `outbox_publish_errors_total`.
