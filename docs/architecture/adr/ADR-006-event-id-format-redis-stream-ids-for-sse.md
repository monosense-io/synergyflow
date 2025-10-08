# ADR 0006 — Event ID Format (Redis Stream IDs for SSE)

Date: 2025-10-08
Status: Accepted
Owner: Developer Agent
Priority: High
Applies To: SSE bridge, event worker, UI clients

## Context
We broadcast domain deltas to clients via Server‑Sent Events (SSE). Clients use `Last-Event-ID` for replay after reconnects. Redis Streams assigns canonical message IDs like `1700000000000-5` (`<unix_ms>-<seq>`). We need a single, stable ID format end‑to‑end.

## Decision
Use the Redis Stream message ID as the SSE `id` verbatim. Treat it as an opaque, monotonically increasing cursor per stream.

### Rules
- Stream key: `sf.events` (domain aggregate deltas). Future domain streams must follow `sf.events.<domain>`.
- `XADD *` returns the ID; pass it through unchanged as SSE `id:`.
- SSE reconnection must honor the `Last-Event-ID` header by querying Redis for messages with IDs `>` that value and replaying up to a bounded window.
- Do not attempt to parse the ID on the client; treat as an opaque string. Servers may compare lexicographically per Redis semantics.
- Include `event_type` and any domain identifiers in the SSE `data` payload; keep `id` purely as a cursor.

### Example (SSE frame)
```
id: 1728400000000-5
event: TicketStateChanged
retry: 3000
data: {"aggregate_id":"…","version":12,"status":"IN_PROGRESS"}

```

### Replay
- On connect with `Last-Event-ID: <id>`, the SSE service issues `XRANGE sf.events (<id> +] COUNT N)` (or `XREAD` with a group) and streams results to the client, then switches to live consumption.
- If the ID is unknown (evicted/truncated), fall back to sending a full snapshot or instruct the client to reload.

## Consequences
- Uniform, monotonic cursor for replay across services.
- No bespoke ID mapping layer; simpler observability and troubleshooting.
- Tight coupling to Redis ID semantics is acceptable given ADR 0003; future Kafka migration would require an adapter translating offsets to opaque IDs.

## Alternatives Considered
- ULID/UUIDv7 as event IDs: globally unique but adds mapping from stream offsets to IDs for replay.
- Per‑aggregate cursors: complicates fan‑out and reconnection logic.

## References
- docs/architecture/adr/0003-redis-streams-vs-kafka.md
- docs/architecture/adr/0004-sse-vs-websocket.md
- docs/architecture/eventing-and-timers.md
