Status: Draft

# Story
As an end user,
I want an in‑app notification center with a bell, unread counts, and a feed,
so that I can see recent updates and act quickly without leaving the app.

## Acceptance Criteria
1. Bell icon shows unread count; opens a panel with recent notifications (grouped by time/topic) and quick actions (open item, mark read/unread, dismiss).
2. Real‑time updates via SSE/WebSocket; polling fallback; performance target: update shows in UI within 1s of dispatch.
3. Preferences respected (channel enablement, topics, quiet hours); localization applied; accessible keyboard navigation.
4. Deep links open related items (ticket, change, release) in appropriate modules; errors handled gracefully.
5. Audit: read/unread/dismiss actions tracked for analytics; privacy respected.

## Tasks / Subtasks
- [ ] Bell + panel UI with unread count and grouping (AC: 1)
- [ ] Real‑time updates and fallback; performance (AC: 2)
- [ ] Preference integration, i18n, a11y (AC: 3)
- [ ] Deep links and error handling (AC: 4)
- [ ] Client‑side action logging to backend (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-11/03-channel-adapters-email-push-inapp.md (in‑app channel)
- PRD: 07 notifications — in‑app
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md (streaming)
- ADRs: 0007 gateway
- APIs: system.yaml (in‑app feed), events.yaml (stream)

## Testing
- Real‑time updates; read/unread/dismiss flows; grouping; a11y; performance

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — in‑app notification center UI    | PO     |

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

