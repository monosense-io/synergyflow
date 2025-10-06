# ADR 0003 — Redis Streams vs Kafka

Date: 2025-10-06
Status: Accepted

## Context
We need low-latency fan-out for UI updates and read model updates at modest scale (≤250 active users). Operational simplicity is a priority in Phase 1.

## Decision
Use Redis Streams (single stream, consumer groups) for event fan-out. Revisit Kafka when scale/throughput or durability needs exceed Redis’ comfort zone.

## Consequences
- Pros: Low latency, simple ops, already needed for caching/rate limiting.
- Cons: Weaker durability/retention guarantees than Kafka; future migration may be needed.

