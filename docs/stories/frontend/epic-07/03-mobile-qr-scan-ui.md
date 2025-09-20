Status: Draft

# Story
As a field technician,
I want a mobile-friendly QR/barcode scan UI,
so that I can quickly update asset status/location while on site.

## Acceptance Criteria
1. Mobile UI accesses camera (where permitted) to scan codes; manual entry fallback available.
2. After scan, shows asset preview and allows updating status/location/owner with required validations.
3. Works offline: queue scans locally and sync when online with idempotency; show sync status and errors.
4. Accessibility and performance: responsive on mobile; scan→submit flow under 2s online.
5. Security: requires authentication; least-privilege UI (only allowed actions shown).

## Tasks / Subtasks
- [ ] Scanner integration and manual entry fallback (AC: 1)
- [ ] Update forms and validations (AC: 2)
- [ ] Offline queue and sync with status UI (AC: 3)
- [ ] a11y/responsive checks; performance budget (AC: 4)
- [ ] Auth guard and role-based UI (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-07/04-mobile-barcode-qr-update-flows.md
- PRD: 2.8 barcode/QR mobile updates
- Architecture/UX: docs/ux/README.md; gateway-envoy.md (mobile policies)
- ADRs: 0007 gateway
- APIs: cmdb.yaml/itam module; users.yaml

## Testing
- Scan flows; offline queue + sync; validations; auth; performance; a11y/mobile

## Change Log
| Date       | Version | Description                               | Author |
|------------|---------|-------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — mobile QR scan UI (07)    | PO     |

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

