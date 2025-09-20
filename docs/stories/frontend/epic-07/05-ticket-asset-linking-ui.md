Status: Draft

# Story
As a service desk agent,
I want to search and link assets to tickets,
so that incident/change/request handling includes accurate asset context and impact.

## Acceptance Criteria
1. Ticket UI includes an Asset field with search (type, owner, serial, hostname) and select; supports multiple assets where appropriate.
2. Linked assets display in the ticket with key fields and quick link to asset detail; unlinked by authorized users with rationale.
3. Impact preview shows related CIs/services (from CMDB) when available; indicates potential risk.
4. Accessibility and performance: search p95 < 500ms; keyboard-friendly selection.
5. RBAC: only authorized roles can link/unlink; actions audited.

## Tasks / Subtasks
- [ ] Asset search and selection component (AC: 1, 4)
- [ ] Linked asset display with quick links and unlink action (AC: 2)
- [ ] Impact preview integration (CMDB) (AC: 3)
- [ ] RBAC UI states and audit capture hooks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-07/02-asset-model-ownership-lifecycle-tracking.md (linking), Epic 08 CMDB for impact
- PRD: 2.8 asset linking; 2.9 CMDB impact for preview
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0005 CMDB impact handshake; 0007 gateway
- APIs: cmdb.yaml/itam module; incidents.yaml; service-requests.yaml; changes.yaml

## Testing
- Search behavior; linking/unlinking with RBAC; impact preview; performance and a11y

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — ticket asset linking UI (07)  | PO     |

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

