Status: Draft

# Story
As an end user,
I want a notification preferences UI for channels, topics, quiet hours, and locale,
so that I receive the right messages at the right times.

## Acceptance Criteria
1. Preferences page shows per‑channel toggles, severity thresholds, topics/subscriptions, locale and time zone; quiet hours editor with validation.
2. Changes persist via backend APIs; effective preferences preview shows how a sample event would be delivered.
3. Digest settings configurable (frequency, channels); critical override explained; accessibility and responsive layout.
4. Error handling shows permission/rate‑limit messages gracefully; no accidental data loss on navigation.
5. Performance: save operations respond < 400ms; a11y AA compliance.

## Tasks / Subtasks
- [ ] Preferences form with channel toggles, topics, locale/tz, quiet hours (AC: 1)
- [ ] Save/load integration and effective preview (AC: 2)
- [ ] Digest settings and critical override copy (AC: 3)
- [ ] Error and UX resilience; a11y/responsive (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-11/02-preferences-subscriptions-apis.md
- PRD: 07 notification system — preferences/localization
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (preferences), users.yaml

## Testing
- Form validations; save/load; effective preview accuracy; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — notification preferences UI   | PO     |

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

