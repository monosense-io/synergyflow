Status: Draft

# Story
As a partner developer,
I want a sandbox console UI for testing APIs and webhooks,
so that I can validate integrations end‑to‑end before going live.

## Acceptance Criteria
1. Generate sandbox credentials (scoped API key, webhook secret) with copy‑once; policies/time‑to‑live shown; RBAC enforced.
2. Webhook test receiver displays received events with signature verification status and payload; resend/acknowledge actions.
3. API test panel sends sample requests with auth and shows responses; rate limit/burst indicators visible.
4. Logs and metrics panel shows recent calls/deliveries; export for support; a11y AA.
5. Performance: UI responsive; real‑time updates via SSE/WebSocket; polling fallback.

## Tasks / Subtasks
- [ ] Credential generation UI with TTL and scopes (AC: 1)
- [ ] Webhook receiver and signature validator (AC: 2)
- [ ] API test panel with auth and rate indicator (AC: 3)
- [ ] Logs/metrics panel and export (AC: 4)
- [ ] Real‑time updates; a11y/perf (AC: 5)

## Dev Notes
- Backend: API clients/keys, webhooks, and governance stories
- PRD: 5 integration — partner certification
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway; 0004 event versioning
- APIs: system.yaml (sandbox credentials, logs), events.yaml (deliveries)

## Testing
- Credential TTL/permissions; webhook signature verification; API tests; logs export; a11y/perf

## Change Log
| Date       | Version | Description                                         | Author |
|------------|---------|-----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — partner integration sandbox UI (15) | PO     |

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

