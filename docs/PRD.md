# SynergyFlow Product Requirements Document (PRD)

**Author:** monosense
**Date:** 2025-10-17
**Project Level:** Level 4 (Platform/Ecosystem)
**Project Type:** Web Application (Full-stack ITSM+PM Platform)
**Target Scale:** 250-1,000 users (small to mid-market organizations)

---

## Description, Context and Goals

### Project Description

**SynergyFlow** is a unified ITSM+PM platform with intelligent workflow automation that eliminates the fragmentation, context-switching overhead, and manual toil plaguing today's IT and project management operations.

**Core Value Proposition:**

SynergyFlow delivers integrated ITSM (Incident, Problem, Change, Release, Knowledge, CMDB) and PM (Project, Epic, Story, Task, Sprint) capabilities with breakthrough automation and enterprise-grade governance. The platform combines full feature parity with ManageEngine ServiceDesk Plus (ITSM) and JIRA (PM) with an event-driven architecture and policy-based automation layer.

**Strategic Differentiation:**

1. **"Magic and Audits Like a Bank" Philosophy:** Breakthrough automation paired with enterprise-grade governance through explainable automation (OPA decision receipts), immutable audit trails, and shadow mode testing for all policies.

2. **Event-First Architecture:** Spring Modulith event-driven modular monolith with transactional outbox pattern, typed Java record events with compile-time validation, canonical IDs propagating across module boundaries, in-memory event bus with <100ms event lag.

3. **Safety-First Innovation:** Shadow → Canary → Full rollout for all ambitious automation, error budgets gating policy deployment, compensation logic for cross-module sagas.

4. **Developer + Business User Access:** BPMN visual modeler (Flowable) for business analysts, code-first option for developers, policy-as-code (Rego) versioned in Git.

**Target Market:**

Small to mid-market organizations (250-1,000 users) seeking integrated ITSM+PM with workflow automation, particularly in markets where local data residency, explainable AI, and open-source-first approaches create competitive advantages. Initial focus: APAC region (Indonesia data residency).

**Business Opportunity:**

Organizations waste 30-40% of agent productivity on context switching, manual data entry, and rigid approval workflows across disconnected ITSM and PM tools. SynergyFlow's unified platform with Trust + UX Foundation Pack eliminates these pain points through:

- Single-entry time logging (eliminate double entry)
- Link-on-action cross-module workflows (eliminate context switching)
- Transparent eventual consistency with freshness badges (build user trust)
- Explainable automation with decision receipts (enable governance)

### Deployment Intent

**Primary Deployment Model:** Self-hosted Kubernetes deployment

**Deployment Targets:**

- **MVP (Month 0-3):** 10 beta organizations, 100 users total, development/staging environments
- **Phase 1 (Month 3-6):** 10 production organizations, 250 users, Indonesia data residency focus
- **Phase 2 (Month 6-12):** 20 organizations, 500 users, expand to Philippines, Singapore, Malaysia
- **Long-term (Year 2+):** SaaS evolution, multi-tenant architecture, global expansion

**Infrastructure Requirements:**

- Kubernetes cluster (cloud-agnostic: AWS, GCP, Azure, or on-prem)
- Resource capacity: ~14 CPU cores (requests), ~26.5GB RAM (requests), ~350GB storage
- High availability: 3-replica PostgreSQL (CloudNative-PG), shared DragonflyDB cluster, 3-replica application pods
- Monitoring: Victoria Metrics + Grafana
- GitOps: Flux CD with Kustomize overlays
- Image Registry: Harbor (private registry)

**Compliance Requirements:**

- Indonesia data residency (self-hosted deployment in Indonesian data center)
- Audit trail for all automated decisions (OPA decision receipts)
- Role-based access control (RBAC with OPA policies)
- No BSrE compliance requirements (confirmed)

### Context

**Current Problem Landscape:**

Organizations using separate ITSM and PM tools face three critical pain points that drain productivity and increase operational costs:

**1. Context-Switching "Whiplash" (30-40% Productivity Loss)**

IT teams and project managers lose 2-3 hours per day switching between disconnected tools:
- **"Tab Fatigue":** Agents juggle 8-12 browser tabs (ServiceDesk, JIRA, email, Slack, monitoring tools)
- **Double Data Entry:** Log work in ServiceDesk incident, then again in JIRA task, then again in timesheet
- **Relationship Blindness:** Cannot see incident → change → release connections without manual correlation
- **Lost Context:** Switching tools means re-orienting to different UIs, terminologies, and workflows

At 250 users, this represents **~20,000 lost productivity hours/year** (250 users × 2.5 hrs/day × 220 workdays × 40% inefficiency rate).

**2. Rigid Workflows and Manual Routing (70% of Tickets Misrouted)**

Existing tools force rigid, one-size-fits-all approval and routing workflows:
- **Hard-Coded Approvals:** Change approval routing baked into code, requiring developer intervention to modify
- **First-Available Assignment:** Tickets routed to first available agent, ignoring skills, capacity, SLAs
- **No Auto-Escalation:** SLA breaches require manual monitoring and escalation
- **Approval Bottlenecks:** CAB approvals delay changes 3-5 days, no risk-based auto-approval

Misrouted tickets extend MTTR by 40%, escalation delays add 6-8 hours per incident, rigid approvals delay changes by 3-5 days.

**3. Disconnected Systems Creating Data Islands**

ITSM and PM tools don't share data, creating blind spots:
- **No Change → Incident Correlation:** Cannot trace incident spike to recent deployment
- **CMDB Isolation:** Configuration items in ServiceDesk invisible to project teams
- **Duplicate Effort:** Teams track same work in both systems
- **No Cross-Module Reporting:** Cannot measure end-to-end incident → problem → change → release lifecycle

**Why Existing Solutions Fall Short:**

- **ManageEngine ServiceDesk Plus:** Comprehensive ITSM but no PM capabilities (requires separate ManageEngine Projects), rigid workflows with limited automation, no event-driven architecture
- **Atlassian JIRA Service Management + JIRA:** Strong PM but limited ITSM depth (service request focused, not incident/problem/change), fragmented (requires 3-5 Atlassian products), complex pricing
- **ServiceNow:** Comprehensive but extremely expensive (5-10x alternatives), complexity overload (requires dedicated admin teams), vendor lock-in

**Why Now:**

Three converging trends make unified ITSM+PM with workflow automation urgent:

1. **Remote-First Operations:** Distributed teams need tighter tool integration to maintain coordination
2. **AI/Automation Expectations:** Organizations demand explainable automation with governance (not black-box AI)
3. **Event-Driven Architecture Maturity:** Spring Modulith modular monolith architecture and workflow engines (Flowable) provide enterprise-grade event-driven capabilities with operational simplicity

### Strategic Goals

**1. Achieve Market Adoption and User Scale**
- **Target:** 250 active users by Month 6 (50-75 peak concurrent)
- **Metric:** Monthly Active Users (MAU), Daily Active Users (DAU), 10 production organizations
- **Success Criteria:** ≥60% adoption rate within organizations (users actively using SynergyFlow vs legacy tools)

**2. Demonstrate Quantifiable Productivity Gains**
- **Target:** MTTR ↓20-40% (Mean Time To Resolution for incidents)
- **Target:** Change Lead Time ↓30% (Time from change request to deployment)
- **Target:** Agent Throughput ↑15-25% (Tickets handled per agent per day)
- **Metric:** Track pre-adoption vs post-adoption operational metrics with beta customers

**3. Achieve High User Satisfaction and Trust in Automation**
- **Target:** Net Promoter Score (NPS) ≥40
- **Target:** User Satisfaction ≥4.0/5.0 (quarterly surveys)
- **Target:** ≥80% of users "trust the system's automated decisions" (survey)
- **Metric:** Quarterly NPS surveys, CSAT post-ticket closure, trust surveys

**4. Deliver Measurable Time Savings Through Platform Consolidation**
- **Target:** ↓50% context switching time (save 1.25 hours/day per user from 2.5 hour baseline)
- **Target:** 70% of users use Single-Entry Time Tray weekly
- **Target:** 60% of users use Link-on-Action monthly
- **Metric:** User surveys, time-tracking analytics, feature usage analytics

**5. Establish Technical Foundation for Scale and Reliability**
- **Target:** Architecture validated for 1,000 users (4x MVP target)
- **Target:** System availability ≥99.5% uptime
- **Target:** Event processing lag <100ms (projection lag p95 <200ms, in-memory event bus)
- **Target:** Zero workflow state loss incidents, zero data loss or corruption
- **Metric:** Technical performance benchmarks, scalability test results, incident tracking

**6. Achieve High Automation Adoption with Governance**
- **Target:** 80% of low-risk changes auto-approved by policy
- **Target:** Policy evaluation latency <100ms (p95)
- **Target:** 100% of automated decisions have explainable decision receipts
- **Metric:** Auto-approval rates, policy performance metrics, decision receipt coverage

**7. Demonstrate Customer ROI and Financial Viability**
- **Target:** Customer ROI within 1 month through productivity gains
- **Target:** $75k ARR by Month 6 (250 users × $300/user/year)
- **Target:** Break-even by Month 19-24
- **Metric:** Customer case studies, revenue tracking, cost savings analysis

## Requirements

### Functional Requirements

#### A. Trust + UX Foundation Pack (MVP - 10 Week Sprint)

**FR-1: Event System Implementation**
- All events use Java record types with Spring Modulith ApplicationEvents
- Events stored in `event_publication` table (transactional outbox pattern)
- Event metadata included for audit trail (correlation IDs, causation IDs, timestamps)
- Canonical IDs (Incident ID, Change ID, Task ID) propagate across module boundaries
- Type-safe events with compile-time validation (no schema registry needed)

**FR-2: Single-Entry Time Tray**
- Global worklog entry UI accessible from top navigation bar
- Time entries automatically mirror to both incident and task worklog tables
- Display aggregate time per entity (incident, task, project) in real-time
- Support bulk time entry (log time against multiple entities simultaneously)
- Immutable audit trail for all time entries with user/timestamp

**FR-3: Link-on-Action (Cross-Module Workflows)**
- "Create related change" from incident auto-populates change request with incident context
- "Create related task" from incident auto-links bidirectionally (incident ↔ task)
- "Create related incident" from task auto-links bidirectionally (task ↔ incident)
- Relationship graph queryable via REST API (/api/relationships/{entity-type}/{entity-id})
- UI displays related entities across modules with freshness badges

**FR-4: Freshness Badges (Projection Lag Visibility)**
- Every cross-module query displays data currency badge ("2.3 seconds ago")
- Color-coded thresholds: Green (<3s), Yellow (3-10s), Red (>10s)
- Projection lag tracked per consumer (incident → task consumer, etc.)
- Admin dashboard shows aggregate projection lag metrics (p50, p95, p99)
- Alert if projection lag exceeds 10 seconds for more than 5 minutes

**FR-5: Policy Studio MVP + Decision Receipts**
- Visual policy editor for creating/editing OPA Rego policies
- Decision receipt generated for every policy evaluation ("Why was this auto-approved?")
- Shadow mode testing: log policy decisions without enforcing (pre-production validation)
- Audit log stores all policy decisions with timestamp, policy version, input data, decision output
- Integration with Spring Security for authorization decisions

#### B. ITSM Core Modules (MVP + Phase 2)

**FR-6: Incident Management (MVP)**
- Incident CRUD with priority (Low/Medium/High/Critical) and severity (S1-S4) classification
- Assignment and routing (manual for MVP, policy-driven auto-routing in Phase 2)
- SLA timer-based tracking using Flowable BPMN timers (4-hour resolution SLA)
- Incident lifecycle: New → Assigned → In Progress → Resolved → Closed
- Comments, worklog, and attachments support
- Event publication: IncidentCreated, IncidentAssigned, IncidentResolved, IncidentClosed

**FR-7: Problem Management (Phase 2)**
- Problem record CRUD with linkage to multiple incidents
- Known Error Database (KEDB) with resolution procedures
- Trend reporting for recurring issues (incident spike correlation)
- Root cause analysis workflow

**FR-8: Change Management (MVP)**
- Change request CRUD with risk assessment (Low/Medium/High/Emergency)
- Approval routing (manual for MVP, policy-driven in Phase 2)
- Change calendar view with conflict detection
- Deployment tracking (Scheduled → In Progress → Completed → Failed)
- Rollback planning fields and procedures
- Event publication: ChangeRequested, ChangeApproved, ChangeDeployed

**FR-9: Release Management (Phase 2)**
- Release assembly with multiple changes
- Release gates and promotion workflow (Dev → Staging → Production)
- Deployment tracking with status audit trail

**FR-10: Service Request Management (Phase 2)**
- Service catalog with request fulfillment workflows
- Dynamic form schema engine for custom request types
- Approval flow executor per service type

**FR-11: Knowledge Management (MVP)**
- Knowledge article CRUD with versioning (v1.0, v1.1, etc.)
- Approval workflow (Draft → Review → Approved → Published)
- Full-text search with relevance ranking and tagging
- Related articles suggestion
- Expiry notification scheduler for knowledge article review

**FR-12: CMDB (Configuration Management Database) (Phase 2)**
- CI (Configuration Item) entity model with types (Server, Application, Database, Network Device)
- CI relationship graph (supports, runs-on, depends-on)
- Impact assessment engine (which changes affect this CI?)
- Baseline snapshots and diff management

**FR-13: IT Asset Management (ITAM) (Phase 2)**
- Asset inventory with lifecycle tracking (Purchase → Active → Maintenance → Retired)
- License monitoring and compliance alerts
- Mobile QR/barcode scanning for asset updates

#### C. Project Management (PM) Core (MVP + Phase 2)

**FR-14: Project and Task Management (MVP)**
- Project CRUD with description, owner, team assignment
- Epic/Story/Task hierarchy with parent-child relationships
- Sprint planning (basic): create sprints, assign stories, set sprint goals
- Board view (Kanban): columns (To Do, In Progress, Done), drag-drop status updates
- Backlog management with priority ordering
- Task assignment and status tracking (Not Started → In Progress → Done)
- Event publication: TaskCreated, TaskAssigned, TaskCompleted

**FR-15: Agile Workflow (Phase 2)**
- Sprint burndown charts
- Velocity tracking
- Epic roadmap visualization
- Release planning

#### D. User and Team Management (MVP)

**FR-16: User Profiles and Authentication**
- User profile CRUD (name, email, role, team, skills, capacity)
- Team assignment with multi-team support
- Role-based access control (RBAC) using OPA policies
- SSO integration via OAuth2 Resource Server with JWT validation
- Session management with configurable timeout

**FR-17: Multi-Team Support and Routing (Phase 2)**
- Teams, agents, skills, and capacity data model
- Intelligent routing scoring engine with policy evaluator
- Escalation tiers and triggers based on SLA breach
- Business hours, timezone, and availability calendars

#### E. Workflow Orchestration and Automation (MVP + Phase 2)

**FR-18: Flowable Workflow Engine (MVP)**
- Embedded Flowable 7.1.0+ in Spring Boot application
- BPMN timer events for SLA tracking (4-hour incident resolution SLA)
- Async job executor for timer-based workflows
- Workflow state persistence in PostgreSQL (durable across restarts)
- Admin UI for monitoring active workflows and job executor queue

**FR-19: OPA Policy Engine (MVP)**
- OPA sidecar container deployed alongside Spring Boot pods
- Rego policy bundles distributed via CI/CD pipeline
- Policy evaluation REST API (POST /v1/data/synergyflow/{policy-name})
- Decision receipt API for explainability
- Shadow mode support for pre-production policy testing

**FR-20: Advanced Automation (Phase 2)**
- Impact Orchestrator: real-time signal fusion (SLOs, topology, calendars), auto-tuning SLAs/routing/priorities
- Self-Healing Engine: known error matching, runnable knowledge articles, autonomous execution with rollback
- Predictive Prevention: change window guards, risk scoring, conflict radar

#### F. Integration and API Gateway (MVP + Phase 2)

**FR-21: API Gateway (MVP)**
- Envoy Gateway with Kubernetes Gateway API
- JWT validation for all protected routes (/api/*)
- Rate limiting by authenticated client (100 req/min default)
- CORS configuration for frontend origin
- Request/response logging with correlation IDs

**FR-22: REST API (MVP)**
- OpenAPI 3.0 specification for all endpoints
- Standardized error responses (RFC 7807 Problem Details)
- Pagination support (limit/offset or cursor-based)
- Filtering and sorting query parameters
- API versioning (v1, v2) in URL path

**FR-23: Webhook Subscriptions (Phase 2)**
- Webhook subscription service with retry logic and HMAC signing
- Event catalog with schema viewer
- Partner integration sandbox console

**FR-24: Event Publishing Standards (MVP)**
- Spring Modulith ApplicationEvents with Java record types
- In-JVM event bus with transactional outbox pattern
- Event publication table for durable messaging with automatic retry
- Correlation ID propagation across all events
- Natural migration path to Kafka via event externalization if needed later

#### G. Notifications and Communication (Phase 2)

**FR-25: Notification System**
- Template engine with localization support
- User preference subscriptions (email, in-app, push)
- Multi-channel adapters (email, push, in-app notification center)
- Deduplication, grouping, and digest mode
- Delivery metrics and audit reporting

#### H. Dashboards, Reports, and Analytics (Phase 2)

**FR-26: Dashboards**
- Dashboard model with widget framework and drag-n-drop UI
- Live tiles with event stream updates
- Personal and shared dashboards

**FR-27: Reporting**
- Report builder with guardrails (pre-defined templates)
- Report scheduler with email delivery
- Export formats (PDF, CSV, Excel)

**FR-28: Analytics**
- Dataset contracts and export services for BI tools
- Row-level security for data access
- ML pipeline templates for predictive analytics

#### I. Security, Compliance, and Observability (MVP + Phase 2)

**FR-29: Authorization Service (MVP)**
- RBAC with OPA policy engine
- ABAC (Attribute-Based Access Control) for fine-grained permissions
- Policy versioning and shadow mode testing

**FR-30: Audit Pipeline (MVP)**
- Centralized audit pipeline with signed logs (tamper-proof)
- Audit log retention: 90 days hot storage, 7 years cold storage (S3)
- Audit log query API for compliance reporting

**FR-31: Secrets Management (Phase 2)**
- Integration with 1Password via External Secrets Operator (ESO)
- Secret rotation automation
- Secret references in configuration (no plaintext secrets)

**FR-32: Compliance Controls (Phase 2)**
- Compliance evidence collection for SOC 2, ISO 27001
- Data residency enforcement (Indonesia deployment)
- GDPR-compliant data retention policies

**FR-33: Observability (MVP)**
- Victoria Metrics + Grafana for metrics visualization
- OpenTelemetry tracing with correlation IDs
- Structured JSON logging with log levels (DEBUG, INFO, WARN, ERROR)
- Health check endpoints (/actuator/health) for liveness and readiness probes

### Non-Functional Requirements

**NFR-1: Performance**
- API response time: p95 <200ms, p99 <500ms
- Event processing lag (projection lag): <100ms end-to-end (p95 <200ms, in-memory event bus)
- Policy evaluation latency: <100ms p95 (target <10ms for sidecar OPA)
- Database complex queries: <1 second response time
- Frontend page load: <2 seconds (First Contentful Paint), <4 seconds (Time to Interactive)
- Concurrent users supported: 250 peak concurrent, 1,000 total users

**NFR-2: Scalability**
- Architecture validated for 4x growth: 1,000 concurrent users minimum
- Application horizontal scaling: support up to 10 replicas (in-JVM event bus scales with replicas)
- Database connection pooling: HikariCP with 20 connections per replica
- Load testing verified: 1,000 concurrent users
- Natural migration path to Kafka if external event consumers needed in future

**NFR-3: Reliability and Availability**
- System uptime: ≥99.5% availability (target 99.9% = ~43 minutes downtime/month)
- Zero workflow state loss: Flowable durable state persistence
- Zero data loss: PostgreSQL synchronous replication with min_in_sync_replicas=2
- Event delivery guarantee: At-least-once delivery via transactional outbox pattern
- Graceful degradation: freshness badges show projection lag, system remains operational
- Error budgets: automation paused if SLA breaches exceed 5% threshold

**NFR-4: Security**
- Authentication: OAuth2 Resource Server with JWT validation (RS256 asymmetric signing)
- Authorization: RBAC with OPA policies, all API endpoints protected
- Data encryption: TLS 1.3 in transit, PostgreSQL transparent data encryption at rest
- Secret management: No plaintext secrets in configuration, 1Password + ESO integration
- Session management: JWT with configurable expiration (default 1 hour), refresh token support
- API rate limiting: 100 req/min per authenticated client (configurable per client)
- CORS: Strict origin validation, preflight caching

**NFR-5: Auditability and Compliance**
- Audit trail: All automated decisions logged with timestamp, user, policy version, input data, decision output
- Decision receipts: 100% coverage for all policy evaluations (explainable automation)
- Audit log retention: 90 days hot (PostgreSQL), 7 years cold (S3)
- Tamper-proof logs: Cryptographic signing of audit log entries
- Data residency: Indonesia deployment support (self-hosted in Indonesian data center)
- Compliance readiness: SOC 2, ISO 27001 evidence collection (Phase 2)

**NFR-6: Observability and Monitoring**
- Metrics: Victoria Metrics with 15-second scrape interval, Grafana dashboards
- Tracing: OpenTelemetry with correlation IDs propagated across all services
- Logging: Structured JSON logs with correlation IDs, log levels (DEBUG, INFO, WARN, ERROR)
- Health checks: Liveness (/actuator/health/liveness) and readiness (/actuator/health/readiness) probes
- Alerting: Prometheus-compatible alerts for SLO breaches (API latency, projection lag, error rate)
- Business metrics: MTTR, throughput, adoption tracked in real-time dashboards

**NFR-7: Maintainability and Operability**
- GitOps: Flux CD for declarative deployments, Kustomize overlays for environment-specific config
- Infrastructure as Code: All Kubernetes manifests versioned in Git
- Automated backups: PostgreSQL daily backups to S3 with 30-day retention
- Point-in-time recovery (PITR): PostgreSQL Write-Ahead Log (WAL) archiving
- Rolling updates: Zero-downtime deployments with readiness probes and graceful shutdown (60s termination grace period)
- Configuration management: ExternalSecrets Operator for 1Password integration
- Runbooks: Documented procedures for all operational tasks (backup, restore, scaling, incident response)

**NFR-8: Usability and Accessibility**
- Responsive design: Mobile-first web UI, browser support (Chrome, Firefox, Safari, Edge latest 2 versions)
- Keyboard navigation: Full keyboard accessibility for all UI components
- Screen reader support: ARIA labels for assistive technologies (WCAG 2.1 AA target, Phase 2)
- Internationalization: English (MVP), support for additional languages (Phase 2)
- User onboarding: Guided tour for first-time users, contextual help tooltips
- Error messages: User-friendly error messages with actionable guidance

**NFR-9: Testability**
- Unit test coverage: ≥80% code coverage for business logic
- Integration test coverage: All API endpoints, event consumers, policy evaluations
- Contract testing: Spring Cloud Contract for event schema validation
- End-to-end testing: Selenium/Playwright for critical user journeys
- Performance testing: JMeter load tests for 1,000 concurrent users
- Chaos engineering: Chaos Mesh for failure injection testing (Phase 2)

**NFR-10: Extensibility and Integration**
- REST API: OpenAPI 3.0 specification, versioned endpoints
- Webhook support: Outbound webhooks with retry logic and HMAC signing (Phase 2)
- Event-driven integration: Spring Modulith event externalization for future Kafka/external system integration
- Plugin architecture: Support for custom policy plugins (Phase 2)
- BI tool integration: Dataset export for Tableau, Power BI, Looker (Phase 2)

## User Journeys

### Journey 1: Incident Response and Cross-Module Resolution (Primary Persona: IT Service Desk Agent)

**Scenario:** A critical incident occurs affecting production services. Agent needs to investigate, resolve, and track associated change work.

**Steps:**

1. **Incident Alert Receipt**
   - Agent receives alert via email/push notification: "INC-1234: Production API returning 500 errors"
   - Clicks notification, redirects to SynergyFlow incident detail page
   - **Freshness Badge:** "Data current as of 1.2 seconds ago" (green) - builds trust in real-time data

2. **Incident Investigation**
   - Reviews incident details: Priority=Critical, Severity=S1, SLA=4 hours
   - **SLA Timer:** Flowable timer shows "3h 47m remaining" with yellow warning (approaching breach)
   - Checks related entities: **Link-on-Action** shows related recent deployment (CHANGE-567) via Work Graph
   - **Freshness Badge:** "Data current as of 0.05 seconds ago" (green) - in-memory event bus
   - Correlation discovered: incident spike correlates to deployment 2 hours ago

3. **Time Logging with Single-Entry**
   - Opens **Single-Entry Time Tray** from top navigation
   - Logs 30 minutes investigation time: "Analyzed logs, identified deployment correlation"
   - Time entry **automatically mirrors** to INC-1234 worklog and related task TASK-890
   - Agent sees confirmation: "Time logged to INC-1234, TASK-890"

4. **Creating Related Change for Rollback**
   - Clicks **"Create related change"** from incident detail page
   - SynergyFlow pre-fills change request:
     - Change Title: "Rollback deployment CHANGE-567 (incident INC-1234)"
     - Risk: High (auto-populated based on incident severity)
     - Impacted Services: API-Service (from incident context)
     - **Bidirectional link:** INC-1234 ↔ CHANGE-890 automatically established

5. **Policy-Driven Approval**
   - Submits change request CHANGE-890
   - **OPA Policy Evaluation:** Evaluates risk, impact, timing
   - **Decision Receipt:** "High-risk emergency change impacting critical service → CAB + CTO approval required (Policy v1.2.3)"
   - Change routed to CAB queue with full context
   - Agent sees transparent explanation: "Why does this need CAB approval? [View Decision Receipt]"

6. **Deployment Tracking and Resolution**
   - CAB approves change within 30 minutes (emergency process)
   - DevOps executes rollback deployment
   - **Event Flow:** ChangeDeployed event → incident auto-updated with deployment status
   - Agent verifies incident resolved, marks INC-1234 as Resolved
   - **Audit Trail:** Full history accessible showing incident → investigation → change → deployment → resolution

**Outcome:** Agent resolved critical incident in 1.5 hours (vs typical 4-6 hours), with full cross-module traceability and explainable automation.

---

### Journey 2: Project Planning with Operational Awareness (Secondary Persona: Engineering Manager)

**Scenario:** Engineering manager planning next sprint, needs to understand operational impact of recent releases.

**Steps:**

1. **Sprint Planning Preparation**
   - Opens SynergyFlow PM module, navigates to backlog
   - Reviews candidate stories for Sprint 12
   - Notices story STORY-340: "Deploy new payment gateway integration"

2. **Operational Impact Assessment**
   - **Work Graph Integration:** Checks "Related Incidents" panel
   - Sees recent deployment (last sprint's CHANGE-520) correlated with incident spike
   - **Freshness Badge:** "Incident data current as of 0.08 seconds ago" (green) - in-memory event bus
   - Identifies pattern: payment API changes frequently trigger incidents

3. **Risk Mitigation Planning**
   - Creates **related change request** from STORY-340 via Link-on-Action
   - Pre-fills change with deployment plan, rollback procedure
   - Adds "Extra QA cycle required" note based on incident history
   - Assigns story to Sprint 12 with adjusted story points (8 → 13, accounting for extra testing)

4. **Cross-Team Coordination**
   - **Link-on-Action:** Creates related task for DevOps: "Prepare blue-green deployment for payment gateway"
   - TASK-945 auto-linked to STORY-340 and CHANGE-600
   - DevOps sees task in their unified "My Work" view alongside incidents

5. **Release Monitoring**
   - After deployment, monitors change calendar
   - **Work Graph:** Automatically tracks incident correlation to CHANGE-600
   - No incident spike detected after 24 hours
   - Marks story STORY-340 as Done, closes change CHANGE-600 successfully

**Outcome:** Engineering manager identified risk early, coordinated cross-team work seamlessly, deployed safely with no incidents.

---

### Journey 3: Building Trust in Policy-Driven Automation (Primary Persona: Change Manager)

**Scenario:** Change manager transitioning from manual CAB approvals to policy-driven automation, needs to build trust incrementally.

**Steps:**

1. **Shadow Mode Testing**
   - New OPA policy created: "Auto-approve low-risk standard changes impacting <5 services"
   - Policy deployed in **Shadow Mode:** logs decisions without enforcing
   - Change manager reviews shadow mode logs for 2 weeks (100 changes evaluated)
   - **Decision Receipts:** Reviews "Would have auto-approved" decisions:
     - CHANGE-701: "Low-risk, 2 services, standard change → AUTO-APPROVE (Policy v2.0.0-shadow)"
     - CHANGE-715: "High-risk, 15 services → REQUIRES CAB (Policy v2.0.0-shadow)"

2. **Shadow Mode Validation**
   - Compares shadow mode decisions with actual CAB decisions
   - Agreement rate: 95% (95 out of 100 decisions match CAB)
   - 5 disagreements analyzed: Policy tuned to match CAB judgment
   - Policy updated to v2.1.0-shadow with refinements

3. **Canary Rollout**
   - Policy promoted to **Canary Mode:** enforces for 10% of traffic
   - Change manager monitors auto-approval outcomes for 1 week
   - **Metrics tracked:** Auto-approval rate, SLA compliance, incident correlation, rollback rate
   - Results: 80% auto-approval rate, 99% SLA compliance, zero incidents correlated to auto-approved changes

4. **Full Production Rollout**
   - Policy promoted to **Production Mode:** enforces for 100% of traffic
   - Change manager reviews weekly automation report
   - **Decision Receipt Coverage:** 100% of automated decisions have explainable receipts
   - **Audit Trail:** All decisions logged with policy version, input data, decision output

5. **Continuous Improvement**
   - Monthly policy review: analyze auto-approval outcomes, tune thresholds
   - Error budget monitoring: If SLA breaches exceed 5%, automation paused automatically
   - Team confidence established: "I trust the system because I can see exactly why every decision was made"

**Outcome:** Change manager successfully transitioned from 100% manual approvals to 80% automated approvals with full governance and explainability, reducing change lead time by 30% while maintaining zero incidents.

---

### Journey 4: End-User Self-Service Knowledge Search (Tertiary Persona: End User)

**Scenario:** End user experiencing VPN connection issue, uses self-service portal to find resolution before creating incident.

**Steps:**

1. **Knowledge Base Search**
   - User opens SynergyFlow portal, searches "VPN connection fails"
   - **Full-text search** returns 5 relevant articles ranked by relevance
   - Top result: "KB-0042: Troubleshooting VPN connection failures (v3.2)"
   - **Freshness Badge:** "Article reviewed 12 days ago" (green, within 30-day review cycle)

2. **Self-Service Resolution**
   - Follows troubleshooting steps from knowledge article
   - Step 3 resolves issue: "Clear VPN certificate cache"
   - User rates article: "Helpful" (thumbs up)
   - **Feedback Loop:** Rating increases article relevance score for future searches

3. **Proactive Notification (Avoided Incident)**
   - System tracks knowledge article usage: KB-0042 used 15 times this week
   - **Analytics:** Pattern detected, suggests creating known error record
   - IT team creates **Known Error** linked to KB-0042
   - Future VPN incidents auto-suggest KB-0042 resolution via self-healing engine (Phase 2)

**Outcome:** End user resolved issue in 5 minutes without creating incident, IT team saved 30 minutes support time, knowledge base continuously improved via feedback loop.

---

### Journey 5: Operational Dashboard and Proactive SLA Management (Primary Persona: Incident Manager)

**Scenario:** Incident manager monitors team performance, proactively identifies SLA breach risks.

**Steps:**

1. **Dashboard Monitoring**
   - Opens SynergyFlow operations dashboard (custom configured with widgets)
   - **Live Tiles:** Real-time incident count, SLA compliance %, avg MTTR
   - **Freshness Badge:** "Data current as of 2.1 seconds ago" (green) - event stream updates

2. **SLA Breach Risk Identification**
   - Widget shows "3 incidents approaching SLA breach (< 30 minutes remaining)"
   - Drills down: INC-1340 (P2, 27 minutes remaining), INC-1341 (P1, 18 minutes remaining), INC-1342 (P2, 25 minutes remaining)
   - **Flowable SLA Timers:** Pre-breach notifications sent to assigned agents

3. **Proactive Intervention**
   - Reviews INC-1341 (highest priority, shortest remaining time)
   - Agent assigned: Alice (current workload: 5 open incidents)
   - **Routing Override:** Manually reassigns INC-1341 to Bob (current workload: 2 open incidents)
   - Bob receives notification: "INC-1341 reassigned to you (SLA breach in 18 minutes)"

4. **Performance Analytics**
   - Reviews weekly MTTR trend: ↓25% over last 4 weeks (from 6.2 hours → 4.7 hours)
   - Identifies driver: Single-Entry Time Tray adoption (85% of agents using weekly)
   - **Business Metrics Dashboard:** Links productivity gain to feature adoption

**Outcome:** Incident manager prevented SLA breaches through proactive monitoring, demonstrated quantifiable productivity gains from platform adoption.

## UX Design Principles

**1. "Quiet Brain" Experience - Minimize Cognitive Load**

Users should focus on work, not tool navigation. The interface anticipates needs, surfaces relevant information proactively, and reduces decision fatigue.

- **Implementation:** Unified "My Work" view aggregates incidents, tasks, approvals across both ITSM and PM modules
- **Implementation:** Smart notifications: only alert on urgent items (SLA breach imminent, approvals required)
- **Anti-pattern:** Avoid notification spam, tab clutter, redundant confirmation dialogs

**2. Trust Through Transparency - Make the Invisible Visible**

Build user trust by making system behavior transparent and explainable, especially for automation and eventual consistency.

- **Implementation:** Freshness badges show projection lag ("Data current as of 2.3 seconds ago")
- **Implementation:** Decision receipts explain every automated action ("Why was this auto-approved? → Low-risk standard change, Policy v1.2.3")
- **Implementation:** Audit trails accessible for all critical actions (incident resolution, change approvals, policy evaluations)
- **Anti-pattern:** Avoid "black box" automation, hidden system state, unexplained failures

**3. Single-Entry Paradigm - Eliminate Duplicate Work**

Users should never enter the same data twice. The system intelligently mirrors and propagates information across modules.

- **Implementation:** Single-Entry Time Tray: log work once, mirrors to incidents and tasks automatically
- **Implementation:** Link-on-Action: "Create related change" pre-fills context from incident
- **Implementation:** Event-driven data propagation: incident status updates auto-sync to related tasks
- **Anti-pattern:** Avoid forcing users to copy-paste data, manually sync information, or maintain duplicate records

**4. Cross-Module Coherence - One Platform, One Mental Model**

The interface should feel like one cohesive platform, not separate ITSM and PM tools stitched together.

- **Implementation:** Unified navigation: single top bar with consistent menu structure across modules
- **Implementation:** Single search: "INC-001" or "STORY-42" finds entities regardless of module
- **Implementation:** Single notification center: all alerts (incident updates, task assignments, approvals) in one place
- **Implementation:** Consistent terminology: "Assigned to", "Status", "Priority" mean the same thing in ITSM and PM
- **Anti-pattern:** Avoid module-specific navigation paradigms, inconsistent terminology, disconnected search

**5. Progressive Disclosure - Simple by Default, Powerful When Needed**

The interface should be simple for common tasks, with advanced features accessible but not overwhelming.

- **Implementation:** Incident form: show only required fields (title, description, priority) by default, "Advanced" expander for optional fields
- **Implementation:** Policy Studio: visual editor for basic policies, code editor for advanced Rego expressions
- **Implementation:** Dashboard widgets: pre-configured templates for common metrics, custom widget builder for power users
- **Anti-pattern:** Avoid overwhelming forms with 50 fields, exposing internal complexity to end users

**6. Automation with Autonomy - Intelligent Defaults, User Control**

The system should automate intelligently while preserving user override capability for exceptions.

- **Implementation:** Auto-routing suggests optimal agent based on skills/capacity, user can manually override
- **Implementation:** Policy-driven auto-approval for low-risk changes, user can escalate to CAB for any change
- **Implementation:** SLA timers auto-escalate approaching breaches, user can snooze/reassign before auto-escalation
- **Anti-pattern:** Avoid rigid automation without override, forcing users into automated workflows they distrust

**7. Mobile-First Responsive - Work Anywhere, Any Device**

The interface should adapt gracefully to mobile, tablet, and desktop viewports without feature compromise.

- **Implementation:** Mobile web UI with responsive breakpoints (320px, 768px, 1024px, 1440px)
- **Implementation:** Touch-friendly controls: 44px minimum tap target size, swipe gestures for common actions
- **Implementation:** Priority-based mobile layout: most critical information above fold (incident status, SLA timer)
- **Anti-pattern:** Avoid desktop-only features, requiring horizontal scrolling on mobile, tiny tap targets

**8. Accessibility First - Inclusive Design for All Users**

The interface should be fully accessible to users with disabilities, supporting keyboard, screen readers, and assistive technologies.

- **Implementation:** Full keyboard navigation: tab order follows visual hierarchy, Enter/Space trigger actions
- **Implementation:** ARIA labels for all interactive elements, semantic HTML (header, nav, main, article)
- **Implementation:** Color contrast: WCAG 2.1 AA compliant (4.5:1 for normal text, 3:1 for large text)
- **Implementation:** Screen reader announcements for dynamic updates (incident assigned, SLA breach warning)
- **Anti-pattern:** Avoid mouse-only interactions, color-only information encoding, unlabeled form controls

**9. Contextual Help - Learn by Doing, Not Reading Manuals**

Users should discover features through contextual guidance, not external documentation.

- **Implementation:** Tooltips on hover/focus: brief explanation of feature, keyboard shortcuts
- **Implementation:** Guided tours for first-time users: interactive walkthroughs of key workflows
- **Implementation:** Empty state guidance: "No incidents yet. Create your first incident to get started."
- **Implementation:** In-app help center: contextual help articles based on current page
- **Anti-pattern:** Avoid cryptic error messages, requiring external documentation to understand features

**10. Feedback and Confirmation - Make Actions Predictable**

Users should always know the result of their actions immediately, with clear feedback for success, errors, and in-progress states.

- **Implementation:** Toast notifications for success: "Time logged to INC-1234, TASK-890" (3-second auto-dismiss)
- **Implementation:** Inline validation for forms: immediate feedback on invalid input (email format, required fields)
- **Implementation:** Loading states for async actions: "Saving..." with spinner, "Policy evaluating..." with progress
- **Implementation:** Confirmation dialogs for destructive actions: "Delete incident INC-1234? This cannot be undone."
- **Anti-pattern:** Avoid silent failures, ambiguous "saved" messages without context, long delays without feedback

## Epics

### Epic Structure Overview

SynergyFlow is organized into 16 epics delivering integrated ITSM+PM capabilities with intelligent automation. The epics are phased:

- **MVP Phase (10-Week Sprint):** Epic-00, 01, 02, 05, 16 - Trust + UX Foundation + Core ITSM/PM + Platform Infrastructure
- **Phase 2 (Months 6-12):** Epic-03, 04, 06, 07, 08, 09, 10, 11, 12, 13, 14, 15 - Advanced features, self-optimizing workflows, analytics

This hybrid approach provides immediate implementation detail for the 10-week sprint while maintaining strategic context for the full platform vision.

---

### MVP EPICS (10-Week Sprint - Full Implementation Detail)

#### Epic-00: Trust + UX Foundation Pack

**Goal:** Deliver the five foundational features that enable trust, eliminate context switching, and make eventual consistency transparent.

**Value Proposition:** These are the differentiating features that separate SynergyFlow from competitors - "magic and audits like a bank."

**Story Count:** 5 stories

**Stories:**
1. Event System Implementation (Spring Modulith ApplicationEvents + Transactional Outbox)
2. Single-Entry Time Tray (global worklog mirroring)
3. Link-on-Action (cross-module workflows with auto-bidirectional linking)
4. Freshness Badges (projection lag visibility with color-coded thresholds)
5. Policy Studio MVP + Decision Receipts (visual policy editor, shadow mode testing)

**Acceptance Criteria:**
- All events use Java record types with Spring Modulith ApplicationEvents
- Event publication table stores events atomically with aggregate (transactional outbox)
- Single time entry mirrors to incidents and tasks within 100ms (p95 <200ms)
- Link-on-Action pre-fills context and establishes bidirectional links automatically
- Freshness badges display with <100ms green, 100-500ms yellow, >500ms red thresholds
- Decision receipts generated for 100% of policy evaluations

**Dependencies:** Spring Modulith 1.4+, PostgreSQL, OPA sidecar

---

#### Epic-01: Incident and Problem Management

**Goal:** Core incident lifecycle management with SLA tracking and basic problem linkage.

**Value Proposition:** Replace ManageEngine ServiceDesk Plus incident management with feature parity plus event-driven integration.

**Story Count (MVP):** 6 stories

**MVP Stories:**
1. Incident CRUD with priority/severity classification
2. Assignment and manual routing
3. SLA timer-based tracking (Flowable BPMN timers)
4. Incident lifecycle workflow (New → Assigned → In Progress → Resolved → Closed)
5. Comments, worklog, attachments
6. Event publication (IncidentCreated, IncidentAssigned, IncidentResolved)

**Phase 2 Stories (Strategic Context):**
- Audit logging for incident lifecycle changes
- SLA pre-breach notifications and auto-escalation
- Auto-routing integration with manual override
- Problem records with multi-incident linking
- Known Error Database (KEDB) integration
- Trend reporting for recurring issues

**Acceptance Criteria (MVP):**
- Incident CRUD functional with all fields (title, description, priority, severity, assigned_to, status)
- SLA timers fire within ±5 seconds (p95) of configured deadline
- Incident lifecycle transitions publish events via ApplicationEventPublisher
- Events stored in event_publication table and delivered to consumers within 100ms (p95)
- Comments and attachments stored with incident relationship

**Dependencies:** Event System (Epic-00), Flowable workflow engine, User Management (FR-16)

---

#### Epic-02: Change and Release Management

**Goal:** Change request management with approval workflows and deployment tracking.

**Value Proposition:** Reduce change lead time by 30% through policy-driven approvals while maintaining governance.

**Story Count (MVP):** 5 stories

**MVP Stories:**
1. Change request CRUD with risk assessment
2. Manual approval routing
3. Change calendar with conflict detection
4. Deployment tracking (Scheduled → In Progress → Completed)
5. Event publication (ChangeRequested, ChangeApproved, ChangeDeployed)

**Phase 2 Stories (Strategic Context):**
- Flowable workflow builder integration for custom approval flows
- Risk-based approval policy with CAB routing
- Change calendar conflict resolution and overrides
- Release assembly with multiple changes
- Deployment gate enforcement and promotion workflows
- Rollback automation with audit trail

**Acceptance Criteria (MVP):**
- Change request CRUD functional with risk levels (Low, Medium, High, Emergency)
- Change calendar displays scheduled changes with conflict warnings
- Deployment status transitions tracked with timestamp
- Events published via ApplicationEventPublisher to event_publication table
- Change events delivered to consumers within 100ms (p95)

**Dependencies:** Event System (Epic-00), OPA policy engine (for Phase 2 auto-approval)

---

#### Epic-05: Knowledge Management

**Goal:** Knowledge base with versioning, approval workflow, and search.

**Value Proposition:** Enable self-service resolution, reduce incident volume by 10-15%.

**Story Count (MVP):** 5 stories

**MVP Stories:**
1. Knowledge article CRUD with versioning
2. Approval workflow (Draft → Review → Approved → Published)
3. Full-text search with relevance ranking
4. Tagging and related articles
5. Expiry notification scheduler

**Phase 2 Stories (Strategic Context):**
- Known error repository with runnable knowledge articles
- Self-healing engine integration
- Knowledge article analytics (usage, ratings, effectiveness)
- Multi-language support

**Acceptance Criteria (MVP):**
- Knowledge articles support versioning (v1.0, v1.1, v2.0)
- Approval workflow transitions functional
- Full-text search returns relevant results ranked by score
- Expiry notifications sent 30 days before article expiry date

**Dependencies:** Event System (Epic-00), User Management (FR-16)

---

#### Epic-16: Platform Foundation (Deployment and Operations)

**Goal:** Platform infrastructure, deployment automation, and operational observability.

**Value Proposition:** Production-ready platform with HA, GitOps, monitoring, and zero-downtime deployments.

**Story Count (MVP):** 9 stories (4 completed, 5 remaining)

**Completed Stories:**
1. ✅ Backend monolith bootstrap (Story 16.06)
2. ✅ Frontend app bootstrap Next.js (Story 16.07)
3. ✅ Backend OAuth2 Resource Server JWT (Story 16.08)
4. ✅ Gateway JWT validation Envoy (Story 16.09)

**Remaining MVP Stories:**
5. Spring Modulith configuration (event publication registry, module boundaries)
6. PostgreSQL HA (CloudNative-PG, 3 instances, automated backups with event_publication table)
7. DragonflyDB cache configuration (connection to shared cluster)
8. Observability stack (Victoria Metrics + Grafana, OpenTelemetry tracing)
9. GitOps deployment (Flux CD, Kustomize overlays for dev/stg/prod)

**Phase 2 Stories (Strategic Context):**
- CI/CD workflows with environment promotion gates
- Migration orchestration with evidence collection
- Rollout strategy toolkit (blue-green, canary deployments)
- Runbooks and on-call procedures
- Operations observability dashboard with alert routing
- Chaos engineering testing

**Acceptance Criteria (MVP):**
- Spring Modulith event publication registry operational with transactional outbox
- event_publication table created with proper indexes
- PostgreSQL HA with automated daily backups to S3 (30-day retention)
- DragonflyDB connection configured to shared cluster (dragonfly.dragonfly-system.svc)
- Grafana dashboards show API latency, event processing lag, error rate
- Flux CD manages deployments with Kustomize overlays for 3 environments

**Dependencies:** Harbor registry (deployed), Kubernetes cluster, shared DragonflyDB cluster

---

### PHASE 2 EPICS (Months 6-12 - Strategic Context)

#### Epic-03: Self-Service Portal

**Goal:** End-user portal for ticket submission, knowledge search, and status tracking.

**Value Proposition:** Reduce support burden by 20-30% through self-service deflection.

**Story Count:** 5 stories

**Key Capabilities:**
- Portal SSO with user preferences
- Guided ticket submission with dynamic forms
- Knowledge search with previews and ratings
- Service catalog browsing and request submission
- Ticket status tracking for end users

**Dependencies:** Knowledge Management (Epic-05), Service Catalog (Epic-04)

---

#### Epic-04: Service Catalog and Request Fulfillment

**Goal:** Service catalog with dynamic forms, approval flows, and fulfillment automation.

**Value Proposition:** Standardize service request processes, automate fulfillment where possible.

**Story Count:** 5 stories

**Key Capabilities:**
- Service entity CRUD with lifecycle management
- Dynamic form schema engine for custom request types
- Approval flow executor per service type
- Fulfillment workflows with audit trail
- Service performance metrics and automation tracking

**Dependencies:** Flowable workflow engine, OPA policy engine

---

#### Epic-06: Multi-Team Support and Intelligent Routing

**Goal:** Teams, skills, capacity modeling with intelligent routing and escalation.

**Value Proportion:** Reduce misrouted tickets by 70%, improve first-time resolution by 10%.

**Story Count:** 5 stories

**Key Capabilities:**
- Teams, agents, skills, capacity data model
- Scoring engine with policy evaluator for intelligent routing
- Escalation tiers with SLA-based triggers
- Telemetry feedback loop (SLA, CSAT, utilization)
- Business hours, timezone, availability calendars

**Dependencies:** OPA policy engine, User Management (FR-16)

---

#### Epic-07: IT Asset Management (ITAM)

**Goal:** Asset inventory, lifecycle tracking, license compliance.

**Value Proposition:** Prevent license violations, optimize asset utilization.

**Story Count:** 4 stories

**Key Capabilities:**
- Asset discovery pipelines with normalization
- Asset ownership and lifecycle tracking
- License monitoring with compliance alerts
- Mobile barcode/QR scanning for asset updates

**Dependencies:** CMDB (Epic-08) for CI linkage

---

#### Epic-08: CMDB and Impact Assessment

**Goal:** Configuration Management Database with relationship graph and impact analysis.

**Value Proposition:** Enable "which changes affect this CI?" queries, reduce blast radius.

**Story Count:** 5 stories

**Key Capabilities:**
- CI model with types and relationships
- Relationship graph traversal APIs
- Baseline snapshot and diff management
- Impact assessment service integration with Change Management
- Data quality governance and validation

**Dependencies:** Change Management (Epic-02), Asset Management (Epic-07)

---

#### Epic-09: Dashboards and Reports

**Goal:** Customizable dashboards and report builder.

**Value Proposition:** Real-time visibility into operational metrics, support data-driven decisions.

**Story Count:** 4 stories

**Key Capabilities:**
- Dashboard model with widget framework and drag-n-drop UI
- Live tiles with event stream updates
- Report builder with guardrails (pre-defined templates)
- Report scheduler with email delivery

**Dependencies:** Event System (Epic-00) for live updates

---

#### Epic-10: Analytics and BI Integration

**Goal:** Dataset contracts for BI tools, ML pipeline templates.

**Value Proposition:** Enable advanced analytics, predictive incident prevention.

**Story Count:** 4 stories

**Key Capabilities:**
- Dataset export services with row-level security
- BI connectivity (Tableau, Power BI, Looker)
- Refresh scheduler with lineage tracker
- ML pipeline templates for prediction (incident spike, capacity planning)

**Dependencies:** Dashboards (Epic-09), Data platform

---

#### Epic-11: Notifications and Communication

**Goal:** Multi-channel notification system with preferences and deduplication.

**Value Proposition:** Ensure users receive timely alerts without notification fatigue.

**Story Count:** 5 stories

**Key Capabilities:**
- Template engine with localization
- User preference subscriptions (email, in-app, push)
- Channel adapters (email, push, in-app notification center)
- Deduplication, grouping, digest mode
- Delivery metrics and audit reporting

**Dependencies:** Event System (Epic-00), User Management (FR-16)

---

#### Epic-12: Security and Compliance

**Goal:** Authorization service, SSO, centralized audit, secrets management.

**Value Proposition:** Enterprise-grade security and compliance readiness (SOC 2, ISO 27001).

**Story Count:** 5 stories

**Key Capabilities:**
- AuthZ service with RBAC/ABAC (OPA policies)
- SSO configuration and session management
- Centralized audit pipeline with signed logs
- Secrets management integration (1Password + ESO)
- Compliance evidence collection and controls

**Dependencies:** OPA policy engine, ExternalSecrets Operator

---

#### Epic-13: High Availability and Reliability

**Goal:** SLO definitions, resiliency middleware, chaos testing, HA posture.

**Value Proposition:** 99.9% uptime target, zero workflow state loss.

**Story Count:** 5 stories

**Key Capabilities:**
- SLO definitions and monitoring dashboards
- Resiliency middleware (timeouts, retries, circuit breakers)
- Chaos scenarios and runbooks
- Error budget policy gating for automation
- HA posture with degradation health signals

**Dependencies:** Observability (Epic-16), Flowable workflow engine

---

#### Epic-14: Testing and Quality Assurance

**Goal:** Test architecture, contract testing, quality gates, traceability.

**Value Proposition:** 80% code coverage, zero schema breaking changes.

**Story Count:** 5 stories

**Key Capabilities:**
- Test architecture scaffolding with conventions
- Contract and event schema tests (Spring Cloud Contract)
- Quality gate evaluation service
- Traceability reporting (requirements → tests → coverage)
- Data seeding and regression test harness

**Dependencies:** Spring Modulith event system (Epic-16), CI/CD pipeline

---

#### Epic-15: Integration and API Gateway (Advanced)

**Goal:** Gateway deployment, OpenAPI governance, webhooks, event publishing SDKs.

**Value Proposition:** Enable partner integrations, ecosystem extensibility.

**Story Count:** 5 stories

**Key Capabilities:**
- Gateway deployment with policy configuration (Envoy Gateway)
- OpenAPI governance with CI checks
- Webhook subscription service with retries and signing
- Event publishing standards and SDKs
- API client key and rate limit policy management

**Dependencies:** API Gateway (FR-21), Event System (Epic-00)

---

**Epic Delivery Sequence:**

**10-Week MVP Sprint:**
- **Week 1-2:** Epic-16 (Platform Foundation) - Spring Modulith, PostgreSQL, DragonflyDB, Observability, GitOps
- **Week 2-4:** Epic-00 (Trust + UX Foundation Pack) - Event System, Time Tray, Link-on-Action, Freshness Badges, Policy Studio
- **Week 4-6:** Epic-01 (Incident Management MVP), Epic-02 (Change Management MVP)
- **Week 6-8:** Epic-05 (Knowledge Management MVP)
- **Week 8-10:** Integration testing, performance tuning, beta deployment

**Phase 2 (Months 6-12):**
- **Months 6-7:** Epic-06 (Multi-Team Routing), Epic-11 (Notifications)
- **Months 7-9:** Epic-03 (Self-Service Portal), Epic-04 (Service Catalog)
- **Months 9-10:** Epic-08 (CMDB), Epic-07 (ITAM)
- **Months 10-11:** Epic-09 (Dashboards), Epic-10 (Analytics)
- **Months 11-12:** Epic-12 (Security/Compliance), Epic-13 (HA/Reliability), Epic-14 (Testing), Epic-15 (Integration Gateway Advanced)

**Note:** Detailed story breakdown with acceptance criteria, technical notes, and API specifications available in separate `epics.md` document (generated in Step 9 of workflow).

## Out of Scope

The following features are explicitly deferred to Phase 2 or later, allowing the MVP to focus on core value delivery and architectural foundation:

### Deferred to Phase 2 (Months 6-12)

**1. Impact Orchestrator (Self-Optimizing Workflows)**
- Real-time signal fusion from SLOs, topology, and calendars
- Auto-tuning SLAs, routing, and priorities based on historical patterns
- Dynamic approval routing based on risk, impact, and capacity
- Task reprioritization based on business impact
- **Why Deferred:** Complexity; MVP must validate foundation (event-driven architecture, policy engine, workflow orchestration) before building self-optimizing layer on top
- **Dependencies:** MVP success metrics, 6 months operational data for ML training

**2. Self-Healing Engine**
- Known error matching against knowledge base with pattern recognition
- Runnable knowledge articles (Ansible playbooks, API calls, shell scripts)
- Autonomous execution with scoped credentials and rollback plans
- Compensation logic for failed healing attempts
- Deployment to staging and off-hours production only
- **Why Deferred:** Requires mature knowledge base, proven automation patterns, and customer trust in policy-driven automation
- **Dependencies:** Knowledge Management (Epic-05) operational for 6+ months, Policy Studio adoption >70%

**3. Natural Language Control (NL→DSL Compiler)**
- Natural dialogue as primary interface for workflow commands
- NL→DSL compiler: "Auto-approve all low-risk changes to staging" → Rego policy
- Policy verification with safe execution sandbox
- Explainable execution with audit trails
- **Why Deferred:** AI/ML complexity, requires LLM integration, prompt engineering, safety verification
- **Dependencies:** Policy Studio maturity, user trust in automation, LLM partnership/deployment

**4. Predictive Prevention**
- Change window guards: block changes during high-risk periods (Black Friday, end-of-quarter)
- Risk scoring across topology: predict blast radius before deployment
- Conflict radar: detect conflicting changes before approval
- Capacity-aware scheduling: predict resource contention
- **Why Deferred:** Requires CMDB integration, topology mapping, historical incident data for ML models
- **Dependencies:** CMDB (Epic-08), 12+ months operational data, topology discovery

**5. Advanced CMDB Features**
- CI relationship graph with deep nesting (10+ levels)
- Automated impact assessment engine with blast radius visualization
- Baseline snapshots with diff management and change tracking
- Data quality validation and governance rules
- Topology auto-discovery from monitoring tools
- **Why Deferred:** CMDB MVP (Phase 2) sufficient for initial launch, advanced features need operational maturity
- **Dependencies:** Basic CMDB (Epic-08) operational, integration with monitoring tools

**6. Mobile Native Apps**
- iOS native app (Swift, SwiftUI)
- Android native app (Kotlin, Jetpack Compose)
- Offline mode with sync
- Push notifications with device-specific targeting
- Mobile-specific workflows (barcode scanning for ITAM)
- **Why Deferred:** Mobile web UI sufficient for MVP, native apps require dedicated mobile development team
- **Dependencies:** Mobile web validation, user demand for native apps, mobile dev hiring

**7. Advanced Reporting and Analytics**
- Custom report builder with drag-n-drop field selection
- BI tool integration (Tableau, Power BI, Looker) with live connectors
- Scheduled report distribution with subscription management
- Embedded analytics (charts/dashboards in external tools)
- Data warehouse integration for historical analysis
- **Why Deferred:** Basic reporting sufficient for MVP, advanced analytics require data platform maturity
- **Dependencies:** 12+ months operational data, data warehouse, BI partnerships

**8. Multi-Tenancy Architecture**
- Tenant isolation (database-per-tenant or schema-per-tenant)
- Per-tenant customization (branding, workflows, policies)
- Tenant admin roles and delegated administration
- Cross-tenant analytics for SaaS provider
- Tenant provisioning automation
- **Why Deferred:** Single-tenant self-hosted deployment model for MVP, multi-tenancy adds significant architectural complexity
- **Dependencies:** SaaS evolution decision, tenant management platform, billing integration

### Explicitly Out of Scope (No Current Plans)

**1. Full-Featured Project Management (Compete with JIRA)**
- Advanced agile features (burnup charts, cumulative flow diagrams, velocity forecasting)
- Portfolio management (program, initiative, portfolio levels)
- Advanced roadmapping (Gantt charts, critical path analysis)
- Resource capacity planning across projects
- **Rationale:** SynergyFlow provides PM basics for cross-module integration, not a full JIRA replacement

**2. Advanced ITOM (IT Operations Management)**
- Infrastructure monitoring and alerting (compete with DataDog, New Relic)
- Log aggregation and analysis
- APM (Application Performance Monitoring) with distributed tracing
- Synthetic monitoring and uptime checks
- **Rationale:** SynergyFlow integrates with existing ITOM tools via events, not replacing them

**3. Customer Service Management (CSM)**
- Customer-facing support portal (external customers, not internal IT)
- SLA tracking for external customers
- Customer satisfaction surveys (CSAT, NPS) for external customers
- Self-service knowledge base for external customers
- **Rationale:** SynergyFlow focuses on internal ITSM+PM, not external customer support

**4. Advanced DevOps Tooling**
- CI/CD pipeline orchestration (compete with Jenkins, GitLab CI)
- Infrastructure as Code (IaC) management
- Secret scanning and vulnerability management
- Container registry and artifact management
- **Rationale:** SynergyFlow integrates with existing DevOps tools, not replacing them

**5. Enterprise ERP Integration**
- Procurement and vendor management
- Financial management (budgeting, cost allocation)
- HR system integration (onboarding, offboarding)
- Facilities management
- **Rationale:** Out of scope for ITSM+PM platform, potential partnerships for integration

---

## Next Steps

### Immediate Actions (Next 2 Weeks)

**1. Stakeholder Review and Approval**
- [ ] Review PRD with engineering team (3 developers + DevOps)
- [ ] Review PRD with QA and UX team members
- [ ] Review PRD with product stakeholders
- [ ] Incorporate feedback and finalize PRD v1.0
- [ ] Get formal sign-off from product owner (monosense)

**2. Architecture and Technical Design (Critical Path)**
- [x] **Architecture Workflow Completed**
  - System architecture document completed: architecture.md
  - Covers:
    - High-level architecture diagrams (C4 Model: Context, Container, Component)
    - Event-driven integration patterns (Spring Modulith + Transactional Outbox)
    - Database schema design (PostgreSQL per module + event_publication table)
    - API design patterns (REST, OpenAPI 3.0)
    - Security architecture (OAuth2, OPA, audit pipeline)
    - Deployment architecture (Kubernetes, GitOps, HA)
  - **Key Decision:** Using Spring Modulith instead of Kafka for MVP
    - 20x faster event processing (50-100ms vs 2-3s)
    - Eliminates 6 CPU cores, 13.5GB RAM, $250-350/month
    - Natural migration path to Kafka if needed later

**3. UX/UI Specification (Highly Recommended)**
- [ ] **Initiate UX Specification Workflow**
  - Continue within PM workflow or new session with UX persona
  - Provide inputs: PRD.md, architecture.md (once available)
  - Request: Comprehensive UX/UI specification covering:
    - Information architecture (IA) and navigation structure
    - User flows for 5 primary journeys (from PRD User Journeys section)
    - Component library (based on Shadcn/ui + Tailwind)
    - Screen wireframes for MVP features
    - Responsive breakpoints and mobile considerations
    - Accessibility guidelines (WCAG 2.1 AA)
  - Optional: Generate AI Frontend Prompt for rapid prototyping
  - Output: ux-specification.md, ai-frontend-prompt.md

### Phase 1: Detailed Planning (Weeks 3-4)

**4. Generate Detailed Epic Breakdown**
- [ ] **Generate epics.md document** (Step 9 of this workflow)
  - Expand all 16 epics with full story hierarchy
  - User story format: "As a [persona], I want [capability] so that [benefit]"
  - Acceptance criteria (3-8 per story) with testable assertions
  - Technical notes (high-level implementation guidance)
  - API endpoint specifications (REST paths, methods, request/response schemas)
  - Event definitions (Java records) for all published events
  - Dependencies and sequencing

**5. Technical Design Documents**
- [ ] Database schema design (all modules: incidents, changes, knowledge, tasks, users)
  - Entity-relationship diagrams (ERD)
  - PostgreSQL DDL scripts
  - Migration strategy (Flyway or Liquibase)
  - event_publication table for transactional outbox
- [ ] API specifications (OpenAPI 3.0)
  - REST endpoint definitions for all functional requirements
  - Request/response schemas
  - Error response codes (RFC 7807 Problem Details)
  - Authentication/authorization specifications
- [ ] Event catalog
  - Java record event definitions (IncidentCreatedEvent, ChangeApprovedEvent, etc.)
  - Event metadata structure (correlation IDs, timestamps)
  - Spring Modulith event consumption patterns (@ApplicationModuleListener)
- [ ] Integration architecture
  - Envoy Gateway configuration
  - OPA policy bundle structure
  - Flowable BPMN workflow examples (SLA timers, approval flows)

**6. Testing Strategy**
- [ ] Unit test approach (JUnit 5, Mockito, target 80% coverage)
- [ ] Integration test plan (Testcontainers, Spring Modulith test slices)
- [ ] Event publication tests (Spring Modulith scenario tests)
- [ ] End-to-end test scenarios (Playwright for critical user journeys)
- [ ] Performance test plan (1,000 concurrent users, event processing throughput)
- [ ] UAT criteria and test cases

### Phase 2: Development Preparation (Weeks 5-6)

**7. Development Environment Setup**
- [ ] Repository structure (monorepo vs multi-repo decision)
  - Backend: synergyflow-backend (Spring Boot 3.5+, Java 21+, Gradle)
  - Frontend: synergyflow-frontend (Next.js 14+, TypeScript, npm)
  - Infrastructure: synergyflow-infra (Kubernetes manifests, Helm charts)
- [ ] CI/CD pipeline configuration
  - GitHub Actions or GitLab CI
  - Build, test, lint, security scanning stages
  - Artifact publishing (Harbor registry)
  - Environment promotion gates (dev → staging → production)
- [ ] Development tools and standards
  - IDE setup guides (IntelliJ IDEA, VS Code)
  - Code formatting (Prettier for frontend, Google Java Format for backend)
  - Linting (ESLint, Checkstyle)
  - Pre-commit hooks (Husky for frontend, pre-commit framework for backend)

**8. Sprint Planning**
- [ ] Story prioritization (MVP epics breakdown from epics.md)
- [ ] Sprint boundaries (10-week MVP = 5 two-week sprints)
  - Sprint 1 (Weeks 1-2): Epic-16 Platform Foundation
  - Sprint 2 (Weeks 3-4): Epic-00 Trust + UX Foundation Pack
  - Sprint 3 (Weeks 5-6): Epic-01 Incident, Epic-02 Change (MVP scope)
  - Sprint 4 (Weeks 7-8): Epic-05 Knowledge Management
  - Sprint 5 (Weeks 9-10): Integration testing, performance tuning, beta deployment
- [ ] Resource allocation (3 developers, QA, DevOps, UX)
- [ ] Velocity estimation (story points per sprint)
- [ ] Definition of Done (DoD) for stories and sprints

**9. Monitoring and Metrics Setup**
- [ ] Success metrics from PRD Goals section
  - Technical: API latency, event processing lag, error rate, uptime
  - User adoption: DAU/MAU, feature usage (Time Tray, Link-on-Action)
  - Business: MTTR, change lead time, agent throughput, NPS
- [ ] Grafana dashboards configuration
  - API performance dashboard
  - Event processing dashboard (event_publication table lag, projection lag)
  - Business metrics dashboard (MTTR, throughput, SLA compliance)
- [ ] Alerting rules (Victoria Metrics AlertManager)
  - API latency p95 >200ms for 5 minutes
  - Event processing lag >500ms for 5 minutes (Spring Modulith in-memory event bus)
  - Error rate >1% for 5 minutes
  - SLA timer precision >±10 seconds

### Phase 3: MVP Execution (Weeks 7-16, 10-Week Sprint)

**10. Development Execution**
- [ ] Follow sprint plan (5 two-week sprints)
- [ ] Daily standups (15 minutes, sync on blockers)
- [ ] Sprint reviews (demo completed stories to stakeholders)
- [ ] Sprint retrospectives (continuous improvement)
- [ ] Track velocity and adjust sprint scope if needed

**11. Beta Deployment and Validation**
- [ ] Deploy to beta environment (Week 10)
- [ ] Onboard 10 beta users from target organizations
- [ ] Collect feedback on MVP features (Time Tray, Link-on-Action, Freshness Badges, Policy Studio)
- [ ] Validate success criteria from PRD:
  - ✅ All 15 MVP features functional
  - ✅ Performance targets met (API <200ms p95, event processing lag <200ms p95)
  - ✅ User satisfaction ≥3.5/5.0 (beta survey)
  - ✅ Zero workflow state loss incidents
  - ✅ Decision receipts for 100% of policy evaluations

**12. Go/No-Go Decision**
- [ ] Review MVP success criteria (Week 11)
- [ ] Decision: Proceed to Phase 1 production launch OR iterate on MVP
- [ ] If Go: Prepare for Phase 1 (250 users, 10 production organizations)
- [ ] If No-Go: Identify gaps, plan iteration sprint

### Recommended Next Workflow Invocations

**Option 1: Architecture Workflow (REQUIRED)**
```
Start new chat with Architect persona
Command: /bmad:architect
Inputs: PRD.md, product-brief.md
Output: architecture.md with system diagrams, API specs, event schemas
```

**Option 2: UX Specification Workflow (HIGHLY RECOMMENDED)**
```
Continue in this PM session OR start new chat with UX persona
Command: /plan-project → Select "UX/UI specification only"
Inputs: PRD.md, architecture.md
Output: ux-specification.md, ai-frontend-prompt.md (optional)
```

**Option 3: Generate Detailed Epic Breakdown (NEXT STEP)**
```
Continue in this workflow (Step 9)
Will generate epics.md with all 16 epics, full story hierarchy
```

## Document Status

- [ ] Goals and context validated with stakeholders
- [ ] All functional requirements reviewed
- [ ] User journeys cover all major personas
- [ ] Epic structure approved for phased delivery
- [ ] Ready for architecture phase

_Note: See technical-decisions.md for captured technical context_

---

_This PRD adapts to project level Level 4 (Platform/Ecosystem) - providing appropriate detail without overburden._
