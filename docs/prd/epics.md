# Epics

## Epic Structure Overview

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

## FOUNDATION PHASE EPICS (Months 1-4)

### Epic-00: Trust + UX Foundation Pack

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

### Epic-01: Incident and Problem Management

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

### Epic-02: Change and Release Management

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

### Epic-05: Knowledge Management

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

### Epic-16: Platform Foundation (Deployment and Operations)

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

## CORE FEATURES PHASE EPICS (Months 4-8)

### Epic-03: Self-Service Portal

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

### Epic-04: Service Catalog and Request Fulfillment

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

### Epic-06: Multi-Team Support and Intelligent Routing

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

## ADVANCED CAPABILITIES PHASE EPICS (Months 8-11)

### Epic-07: IT Asset Management (ITAM)

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

### Epic-08: CMDB and Impact Assessment

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

### Epic-09: Dashboards and Reports

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

### Epic-10: Analytics and BI Integration

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

### Epic-11: Notifications and Communication

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

## OPTIMIZATION & SCALE PHASE EPICS (Months 11-12)

### Epic-12: Security and Compliance

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

### Epic-13: High Availability and Reliability

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

### Epic-14: Testing and Quality Assurance

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

### Epic-15: Integration and API Gateway (Advanced)

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
