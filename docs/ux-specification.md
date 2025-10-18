# SynergyFlow UX Specification (MVP)

- Version: 0.1 (Draft)
- Date: 2025-10-18
- Author: monosense (UX)
- Sources: docs/PRD.md (2025-10-17), docs/architecture/architecture.md (2025-10-17)

> Purpose: Translate the PRD and Architecture into a practical, testable UX plan for the 10‑week MVP. Defines information architecture, key journeys, UI patterns, states, accessibility, and analytics to guide design and implementation.

---

## 1. Scope and Goals

### 1.1 MVP Goals (from PRD)
- Deliver Trust + UX Foundation Pack:
  - Event system visibility (Freshness Badges)
  - Single‑Entry Time Tray
  - Link‑on‑Action cross‑module linking
  - Policy Studio MVP + Decision Receipts
- Ship core ITSM/PM slices sufficient for pilot usage (Incidents, Tasks, Time tracking).
- Establish patterns that scale to 1,000 users while meeting governance requirements.

### 1.2 Non‑Goals (MVP)
- Full parity with all ITSM/PM epics.
- Advanced analytics, DR/HA admin surfaces, or full multi‑tenant SaaS controls.
- Native mobile apps (responsive web only).

---

## 2. Users and Roles

- IT Service Desk Agents: triage incidents, fulfill requests, collaborate with PM.
- Project Managers: manage projects, sprints, tasks, link to incidents/changes.
- End Users (Requesters): submit requests, search KB, track status.
- Admin/Policy Owners: configure RBAC and policies (OPA), review decision receipts.

Primary accessibility support: keyboard‑first agents, screen‑reader compatibility, color‑contrast guarantees.

---

## 3. Information Architecture

### 3.1 Global Navigation
- Home (Dashboard)
- Incidents
- Problems (Phase 2)
- Changes (Phase 2)
- Knowledge (Phase 2)
- Projects
- Tasks
- Time
- Policies
- Audit & Receipts
- Admin

Notes:
- Keep global nav stable across roles; hide items the user cannot access, but preserve predictable locations.
- Surface cross‑module relations inline via Link‑on‑Action and related panels instead of forcing app switches.

### 3.2 Cross‑Object Linking
- Every primary object (Incident, Task, Change, Release, Knowledge) exposes a Related panel.
- Link‑on‑Action creates and links related objects with prefilled context (see Journey J2). Bidirectional links required.

### 3.3 Search and Wayfinding
- Global command bar (Ctrl/Cmd+K): search entities, jump to pages, quick actions.
- Contextual breadcrumbs: App > Module > View > Object.
- Consistent filters and saved views per list.

---

## 4. Key Journeys (MVP)

Each journey includes goals, happy path, edge cases, states, and success metrics.

### J1. Single‑Entry Time Tray
- Goal: Log time once, mirror to related Incident and Task within 100–200 ms p95.
- Trigger: Global Time Tray toggle in the header.
- Happy Path:
  1) User opens tray (panel slides from right, focus lands on description field).
  2) Select related entities (auto‑detect current context; allow manual lookup).
  3) Enter duration/interval; submit.
  4) System mirrors worklog to linked records; toast confirms with entity IDs (e.g., "Time logged to INC‑1234, TASK‑890").
- Edge Cases:
  - Offline/network lag: show optimistic entry with syncing status; retry with exponential backoff.
  - Validation errors: inline messages under fields; never discard user input.
- States:
  - Loading/saving skeletons; progress bar for policy evaluations if applicable.
- Metrics:
  - p95 time‑to‑mirror, % mirrored within 200 ms, Time Tray adoption rate, error rate.

### J2. Link‑on‑Action (Cross‑Module Linking)
- Goal: Create a related object from the current context without context switching.
- Trigger: "Link on action" button next to primary actions (e.g., on Incident: Create Task, Create Change).
- Happy Path:
  1) Click Link‑on‑Action; modal opens prefilled with source context.
  2) Required fields validated live; submit creates the target object and bidirectional links.
  3) Inline Related panel updates instantly.
- Edge Cases:
  - Permission denied: show reason and policy decision receipt link.
  - Partial failure (link created, object failed): surface recovery action with retry.
- Metrics: mean time to create+link, usage frequency per module, failure rate, permission denial rate.

### J3. Freshness Badges (Eventual Consistency Transparency)
- Goal: Make projection lag visible and trustworthy.
- UI Spec:
  - Badge shows age since last projection update per view or widget.
  - Thresholds (PRD): <100 ms green; 100–500 ms yellow; >500 ms red.
  - Tooltip shows last update timestamp and source.
- Metrics: % views within green threshold, user trust survey score, help‑desk tickets about “stale data”.

### J4. Decision Receipts (Explainable Automation)
- Goal: Explain policy decisions and build governance confidence.
- UI Spec:
  - Inline banner when policy affects an action (approve/deny/modify).
  - "View receipt" opens side panel: policy name, inputs, evaluation result, rules matched, timestamps, actor, hash/id.
  - Share/download receipt; linkable for audits.
- Metrics: views per decision, appeal/retry actions, average review time.

### J5. Incident Lifecycle with SLA
- Goal: Efficient triage with SLA awareness and zero silent breaches.
- Highlights: SLA timer component, escalations, snooze/reassign, inline updates, keyboard shortcuts.
- Metrics: breach rate, response time p95, reassignment cycles, time‑to‑resolution.

---

## 5. Screen Inventory (MVP)

1. Home Dashboard
   - Widgets: My Queue, SLA at Risk, Recent Activity, Freshness overview, Time Today.
2. Incidents
   - List: density toggle, saved views, bulk actions, export.
   - Details: summary, properties, activity, Related panel, decision receipts.
3. Tasks
   - List + Details similar to Incidents; includes project/sprint context.
4. Time
   - Global Time Tray, personal timesheet view, week calendar, edit history.
5. Policies
   - List of policies, detail view with evaluation history and receipts.
6. Audit & Receipts
   - Searchable receipts list with filters (entity, action, policy, status, time).
7. Admin (MVP‑light)
   - Users/Roles overview; feature flags; policy synchronization status.

Wireframe guidance: prioritize content-first layouts, clear hierarchy, generous whitespace, and consistent component placements.

---

## 6. Core Interaction Patterns and Components

### 6.1 Foundations
- Density: Comfortable and Compact modes per user preference.
- Keyboard: Full navigation; shortcut cheat‑sheet via "?"; focus ring always visible; Escape closes modals.
- Undo: Where feasible, offer 5–10s undo for non‑destructive actions.

### 6.2 Components (Selected)
- Global Header
  - Left: product logo, global nav, command bar (Ctrl/Cmd+K).
  - Right: Time Tray toggle, inbox/notifications, avatar menu.
- List View
  - Columns: select, key fields, SLA/freshness badges; sticky header; column picker; server‑side sort/filter; saved views.
- Detail View
  - Tabs: Activity, Properties, Related, Receipts.
  - Sidebars: Related panel (Link‑on‑Action), metadata quick‑edit.
- Time Tray (Panel)
  - Fields: description, duration, date/time, related entities; optimistic UI; conflict detection.
- Freshness Badge
  - Visual: dot + ms label; color thresholds per PRD; tooltip details.
- SLA Timer
  - Countdown with states: normal, warning, breach; snooze/reassign CTA.
- Decision Receipt Panel
  - Sections: Summary, Inputs, Rules Matched, Outcome, Audit info; copy/share actions.
- Toasts & Inline Validation
  - Standardized variants (success, info, warning, error); concise microcopy.

---

## 7. Design System (MVP Tokens)

- Color tokens (WCAG AA): `--fg`, `--bg`, `--muted`, `--primary`, `--warning`, `--danger`, `--success`, `--info`.
- Elevation tokens: `--elev-1` (1–2dp), `--elev-2` (4dp), `--elev-3` (8dp).
- Radius tokens: `--radius-sm` (4px), `--radius-md` (8px), `--radius-lg` (12px).
- Spacing scale: 4‑point grid (4,8,12,16,24,32,48,64).
- Typography: System font stack; sizes 12–20px; 1.4–1.6 line height.
- Motion: 120–180ms ease‑out for entrances; prefers‑reduced‑motion respected.

Note: Visual brand theming will be layered on these tokens in Phase 2.

---

## 8. Accessibility (WCAG 2.1 AA)

- Keyboard support for all components; tab order follows visual hierarchy.
- ARIA labels for interactive elements; roles/landmarks (header, nav, main, aside, footer).
- Color contrast: 4.5:1 for text, 3:1 for large text and UI glyphs.
- Live regions for dynamic updates (e.g., time logged, policy evaluated, SLA breached).
- Focus management: send focus to first errored field on submit; restore focus after close.
- Testing: NVDA/JAWS/VoiceOver smoke tests on critical flows.

---

## 9. Responsive Layout

- Breakpoints: 320, 768, 1024, 1440.
- Mobile: single column; critical info above the fold (status, SLA, actions).
- Tablet: two columns; related panel collapsible.
- Desktop: three columns on wide screens; persistent Related panel.
- Touch: 44px min targets; swipe actions on mobile lists (archive, assign).

---

## 10. Performance and Reliability UX

- Skeletons for first paint; spinners under 400ms avoided (prefer skeleton/optimistic).
- Optimistic UI for Time Tray and Link‑on‑Action with rollback on error.
- Freshness source of truth: show badge and tooltip with last update timestamp.
- Degradation: when projections stale (red), show refresh CTA and explain implications.

Targets (from Architecture):
- API p95 <200ms; event lag <100ms (p95 <200ms); policy eval <100ms p95.

---

## 11. Security and Privacy UX

- RBAC awareness: disable with explanation; provide "Request Access" inline flow (Phase 2).
- Decision Receipts: always link from denials/auto‑actions; redact sensitive fields where required.
- Session: clear messaging on auth expiry; seamless re‑auth with redirect preservation.

---

## 12. Product Analytics and Telemetry

Events (redacted for PII, GDPR‑friendly by default):
- `time_tray.opened|submitted|mirror_success|mirror_error` (durations, entity_ids hashed)
- `link_on_action.opened|submitted|link_success|link_error`
- `freshness.view_rendered` with `ms_since_projection`
- `policy.receipt_viewed` with `policy_id`, `result`
- `incident.viewed|updated`, `task.viewed|updated`

KPIs:
- Adoption: Time Tray daily active users; Link‑on‑Action usage per user.
- Reliability: % mirrors <200ms; % freshness green; policy denial rate with receipts viewed.
- Outcomes: SLA breach rate; MTTR; time‑to‑triage.

---

## 13. Content Strategy and Microcopy

Tone: confident, concise, human. Prefer action‑first phrasing.

Examples:
- Success: "Time logged to INC‑1234, TASK‑890."
- Policy: "Action adjusted by policy ‘Change‑Minor‑AutoApprove’. View receipt."
- Error: "We couldn’t save this change. Your edits are safe. Try again."
- Empty: "No incidents yet. Create your first incident to get started."

---

## 14. Acceptance Criteria (MVP)

- A11y: Keyboard‑complete, screen‑reader labels, contrast passes.
- Performance: p95 thresholds met (API, events, policy, mirroring).
- Freshness badges accurate to within 50ms of projection update timestamps.
- Decision receipts accessible from any policy‑affected action.
- Time Tray mirrors to all linked entities within 200ms p95; failure recovery available.
- Link‑on‑Action establishes bidirectional links and prefilled forms.

---

## 15. Open Questions and Assumptions

- Do we expose policy simulation (“shadow mode”) in UI for Admins in MVP?
- Exact brand palette and typography for v1? (Tokens ready.)
- Which saved views ship by default per module?
- Receipt retention period and export format (JSON, PDF)?
- Localization: English‑only for MVP; confirm locales for Phase 2.

---

## 16. Delivery Plan (10 Weeks)

Week 1–2: Foundations
- Nav, command bar, list/detail scaffolds, tokens, a11y baseline.

Week 3–4: Time Tray + Freshness
- Global tray, mirroring pipeline UX, badges, skeletons.

Week 5–6: Link‑on‑Action + Related panel
- Cross‑module create/link, bidirectional links, receipts plumbing.

Week 7–8: Incident slice with SLA
- SLA component, states, shortcuts, escalations and snooze.

Week 9–10: Policies + Audit slice; hardening
- Receipt panel, audit search, telemetry, perf/a11y sweeps.

---

## 17. Appendices

- Glossary (PRD terms): Incident, Change, Task, Receipt, Freshness, Shadow Mode.
- References: docs/PRD.md, docs/architecture/architecture.md.
- Test heuristics: RITE testing each sprint; usability score target ≥75 SUS by Week 8.

*** End of UX Specification (MVP) ***
