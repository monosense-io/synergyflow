Status: Draft

# Story
As a release planner,
I want an interactive change calendar UI with conflict visualization and governed overrides,
so that I can schedule safely and document exceptions.

## Acceptance Criteria
1. Calendar UI displays scheduled changes with filters (service, team, environment, risk tier) and supports month/week/day views.
2. **Conflict Interaction**:
   - A conflict is indicated with a clear visual marker on the change event.
   - **Hovering** over the marker shows a simple tooltip with the conflict name (e.g., "Production Freeze").
   - **Clicking** the change event opens a stable view (e.g., side panel) with full conflict details and the "Request Override" CTA.
3. **Override Workflow & Audit**:
   - **Authorized users** can request an override via the conflict details view. The UI enforces providing a rationale.
   - For **unauthorized users**, the override CTA is disabled. A tooltip explains the required role/permission and guides the user on next steps.
   - After an override is approved, the change event displays a permanent icon (e.g., a shield). Clicking this icon shows the read-only audit trail (who, when, why).
4. **Information Density & Performance**:
   - Performance p95 < 1s for a month view with 1000 items.
   - To manage visual density, if a day has more than 3 events, the UI displays an aggregated item (e.g., "+5 more"). Clicking this item navigates to the day view or opens a popover listing all events.
5. **Accessibility & Usability**:
   - Fully keyboard navigable.
   - Meets WCAG AA contrast and ARIA label standards.
   - Responsive layout for common screen sizes.
   - All interactions are timezone-aware and DST-safe.

## Tasks / Subtasks
- [ ] Calendar component with virtualized rendering and filters (AC: 1, 4)
- [ ] Conflict badge/tooltip UI wired to conflict API (AC: 2)
- [ ] Create/update window forms with validation; override modal with rationale (AC: 3)
- [ ] TZ/DST utilities and tests (AC: 4)
- [ ] Accessibility and responsive checks (AC: 5)

## Dev Notes
- Backend dependencies: docs/stories/backend/epic-02/03-change-calendar-conflicts-overrides.md
- PRD: 2.3 change calendar
- Architecture: API gateway policies (docs/architecture/gateway-envoy.md), UX standards (docs/ux/README.md)
- ADRs: 0007 gateway placement; 0010 Envoy gateway adoption
- APIs: changes.yaml calendar endpoints; cmdb.yaml for conflict details

## Testing
- UI tests for filters, navigation, and conflict details
- Form validation; override flows; RBAC simulated via mocks
- Performance tests (virtualization) and a11y checks

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — calendar UI for Epic 02     | PO     |

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

