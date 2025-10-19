# Requirements

## Functional Requirements

### A. Trust + UX Foundation Pack (Foundation Phase)

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

### B. ITSM Core Modules

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

### C. Project Management (PM) Core

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

### D. User and Team Management

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

### E. Workflow Orchestration and Automation

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

### F. Integration and API Gateway

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

### G. Notifications and Communication

**FR-25: Notification System**
- Template engine with localization support
- User preference subscriptions (email, in-app, push)
- Multi-channel adapters (email, push, in-app notification center)
- Deduplication, grouping, and digest mode
- Delivery metrics and audit reporting

### H. Dashboards, Reports, and Analytics

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

### I. Security, Compliance, and Observability

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

## Non-Functional Requirements

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
