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

**Architecture Philosophy:**

SynergyFlow is built as a **modular monolith** using Spring Modulith, not microservices. This architectural choice provides:
- **Operational simplicity**: Single deployable artifact, no distributed system complexity
- **Development velocity**: 5-person team can deliver complete feature set without microservices overhead
- **Performance**: In-process communication (sub-millisecond) vs network calls (milliseconds)
- **Cost efficiency**: Eliminates message broker infrastructure, API gateway complexity, distributed tracing overhead
- **Module boundaries**: Spring Modulith enforces module isolation equivalent to microservices without operational burden

**Business Opportunity:**

Organizations waste 30-40% of agent productivity on context switching, manual data entry, and rigid approval workflows across disconnected ITSM and PM tools. SynergyFlow's unified platform with Trust + UX Foundation Pack eliminates these pain points through:

- Single-entry time logging (eliminate double entry)
- Link-on-action cross-module workflows (eliminate context switching)
- Transparent eventual consistency with freshness badges (build user trust)
- Explainable automation with decision receipts (enable governance)

### Deployment Intent

**Primary Deployment Model:** Self-hosted Kubernetes deployment

**Deployment Targets:**

- **Initial Release (Month 0-12):** Complete feature set with all ITSM+PM capabilities, 250-500 users, Indonesia data residency focus
- **Production Scale (Month 12-18):** 10-20 production organizations, 500-1,000 users, expand to Philippines, Singapore, Malaysia
- **Long-term (Year 2+):** SaaS evolution, multi-tenant architecture, global expansion

**Infrastructure Requirements:**

- Kubernetes cluster (cloud-agnostic: AWS, GCP, Azure, or on-prem)
- Resource capacity (SynergyFlow-dedicated): ~8-15 CPU cores (requests), ~12-18GB RAM (requests), ~100GB storage
- Shared infrastructure: PostgreSQL cluster (shared with gitlab, harbor, keycloak, mattermost), DragonflyDB cluster
- High availability: Shared PostgreSQL cluster via PgBouncer pooler (3 instances), shared DragonflyDB cluster, 3-replica application pods
- PostgreSQL: Shared cluster pattern (shared-postgres in cnpg-system namespace)
  - PgBouncer pooler: synergyflow-pooler (3 instances, transaction mode)
  - Database: synergyflow with 7 schemas
  - Connection pooling: 1000 max clients → 50 DB connections
  - Connection string: jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow
- Data Access Layer: Spring Data JPA 3.2.0 (Spring Boot 3.5.6 managed)
  - Hibernate 6.4.0 as JPA provider
  - Declarative repository pattern with type-safe queries
  - Automatic CRUD operations via JpaRepository
  - Query methods by naming convention and @Query annotations
  - Auditing support (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)
- Monitoring: Victoria Metrics + Grafana
- GitOps: Flux CD with Kustomize overlays
- Image Registry: Harbor (private registry)

**Kubernetes Namespace Structure:**
- `synergyflow`: SynergyFlow application components (backend, frontend)
- `cnpg-system`: Shared PostgreSQL cluster and poolers (cross-namespace access)
- `dragonfly-system`: Shared DragonflyDB cluster (cross-namespace access)
- `monitoring`: Victoria Metrics, Grafana (observability stack)
- `flux-system`: Flux CD GitOps automation

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
- **Target:** 500 active users by Month 18 (100-150 peak concurrent)
- **Metric:** Monthly Active Users (MAU), Daily Active Users (DAU), 20 production organizations
- **Success Criteria:** ≥70% adoption rate within organizations (users actively using SynergyFlow vs legacy tools)

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
- **Target:** Architecture validated for 1,000 users (production scale)
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
- **Target:** Customer ROI within 2 months through productivity gains
- **Target:** $150k ARR by Month 18 (500 users × $300/user/year)
- **Target:** Break-even by Month 24-30
- **Metric:** Customer case studies, revenue tracking, cost savings analysis

**8. Deliver Complete Feature Set**
- **Target:** All 16 epics delivered with complete ITSM and PM feature parity
- **Target:** Self-Service Portal, Service Catalog, CMDB, ITAM, Dashboards, Reports, Analytics operational
- **Target:** Multi-Team Routing, Notifications, Advanced Automation capabilities
- **Target:** High availability, security, compliance, and testing infrastructure complete
- **Metric:** Feature completeness scorecard, user satisfaction surveys per module

## Requirements

### Functional Requirements

#### A. Trust + UX Foundation Pack (Foundation Phase)

**FR-1: Event System Implementation (Spring Modulith 1.4.2)**
- All events use Java record types with Spring Modulith ApplicationEvents
- Events stored in `event_publication` table atomically during write transaction (transactional outbox pattern)
- @ApplicationModuleListener for type-safe event consumption
- Automatic retry with exponential backoff: 5 retries, 2x multiplier, 2s initial delay
- At-least-once delivery semantics (idempotent consumers required)
- Processed_events table for idempotency tracking
- Event metadata: correlation IDs, causation IDs, timestamps, listener_id
- Canonical IDs propagate across module boundaries (Incident ID, Change ID, Task ID)
- Type-safe events with compile-time validation (no external schema registry)
- In-JVM event bus: <1ms delivery latency, <100ms end-to-end processing lag (p95 <200ms)
- Event completion tracking: 5s check interval, 5m timeout

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

**FR-5: Policy Studio + Decision Receipts**
- Visual policy editor for creating/editing OPA Rego policies
- Decision receipt generated for every policy evaluation ("Why was this auto-approved?")
- Shadow mode testing: log policy decisions without enforcing (pre-production validation)
- Audit log stores all policy decisions with timestamp, policy version, input data, decision output
- Integration with Spring Security for authorization decisions

#### B. ITSM Core Modules

**FR-6: Incident Management**
- Incident CRUD with priority (Low/Medium/High/Critical) and severity (S1-S4) classification
- Assignment and routing (manual and policy-driven auto-routing)
- SLA timer-based tracking using Flowable BPMN timers (4-hour resolution SLA)
- Incident lifecycle: New → Assigned → In Progress → Resolved → Closed
- Comments, worklog, and attachments support
- Event publication: IncidentCreated, IncidentAssigned, IncidentResolved, IncidentClosed

**FR-7: Problem Management**
- Problem record CRUD with linkage to multiple incidents
- Known Error Database (KEDB) with resolution procedures
- Trend reporting for recurring issues (incident spike correlation)
- Root cause analysis workflow

**FR-8: Change Management**
- Change request CRUD with risk assessment (Low/Medium/High/Emergency)
- Approval routing (manual and policy-driven)
- Change calendar view with conflict detection
- Deployment tracking (Scheduled → In Progress → Completed → Failed)
- Rollback planning fields and procedures
- Event publication: ChangeRequested, ChangeApproved, ChangeDeployed

**FR-9: Release Management**
- Release assembly with multiple changes
- Release gates and promotion workflow (Dev → Staging → Production)
- Deployment tracking with status audit trail

**FR-10: Service Request Management**
- Service catalog with request fulfillment workflows
- Dynamic form schema engine for custom request types
- Approval flow executor per service type

**FR-11: Knowledge Management**
- Knowledge article CRUD with versioning (v1.0, v1.1, etc.)
- Approval workflow (Draft → Review → Approved → Published)
- Full-text search with relevance ranking and tagging
- Related articles suggestion
- Expiry notification scheduler for knowledge article review

**FR-12: CMDB (Configuration Management Database)**
- CI (Configuration Item) entity model with types (Server, Application, Database, Network Device)
- CI relationship graph (supports, runs-on, depends-on)
- Impact assessment engine (which changes affect this CI?)
- Baseline snapshots and diff management

**FR-13: IT Asset Management (ITAM)**
- Asset inventory with lifecycle tracking (Purchase → Active → Maintenance → Retired)
- License monitoring and compliance alerts
- Mobile QR/barcode scanning for asset updates

#### C. Project Management (PM) Core

**FR-14: Project and Task Management**
- Project CRUD with description, owner, team assignment
- Epic/Story/Task hierarchy with parent-child relationships
- Sprint planning: create sprints, assign stories, set sprint goals
- Board view (Kanban): columns (To Do, In Progress, Done), drag-drop status updates
- Backlog management with priority ordering
- Task assignment and status tracking (Not Started → In Progress → Done)
- Event publication: TaskCreated, TaskAssigned, TaskCompleted

**FR-15: Agile Workflow**
- Sprint burndown charts
- Velocity tracking
- Epic roadmap visualization
- Release planning

#### D. User and Team Management

**FR-16: User Profiles and Authentication**
- User profile CRUD (name, email, role, team, skills, capacity)
- Team assignment with multi-team support
- Role-based access control (RBAC) using OPA policies
- SSO integration via OAuth2 Resource Server with JWT validation
- Session management with configurable timeout

**FR-17: Multi-Team Support and Routing**
- Teams, agents, skills, and capacity data model
- Intelligent routing scoring engine with policy evaluator
- Escalation tiers and triggers based on SLA breach
- Business hours, timezone, and availability calendars

#### E. Workflow Orchestration and Automation

**FR-18: Flowable Workflow Engine**
- Embedded Flowable 7.1.0 in Spring Boot 3.5.6 application
- BPMN timer events for SLA tracking (4-hour incident resolution SLA)
- Async job executor for timer-based workflows (4-10 threads, 100 queue size)
- Workflow state persistence in PostgreSQL shared cluster (durable across restarts)
- Admin UI for monitoring active workflows and job executor queue

**FR-19: OPA Policy Engine**
- OPA 0.68.0 sidecar container deployed alongside Spring Boot pods (1 per pod)
- Localhost communication (port 8181) for <10ms policy evaluation latency
- Resources: 100m-500m CPU, 128-256Mi RAM per sidecar
- Rego policy bundles distributed via CI/CD pipeline (ConfigMap volumes)
- Policy evaluation REST API (POST /v1/data/synergyflow/{policy-name})
- Decision receipt API for explainability
- Shadow mode support for pre-production policy testing

**FR-20: Advanced Automation**
- Impact Orchestrator: real-time signal fusion (SLOs, topology, calendars), auto-tuning SLAs/routing/priorities
- Self-Healing Engine: known error matching, runnable knowledge articles, autonomous execution with rollback
- Predictive Prevention: change window guards, risk scoring, conflict radar

#### F. Integration and API Gateway

**FR-21: API Gateway**
- Envoy Gateway with Kubernetes Gateway API
- JWT validation for all protected routes (/api/*)
- Rate limiting by authenticated client (100 req/min default)
- CORS configuration for frontend origin
- Request/response logging with correlation IDs

**FR-22: REST API**
- OpenAPI 3.0 specification for all endpoints
- Standardized error responses (RFC 7807 Problem Details)
- Pagination support (limit/offset or cursor-based)
- Filtering and sorting query parameters
- API versioning (v1, v2) in URL path

**FR-23: Webhook Subscriptions**
- Webhook subscription service with retry logic and HMAC signing
- Event catalog with schema viewer
- Partner integration sandbox console

**FR-24: Event Publishing Standards**
- Spring Modulith ApplicationEvents with Java record types
- In-JVM event bus with transactional outbox pattern
- Event publication table for durable messaging with automatic retry
- Correlation ID propagation across all events
- Natural migration path to Kafka via event externalization if needed later

#### G. Notifications and Communication

**FR-25: Notification System**
- Template engine with localization support
- User preference subscriptions (email, in-app, push)
- Multi-channel adapters (email, push, in-app notification center)
- Deduplication, grouping, and digest mode
- Delivery metrics and audit reporting

#### H. Dashboards, Reports, and Analytics

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

#### I. Security, Compliance, and Observability

**FR-29: Authorization Service**
- RBAC with OPA policy engine
- ABAC (Attribute-Based Access Control) for fine-grained permissions
- Policy versioning and shadow mode testing

**FR-30: Audit Pipeline**
- Centralized audit pipeline with signed logs (tamper-proof)
- Audit log retention: 90 days hot storage, 7 years cold storage (S3)
- Audit log query API for compliance reporting

**FR-31: Secrets Management**
- Integration with 1Password via External Secrets Operator (ESO)
- Secret rotation automation
- Secret references in configuration (no plaintext secrets)

**FR-32: Compliance Controls**
- Compliance evidence collection for SOC 2, ISO 27001
- Data residency enforcement (Indonesia deployment)
- GDPR-compliant data retention policies

**FR-33: Observability**
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
- Database connection pooling: Two-layer architecture
  - PgBouncer pooler (3 instances, transaction mode): 1000 max clients → 50 DB connections
  - HikariCP (application side): 20 connections per backend replica
  - Cross-namespace connection: synergyflow → cnpg-system via pooler service
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
- Compliance readiness: SOC 2, ISO 27001 evidence collection

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
- Screen reader support: ARIA labels for assistive technologies (WCAG 2.1 AA target)
- Internationalization: English, Bahasa Indonesia, Tagalog support
- User onboarding: Guided tour for first-time users, contextual help tooltips
- Error messages: User-friendly error messages with actionable guidance

**NFR-9: Testability**
- Unit test coverage: ≥80% code coverage for business logic
- Integration test coverage: All API endpoints, event consumers, policy evaluations
- Contract testing: Spring Cloud Contract for event schema validation
- End-to-end testing: Selenium/Playwright for critical user journeys
- Performance testing: JMeter load tests for 1,000 concurrent users
- Chaos engineering: Chaos Mesh for failure injection testing

**NFR-10: Extensibility and Integration**
- REST API: OpenAPI 3.0 specification, versioned endpoints
- Webhook support: Outbound webhooks with retry logic and HMAC signing
- Event-driven integration: Spring Modulith event externalization for future Kafka/external system integration
- Plugin architecture: Support for custom policy plugins
- BI tool integration: Dataset export for Tableau, Power BI, Looker

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
   - Future VPN incidents auto-suggest KB-0042 resolution via self-healing engine

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

SynergyFlow is organized into 16 epics delivering integrated ITSM+PM capabilities with intelligent automation. All epics will be delivered as a complete feature set over a 12-month development timeline:

**Foundation Phase (Months 1-4):**
- Epic-16: Platform Foundation - Core infrastructure, event system, shared services
- Epic-00: Trust + UX Foundation - Progressive disclosure, canonical IDs, transparent eventual consistency
- Epic-01: Incident & Problem Management - Core ITSM with intelligent triaging
- Epic-02: Change & Release Management - Workflow automation and risk assessment

**Core Features Phase (Months 4-8):**
- Epic-03: Self-Service Portal - User-facing service request system
- Epic-04: Service Catalog - Centralized service offering management
- Epic-06: Multi-Team Routing - Intelligent workload distribution

**Advanced Capabilities Phase (Months 8-11):**
- Epic-07: IT Asset Management - Lifecycle and compliance tracking
- Epic-08: CMDB & Impact Assessment - Configuration and relationship tracking
- Epic-09: Dashboards & Reports - Real-time analytics and insights
- Epic-10: Analytics & BI Integration - Predictive analytics and trend detection
- Epic-11: Notifications & Communication - Multi-channel notification system

**Optimization & Scale Phase (Months 11-12):**
- Epic-12: Security & Compliance - Enterprise-grade security and compliance readiness
- Epic-13: High Availability & Reliability - Production-grade resilience and disaster recovery
- Epic-14: Testing & Quality Assurance - Comprehensive test coverage and quality gates
- Epic-15: Integration & API Gateway - Advanced integration capabilities

This complete product approach ensures all features are production-ready before first customer release, with 5-person team delivering integrated capabilities without technical debt.

---

### FOUNDATION PHASE EPICS (Months 1-4)

#### Epic-00: Trust + UX Foundation Pack

**Goal:** Deliver the five foundational features that enable trust, eliminate context switching, and make eventual consistency transparent.

**Value Proposition:** These are the differentiating features that separate SynergyFlow from competitors - "magic and audits like a bank."

**Story Count:** 5 stories

**Stories:**
1. Event System Implementation (Spring Modulith ApplicationEvents + Transactional Outbox)
2. Single-Entry Time Tray (global worklog mirroring)
3. Link-on-Action (cross-module workflows with auto-bidirectional linking)
4. Freshness Badges (projection lag visibility with color-coded thresholds)
5. Policy Studio + Decision Receipts (visual policy editor, shadow mode testing)

**Acceptance Criteria:**
- All events use Java record types with Spring Modulith ApplicationEvents
- Event publication table stores events atomically with aggregate (transactional outbox)
- Single time entry mirrors to incidents and tasks within 100ms (p95 <200ms)
- Link-on-Action pre-fills context and establishes bidirectional links automatically
- Freshness badges display with <100ms green, 100-500ms yellow, >500ms red thresholds
- Decision receipts generated for 100% of policy evaluations

**Dependencies:** Spring Modulith 1.4.2, PostgreSQL 16.8 shared cluster, OPA 0.68.0 sidecar

---

#### Epic-01: Incident and Problem Management

**Goal:** Core incident lifecycle management with SLA tracking and basic problem linkage.

**Value Proposition:** Replace ManageEngine ServiceDesk Plus incident management with feature parity plus event-driven integration.

**Story Count:** 12 stories

**Stories:**
1. Incident CRUD with priority/severity classification
2. Assignment and manual routing
3. SLA timer-based tracking (Flowable BPMN timers)
4. Incident lifecycle workflow (New → Assigned → In Progress → Resolved → Closed)
5. Comments, worklog, attachments
6. Event publication (IncidentCreated, IncidentAssigned, IncidentResolved)
7. Audit logging for incident lifecycle changes
8. SLA pre-breach notifications and auto-escalation
9. Auto-routing integration with manual override
10. Problem records with multi-incident linking
11. Known Error Database (KEDB) integration
12. Trend reporting for recurring issues

**Acceptance Criteria:**
- Incident CRUD functional with all fields (title, description, priority, severity, assigned_to, status)
- SLA timers fire within ±5 seconds (p95) of configured deadline
- Incident lifecycle transitions publish events via ApplicationEventPublisher
- Events stored in event_publication table and delivered to consumers within 100ms (p95)
- Comments and attachments stored with incident relationship
- Audit log captures all incident lifecycle changes with user, timestamp, and field-level changes
- SLA pre-breach notifications sent at 80% threshold with auto-escalation at 100%
- Auto-routing assigns incidents based on category/priority rules with manual override capability
- Problem records created with multi-incident linking and root cause analysis tracking
- KEDB integration enables known error matching and solution suggestions
- Trend reporting identifies recurring issues with pattern detection and analytics

**Dependencies:** Event System (Epic-00), Flowable 7.1.0 workflow engine, User Management (FR-16)

---

#### Epic-02: Change and Release Management

**Goal:** Change request management with approval workflows and deployment tracking.

**Value Proposition:** Reduce change lead time by 30% through policy-driven approvals while maintaining governance.

**Story Count:** 11 stories

**Stories:**
1. Change request CRUD with risk assessment
2. Manual approval routing
3. Change calendar with conflict detection
4. Deployment tracking (Scheduled → In Progress → Completed)
5. Event publication (ChangeRequested, ChangeApproved, ChangeDeployed)
6. Flowable workflow builder integration for custom approval flows
7. Risk-based approval policy with CAB routing
8. Change calendar conflict resolution and overrides
9. Release assembly with multiple changes
10. Deployment gate enforcement and promotion workflows
11. Rollback automation with audit trail

**Acceptance Criteria:**
- Change request CRUD functional with risk levels (Low, Medium, High, Emergency)
- Change calendar displays scheduled changes with conflict warnings
- Deployment status transitions tracked with timestamp
- Events published via ApplicationEventPublisher to event_publication table
- Change events delivered to consumers within 100ms (p95)
- Flowable workflow builder enables custom approval flow design with visual BPMN editor
- Risk-based approval policy automatically routes High/Emergency changes to CAB
- Change calendar conflict resolution provides override capability with justification tracking
- Release assembly groups multiple changes with dependency management
- Deployment gates enforce approval requirements and automated validation checks
- Rollback automation triggers on deployment failure with complete audit trail

**Dependencies:** Event System (Epic-00), Flowable 7.1.0 workflow engine, OPA policy engine

---

#### Epic-05: Knowledge Management

**Goal:** Knowledge base with versioning, approval workflow, and search.

**Value Proposition:** Enable self-service resolution, reduce incident volume by 10-15%.

**Story Count:** 9 stories

**Stories:**
1. Knowledge article CRUD with versioning
2. Approval workflow (Draft → Review → Approved → Published)
3. Full-text search with relevance ranking
4. Tagging and related articles
5. Expiry notification scheduler
6. Known error repository with runnable knowledge articles
7. Self-healing engine integration
8. Knowledge article analytics (usage, ratings, effectiveness)
9. Multi-language support

**Acceptance Criteria:**
- Knowledge articles support versioning (v1.0, v1.1, v2.0)
- Approval workflow transitions functional
- Full-text search returns relevant results ranked by score
- Expiry notifications sent 30 days before article expiry date
- Known error repository stores runnable remediation scripts linked to problem records
- Self-healing engine executes automated remediation based on known error matching
- Knowledge article analytics track usage patterns, ratings, and resolution effectiveness
- Multi-language support enables content creation and search in English, Bahasa Indonesia, Tagalog

**Dependencies:** Event System (Epic-00), User Management (FR-16)

---

#### Epic-16: Platform Foundation (Deployment and Operations)

**Goal:** Platform infrastructure, deployment automation, and operational observability.

**Value Proposition:** Production-ready platform with HA, GitOps, monitoring, and zero-downtime deployments.

**Story Count:** 15 stories (4 completed, 11 remaining)

**Completed Stories:**
1. ✅ Backend monolith bootstrap (Story 16.06)
2. ✅ Frontend app bootstrap Next.js (Story 16.07)
3. ✅ Backend OAuth2 Resource Server JWT (Story 16.08)
4. ✅ Gateway JWT validation Envoy (Story 16.09)

**Remaining Stories:**
5. Spring Modulith 1.4.2 configuration (event publication registry, module boundaries, @ApplicationModuleListener setup)
6. PostgreSQL shared cluster access (PgBouncer pooler creation, database/schema setup, Spring Data JPA entity configuration)
7. DragonflyDB cache configuration (connection to shared cluster in dragonfly-system namespace)
8. Observability stack (Victoria Metrics 1.100.0 + Grafana 10.4.2, OpenTelemetry 1.28.0 tracing)
9. GitOps deployment (Flux CD 2.2.3, Kustomize overlays for dev/stg/prod)
10. CI/CD workflows with environment promotion gates
11. Migration orchestration with evidence collection
12. Rollout strategy toolkit (blue-green, canary deployments)
13. Runbooks and on-call procedures
14. Operations observability dashboard with alert routing
15. Chaos engineering testing

**Acceptance Criteria:**
- Spring Modulith 1.4.2 event publication registry operational with transactional outbox (in-JVM event bus)
- event_publication table created with proper indexes in synergyflow database
- PgBouncer pooler (synergyflow-pooler) operational with 3 instances, connecting to shared PostgreSQL cluster
- Database synergyflow created with 7 schemas (synergyflow_users, synergyflow_incidents, synergyflow_changes, synergyflow_knowledge, synergyflow_tasks, synergyflow_audit, synergyflow_workflows)
- Connection string configured: jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow
- Spring Data JPA repositories operational for all modules with Hibernate 6.4.0
- DragonflyDB connection configured to shared cluster (dragonfly.dragonfly-system.svc)
- Grafana dashboards show API latency, event processing lag, error rate
- Flux CD manages deployments with Kustomize overlays for 3 environments (dev/stg/prod)
- CI/CD pipelines implement promotion gates with automated testing between environments
- Migration orchestration automates database migrations with rollback capability and evidence collection
- Blue-green and canary deployment strategies implemented for zero-downtime releases
- Runbooks documented for common operational scenarios with on-call procedures
- Operations dashboard aggregates observability metrics with intelligent alert routing
- Chaos engineering tests validate system resilience under failure conditions

**Dependencies:** Harbor registry (deployed), Kubernetes cluster, shared PostgreSQL cluster, shared DragonflyDB cluster

**Architecture Note:**
- Kafka message broker NOT used (Spring Modulith in-JVM event bus for modular monolith architecture)
- Schema Registry NOT needed (Java record compile-time type safety)
- External event consumers NOT in scope (internal event bus sufficient for modular monolith)

---

### CORE FEATURES PHASE EPICS (Months 4-8)

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

**Dependencies:** Flowable 7.1.0 workflow engine, OPA 0.68.0 policy engine

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

### ADVANCED CAPABILITIES PHASE EPICS (Months 8-11)

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

### OPTIMIZATION & SCALE PHASE EPICS (Months 11-12)

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

**Dependencies:** Observability (Epic-16), Flowable 7.1.0 workflow engine

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

**Epic Delivery Sequence (12-Month Complete Product Delivery):**

**Foundation Phase (Months 1-4):**
- **Month 1:** Epic-16 (Platform Foundation) - Spring Modulith, PostgreSQL, DragonflyDB, Observability, GitOps
- **Month 2:** Epic-00 (Trust + UX Foundation Pack) - Event System, Time Tray, Link-on-Action, Freshness Badges, Policy Studio
- **Month 3:** Epic-01 (Incident & Problem Management) - Complete ITSM incident lifecycle with problem tracking
- **Month 4:** Epic-02 (Change & Release Management) - Workflow automation and deployment tracking

**Core Features Phase (Months 4-8):**
- **Months 5-6:** Epic-03 (Self-Service Portal), Epic-04 (Service Catalog)
- **Months 7-8:** Epic-06 (Multi-Team Routing) - Intelligent workload distribution

**Advanced Capabilities Phase (Months 8-11):**
- **Month 8:** Epic-07 (IT Asset Management), Epic-08 (CMDB & Impact Assessment)
- **Month 9:** Epic-09 (Dashboards & Reports), Epic-10 (Analytics & BI Integration)
- **Month 10-11:** Epic-11 (Notifications & Communication), Epic-05 (Knowledge Management - Advanced Features)

**Optimization & Scale Phase (Months 11-12):**
- **Month 11:** Epic-12 (Security & Compliance), Epic-13 (High Availability & Reliability)
- **Month 12:** Epic-14 (Testing & Quality Assurance), Epic-15 (Integration & API Gateway), Final integration testing and production hardening

**Note:** Detailed story breakdown with acceptance criteria, technical notes, and API specifications available in separate `epics.md` document (generated in Step 9 of workflow).

## Out of Scope

The following features are explicitly excluded from the initial product delivery to maintain focus on integrated ITSM+PM capabilities. SynergyFlow is built as a modular monolith with complete feature parity to ManageEngine ServiceDesk Plus, not as a replacement for specialized tools in these domains:

### Architecture Decisions (Permanently Out of Scope)

**1. Kafka Message Broker (Architectural Decision)**
- Kafka 3-broker cluster deployment with Strimzi operator
- Schema Registry (Confluent Community Edition) for Avro schemas
- External event publishing for third-party integrations
- Cross-system event streaming (monitoring tools, external APIs)
- **Rationale:** Spring Modulith in-JVM event bus is the permanent choice for modular monolith architecture (20x faster, eliminates 6 CPU cores, 13.5GB RAM, $250-350/month infrastructure costs)
- **Migration Path Available:** Spring Modulith event externalization enables future migration to Kafka if external event consumers become critical requirement (not currently planned)
- **Decision Documented:** Architecture.md Section 5 (Event-Driven Architecture), lines 1125-1545
- **Would Only Be Needed If:** External systems require consuming SynergyFlow events in real-time, or event throughput exceeds 10,000 events/second (not expected for target scale)

**2. Impact Orchestrator (Self-Optimizing Workflows)**
- Real-time signal fusion from SLOs, topology, and calendars
- Auto-tuning SLAs, routing, and priorities based on historical patterns
- Dynamic approval routing based on risk, impact, and capacity
- Task reprioritization based on business impact
- **Rationale:** Requires operational maturity and significant ML infrastructure; not essential for initial product delivery; can be evaluated after 12+ months operational data collection
- **Would Require:** Production deployment with 6+ months operational data, ML platform integration, dedicated data science resources

**3. Natural Language Control (NL→DSL Compiler)**
- Natural dialogue as primary interface for workflow commands
- NL→DSL compiler: "Auto-approve all low-risk changes to staging" → Rego policy
- Policy verification with safe execution sandbox
- Explainable execution with audit trails
- **Rationale:** Advanced AI/ML capability requiring LLM integration, prompt engineering, and extensive safety verification; not essential for initial product delivery
- **Would Require:** LLM partnership or self-hosted deployment, dedicated ML engineering resources, policy safety verification framework

**4. Mobile Native Apps**
- iOS native app (Swift, SwiftUI)
- Android native app (Kotlin, Jetpack Compose)
- Offline mode with sync
- Push notifications with device-specific targeting
- Mobile-specific workflows (barcode scanning for ITAM)
- **Rationale:** Progressive Web App (PWA) with responsive design sufficient for mobile access; native apps require dedicated mobile development team and ongoing maintenance
- **Would Require:** User validation of native app demand, dedicated iOS/Android developers, app store distribution infrastructure

**5. Multi-Tenancy Architecture**
- Tenant isolation (database-per-tenant or schema-per-tenant)
- Per-tenant customization (branding, workflows, policies)
- Tenant admin roles and delegated administration
- Cross-tenant analytics for SaaS provider
- Tenant provisioning automation
- **Rationale:** Product designed for single-tenant self-hosted deployment model; multi-tenancy adds significant architectural complexity and operational overhead not justified for target market
- **Would Require:** Fundamental architectural redesign, tenant management platform, billing integration, SaaS operational infrastructure

### Product Boundaries (Integration Points, Not Replacements)

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
- [ ] Review PRD with 5-person engineering team (3 backend developers, 1 frontend developer, 1 DevOps/SRE)
- [ ] Review PRD with product stakeholders
- [ ] Incorporate feedback and finalize PRD v1.0
- [ ] Get formal sign-off from product owner (monosense)

**2. Architecture and Technical Design (Critical Path)**
- [x] **Architecture Workflow Completed**
  - System architecture document completed: architecture.md
  - Covers:
    - High-level architecture diagrams (C4 Model: Context, Container, Component)
    - Event-driven integration patterns (Spring Modulith 1.4.2 + Transactional Outbox)
    - Database schema design (Shared PostgreSQL cluster + PgBouncer pooler, 7 schemas, event_publication table)
    - API design patterns (REST, OpenAPI 3.0, Spring Data JPA 3.2.0 data access)
    - Security architecture (OAuth2, OPA 0.68.0 sidecar, audit pipeline)
    - Deployment architecture (Kubernetes, GitOps with Flux CD 2.2.3, HA)
  - **Key Decisions:**
    - **Spring Modulith modular monolith architecture:** 20x faster event processing (50-100ms vs 2-3s with Kafka), eliminates 6 CPU cores, 13.5GB RAM, $250-350/month infrastructure savings, operational simplicity for 5-person team
    - **Shared PostgreSQL cluster pattern:** Consistent with platform (gitlab, harbor, keycloak, mattermost), PgBouncer pooler for connection management, 50% CPU savings, 87% memory savings
    - **Spring Data JPA instead of MyBatis:** Simpler, Spring-native, declarative repository pattern, sufficient for complete product needs, better Spring ecosystem integration

**3. UX/UI Specification (Highly Recommended)**
- [ ] **Initiate UX Specification Workflow**
  - Continue within PM workflow or new session with UX persona
  - Provide inputs: PRD.md, architecture.md (once available)
  - Request: Comprehensive UX/UI specification covering:
    - Information architecture (IA) and navigation structure
    - User flows for 5 primary journeys (from PRD User Journeys section)
    - Component library (based on Shadcn/ui + Tailwind)
    - Screen wireframes for Foundation Phase features
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
  - PostgreSQL 16.8 DDL scripts with Flyway 10.18.0 migrations
  - Schema naming: synergyflow_* prefix for module schemas
  - event_publication table for Spring Modulith transactional outbox
  - JPA entity definitions with Hibernate 6.4.0 annotations (@Entity, @Table, @ManyToOne, etc.)
  - Spring Data JPA repository interfaces (JpaRepository, custom query methods)
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

### Phase 1: Development Preparation (Weeks 3-4)

**7. Development Environment Setup**
- [ ] Repository structure (monorepo vs multi-repo decision)
  - Backend: synergyflow-backend (Spring Boot 3.5.6, Java 21.0.2 LTS, Gradle 8.7.0, Spring Modulith 1.4.2, Spring Data JPA 3.2.0, Hibernate 6.4.0)
  - Frontend: synergyflow-frontend (Next.js 14.2.5, React 18.3.1, TypeScript, Node.js 20.12.0 LTS)
  - Infrastructure: synergyflow-infra (Kubernetes manifests, Flux CD 2.2.3, Kustomize overlays)
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

**8. Sprint Planning (12-Month Timeline)**
- [ ] Story prioritization (all epics breakdown from epics.md)
- [ ] Sprint boundaries (48-week delivery = 24 two-week sprints organized by delivery phases)
  - **Foundation Phase (Sprints 1-8, Months 1-4):** Epic-16, 00, 01, 02
  - **Core Features Phase (Sprints 9-16, Months 4-8):** Epic-03, 04, 06
  - **Advanced Capabilities Phase (Sprints 17-22, Months 8-11):** Epic-07, 08, 09, 10, 11
  - **Optimization & Scale Phase (Sprints 23-24, Months 11-12):** Epic-12, 13, 14, 15, Final integration testing
- [ ] Resource allocation (5-person team: 3 backend developers, 1 frontend developer, 1 DevOps/SRE)
- [ ] Velocity estimation (story points per sprint across 24 sprints)
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

### Phase 2: Product Development (Months 1-12)

**10. Development Execution**
- [ ] Follow 12-month sprint plan (24 two-week sprints across 4 delivery phases)
- [ ] Daily standups (15 minutes, sync on blockers)
- [ ] Sprint reviews (demo completed stories to stakeholders every 2 weeks)
- [ ] Sprint retrospectives (continuous improvement after each sprint)
- [ ] Track velocity and adjust sprint scope based on team capacity
- [ ] Phase-gate reviews after each delivery phase (Months 4, 8, 11, 12)

**11. Continuous Integration and Staging Validation**
- [ ] Deploy to staging environment continuously throughout development
- [ ] Progressive beta user onboarding (10 users per quarter, target 40 beta users by Month 12)
- [ ] Collect feedback on all features iteratively (Foundation, Core, Advanced, Optimization phases)
- [ ] Validate success criteria from PRD progressively:
  - ✅ All 16 epics completed with acceptance criteria met
  - ✅ Performance targets met (API <200ms p95, event processing lag <200ms p95)
  - ✅ User satisfaction ≥3.5/5.0 (quarterly beta surveys)
  - ✅ Zero workflow state loss incidents
  - ✅ Complete feature parity with ManageEngine ServiceDesk Plus ITSM+PM capabilities
  - ✅ Decision receipts for 100% of policy evaluations

**12. Production Readiness Review (Month 12)**
- [ ] Review complete product success criteria against all 16 epics
- [ ] Final security audit and penetration testing
- [ ] Production environment hardening and scaling validation
- [ ] Stakeholder sign-off on production release
- [ ] Go/No-Go Decision: Proceed to production launch with complete feature set
- [ ] If Go: Prepare for initial production deployment (250-500 users, 10-20 organizations)
- [ ] If No-Go: Identify critical gaps, plan remediation sprints before launch

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
