# ADR 0004 — SSE vs WebSocket

Date: 2025-10-06
Status: Accepted

## Context
Client updates are server → client broadcast with minimal upstream chatter; reconnection and HTTP infra compatibility matter.

## Decision
Use Server‑Sent Events (SSE) for realtime UI updates with replay cursor. No bidirectional requirement in Phase 1.

## Consequences
- Pros: Simpler infra (HTTP/2 friendly), automatic reconnection, fewer moving parts.
- Cons: Client → server push not supported; consider WebSocket or HTTP POST for upstream actions if needed.

