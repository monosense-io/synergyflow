Status: Draft

# Story
As an integration engineer,
I want a webhook subscription admin UI,
so that partners can self‑serve subscriptions and monitor deliveries.

## Acceptance Criteria
1. Create/edit subscription with callback URL, topics, secret (copy‑once), retry policy; verification challenge flow with status.
2. Delivery log shows recent attempts with status, latency, and error summaries; filters and pagination; retry action (admin‑only) where safe.
3. Security: URL allow/deny warnings; rate limit preview; RBAC; secrets never displayed after creation.
4. Notifications for repeated failures opt‑in; export deliveries CSV; a11y AA; performance responsive.
5. Real‑time updates when deliveries occur; polling fallback.

## Tasks / Subtasks
- [ ] Subscription forms and verification flow (AC: 1)
- [ ] Delivery log with filters, retry action, export (AC: 2, 4)
- [ ] Security warnings; RBAC; secret handling (AC: 3)
- [ ] Notifications opt‑in; real‑time updates (AC: 4, 5)
- [ ] a11y/perf checks (AC: 4)

## Dev Notes
- Backend: docs/stories/backend/epic-15/03-webhook-subscription-service-retries-signing.md
- PRD: 5 integration — webhooks/events
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway; 0004 event versioning
- APIs: system.yaml (subscriptions), events.yaml (deliveries stream)

## Testing
- Verification flows; delivery logs; retry/export; RBAC; real‑time updates; a11y/perf

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — webhook subscription UI (15)     | PO     |

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

