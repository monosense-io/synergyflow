# Enterprise ITSM & Project Management Platform - Product Requirements Document (PRD)

**Project:** SynergyFlow (Enterprise ITSM & PM Platform)
**Version:** 3.0
**Date:** 2025-10-05
**Status:** Architecture Finalized - Ready for Development
**Author:** monosense (Product Manager)
**Delivery Model:** Waterfall with 2-week sprints (hybrid)
**Change Notes:** AI-augmented UX innovations + physics-based architecture refinements from brainstorming session

---

## Table of Contents

1. [Goals and Background Context](#goals-and-background-context)
2. [Requirements](#requirements)
3. [User Interface Design Goals](#user-interface-design-goals)
4. [Technical Assumptions](#technical-assumptions)
5. [Epic List](#epic-list)
6. [Epic Details](#epic-details)
7. [Success Metrics](#success-metrics)
8. [Next Steps](#next-steps)

---

## Goals and Background Context

### Goals

- **Deliver functional ITSM platform** with incident management, service requests, agent console, and SLA tracking matching ManageEngine ServiceDesk Plus core capabilities
- **Deliver functional PM platform** with issue tracking, Kanban boards, sprint management, and backlog views matching JIRA core capabilities
- **Enhance both platforms with simple workflow automation** including approval workflows, ticket routing rules, and automated state transitions
- **Achieve operational efficiency** through unified real-time dashboard showing ITSM tickets and PM tasks with live updates
- **Enable 95%+ SLA compliance** for ITSM incidents through automated SLA tracking and escalation
- **Maintain stable sprint velocity** (variance <15%) for PM through predictable sprint planning and burndown tracking
- **Provide enterprise-grade audit trail** with full history of ticket/issue changes and approvals
- **Support 250 concurrent users** (helpdesk agents, developers, managers) with real-time collaboration

### Background Context

Organizations using traditional ITSM tools (ManageEngine ServiceDesk Plus) and project management platforms (JIRA) face several critical limitations:

**ITSM Pain Points:**
1. **Rigid Approval Workflows:** Every service request requires manual approval, creating bottlenecks for low-risk requests
2. **No Intelligent Routing:** Tickets manually assigned instead of auto-routed based on category, priority, or team workload
3. **Poor Agent Experience:** Slow, portal-centric UIs not optimized for helpdesk agents handling 50+ tickets daily
4. **Limited Cross-Department Coordination:** IT service requests that require HR, Facilities, or Finance involvement become email coordination nightmares

**PM Pain Points:**
1. **JIRA Complexity Overload:** Designed for enterprise-scale software teams, overwhelming for smaller product teams
2. **Disconnected from Operations:** Project tasks have no integration with IT service management or operational workflows
3. **No Workflow Automation:** State transitions and approvals entirely manual, slowing delivery cycles

**The Opportunity:** Build a unified platform that combines best-of-breed ITSM and PM capabilities with intelligent workflow automation, eliminating coordination overhead while maintaining simplicity and speed.

This PRD defines a dual-module platform: **(1) ITSM Module** providing incident management, service request fulfillment, agent console, and SLA tracking; **(2) PM Module** providing issue tracking, board views, sprint management, and backlog prioritization; **(3) Shared Workflow Engine** enabling approval workflows, routing automation, and state transition rules across both modules.

The platform targets internal organizational use for companies up to 1,000 users (50 helpdesk agents, 200 developers, 750 general employees), prioritizing deep feature quality over massive scalability. This PRD focuses on Phase 3 (Implementation: 20 weeks) of a broader 32-40 week waterfall initiative, structuring development as parallel epics (ITSM Team + PM Team) that deliver incremental, testable functionality.

### Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2025-10-04 | 1.0 | Initial PRD creation (orchestration-first approach) | John (PM) |
| 2025-10-04 | 2.0 | **MAJOR PIVOT:** ITSM/PM-first architecture with simple workflow support | John (PM) |
| 2025-10-05 | 3.0 | **ARCHITECTURE FINALIZED:** Zero-Hunt Console, Developer Copilot Board, Modulith+Companions, CQRS-lite, Durable Timers, Batch Auth, OpenSearch integration | John (PM) + Architecture Team |

---

## Requirements

### Functional Requirements - ITSM Module

**FR-ITSM-1:** The system shall support creation, assignment, updating, and closure of Incidents with fields: title, description, priority (Low/Medium/High/Critical), severity (Sev 1-4), status (New/Assigned/In Progress/Resolved/Closed), assignee, requester, category, subcategory, and resolution notes.

**FR-ITSM-2:** The system shall support creation and fulfillment of Service Requests with fields: title, description, request type, status (Submitted/Pending Approval/Approved/Rejected/Fulfilled/Closed), requester, fulfiller, and approval history.

**FR-ITSM-3:** The system shall provide an Agent Console with multi-ticket queue view showing: ticket ID, type (Incident/Request), title, status, priority, assignee, SLA due date, and created date, with ability to filter by status, priority, assignee, and category.

**FR-ITSM-4:** The system shall support ticket assignment and reassignment with audit trail capturing: who assigned, when, to whom, and optional assignment notes.

**FR-ITSM-5:** The system shall calculate and display SLA due dates for incidents based on priority: Critical (2 hours), High (4 hours), Medium (8 hours), Low (24 hours), with visual countdown indicators showing time remaining.

**FR-ITSM-6:** The system shall auto-escalate incidents approaching SLA breach (< 25% time remaining) by notifying assignee and manager.

**FR-ITSM-7:** The system shall support ticket routing rules that auto-assign new incidents to teams/agents based on: category (Network → Network Team), priority (Critical → Senior Agents), or round-robin load balancing.

**FR-ITSM-8:** The system shall maintain ticket history showing all state changes, assignments, comments, and field updates with timestamp and actor.

**FR-ITSM-9:** The system shall support ticket comments/notes with ability to mark as internal (agent-only) or public (visible to requester).

**FR-ITSM-10:** The system shall provide service catalog with request templates (e.g., "Laptop Request", "Software Access Request") that pre-populate category, priority, and custom fields.

**FR-ITSM-11:** The system shall support bulk ticket operations allowing agents to: bulk assign, bulk update status, bulk add tags.

**FR-ITSM-12:** The system shall provide ticket search with filters: keyword search (title/description), status, priority, assignee, requester, date range, category.

**FR-ITSM-13:** The system shall support canned responses allowing agents to insert pre-written text for common scenarios (e.g., "Password reset instructions").

**FR-ITSM-14:** The system shall provide ITSM metrics dashboard showing: tickets created/resolved today, mean time to resolution (MTTR), first response time (FRT), SLA compliance rate, tickets by status breakdown.

**FR-ITSM-15:** The system shall export ticket data to CSV/Excel for reporting with configurable field selection.

**FR-ITSM-16 (NEW - Zero-Hunt Console):** The system shall provide a "Zero-Hunt Agent Console" with Loft Layout consisting of: (1) Priority Queue (left panel) showing ticket cards sorted by SLA risk → priority → ownership, (2) Action Canvas (center panel) displaying focused ticket with auto-loaded context, (3) Autopilot Rail (right panel) with Next Best Action button and "Why?" explainability panel.

**FR-ITSM-17 (NEW - Suggested Resolution):** The system shall generate AI-powered Suggested Resolutions for tickets including: draft reply with variable substitution, fix steps/scripts, linked KB article highlights, related incidents with similarity scores, affected asset card with change history, and confidence score with signal explanation.

**FR-ITSM-18 (NEW - Composite Side-Panel API):** The system shall provide composite side-panel endpoint returning in single API call: requester history (last 5 tickets, satisfaction score), related incidents (top 3 by similarity), KB suggestions (top 3 relevant articles), asset information (current state, recent changes, owner), and compliance checks (approval requirements, data egress flags).

**FR-ITSM-19 (NEW - Contextual Prefetch):** The system shall implement predictive prefetching where focusing on queue row i triggers background fetch of side-panel data for rows i±3, maintaining <300ms p95 latency for side-panel hydration.

**FR-ITSM-20 (NEW - Keyboard-First Actions):** The system shall support keyboard shortcuts: J/K (navigate queue), Enter (open ticket), Ctrl+Enter (Approve & Send), A (assign), E (escalate), / (spotlight search), with shortcuts discoverable via ? help overlay.

---

### Functional Requirements - PM Module

**FR-PM-1:** The system shall support creation, assignment, updating, and closure of Issues with fields: title, description, type (Story/Task/Bug/Epic), status (Backlog/To Do/In Progress/In Review/Done), assignee, reporter, priority (Highest/High/Medium/Low/Lowest), story points, sprint, epic link, labels, and comments.

**FR-PM-2:** The system shall provide Kanban Board view displaying issues as cards organized by status columns (Backlog → To Do → In Progress → In Review → Done) with drag-and-drop to change status.

**FR-PM-3:** The system shall support Sprint creation with fields: sprint name, start date, end date, sprint goal, and capacity (story points or hours).

**FR-PM-4:** The system shall support assigning issues to sprints with validation preventing over-allocation beyond sprint capacity.

**FR-PM-5:** The system shall provide Backlog view showing unassigned issues sorted by priority, with drag-and-drop to reorder priority.

**FR-PM-6:** The system shall calculate and display sprint burndown showing: total story points at sprint start, ideal burndown line, actual story points remaining per day.

**FR-PM-7:** The system shall support issue state transitions with workflow rules (e.g., "Bug cannot transition from To Do to Done without passing through In Progress and In Review").

**FR-PM-8:** The system shall maintain issue history showing all field changes, status transitions, comments, and attachments with timestamp and actor.

**FR-PM-9:** The system shall support issue linking to create relationships: blocks/blocked by, relates to, duplicates, child of (for Epic-Story hierarchy).

**FR-PM-10:** The system shall provide issue search with filters: keyword search (title/description), type, status, assignee, reporter, sprint, epic, labels, date range.

**FR-PM-11:** The system shall support issue comments with @mention notifications to tag team members for attention.

**FR-PM-12:** The system shall provide board customization allowing teams to: rename status columns, add/remove columns, configure column WIP (work-in-progress) limits.

**FR-PM-13:** The system shall support multiple boards per project with independent configurations (e.g., "Development Board", "QA Board", "Design Board").

**FR-PM-14:** The system shall provide PM metrics dashboard showing: sprint velocity (story points completed per sprint), cumulative flow diagram, issue throughput (issues completed per week), cycle time (days from To Do to Done).

**FR-PM-15:** The system shall export sprint/issue data to CSV/Excel for reporting with configurable field selection.

**FR-PM-16 (NEW - Card Intelligence):** The system shall analyze issue cards and display: (1) Ambiguity Score using NLP to detect missing Given/When/Then, vague terms, undefined dependencies with inline AC editor suggestions, (2) Estimate Reality Check comparing story points to similar historical issues with one-tap re-estimate or justify actions.

**FR-PM-17 (NEW - Auto-PR Scaffolding):** The system shall auto-generate upon moving issue to "In Progress" within 3 seconds: branch name from issue ID, PR template with linked AC and checklist, CI checks configuration, reviewer set based on CODEOWNERS, test skeleton stubs (JUnit/WireMock) committed to branch.

**FR-PM-18 (NEW - Contract-First Guard):** The system shall detect issues tagged "API" and: (1) auto-draft OpenAPI specification stub from issue description, (2) create PR to contracts repository, (3) block transition to "In Progress" until contract PR merged, (4) monitor for spec drift and raise "AC vs spec mismatch" banner with diff.

**FR-PM-19 (NEW - QA/Env Awareness):** The system shall ping environment telemetry continuously and when developer attempts to move card to "Done" while QA environment is down or deployment queue is blocked, display warning: "QA env down; moving to Done will stall deploy. Hold?" with one-tap "Hold + notify QA" or "Bypass (with reason)" actions.

**FR-PM-20 (NEW - Board Command Palette):** The system shall provide ⌘K command palette for rapid actions: Add reviewer, Split story, Generate API contract, Create PR, Page QA team, with E (edit AC inline), R (re-estimate), Shift+M (split into subtasks with code boundary suggestions).

---

### Functional Requirements - Simple Workflow Engine

**FR-WF-1:** The system shall support single-level approval workflows for service requests, requiring manager approval before fulfillment for request types configured as "requires approval."

**FR-WF-2:** The system shall provide in-app approval interface showing: request details, requester information, approval history, and Approve/Reject/Request More Info buttons.

**FR-WF-3:** The system shall send email notifications to approvers when approval is pending, with deep link to approval interface.

**FR-WF-4:** The system shall auto-approve service requests meeting policy criteria (e.g., "Requests <$500 auto-approved for employees with tenure >6 months").

**FR-WF-5:** The system shall support ticket routing rules with conditions: if category="Network Issue" then assign to "Network Team", if priority="Critical" then assign to "Senior Agent Queue."

**FR-WF-6:** The system shall enforce state transition rules preventing invalid transitions (e.g., "Closed tickets cannot reopen", "Incidents cannot skip from New to Resolved").

**FR-WF-7:** The system shall support scheduled workflow actions: auto-close resolved tickets after 7 days, auto-escalate pending approvals after 48 hours.

**FR-WF-8:** The system shall maintain workflow execution audit trail showing: which rules triggered, when, what actions executed, and outcomes.

**FR-WF-9:** The system shall provide workflow configuration UI for administrators to: create routing rules, define approval requirements, set auto-escalation timers, configure auto-close policies.

**FR-WF-10:** The system shall support workflow testing allowing administrators to simulate rule execution with test data before activating in production.

**FR-WF-11 (NEW - Approval Bundles):** The system shall group pending approval requests by policy archetype and risk profile, enabling managers to approve bundles with single click while auto-breaking out outlier requests that don't match bundle criteria for individual review.

**FR-WF-12 (NEW - Policy × Budget × Inventory Card):** The system shall display for each approval request a composite context strip showing: policy match status, budget line availability with remaining balance, stock-on-hand inventory levels, with auto-split suggestion when stock is insufficient (e.g., "Approve 7 now, defer 3 to next batch").

**FR-WF-13 (NEW - Approval Intelligence):** The system shall provide for each request: confidence score based on precedent analysis ("93% of similar requests approved"), risk flags from vendor recall feeds and security advisories, precedent panel showing relevant prior decisions with one-tap rationale reuse, and "Why?" explainer with decision signals.

**FR-WF-14 (NEW - Multi-Approver Coordination):** The system shall display braided approval timeline showing Finance + IT sign-off progress, identify long-pole approver, and provide "Nudge [approver] (context attached)" action with SLA clock countdown.

**FR-WF-15 (NEW - Approval Simulation):** The system shall enable managers to simulate approval decisions before committing, showing: budget impact delta, inventory depletion curve with stock-out predictions, fulfillment ETA based on warehouse queue, and breach probability if policies violated, with live quantity tweaking.

---

### Non-Functional Requirements

**NFR1:** The system shall achieve p95 API latency <200ms for queue/list operations, <300ms for composite side-panel hydration, and <400ms for ticket/issue CRUD operations, with p99 <800ms across all operations.

**NFR2:** The system shall support 250 concurrent active users (50 helpdesk agents + 100 developers + 100 managers/employees) with infrastructure headroom to 500 for spike scenarios.

**NFR3:** The system shall maintain 99.9% availability (max 45 minutes downtime per month).

**NFR4:** The system shall enable full database backup and restore completing within 1 hour for disaster recovery.

**NFR5:** The system shall achieve SLA compliance rate >95% for ITSM incidents through accurate SLA tracking and escalation.

**NFR6:** The system shall maintain sprint velocity variance <15% sprint-to-sprint for PM teams through consistent planning.

**NFR7:** The system shall deliver mean time to resolution (MTTR) <8 hours for ITSM incidents.

**NFR8:** The system shall deliver first response time (FRT) <30 minutes for ITSM incidents during business hours (9am-5pm).

**NFR9:** The system shall support real-time updates via Server-Sent Events (SSE) with end-to-end event propagation ≤2s p95 from database commit to client UI update, reconnection catch-up <1s, and idempotent client reducers that drop stale updates based on version/updated_at timestamps.

**NFR10:** The system shall maintain full audit trail for all ticket/issue changes with 7-year retention for compliance.

**NFR11:** The system shall integrate with identity providers via OIDC/SAML for SSO authentication.

**NFR12:** The system shall implement role-based access control (RBAC) with roles: Admin, ITSM Agent, Developer, Manager, Employee.

**NFR13:** The system shall implement observability using OpenTelemetry for distributed tracing, metrics, and logs.

**NFR14:** The system shall support blue/green or canary deployment strategies for zero-downtime releases.

**NFR15:** The system shall support modern evergreen browsers (Chrome, Firefox, Safari, Edge) - last 2 versions only.

**NFR16:** The system shall meet WCAG 2.1 Level AA accessibility standards for web interfaces.

**NFR17:** The system shall align with SOC 2 Type II and ISO 27001 compliance requirements through continuous evidence collection.

**NFR18 (NEW - Timer Precision):** The system shall maintain SLA timer precision with drift <1s, zero missed escalations, and timer mutation (create/update/cancel) operations completing in O(1) with p95 <50ms.

**NFR19 (NEW - Search Performance):** The system shall deliver search query results with p95 <300ms, search index lag p95 <2s from write to searchability, and related tickets/issues correlation precision @k≥5 of >0.6 (tunable based on training data quality).

**NFR20 (NEW - Authorization Overhead):** The system shall maintain authorization overhead <10% of total request time through batch authorization checks (100 IDs in <3ms p95) and scope-first SQL filtering by team/project/role before per-row evaluation.

**NFR21 (NEW - Payload Discipline):** The system shall limit queue card payloads to <6KB per row, composite side-panel responses to <60KB, and implement predictive prefetch of i±3 neighbor rows on focus to maintain scroll performance at 50-100 items.

**NFR22 (NEW - Zero-Hunt Console Performance):** The system shall achieve for Zero-Hunt Agent Console: time-to-first-action reduction of −30% vs baseline manual context gathering, first-touch resolution rate improvement of +20% (agents resolve without handoffs), and AI-assisted suggestion acceptance rate ≥60% within 4 weeks of launch.

**NFR23 (NEW - Developer Copilot Board Performance):** The system shall achieve for Developer Copilot Board: PR scaffolding <3s from card drag to "In Progress", ambiguity flagging detection ≥70% of risky cards validated against historical rework data, QA/env guards preventing ≥90% of premature "Done" transitions, and Contract-First Guard blocking 100% of API work without merged OpenAPI spec.

---

## User Interface Design Goals

### Overall UX Vision

The platform embraces a **domain-specific, AI-augmented, power-user-optimized** design philosophy, recognizing that helpdesk agents and development teams use these tools for 8+ hours daily and require maximum efficiency over simplicity. The UX vision has evolved to center on three breakthrough paradigms:

1. **Zero-Hunt Agent Console ("Approve Station, Not Research Desk"):** Transforms agent experience from manual context-gathering to AI-assisted decision-making. Agents see suggested resolutions with explainability ("Why?"), approve/edit/send with Ctrl+Enter, and access all context (requester history, related tickets, KB articles, asset info) in single composite view without tab-hunting. Keyboard-driven (J/K/Enter/A/E) with prefetch intelligence that loads context before agents ask.

2. **Developer Copilot Board ("Live Reasoning Surface, Not Static Task Tracker"):** Moves beyond drag-and-drop to predictive intelligence. Board detects ambiguous acceptance criteria, warns of estimate risks, auto-generates PR scaffolding in 3s (branch, template, CI, reviewers, test stubs), blocks API work until contracts merge, and prevents premature "Done" moves when QA environments are down. Developers get ⌘K command palette for rapid actions.

3. **Approval Cockpit ("Decision Engine, Not Table with Buttons"):** Combines policy, budget, inventory, and risk into intelligent approval bundles. Managers see confidence scores based on precedents ("93% of similar approved"), simulate decisions before committing (budget impact, stock depletion, ETA), approve bundles with one click, and get braided multi-approver timelines showing long-pole blockers.

Visual aesthetics prioritize **speed, explainability, and trust**—confidence scores must be instantly understood, AI suggestions must show reasoning, SLA indicators must scream urgency, and all automated actions must have escape hatches ("Approve & Send" default with "Tweak & Send" option).

### Key Interaction Paradigms

**Keyboard-First Navigation (ITSM Agent Console):**
- Agents navigate ticket queue with arrow keys, open tickets with Enter, assign with 'A', close with 'C'
- Keyboard shortcuts displayed in tooltips and help overlay (?)
- Tab key moves focus through ticket fields without touching mouse

**Drag-and-Drop State Changes (PM Board):**
- Issues move between columns by dragging cards (triggers status update API call)
- Visual feedback during drag: highlight drop zones, show shadow card in target column
- Optimistic UI updates with rollback on API failure

**Inline Editing Without Modals:**
- Click ticket/issue title → editable input appears in place
- Click assignee avatar → dropdown appears for reassignment
- Escape key cancels edit, Enter key saves

**Real-Time Collaboration Indicators:**
- Show "John is viewing this ticket" presence indicator when multiple users on same item
- Live cursor positions for concurrent editors (Google Docs-style)
- Toast notifications when ticket/issue updated by another user: "Jane just assigned this to you"

**Smart Notification Strategy:**
- ITSM agents see: new ticket assignments, SLA breach warnings, comment mentions
- Developers see: issue assignments, @mentions in comments, sprint start/end
- Managers see: approval requests, SLA breaches, sprint goal at risk alerts
- Notification preferences configurable per user

**Contextual Bulk Operations:**
- ITSM: Select multiple tickets with checkboxes → bulk assign/update/tag actions appear in action bar
- PM: Select multiple issues on board → bulk move to sprint, bulk tag, bulk delete options

### Core Screens and Views

**1. ITSM Agent Console (Primary ITSM Interface)**
- Multi-column ticket queue (ID, Title, Status, Priority, Assignee, SLA, Created)
- Left sidebar: My Tickets, Unassigned, Critical Priority, SLA Breach Risk filters
- Top bar: Quick search, Create Incident/Request buttons, Agent presence indicators
- Ticket detail panel slides from right on row click (no full page navigation)
- Action buttons: Assign, Add Comment, Change Status, Escalate, Close
- Optimized for 1440px+ screens with 50-row pagination

**2. Service Catalog Portal (Employee Self-Service)**
- Grid of request templates (Laptop Request, Software Access, Desk Booking)
- Click template → form with pre-filled fields (category, priority, SLA)
- Approval preview: "This request requires manager approval (estimated 1-2 days)"
- Submit → confirmation with ticket ID and tracking link

**3. PM Kanban Board (Primary PM Interface)**
- Horizontal swim lanes showing status columns (Backlog, To Do, In Progress, In Review, Done)
- Issue cards display: ID, title, assignee avatar, story points, labels, priority color
- Top filters: Sprint dropdown, Assignee filter, Quick search, Board settings
- Drag-drop cards between columns to change status
- Double-click card → detail modal with full issue information and comments

**4. PM Backlog View (Sprint Planning Interface)**
- Vertical list of unassigned issues sorted by priority
- Drag-drop to reorder priority
- Right panel: Sprint planning - drag issues into "Next Sprint" box
- Story point counter shows sprint capacity vs allocated points
- Quick actions: Bulk estimate (select 5 stories → set all to 3 points)

**5. Unified Dashboard (Cross-Module Overview)**
- Top row: My Active Tickets (ITSM) + My Active Issues (PM) counts with trend arrows
- Middle: Real-time activity feed showing: "Ticket #1234 assigned to you", "Issue DEV-456 moved to Done"
- Bottom: Metrics cards - ITSM: SLA compliance %, MTTR | PM: Sprint velocity, Issues in progress
- Live SSE updates with yellow highlight fade animation on new items

**6. Approval Queue (Manager Interface)**
- Table of pending service requests requiring approval
- Columns: Requester, Request Type, Cost, Justification, Risk Score, Actions
- Inline Approve/Reject buttons with optional comment field
- Mobile-optimized for quick approvals on phone (swipe actions)

**7. ITSM Metrics Dashboard (Reports)**
- Charts: Tickets created/resolved trend (line chart), SLA compliance by category (bar chart)
- KPI cards: MTTR, FRT, Open Tickets, SLA Breach %
- Date range picker, export to CSV button

**8. PM Metrics Dashboard (Reports)**
- Charts: Sprint velocity trend (line chart), Cumulative flow diagram (stacked area), Burndown (line chart)
- KPI cards: Current sprint progress, Avg cycle time, Issues completed this week
- Sprint selector, export to CSV button

**9. Workflow Configuration (Admin Interface)**
- Left tabs: Routing Rules, Approval Policies, Auto-Close Settings, Escalation Rules
- Routing Rules: "If Category = Network Issue, Then Assign to Network Team" rule builder
- Approval Policies: "If Cost > $500, Require Manager Approval" condition builder
- Test workflow button to simulate rules before activating

**10. Ticket/Issue Detail View (Deep Dive)**
- ITSM: Full ticket information, comment timeline, related tickets, SLA history, audit log
- PM: Full issue information, comment thread with @mentions, linked issues, sprint history, workflow transition log
- Side-by-side comments and history tabs

**11. User Profile & Settings**
- Notification preferences, keyboard shortcuts customization, default filters, avatar upload

**12. Admin Settings**
- User management, role assignment, category configuration (ITSM), board templates (PM)

### Accessibility: WCAG AA

All web interfaces must meet WCAG 2.1 Level AA standards:
- Keyboard navigation for all interactive elements (Tab, Enter, Escape, Arrow keys)
- Screen reader compatibility with ARIA labels for icon-only buttons and status indicators
- Color contrast ratios ≥4.5:1 for text, ≥3:1 for interactive components
- Focus indicators with 2px visible outline on all focusable elements
- Semantic HTML (`<main>`, `<nav>`, `<section>`, `<article>`)
- `aria-live="polite"` regions for real-time SSE updates announced to screen readers
- Skip-to-main-content link for keyboard users
- Dedicated accessibility audit sprint planned before UAT phase

### Branding

The platform uses **Ant Design (AntD) 5.x** component library as the visual foundation, providing professional enterprise aesthetics. Custom branding intentionally minimal in Phase 1:

**Design Tokens (AntD ConfigProvider):**
- Primary color: `#1677FF` (enterprise blue)
- Success: `#52C41A` (green for resolved tickets, done issues)
- Warning: `#FAAD14` (amber for SLA warnings, in-progress)
- Error: `#FF4D4F` (red for critical priority, SLA breaches)
- Font: System stack (`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto`)
- Spacing: 8px base unit
- Border radius: 6px

**Status Color Conventions:**
- ITSM Priority: Critical (red), High (orange), Medium (yellow), Low (gray)
- ITSM Status: New (blue), Assigned (cyan), In Progress (amber), Resolved (green), Closed (gray)
- PM Priority: Highest (red), High (orange), Medium (yellow), Low (gray), Lowest (light gray)
- PM Status: Backlog (gray), To Do (blue), In Progress (amber), In Review (purple), Done (green)

### Target Device and Platforms

**Primary:** Web Responsive (desktop and tablet browsers) - optimized for 1440px+ screens with graceful degradation to 768px tablets

**Desktop (≥992px):** Full feature set - agent console with all columns, board with all swim lanes, detailed charts

**Tablet (768-991px):** Simplified views - agent console with key columns only, board with scroll, basic charts

**Mobile (<768px):** Limited support - approval queue only (for manager mobile approvals), redirect to desktop for full ITSM/PM features

**Out of Scope Phase 1:** Dedicated mobile apps, conversational UI (Teams/Slack bots), offline mode

### Testability Requirements

**Frontend Testing:**
- Visual regression testing (Percy or Chromatic) for agent console and board views
- Cross-browser compatibility testing (Chrome, Firefox, Safari, Edge - last 2 versions)
- Accessibility testing with axe-core and Lighthouse integrated into CI/CD from Sprint 1
- Manual screen reader testing (NVDA, JAWS, VoiceOver) before Epic completion

**Performance Baselines:**
- Agent console loads 50 tickets in <1 second
- Board view renders 100 issue cards in <1 second
- Drag-drop operations complete with <16ms frame time (60fps)
- SSE updates reflect in UI within 2 seconds

**Real-Time Sync Testing:**
- Multi-client WebSocket/SSE synchronization tests
- Network partition simulation and reconnection handling
- Chaos testing for eventual consistency scenarios

---

## Technical Assumptions

### Repository Structure: Monorepo

**Decision:** Single Git repository with clear module boundaries separating backend, frontend, and shared contracts

**Structure:**
```
/backend          - Spring Boot 3.5.x application (modulith)
  /itsm-module    - ITSM domain logic (incidents, service requests, agents)
  /pm-module      - PM domain logic (issues, sprints, boards)
  /workflow-module - Shared workflow engine (approvals, routing, state machines)
  /shared         - Common services (auth, notifications, audit)
/frontend         - React 18 + Vite application
  /itsm-ui        - ITSM agent console, ticket views
  /pm-ui          - PM board, backlog views
  /shared-ui      - Common components, dashboard, layout
/infrastructure   - Kubernetes manifests, Envoy Gateway HTTPRoutes
/docs             - Architecture, PRD v2.0, ADRs, runbooks
/testing          - Integration tests, E2E tests, performance tests
```

**Rationale:** Monorepo enables atomic commits across ITSM/PM modules, simplifies versioning, supports parallel team development.

### Service Architecture

**Deployment Unit:** Modulith + 3 Companions Pattern (physics-based separation)

**Core Application (Spring Boot Modulith):**
- **ITSM Module:** Ticket CRUD, composite APIs (queue cards, side-panel), AI suggestion coordination
- **PM Module:** Issue CRUD, board services, card intelligence, contract guard coordination
- **Workflow Module:** Approval workflows, bundles, routing rules, state machine enforcement
- **Shared Services:** Authentication (Keycloak), CQRS-lite read models, transactional outbox writes, audit logging

**Companion Services (Separate Processes, Same Repo):**

1. **Event Worker** (drains outbox, fans out events, updates read models)
   - Polls transactional outbox in FIFO with gap detection
   - Publishes to Redis Stream (fan-out channel for SSE + indexers)
   - Updates materialized read models (ticket_card, queue_row, issue_card, sprint_summary)
   - Autoscale on lag (rows behind) and processing time

2. **Timer/SLA Service** (durable scheduler with business calendar brains)
   - Persistent job queue with O(1) create/update/cancel operations
   - Business calendar evaluator for pause/resume logic (changes due_at)
   - Idempotent escalation handlers with exponential backoff
   - Autoscale on active timers and due-window density (e.g., next 5 min)

3. **Search Indexer** (maintains OpenSearch for correlation/discovery)
   - Consumes change events from Event Worker
   - Maintains inverted index for tickets, issues, comments, KB with synonyms + phonetics
   - Precomputes related_incident_ids[] and related_issue_ids[] for O(1) side-panel loads
   - Can merge into Event Worker at small scale; extract when CPU-bound

**Architecture Principles (From Brainstorming Session):**
- **Physics > Features:** Build for hot-read/cool-write workload, fan-out events, clock-precision timers
- **Composite Over CRUD:** Task-shaped APIs (queue cards, side-panel bundle) are product surface; entity CRUD is plumbing
- **Idempotency Everywhere:** All writes/events/actions carry {id, version, idempotencyKey}; client reducers drop stale by version
- **Batching is Universal Antidote:** Batch reads, batch auth, batch writes, prefetch on focus

**Module Boundary Enforcement:**
- Spring Modulith library for explicit module declarations within monolith
- ArchUnit tests enforcing module access rules
- Event contracts versioned and documented (this is the "platform API")
- Code review checklist includes module violation checks

**Rationale:** Modulith keeps domain cohesive (atomic refactors, shared types) while companions isolate physics-heavy loops (events, timers, search) that would murder HTTP tail latency if coupled. Enables 250 concurrent users with <200ms p95 queue loads. Clear event contracts enable future microservice extraction when tripwires trigger (p95 >400ms after offloading, deploy cadence diverges, cross-module changes <5%).

### Testing Requirements

**Levels:**
1. **Unit Tests:** JUnit 5 + Mockito for business logic. Target >80% coverage for ITSM/PM domain logic.
2. **Integration Tests:** Spring Boot Test with Testcontainers (PostgreSQL + Redis). Test ITSM workflows, PM sprint calculations, approval logic.
3. **Contract Tests:** Spring Cloud Contract for API versioning between frontend and backend.
4. **E2E Tests:** Playwright for web UI workflows - "Create incident → Assign → Resolve" and "Create issue → Move through board → Complete sprint"
5. **Performance Tests:** Gatling scenarios - 60% agent console operations, 30% board interactions, 10% approvals. Ramp to 250 concurrent users, validate p95 <400ms.
6. **Accessibility Tests:** axe-core + Lighthouse automated scanning, manual screen reader testing before Epic completion.

### Database & Persistence

**Primary Datastore:** PostgreSQL 16+
- **Sizing:** 4 vCPU, 16GB RAM, 500GB SSD
- **High Availability:** Streaming replication (active-passive)
- **Connection Pool:** HikariCP `maximumPoolSize = 50`

**Schema Design:**
- **ITSM OLTP Tables:** tickets, incidents, service_requests, ticket_comments, routing_rules
- **ITSM Read Models:** ticket_card (denormalized queue view), sla_tracking (computed due dates), related_incidents (precomputed correlations)
- **PM OLTP Tables:** issues, sprints, boards, board_columns, issue_links
- **PM Read Models:** queue_row (denormalized backlog view), issue_card (board card data), sprint_summary (burndown metrics)
- **Shared Tables:** users, roles, approvals, workflow_states, audit_log, outbox (transactional event log)
- **Outbox Schema:** `{id, aggregate_id, aggregate_type, event_type, version, occurred_at, payload, processed_at}`

**Migrations:** Flyway for versioned schema changes

**Backup & DR:**
- Continuous WAL archiving to S3
- RPO: 15 minutes, RTO: 4 hours

### Cache & Pub/Sub

**Redis 7.x:**
- Session data caching
- Real-time SSE fanout via Redis Stream (24-48h retention for event replay on reconnect)
- Hot ACL cache (per-team capability bitsets, refresh on role change)
- Rate limiting (1000 req/min per user)

### Event Bus & Messaging

**Internal - Transactional Outbox Pattern:**
1. **Write Phase:** Application writes domain changes + event to outbox table in single transaction (ACID guarantee)
2. **Poll Phase:** Event Worker polls outbox in FIFO order with gap detection, marks processed
3. **Fan-out Phase:** Event Worker publishes to Redis Stream → SSE gateways + Search Indexer subscribe
4. **Consume Phase:** Clients receive via SSE with reconnect cursor (lastEventId), idempotent reducers drop stale by version

**Event Schema:**
```json
{
  "aggregate_id": "TICKET-123",
  "aggregate_type": "Ticket",
  "event_type": "TicketAssigned",
  "version": 5,
  "occurred_at": "2025-10-05T14:23:15Z",
  "payload": { "assignee_id": "user-456", "reason": "Network expertise" }
}
```

**External:** Not required in Phase 1 (no external system integrations)

### Search & Correlation

**OpenSearch** (minimal on-ramp for MVP, 1 hot node + 1 replica)
- **Write-Behind Indexing:** Search Indexer consumes change events from Event Worker, maintains versioned documents
- **Indexed Entities:** tickets, issues, comments, KB articles with text analysis (synonyms, phonetics for "printer won't print" variations)
- **Precomputed Correlations:** related_incident_ids[] and related_issue_ids[] computed on write for O(1) side-panel "Related Tickets" lookups
- **Similarity Algorithm:** Vector embeddings (sentence-transformers) + nearest-neighbor search for "show me similar tickets"
- **Performance Target:** Query p95 <300ms, index lag p95 <2s from write to searchability, correlation precision @k≥5 >0.6

### Identity & Authentication

**Keycloak** (existing self-hosted on-prem, version 23.x)
- OIDC/SAML for SSO
- Custom RBAC: Admin, ITSM Agent, Developer, Manager, Employee roles
- JWT access tokens with refresh rotation

**Batch Authorization Pattern:**
- **Scope-First SQL:** All list queries filter by team/project/role at database level BEFORE per-row checks
- **Batch Check API:** `POST /auth/batchCheck` evaluates up to 200 resource IDs in single call, p95 <3ms per 100 IDs
- **Hot ACL Cache:** Per-team capability bitsets cached in Redis, refresh on role change, avoid per-row synchronous IAM calls
- **Auth Overhead Budget:** <10% of total request time (e.g., 20ms auth for 200ms queue query)

### Infrastructure

**Envoy Gateway** (existing on-prem with Kubernetes Gateway API)
- Rate limiting: 1000 req/min per user
- HTTPRoute for REST + SSE endpoints

**Kubernetes Deployment (4 Deployments, 1 Namespace, Single Helm Chart):**

1. **HTTP/SSE App** (Spring Boot Modulith with "app" profile)
   - Replicas: 5 instances (50 users per instance)
   - Resources: 1 CPU / 2GB RAM requests, 2 CPU / 4GB RAM limits
   - Exposes REST + SSE endpoints via Envoy Gateway

2. **Event Worker** (Spring Boot with "worker" profile, no HTTP)
   - Replicas: 2 instances (can scale on outbox lag)
   - Resources: 0.5 CPU / 1GB RAM requests, 1 CPU / 2GB RAM limits
   - Polls outbox, publishes to Redis Stream, updates read models

3. **Timer/SLA Service** (Spring Boot with "timer" profile)
   - Replicas: 2 instances (active-passive or sharded by timer buckets)
   - Resources: 0.5 CPU / 1GB RAM requests, 1 CPU / 2GB RAM limits
   - Durable job queue with business calendar logic

4. **Search Indexer** (Spring Boot with "indexer" profile, can merge into Worker at small scale)
   - Replicas: 1 instance (CPU-bound workload, scale when needed)
   - Resources: 1 CPU / 2GB RAM requests, 2 CPU / 4GB RAM limits
   - Consumes events, maintains OpenSearch indices

**Deployment Strategy:** Blue/green for App; rolling updates for Workers/Timers/Indexer

### Observability

**OpenTelemetry:** Distributed tracing, metrics, structured logging
**Prometheus + Grafana:** Metrics dashboards
**Alerts:** API latency p95 >400ms, SLA compliance <95%, error rate >1%

### CI/CD

**Pipeline Stages:**
- Build: Compile, unit tests, ArchUnit module validation
- Security: SAST (Qodana), dependency scanning (OWASP Dependency-Check + Trivy)
- Integration Tests: Testcontainers
- E2E Tests: Playwright against staging
- Performance Gate: Gatling smoke test (50 users, p95 <400ms)
- Deploy: Blue/green to production

### Browser Support

Modern evergreen browsers only (Chrome, Firefox, Safari, Edge - last 2 versions). No IE11.

### User Segmentation

- **Total Users:** 1,000 employees
- **Helpdesk Agents:** 50 agents (heavy ITSM usage, 8+ hours daily)
- **Developers:** 200 developers (heavy PM usage, 8+ hours daily)
- **Managers:** 100 managers (moderate usage, approvals + dashboards)
- **General Employees:** 650 employees (light usage, service requests only)
- **Concurrent Peak:** 250 concurrent users

---

## Epic List

### Epic 1: Foundation & Infrastructure
**Goal:** Establish project foundation with Spring Boot modulith, PostgreSQL, Redis, Keycloak authentication, and CI/CD pipeline

**Deliverable:** Deployable Spring Boot application with authenticated health-check endpoint, ITSM/PM module structure, database schema, CI/CD pipeline

**Timeline:** Weeks 1-4 (4 weeks) | **Team:** 2 BE + 1 Infra + 1 QA

**Acceptance Criteria:**
- Authenticated `/actuator/health` endpoint returns 200 OK with JWT token
- PostgreSQL schema created with ITSM and PM tables via Flyway migrations
- CI/CD pipeline executes: build → unit tests → SAST → deploy to staging
- Kubernetes pod deployed via Envoy Gateway with health probe
- Local Docker Compose dev environment matches production stack

**Demo Milestone:** "Health check deployed to Kubernetes, accessible via Envoy Gateway, CI/CD pipeline green"

**Story Points:** 80 points (2 sprints @ 40 velocity)

---

### Epic 2-ITSM: Core ITSM Module
**Goal:** Deliver functional ITSM platform with incident management, service requests, agent console, and SLA tracking

**Deliverable:** ITSM module with ticket CRUD, agent console UI, SLA calculation, routing rules, and metrics dashboard

**Timeline:** Weeks 3-10 (8 weeks, starts week 3 when Epic 1 auth ready) | **Team:** 2 BE + 1 FE (Team A)

**Acceptance Criteria:**
- Agents can create/assign/update/close incidents via agent console
- Service requests flow through approval workflow (submit → manager approves → fulfillment)
- SLA countdown displays for all incidents with color-coded urgency (green/amber/red)
- Routing rules auto-assign tickets based on category (Network → Network Team)
- Agent console displays 50 tickets with <1 second load time
- ITSM metrics dashboard shows: MTTR, FRT, SLA compliance %, tickets by status

**Demo Milestone:** "Agent creates incident 'Server Down', system auto-assigns to Network Team with 2-hour SLA, agent resolves ticket, MTTR metric updates in real-time"

**Story Points:** 160 points (4 sprints)

---

### Epic 3-PM: Core PM Module
**Goal:** Deliver functional PM platform with issue tracking, Kanban board, sprint management, and backlog views

**Deliverable:** PM module with issue CRUD, board UI with drag-drop, sprint planning, backlog, and metrics dashboard

**Timeline:** Weeks 3-10 (8 weeks, parallel with Epic 2-ITSM) | **Team:** 2 BE + 1 FE (Team B)

**Acceptance Criteria:**
- Developers can create/assign/update issues (stories, tasks, bugs)
- Kanban board displays issues grouped by status column (To Do, In Progress, Done)
- Drag-drop issue cards between columns updates status via API
- Sprint planning: create sprint, drag issues from backlog into sprint, capacity validation
- Burndown chart displays for active sprint with ideal vs actual story points
- PM metrics dashboard shows: sprint velocity, cumulative flow, cycle time

**Demo Milestone:** "Product owner creates sprint 'Sprint 5', drags 10 stories from backlog, developer moves story through board To Do → In Progress → Done, burndown chart updates, sprint completes with velocity metric recorded"

**Story Points:** 160 points (4 sprints)

---

### Epic 4: Simple Workflow Engine
**Goal:** Build approval workflows, routing automation, and state transition rules supporting both ITSM and PM modules

**Deliverable:** Workflow engine with single-level approvals, routing rules, auto-escalation, and admin configuration UI

**Timeline:** Weeks 9-14 (6 weeks, starts week 9 overlapping Epic 2/3 final weeks) | **Team:** 2 BE + 1 FE

**Acceptance Criteria:**
- Service requests >$500 require manager approval before fulfillment
- Managers approve/reject requests via approval queue UI with comment
- Auto-approval rules execute: requests <$500 auto-approved for employees with tenure >6 months
- Routing rules auto-assign tickets: if category="Network" then assign to "Network Team"
- State transition validation: closed tickets cannot reopen, incidents cannot skip states
- Auto-escalation: pending approvals >48 hours send reminder notification
- Workflow config UI allows admins to create/edit routing rules and approval policies

**Demo Milestone:** "Employee submits $300 laptop request → auto-approved by policy engine, request fulfilled immediately. Manager submits $1500 request → routed to approval queue → director approves → request fulfilled"

**Story Points:** 120 points (3 sprints)

---

### Epic 5: Unified Dashboard & Real-Time Updates
**Goal:** Deliver cross-module dashboard showing ITSM tickets + PM tasks with live SSE updates

**Deliverable:** Unified dashboard with "My Tickets", "My Issues", real-time activity feed, SSE infrastructure

**Timeline:** Weeks 13-18 (6 weeks, starts week 13 when ITSM/PM modules complete) | **Team:** 1 BE + 2 FE

**Acceptance Criteria:**
- Dashboard displays user's active ITSM tickets and PM issues in unified view
- Real-time activity feed shows: "Ticket #1234 assigned to you", "Issue DEV-456 moved to Done"
- SSE connection updates dashboard within 2 seconds of server event
- Load test: 250 concurrent SSE connections sustained for 8 hours without memory leak
- Dashboard responsive: desktop (1440px+) full view, tablet (768px+) simplified, mobile redirect
- Empty state when user has no active tickets/issues

**Demo Milestone:** "User opens dashboard, sees 3 active tickets + 5 active issues. Colleague assigns new ticket in background, dashboard auto-updates with yellow highlight within 2 seconds via SSE"

**Story Points:** 120 points (3 sprints)

---

### Epic 6: SLA Management & Reporting (OPTIONAL - Phase 2 candidate)
**Goal:** Advanced SLA reporting, custom SLA policies, and ITSM/PM analytics

**Deliverable:** Custom SLA policy editor, advanced reports (SLA by category, agent performance), export to PDF

**Timeline:** Weeks 16-20 (5 weeks) | **Team:** 1 BE + 1 FE

**Note:** This epic can be deferred to Phase 2 if timeline pressure requires reducing to 18 weeks.

---

### Phase 3 Timeline with Parallelization

```
Week  Epic Activity                                  Team Allocation
──────────────────────────────────────────────────────────────────────
1-4   Epic 1: Foundation (DB, Auth, CI/CD)          2 BE, 1 Infra, 1 QA

3-10  Epic 2-ITSM: Incidents, Requests, Console     2 BE, 1 FE (Team A)
      Epic 3-PM: Issues, Board, Sprint (parallel)   2 BE, 1 FE (Team B)

9-14  Epic 4: Workflow Engine (approvals, routing)  2 BE, 1 FE

13-18 Epic 5: Dashboard + SSE real-time updates     1 BE, 2 FE

16-20 Epic 6: SLA Reporting (optional)              1 BE, 1 FE
──────────────────────────────────────────────────────────────────────
TOTAL: 20 weeks (18 weeks if Epic 6 deferred)
```

**Critical Path:** Epic 1 → Epic 2-ITSM & Epic 3-PM (parallel) → Epic 4 → Epic 5 = 18 weeks

**Resource Requirements:**
- Peak: 10 people (Weeks 3-10 during parallel ITSM/PM development)
- Average: 7 people

---

## Success Metrics

### ITSM Success Metrics

**Operational Metrics:**
1. **Mean Time To Resolution (MTTR):** <8 hours (industry benchmark for internal IT)
2. **First Response Time (FRT):** <30 minutes during business hours
3. **SLA Compliance Rate:** >95% (tickets resolved within SLA)
4. **Agent Productivity:** >15 tickets resolved per agent per day
5. **Self-Service Adoption:** >40% of requests submitted via service catalog (vs email/phone)

**Technical Metrics:**
1. **Queue Load Time:** <200ms p95 for 50-ticket queue (denormalized read model)
2. **Side-Panel Hydration:** <300ms p95 for composite context (requester history + related tickets + KB + asset)
3. **Ticket Creation Latency:** p95 <300ms
4. **SLA Timer Precision:** Drift <1s, zero missed escalations
5. **AI Suggestion Quality:** ≥60% acceptance rate for Suggested Resolutions within 4 weeks

**Zero-Hunt Console Metrics (NEW):**
1. **Time-to-First-Action:** −30% reduction vs baseline manual context-gathering
2. **First-Touch Resolution Rate:** +20% improvement (agents resolve without handoffs)
3. **Keyboard Navigation Adoption:** >80% of agents using J/K/Enter shortcuts by week 8

### PM Success Metrics

**Operational Metrics:**
1. **Sprint Velocity Stability:** Variance <15% sprint-to-sprint
2. **Sprint Completion Rate:** >80% of committed story points completed
3. **Cycle Time:** <5 days average from To Do to Done for stories
4. **Issue Throughput:** >30 issues completed per sprint per team
5. **Board Adoption:** >90% of dev teams using board for daily standup

**Technical Metrics:**
1. **Board Render Time:** <1 second for 100-card board
2. **Drag-Drop Latency:** <100ms from drop to status update
3. **Burndown Accuracy:** Daily refresh at midnight, 100% accurate calculation

**Developer Copilot Board Metrics (NEW):**
1. **PR Scaffolding Speed:** <3s from card drag to "In Progress" with full scaffolding (branch, PR, CI, reviewers, test stubs)
2. **Ambiguity Detection Accuracy:** ≥70% of risky cards flagged (validated against historical rework data)
3. **QA/Env Guard Effectiveness:** ≥90% prevention rate for premature "Done" moves when environments unavailable
4. **Contract-First Compliance:** 100% blocking rate for API work without merged OpenAPI specs
5. **Rework Reduction:** −25% reduction in mid-sprint story changes due to ambiguous ACs

### Workflow Engine Success Metrics

**Operational Metrics:**
1. **Auto-Approval Rate:** >60% of service requests auto-approved without manual intervention
2. **Routing Accuracy:** >95% of tickets auto-assigned to correct team
3. **Approval Response Time:** <24 hours average for pending approvals

**Technical Metrics:**
1. **Rule Execution Latency:** <50ms for routing rule evaluation
2. **Workflow Audit Completeness:** 100% of workflow actions logged

**Approval Cockpit Metrics (NEW):**
1. **AI-Assisted Approval Rate:** ≥60% of approvals using suggested decisions within 4 weeks
2. **Decision Speed:** p50 <12s, p95 <45s for standard approval items (vs 2-3 minutes manual)
3. **Bundle Efficiency:** Average bundle size 12-15 requests (reduces 15 clicks to 1)
4. **Exception Rate:** ≤10% of approvals override policy recommendations, trending down
5. **Simulation Adoption:** >40% of managers use "what-if" simulation before large batch approvals

### Platform Success Metrics

**User Adoption:**
1. **Active Users:** >80% of 1,000 target users log in monthly
2. **Daily Active Agents:** >90% of 50 helpdesk agents use system daily
3. **Daily Active Developers:** >70% of 200 developers use PM boards daily

**Performance:**
1. **API Latency:** p95 <400ms for all CRUD operations
2. **Availability:** >99.9% uptime
3. **Concurrent Users:** 250 concurrent supported without degradation

**Compliance:**
1. **Audit Trail Completeness:** 100% of ticket/issue changes logged
2. **Accessibility:** WCAG AA compliance for all screens
3. **Security:** Zero critical/high vulnerabilities in production

---

## Next Steps

### UX Expert Deliverables

**Sally (UX Expert)** - Create two parallel prototypes:

**1. ITSM Agent Console Prototype**
- Agent queue table with 50 sample incidents/requests
- Ticket detail panel with inline editing
- SLA countdown indicators with color coding
- Quick actions: Assign, Comment, Close
- Deliverable by: Week 6

**2. PM Kanban Board Prototype**
- Board view with 5 status columns and 30 issue cards
- Drag-and-drop simulation (visual feedback)
- Sprint selector and burndown chart
- Issue detail modal
- Deliverable by: Week 6

**Critical Focus Areas:**
- ITSM: Keyboard navigation (Tab, Enter, arrow keys), SLA urgency visual hierarchy
- PM: Drag-and-drop UX, card information density optimization
- Both: Real-time update animations (yellow highlight fade)

---

### Architect Deliverables

**Backend Architect** - Review PRD v2.0 and update architecture:

**1. High-Level Design (HLD)**
- ITSM module architecture (ticket services, SLA calculator, routing engine)
- PM module architecture (issue services, sprint manager, burndown calculator)
- Workflow module architecture (approval state machine, rule evaluator)
- Database schema (ITSM tables, PM tables, shared tables)
- Integration architecture (Keycloak OIDC, Envoy Gateway, SSE)

**2. Low-Level Design (LLD) for Critical Components**
- SLA calculation engine (priority-based due dates, countdown logic)
- Routing rule evaluator (condition matching, team assignment logic)
- Board state synchronization (optimistic updates, conflict resolution)
- SSE connection pool management (250+ connections, reconnection handling)

**3. API Contracts (OpenAPI 3.0)**
- ITSM API (ticket CRUD, assignment, SLA endpoints)
- PM API (issue CRUD, board operations, sprint management)
- Workflow API (approval actions, routing rules config)
- Publish by: Week 5 to unblock frontend development

**4. Performance Model**
- Database sizing justification (4 vCPU, 16GB RAM, 500GB SSD)
- Connection pool sizing (HikariCP maxPoolSize=50)
- Load test scenarios (250 concurrent users, 60/30/10 operation split)

---

### Development Team Kickoff

**Week 1 Actions:**
1. **Team Formation:** Assign Team A (ITSM) and Team B (PM) with 2 BE + 1 FE each
2. **Epic 1 Start:** Both teams collaborate on foundation (auth, database, CI/CD)
3. **Prototype Reviews:** Review UX prototypes to understand target UI

**Week 3 Actions:**
1. **Parallel Development Begins:** Team A starts Epic 2-ITSM, Team B starts Epic 3-PM
2. **Daily Standups:** Separate standups per team, weekly sync for cross-module dependencies
3. **API Contract Reviews:** Frontend reviews backend API contracts, provides feedback

---

**End of PRD v2.0 Draft**

**Document Status:** Draft - Awaiting Stakeholder Review
**Next Phase:** Architecture Phase (HLD/LLD Development)
**Approvals Required:** Executive Sponsor, Engineering Manager, Finance (budget for 10-person team, 20 weeks)
**Prepared By:** John (Product Manager)
**Date:** 2025-10-04
