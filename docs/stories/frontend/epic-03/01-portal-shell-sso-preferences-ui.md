Status: Draft

# Story
As an end user,
I want a portal shell with SSO login and persisted preferences (theme, locale, notifications),
so that I have a personalized, accessible experience.

## Acceptance Criteria
1. SSO login/logout flow integrated; session status reflected in UI; protected routes redirect to login.
2. Preferences panel allows changing theme, locale, and notification settings; persists via backend; defaults applied at first login.
3. Branding (logo/colors) and localization loaded at startup; language switch updates UI strings.
4. Accessibility: keyboard navigation, ARIA labels, contrast; responsive layout (mobile/desktop).
5. Performance: initial load p95 < 2s on 3G Fast; route transitions p95 < 500ms.

## Tasks / Subtasks
- [ ] Auth guard, login/logout actions, session indicator (AC: 1)
- [ ] Preferences UI + integration with backend (AC: 2)
- [ ] Branding and i18n loader; language switcher (AC: 3)
- [ ] a11y and responsive checks (AC: 4)
- [ ] Performance budget and lazy loading (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-03/01-portal-sso-preferences.md
- PRD: 2.5 Portal — SSO, preferences, localization
- Architecture: gateway-envoy.md for auth flows; UX standards docs/ux/README.md
- ADRs: 0007 gateway
- APIs: users.yaml (preferences/profile), system.yaml (branding/localization)

## Testing
- E2E auth flows; preferences persistence; i18n switching
- a11y audit; performance traces; responsive tests

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — portal shell (Epic 03)        | PO     |

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

