# ADR 0005 — Database‑Level Idempotency (WHERE version < ?)

Date: 2025-10-08
Status: Accepted
Owner: Developer Agent
Priority: High
Applies To: Command writes, read‑model updaters, outbox consumers

## Context
Our delivery is at‑least‑once (outbox → Redis Streams). Consumers may see duplicates and out‑of‑order events. We also use optimistic concurrency on command paths. We need a simple, uniform guard that prevents re‑applying stale changes without central dedupe state.

## Decision
Use a monotonic `version` per aggregate and enforce idempotency at the database layer by guarding writes with `WHERE version < :newVersion` (or equality for command writes). Treat “0 rows affected” as a benign duplicate/stale update and proceed without error.

### Read‑model/event reducer pattern (PostgreSQL)
```sql
-- Upsert with version guard
INSERT INTO ticket_card (id, status, version, updated_at)
VALUES (:id, :status, :v, now())
ON CONFLICT (id) DO UPDATE
SET status = EXCLUDED.status,
    version = EXCLUDED.version,
    updated_at = now()
WHERE EXCLUDED.version > ticket_card.version; -- drop stale/out‑of‑order
```

```sql
-- Pure update with guard
UPDATE ticket_card
SET status = :status,
    version = :v,
    updated_at = now()
WHERE id = :id
  AND version < :v; -- if 0 rows → duplicate/stale
```

### Read‑model/event reducer pattern (MySQL)
```sql
INSERT INTO ticket_card (id, status, version, updated_at)
VALUES (?, ?, ?, NOW())
ON DUPLICATE KEY UPDATE
  status = IF(VALUES(version) > version, VALUES(status), status),
  version = GREATEST(version, VALUES(version)),
  updated_at = IF(VALUES(version) > version, NOW(), updated_at);
-- Older versions become no‑ops; newer versions apply.
```

### Command path (optimistic concurrency)
```sql
UPDATE ticket
SET summary = :summary,
    version = :currentVersion + 1
WHERE id = :id
  AND version = :currentVersion; -- if 0 rows → retry/client precondition failed
```

## Consequences
- Idempotent by construction: duplicates and stale events are dropped at the DB.
- Works under concurrency without global locks; relies on row‑level atomicity.
- Requires a stored monotonic `version` per aggregate and consistent event ordering semantics.
- Telemetry should count affected rows to distinguish `applied` vs `duplicate/stale` results.

## Alternatives Considered
- Application‑level idempotency keys store: heavier state and invalidation logic.
- Hash‑based dedupe per aggregate/version: similar effect but duplicates DB logic in app code.

## Notes & Implementation Details
- Version must be monotonic per aggregate. Event producers set `version` in the outbox envelope; consumers use it as `:v`.
- For partial projections, prefer the `ON CONFLICT … DO UPDATE … WHERE EXCLUDED.version > current.version` shape when available.
- Surface zero‑row updates as `result=duplicate` in metrics, not as errors.

## References
- docs/architecture/eventing-and-timers.md
- docs/architecture/adr/0001-modulith-and-companions.md
- docs/architecture/adr/0003-redis-streams-vs-kafka.md
- docs/architecture/adr/0004-sse-vs-websocket.md
