Status: Draft

# Story
As a notifications platform engineer,
I want a template and localization system for all channels (email, push, in‑app),
so that messages are consistent, localized, and easy to maintain.

## Acceptance Criteria
1. Template model supports named templates with variables, per‑channel parts (email: subject/body; push: title/body; in‑app: text/link), and locale variants with fallback to default.
2. Localization bundles managed per locale (keys/values) with interpolation and ICU‑style pluralization where needed.
3. Rendering service validates variables and produces channel‑specific payloads from template + localization + context; invalid/missing variables fail fast with diagnostics.
4. Versioning: templates are versioned; preview API renders a given version with sample context; diff available between versions.
5. CRUD APIs for templates and locales with RBAC (admin‑only modifications); audit entries for changes; event template.updated emitted on publish.

## Tasks / Subtasks
- [ ] Data models: NotificationTemplate (versions), LocaleBundle (AC: 1, 2, 4)
- [ ] Rendering service with variable validation and channel payload builders (AC: 3)
- [ ] Preview and diff endpoints; version publish flow (AC: 4)
- [ ] CRUD APIs with RBAC + audit; template.updated event (AC: 5)
- [ ] Localization fallback logic and tests (AC: 2)

## Dev Notes
- PRD: docs/prd/07-notification-system-design.md
- ADRs: 0001 event bus; 0006 event partitioning; 0008 mobile push egress policy; 0004 contract versioning (for template.updated)
- Architecture: gateway-envoy.md (admin routes), data-architecture.md
- APIs: docs/api/modules/system.yaml (templates), docs/api/modules/users.yaml (owner/admin), docs/api/modules/events.yaml (events)

## Testing
- Rendering with valid/missing variables; locale fallback; pluralization
- Version preview/diff; RBAC on CRUD; audit integrity; event contract for template.updated

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — template & localization       | PO     |

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

