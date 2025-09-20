Status: Draft

# Story
As an operations engineer,
I want live tile updates driven by event streams with caching,
so that dashboard widgets reflect near‑real‑time conditions efficiently.

## Acceptance Criteria
1. Consume metrics.updated (and relevant domain events) to update cache entries for live tiles; tiles read from cache with TTLs.
2. Tile types include counts, trend lines, SLA risk, backlog breakdown; each defines a refresh policy and data contract.
3. SSE/WebSocket endpoints stream tile updates to clients; polling fallback available with ETags and 304s.
4. Backpressure and rate limits in place; partitioning strategy for high‑volume events; retries/backoff on failures.
5. p95 tile fetch latency < 300ms from cache; streaming updates under 1s end‑to‑end after event arrival.

## Tasks / Subtasks
- [ ] Define tile contracts and cache keys/TTLs (AC: 1, 2)
- [ ] Event consumers and cache update logic (AC: 1)
- [ ] Streaming endpoints (SSE/WebSocket) and polling fallback (AC: 3)
- [ ] Backpressure, rate limiting, and partitioning config (AC: 4)
- [ ] Performance tests and metrics for latency SLAs (AC: 5)

## Dev Notes
- PRD: 2.10 Dashboards (live tiles)
- ADRs: 0006 event partitioning; 0011 cache store; 0001 event bus
- Architecture: observability-reliability.md; gateway-envoy.md for streaming
- APIs/Events: system.yaml (tiles), events.yaml (metrics.updated)

## Testing
- Event→cache update correctness; TTL expiry behavior
- SSE/WebSocket streaming correctness and fallback polling behavior
- Performance and rate limiting tests

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Live tiles via events        | PO     |

## Dev Agent Record

### Agent Model Used
<record at implementation time>

### Debug Log References
<links at implementation time>

### Completion Notes List
<notes at implementation time>

### File List
<files at implementation time>

## QA Results
<QA to fill>

