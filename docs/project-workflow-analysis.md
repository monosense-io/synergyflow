# Project Workflow Analysis

**Date:** 2025-10-06
**Project:** SynergyFlow
**Analyst:** monosense

## Assessment Results

### Project Classification

- **Project Type:** Enterprise Web Application (Dual-Module Platform: ITSM + PM)
- **Project Level:** Level 4 (Platform/Ecosystem)
- **Instruction Set:** instructions-lg.md (Enterprise PRD with architect handoff)

### Scope Summary

- **Brief Description:** Unified enterprise ITSM & Project Management platform combining incident management, service requests, agent console, SLA tracking (ITSM Module) with issue tracking, Kanban boards, sprint management, backlog views (PM Module), enhanced with AI-augmented workflow automation (approval workflows, routing rules, state transition enforcement). Targets 1,000 users (250 concurrent) with breakthrough UX innovations: Zero-Hunt Agent Console, Developer Copilot Board, Approval Cockpit.

- **Estimated Stories:** 120+ stories across 6 epics
- **Estimated Epics:** 6 epics (Foundation, Core ITSM, Core PM, Workflow Engine, Unified Dashboard, SLA Reporting)
- **Timeline:** 20 weeks (18 weeks critical path)

### Context

- **Greenfield/Brownfield:** Greenfield (new platform development)
- **Existing Documentation:**
  - ✅ PRD v3.0 (`docs/product/prd.md`)
  - ✅ Brainstorming session results (`docs/brainstorming-session-results-2025-10-05.md`)
  - ✅ Epics (`docs/epics/*`)
  - ✅ Architecture package (`docs/architecture/*`)
  - ✅ API contracts (stubs under `docs/api/*`)
  - ❌ UX specifications (need generation)
  - ❌ User stories breakdown (need epic-to-story decomposition)

- **Team Size:** 10 people peak (2 BE + 1 FE + 1 Infra + 1 QA for Epic 1; then 2 parallel teams for Epics 2-3; averaging 7 people)
- **Deployment Intent:** On-premises Kubernetes cluster with Envoy Gateway, PostgreSQL, Redis, OpenSearch, Keycloak

## Recommended Workflow Path

### Primary Outputs

Given existing comprehensive PRD v3.0 and architecture finalization, the workflow will focus on **implementation-ready artifacts**:

**Track A: Missing Artifacts Generation**
1. **Tech Specs for Epics** (Epic 1-6)
   - Epic 1: Foundation & Infrastructure (DB schema, auth, CI/CD, outbox pattern)
   - Epic 2-ITSM: Core ITSM Module (ticket CRUD, agent console, SLA calculation, routing)
   - Epic 3-PM: Core PM Module (issue CRUD, board, sprint management, burndown)
   - Epic 4: Workflow Engine (approvals, routing automation, state machines)
   - Epic 5: Unified Dashboard & Real-Time (SSE infrastructure, composite APIs)
   - Epic 6: SLA Management & Reporting (advanced reporting, custom SLA policies)

2. **UX Specifications**
   - Zero-Hunt Agent Console (Loft Layout, Composite Side-Panel API, Suggested Resolution, Autopilot Rail)
   - Developer Copilot Board (Card Intelligence, Auto-PR Scaffolding, Contract-First Guard, QA/Env Awareness)
   - Approval Cockpit (Approval Bundles, Policy×Budget×Inventory Card, Simulation Engine)

3. **API Contracts** (OpenAPI 3.0 specs)
   - ITSM API: Ticket CRUD, composite queue cards, side-panel bundle, assignment, SLA endpoints
   - PM API: Issue CRUD, board operations, sprint management, card intelligence
   - Workflow API: Approval actions, routing rules config, batch authorization
   - Real-time API: SSE endpoints, reconnect cursor, event schemas

4. **Database Schema DDL**
   - ITSM OLTP tables (tickets, incidents, service_requests, ticket_comments, routing_rules)
   - ITSM read models (ticket_card, sla_tracking, related_incidents)
   - PM OLTP tables (issues, sprints, boards, board_columns, issue_links)
   - PM read models (queue_row, issue_card, sprint_summary)
   - Shared tables (users, roles, approvals, workflow_states, audit_log, outbox)

5. **User Stories Breakdown**
   - Epic decomposition into implementable stories (3-8 story points each)
   - Story acceptance criteria with Given/When/Then format
   - Story dependencies and sequencing

**Track B: Validation & Enhancement**
6. **PRD Validation Report**
   - Checklist validation against PRD v3.0
   - Gap analysis (missing sections, inconsistencies)
   - Recommendations for enhancement

7. **Architect Handoff Package**
   - High-Level Design (HLD) template
   - Low-Level Design (LLD) guidance for critical components
   - Technology stack validation
   - Performance model validation

**Track C: Implementation Artifacts**
8. **Sprint Planning Breakdown**
   - 20 weeks → 10 sprints (2-week sprints)
   - Story allocation per sprint
   - Velocity planning (40 points/sprint baseline)
   - Sprint goals and demo milestones

9. **Team Allocation Matrix**
   - Team A (ITSM): 2 BE + 1 FE assignments
   - Team B (PM): 2 BE + 1 FE assignments
   - Shared resources coordination (Infra, QA)
   - Parallel vs sequential epic execution

10. **Dependency Mapping**
    - Critical path analysis (Epic 1 → Epic 2&3 parallel → Epic 4 → Epic 5)
    - Cross-epic dependencies (e.g., Workflow Engine depends on ITSM/PM foundations)
    - External dependencies (Keycloak, Envoy Gateway, OpenSearch setup)
    - Risk mitigation for blocking dependencies

11. **Testing Strategy Document**
    - Unit testing (JUnit 5 + Mockito, >80% coverage for domain logic)
    - Integration testing (Spring Boot Test + Testcontainers)
    - Contract testing (Spring Cloud Contract for API versioning)
    - E2E testing (Playwright for web UI workflows)
    - Performance testing (Gatling scenarios, 250 concurrent users)
    - Accessibility testing (axe-core + Lighthouse + manual screen reader)

### Workflow Sequence

**Phase 1: Analysis & Validation (Current Step)**
- ✅ Load existing PRD v3.0 and brainstorming results
- 🔄 Create project-workflow-analysis.md
- ⏳ Validate PRD against checklist
- ⏳ Generate validation report

**Phase 2: Epic-Level Tech Specs (Weeks 1-4 parallel with Epic 1)**
- ⏳ Generate Epic 1 tech spec (Foundation & Infrastructure)
- ⏳ Generate Epic 2 tech spec (Core ITSM Module)
- ⏳ Generate Epic 3 tech spec (Core PM Module)
- ⏳ Generate Epic 4 tech spec (Workflow Engine)
- ⏳ Generate Epic 5 tech spec (Unified Dashboard)
- ⏳ Generate Epic 6 tech spec (SLA Reporting - optional Phase 2)

**Phase 3: UX Specifications (Weeks 1-6 parallel with foundation)**
- ⏳ Generate Zero-Hunt Agent Console UX spec
- ⏳ Generate Developer Copilot Board UX spec
- ⏳ Generate Approval Cockpit UX spec

**Phase 4: API Contracts & Schema (Weeks 2-5)**
- ⏳ Generate OpenAPI 3.0 specs for all modules
- ⏳ Generate database schema DDL (Flyway migrations)
- ⏳ Generate event schema documentation (outbox format, versioning)

**Phase 5: User Stories Breakdown (Weeks 1-3)**
- ⏳ Decompose Epic 1 into stories
- ⏳ Decompose Epic 2 into stories
- ⏳ Decompose Epic 3 into stories
- ⏳ Decompose Epic 4 into stories
- ⏳ Decompose Epic 5 into stories
- ⏳ Decompose Epic 6 into stories (if included)

**Phase 6: Implementation Planning (Weeks 1-2)**
- ⏳ Generate sprint planning breakdown
- ⏳ Generate team allocation matrix
- ⏳ Generate dependency mapping
- ⏳ Generate testing strategy document

**Phase 7: Architect Handoff**
- ⏳ Create HLD/LLD guidance package
- ⏳ Validate technology stack against PRD assumptions
- ⏳ Performance model validation (250 concurrent users, p95 latency budgets)

### Next Actions

**Immediate (Today):**
1. ✅ Complete project-workflow-analysis.md
2. ⏳ Run PRD validation checklist → Generate validation report
3. ⏳ Begin Epic 1 tech spec (Foundation & Infrastructure) - CRITICAL PATH

**Short-term (This Week):**
4. ⏳ Generate UX specifications for breakthrough features (Zero-Hunt Console, Developer Copilot Board, Approval Cockpit)
5. ⏳ Generate OpenAPI 3.0 contracts for composite endpoints (queue cards, side-panel bundle, approve-with-guardrails)
6. ⏳ Generate database schema DDL for OLTP tables and read models
7. ⏳ Decompose Epic 1 into implementable stories (foundation work)

**Medium-term (Next 2 Weeks):**
8. ⏳ Generate Epic 2 & 3 tech specs (ITSM + PM parallel development)
9. ⏳ Complete user stories breakdown for Epics 2-3
10. ⏳ Generate sprint planning breakdown (20 weeks → 10 sprints)
11. ⏳ Generate team allocation matrix and dependency mapping

**Before Development Kickoff (Week 1 of implementation):**
12. ⏳ Complete all tech specs (Epics 1-5)
13. ⏳ Finalize API contracts (OpenAPI specs published)
14. ⏳ Complete testing strategy document
15. ⏳ Architect handoff package ready (HLD/LLD guidance)

## Special Considerations

### Critical Architecture Decisions Already Finalized

**Modulith + 3 Companions Pattern:**
- Core Application (Spring Boot Modulith): ITSM Module, PM Module, Workflow Module, Shared Services
- Companion 1: Event Worker (drains outbox, fans out to Redis Stream, updates read models)
- Companion 2: Timer/SLA Service (durable scheduler, business calendar logic, O(1) create/update/cancel)
- Companion 3: Search Indexer (maintains OpenSearch for correlation/discovery)

**Infrastructure Stack:**
- Kubernetes (4 Deployments: App, Event Worker, Timer/SLA, Search Indexer)
- PostgreSQL 16+ (OLTP + read models, 4 vCPU, 16GB RAM, 500GB SSD)
- Redis 7.x (SSE fan-out via Redis Stream, hot ACL cache, rate limiting)
- OpenSearch (minimal on-ramp, 1 hot node + 1 replica)
- Keycloak 23.x (existing on-prem, OIDC/SAML SSO)
- Envoy Gateway (existing on-prem with Kubernetes Gateway API)

**Performance Budgets (Non-Negotiable):**
- Queue p95 <200ms, side-panel p95 <300ms, p99 <800ms
- Event lag (DB→UI) ≤2s p95; reconnect catch-up <1s
- Timer precision drift <1s; missed escalations = 0
- Search p95 <300ms; index lag p95 <2s
- Auth overhead <10% of request time; batch-check 100 IDs p95 <3ms

### UX Innovation Breakthroughs (From Brainstorming Session)

**Zero-Hunt Agent Console (ITSM differentiator):**
- Paradigm shift: "Approve Station, not Research Desk"
- Loft Layout: Priority Queue (left) | Action Canvas (center) | Autopilot Rail (right)
- Composite Side-Panel API: One call returns requester history, related incidents, top-3 KB, asset info
- Suggested Resolution with "Why?" explainability
- Keyboard-first navigation (J/K/Enter/Ctrl+Enter)
- Success Gates: Time-to-first-action −30%, first-touch resolution +20%, SLA breach <5%

**Developer Copilot Board (PM differentiator):**
- Paradigm shift: "Live Reasoning Surface, not Static Task Tracker"
- Card Intelligence: Ambiguity Score, Estimate Reality Check, Blocker Detection
- Auto-PR Scaffolding: 3s to generate branch, PR template, CI checks, reviewers, test stubs
- Contract-First Guard: Blocks "In Progress" until OpenAPI spec merged
- QA/Env Awareness: "QA env down; moving to Done will stall deploy. Hold?"
- Success Gates: PR scaffolding <3s, ambiguity flagging ≥70%, QA guards ≥90% prevention

**Approval Cockpit (Workflow differentiator):**
- Paradigm shift: "Decision Engine, not Table with Buttons"
- Approval Bundles: Group by policy archetype + risk profile, one-click approves bundle
- Policy×Budget×Inventory Card: All context in one mini-strip
- Inline Simulation: Preview budget impact, inventory depletion, ETAs before commit
- Success Gates: AI-assisted ≥60%, decision time p50 <12s/p95 <45s, audit completeness 100%

### Key Insights from Brainstorming Session

1. **Physics > Features** - Build for workload physics first (context-at-fingertips, fan-out events, clock-precision timers)
2. **AI as Copilot, Not Autopilot** - Put AI in approval loop with explainability ("Why?") and escape hatches
3. **Combine > Add** - Fusion beats fragmentation (Policy×Budget×Inventory in one card vs 3 tabs)
4. **Modulith + Companions** - Tighten modulith, add single-purpose companions; keep dev velocity while isolating physics loops
5. **Tripwires > Rewrites** - Define WHEN to change architecture with metrics (p95 >400ms, deploy cadence diverges)
6. **Composite Over CRUD** - Task-shaped composite APIs (queue cards, side-panel bundle) are product surface; CRUD is plumbing
7. **Latency Budgets = Product Requirements** - Gate releases on p95/p99 budgets (queue <200ms, side-panel <300ms, event lag ≤2s)

### Risk Factors

**High Risk:**
1. **ML/AI Pipeline Maturity** - Suggested Resolution engine, Ambiguity Score NLP, Estimate Reality Check require training data. Cold start problem for new platform.
   - Mitigation: Transfer learning from public ITSM datasets, synthetic data generation, phased rollout with confidence thresholds

2. **Timer/SLA Service Precision** - Business calendar logic, O(1) mutations, zero missed escalations is technically challenging
   - Mitigation: Dedicated spike in Epic 1 (weeks 3-4), prove outbox → timer service contract early

3. **Real-Time Collaboration Edge Cases** - SSE connection storms on reconnect, conflict resolution when two agents edit same ticket
   - Mitigation: Idempotent reducers, version-based deduplication, throttling on reconnect, chaos testing in Epic 5

**Medium Risk:**
4. **OpenSearch Integration Complexity** - Write-behind indexing with <2s lag, correlation precision @k≥5 >0.6
   - Mitigation: Minimal on-ramp in Epic 1, can defer advanced correlation to Phase 2

5. **Team Velocity Assumptions** - 40 points/sprint assumes experienced team, stable requirements
   - Mitigation: Buffer with Epic 6 as optional, critical path is 18 weeks (not 20)

6. **UX Innovation Adoption** - Keyboard-first navigation (J/K/Enter) may have learning curve for agents
   - Mitigation: Onboarding tooltips, help overlay (?), progressive disclosure

**Low Risk:**
7. **Technology Stack Maturity** - Spring Boot 3.5, PostgreSQL 16, Redis 7, Keycloak 23 are all proven
8. **Architecture Pattern Validation** - Modulith + Companions validated through Assumption Reversal technique
9. **Performance Budgets** - CQRS-lite read models + batch auth proven patterns for 250 concurrent users

## Technical Preferences Captured

**Backend Stack:**
- Spring Boot 3.5.x (modulith with Spring Modulith library)
- Java 21 (LTS)
- PostgreSQL 16+ (OLTP + read models)
- Redis 7.x (SSE fan-out, caching)
- OpenSearch (search/correlation)
- Flyway (schema migrations)
- HikariCP (connection pooling, maxPoolSize=50)

**Frontend Stack:**
- React 18
- Vite (build tool)
- Ant Design (AntD) 5.x (component library)
- TypeScript (strongly implied by enterprise nature)

**Infrastructure:**
- Kubernetes (on-premises cluster)
- Envoy Gateway (Kubernetes Gateway API)
- Helm (deployment)
- Docker (containerization)

**Testing:**
- JUnit 5 + Mockito (unit tests)
- Spring Boot Test + Testcontainers (integration tests)
- Spring Cloud Contract (contract tests)
- Playwright (E2E tests)
- Gatling (performance tests)
- axe-core + Lighthouse (accessibility tests)

**Observability:**
- OpenTelemetry (distributed tracing, metrics, logs)
- Prometheus + Grafana (metrics dashboards)
- Structured logging (JSON format)

**CI/CD:**
- Build: Compile, unit tests, ArchUnit module validation
- Security: SAST (Qodana), dependency scanning (OWASP Dependency-Check + Trivy)
- Integration Tests: Testcontainers
- E2E Tests: Playwright against staging
- Performance Gate: Gatling smoke test (50 users, p95 <400ms)
- Deploy: Blue/green to production

**Design Patterns:**
- CQRS-lite (denormalized read models for hot reads)
- Transactional Outbox (guaranteed event delivery)
- Event-Driven Architecture (outbox → fan-out → SSE)
- Durable Scheduler (Timer/SLA Service)
- Batch Authorization (scope-first SQL, batch checks)
- Composite APIs (task-shaped endpoints spanning entities)
- Idempotent Operations (version-based deduplication)

**Accessibility:**
- WCAG 2.1 Level AA compliance (mandatory)
- Keyboard navigation for all interactive elements
- Screen reader compatibility (ARIA labels)
- Color contrast ratios ≥4.5:1 for text
- Focus indicators with 2px visible outline

**Browser Support:**
- Modern evergreen browsers only (Chrome, Firefox, Safari, Edge - last 2 versions)
- No IE11 support

---

_This analysis serves as the routing decision for the adaptive PRD workflow and will be referenced by future orchestration workflows._
