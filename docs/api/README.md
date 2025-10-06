# API Contracts — SynergyFlow

Status: Stubs (v0.1.0)

---

## Versioning Strategy

- External/public endpoints use path versioning: `/v1/...` at GA.
- During development, keep unversioned paths but tag OpenAPI `info.version`.
- Breaking changes → bump major; additive → minor; docs/typos → patch.

## Auth & Scoping

- OIDC JWT; project/team scopes enforced server‑side on all queries.

## Cursor & Replay Semantics

- Queue endpoints accept `cursor` (opaque). Server returns `nextCursor`.
- SSE accepts optional `cursor` (last seen sequence). Server replays gap then switches to live.
- Events include `{id, version}`; clients reduce idempotently by dropping stale versions.

## Files

- `itsm-api.yaml` — Tickets, queue, side‑panel composite
- `pm-api.yaml` — Issues, board moves, sprints, metrics
- `workflow-api.yaml` — Approvals, rules, batch authorization
- `sse-api.yaml` — Stream subscription and cursor
 - `error-catalog.md` — Standard error schema and common codes
