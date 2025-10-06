# Epic 1: Foundation & Infrastructure - Technical Specification

**Epic:** Epic 1 - Foundation & Infrastructure
**Project:** SynergyFlow (Enterprise ITSM & PM Platform)
**Author:** monosense (Backend Architect + Product Manager)
**Date:** 2025-10-06
**Timeline:** Weeks 1-4 (4 weeks)
**Team:** 2 BE + 1 Infra + 1 QA
**Story Points:** 80 points (2 sprints @ 40 velocity)
**Project Level:** Level 4 (Platform/Ecosystem)
**Development Context:** Greenfield

---

## Overview

Epic 1 establishes the complete technical foundation for the Enterprise ITSM & PM Platform. This epic is CRITICAL PATH - all subsequent epics (Epic 2-6) depend on successful completion of this foundation.

**Key Deliverables:**
1. Monorepo structure with modulith + 3 companions deployment pattern
2. PostgreSQL database with OLTP tables + CQRS-lite read models
3. Transactional outbox pattern → Redis Stream → SSE fan-out infrastructure
4. Durable Timer/SLA Service with business calendar support
5. Keycloak OIDC integration with batch authorization
6. CI/CD pipeline with performance gates
7. Local Docker Compose development environment matching production
8. Kubernetes deployment manifests (4 deployments: App, Event Worker, Timer, Indexer)

**Success Criteria:**
- ✅ Authenticated `/actuator/health` endpoint returns 200 OK with JWT token
- ✅ PostgreSQL schema created with ITSM and PM tables via Flyway migrations
- ✅ CI/CD pipeline executes: build → unit tests → SAST → deploy to staging
- ✅ Kubernetes pod deployed via Envoy Gateway with health probe
- ✅ Local Docker Compose dev environment matches production stack

---

## Source Tree Structure

```
synergyflow/                           # Project root (monorepo)
├── backend/                           # Spring Boot 3.5.x modulith
│   ├── src/main/java/io/monosense/synergyflow/
│   │   ├── SynergyFlowApplication.java # Main Spring Boot application
│   │   ├── itsm/                      # ITSM Module
│   │   │   ├── api/                   # REST controllers (TicketController, AgentConsoleController)
│   │   │   ├── spi/                   # Service Provider Interface (@NamedInterface)
│   │   │   │   └── TicketQueryService.java # For other modules to query tickets
│   │   │   ├── internal/              # Internal implementation (package-private)
│   │   │   │   ├── domain/            # Domain entities (Ticket, Incident, ServiceRequest)
│   │   │   │   ├── service/           # Business logic (TicketService, RoutingEngine)
│   │   │   │   ├── repository/        # JPA repositories (TicketRepository, IncidentRepository)
│   │   │   │   └── projection/        # Read model projections (TicketCardProjection)
│   │   │   ├── events/                # Domain events (TicketCreated, TicketAssigned, SlaBreached)
│   │   │   └── package-info.java      # @ApplicationModule annotation
│   │   ├── pm/                        # PM Module
│   │   │   ├── api/                   # REST controllers (IssueController, BoardController)
│   │   │   ├── spi/                   # Service Provider Interface
│   │   │   │   └── IssueQueryService.java
│   │   │   ├── internal/
│   │   │   │   ├── domain/            # Domain entities (Issue, Sprint, Board)
│   │   │   │   ├── service/           # Business logic (IssueService, SprintManager, BurndownCalculator)
│   │   │   │   ├── repository/        # JPA repositories (IssueRepository, SprintRepository)
│   │   │   │   └── projection/        # Read model projections
│   │   │   ├── events/                # Domain events (IssueCreated, IssueStateChanged)
│   │   │   └── package-info.java
│   │   ├── workflow/                  # Workflow Module
│   │   │   ├── api/                   # REST controllers (ApprovalController, WorkflowConfigController)
│   │   │   ├── spi/
│   │   │   │   └── ApprovalQueryService.java
│   │   │   ├── internal/
│   │   │   │   ├── domain/            # Domain entities (Approval, RoutingRule, WorkflowState)
│   │   │   │   ├── service/           # Business logic (ApprovalEngine, RuleEvaluator, StateMachine)
│   │   │   │   └── repository/        # JPA repositories
│   │   │   ├── events/                # Domain events (ApprovalRequested, ApprovalDecided)
│   │   │   └── package-info.java
│   │   ├── security/                  # Security Module (authentication & authorization)
│   │   │   ├── api/                   # Public security APIs
│   │   │   │   ├── JwtValidator.java
│   │   │   │   ├── RoleMapper.java
│   │   │   │   └── BatchAuthService.java
│   │   │   ├── internal/
│   │   │   │   ├── KeycloakClient.java
│   │   │   │   └── AclCacheManager.java
│   │   │   ├── config/                # Spring Security configuration
│   │   │   │   └── SecurityConfig.java
│   │   │   └── package-info.java
│   │   ├── eventing/                  # Event Infrastructure Module
│   │   │   ├── api/
│   │   │   │   ├── OutboxWriter.java
│   │   │   │   └── EventPublisher.java
│   │   │   ├── internal/
│   │   │   │   ├── worker/            # @Profile("worker")
│   │   │   │   │   ├── OutboxPoller.java
│   │   │   │   │   ├── RedisStreamPublisher.java
│   │   │   │   │   ├── ReadModelUpdater.java
│   │   │   │   │   └── GapDetector.java
│   │   │   │   ├── indexer/           # @Profile("indexer")
│   │   │   │   │   ├── OpenSearchClient.java
│   │   │   │   │   ├── EventConsumer.java
│   │   │   │   │   ├── TicketIndexer.java
│   │   │   │   │   └── CorrelationEngine.java
│   │   │   │   └── repository/
│   │   │   │       └── OutboxRepository.java
│   │   │   └── package-info.java
│   │   ├── sla/                       # SLA Management Module
│   │   │   ├── api/
│   │   │   │   └── SlaCalculator.java
│   │   │   ├── internal/
│   │   │   │   ├── timer/             # @Profile("timer")
│   │   │   │   │   ├── TimerScheduler.java
│   │   │   │   │   ├── BusinessCalendar.java
│   │   │   │   │   └── EscalationHandler.java
│   │   │   │   └── repository/
│   │   │   │       └── SlaTrackingRepository.java
│   │   │   └── package-info.java
│   │   ├── sse/                       # Server-Sent Events Module
│   │   │   ├── api/
│   │   │   │   └── SseController.java
│   │   │   ├── internal/
│   │   │   │   ├── SseConnectionManager.java
│   │   │   │   └── RedisStreamSubscriber.java
│   │   │   └── package-info.java
│   │   └── audit/                     # Audit Logging Module
│   │       ├── api/
│   │       │   ├── AuditLogWriter.java
│   │       │   └── AuditEvent.java
│   │       ├── internal/
│   │       │   └── repository/
│   │       │       └── AuditLogRepository.java
│   │       └── package-info.java
│   ├── src/main/resources/
│   │   ├── application.yml            # Main config (datasource, redis, keycloak)
│   │   ├── application-app.yml        # HTTP/SSE App profile config
│   │   ├── application-worker.yml     # Event Worker profile config
│   │   ├── application-timer.yml      # Timer/SLA Service profile config
│   │   ├── application-indexer.yml    # Search Indexer profile config
│   │   └── db/migration/              # Flyway migrations
│   │       ├── V1__create_itsm_tables.sql
│   │       ├── V2__create_pm_tables.sql
│   │       ├── V3__create_shared_tables.sql
│   │       ├── V4__create_read_models.sql
│   │       └── V5__create_outbox_table.sql
│   ├── build.gradle.kts               # Gradle (Kotlin DSL) build config
│   ├── settings.gradle.kts            # Gradle settings
│   └── Dockerfile                     # Multi-stage Docker build
│
├── frontend/                          # React 18 + Vite application
│   ├── src/
│   │   ├── itsm-ui/                   # ITSM UI components
│   │   │   ├── AgentConsole.tsx       # Zero-Hunt Agent Console (Loft Layout)
│   │   │   ├── ServiceCatalog.tsx     # Employee self-service portal
│   │   │   └── MetricsDashboard.tsx   # ITSM metrics (MTTR, FRT, SLA compliance)
│   │   ├── pm-ui/                     # PM UI components
│   │   │   ├── KanbanBoard.tsx        # Developer Copilot Board (drag-drop)
│   │   │   ├── BacklogView.tsx        # Sprint planning interface
│   │   │   └── MetricsDashboard.tsx   # PM metrics (velocity, burndown, cycle time)
│   │   ├── shared-ui/                 # Shared components
│   │   │   ├── UnifiedDashboard.tsx   # Cross-module overview
│   │   │   ├── Layout.tsx             # App shell with nav
│   │   │   └── SseProvider.tsx        # Real-time update provider
│   │   ├── api/                       # API client with axios
│   │   │   ├── itsmApi.ts             # ITSM API calls
│   │   │   ├── pmApi.ts               # PM API calls
│   │   │   └── sseClient.ts           # SSE reconnect logic with cursor
│   │   ├── store/                     # Redux state management
│   │   │   ├── itsmSlice.ts           # ITSM state
│   │   │   ├── pmSlice.ts             # PM state
│   │   │   └── reducers.ts            # Idempotent reducers (drop stale by version)
│   │   ├── App.tsx                    # Root component
│   │   └── main.tsx                   # Entry point
│   ├── package.json                   # NPM dependencies (React 18, AntD 5.x, Vite)
│   ├── vite.config.ts                 # Vite build config
│   └── Dockerfile                     # Nginx static file server
│
├── infrastructure/                    # Kubernetes manifests + Helm
│   ├── helm/                          # Helm chart
│   │   ├── Chart.yaml                 # Chart metadata
│   │   ├── values.yaml                # Default values (replicas, resources, images)
│   │   └── templates/
│   │       ├── app-deployment.yaml    # HTTP/SSE App (5 replicas)
│   │       ├── worker-deployment.yaml # Event Worker (2 replicas)
│   │       ├── timer-deployment.yaml  # Timer/SLA Service (2 replicas)
│   │       ├── indexer-deployment.yaml # Search Indexer (1 replica)
│   │       ├── service.yaml           # ClusterIP services
│   │       ├── httproute.yaml         # Envoy Gateway HTTPRoute
│   │       ├── configmap.yaml         # Application config
│   │       └── secrets.yaml           # Keycloak credentials (template)
│   ├── docker-compose.yml             # Local dev environment
│   └── k8s-dev/                       # Development namespace manifests
│
├── docs/                              # Documentation
│   ├── product/
│   │   ├── prd.md                     # Product Requirements (v3.0)
│   │   └── prd-validation-report.md   # Validation report for PRD
│   ├── epics/                         # Epic technical specifications
│   │   ├── epic-1-foundation-tech-spec.md
│   │   ├── epic-2-itsm-tech-spec.md
│   │   ├── epic-3-pm-tech-spec.md
│   │   └── epic-4-workflow-tech-spec.md
│   ├── architecture/                  # Architecture package
│   │   ├── architecture-blueprint.md
│   │   ├── module-boundaries.md
│   │   ├── data-model.md
│   │   ├── performance-model.md
│   │   ├── eventing-and-timers.md
│   │   ├── architecture-validation-report.md
│   │   ├── gradle-build-config.md
│   │   ├── adr/
│   │   └── db/migrations/
│   ├── api/                           # OpenAPI 3.0 specs
│   │   ├── itsm-api.yaml
│   │   ├── pm-api.yaml
│   │   ├── workflow-api.yaml
│   │   └── sse-api.yaml
│   ├── project-workflow-analysis.md
│   ├── brainstorming-session-results-2025-10-05.md
│   ├── corrections-summary.md
│   └── README.md
│
└── testing/                           # Integration + E2E + Performance tests
    ├── integration/                   # Spring Boot Test + Testcontainers
    ├── e2e/                           # Playwright tests
    └── performance/                   # Gatling scenarios
```

**Key Design Decisions:**
- **Monorepo:** Single Git repository with clear module boundaries (backend, frontend, infrastructure, docs, testing)
- **Modulith:** Spring Boot modulith using Spring Modulith library for explicit module declarations
- **Companions:** Event Worker, Timer/SLA, Indexer as separate Spring Boot profiles (same codebase, separate deployments)
- **CQRS-lite:** Denormalized read models (`ticket_card`, `queue_row`, `issue_card`) for hot reads (<200ms p95)
- **Event-Driven:** Transactional outbox → Redis Stream → SSE fan-out + read model updates

---

## Technical Approach

### Architecture Pattern: Modulith + 3 Companions

**Core Philosophy:** Keep domain cohesive (atomic refactors, shared types) while isolating physics-heavy loops (events, timers, search) that would murder HTTP tail latency if coupled.

#### 1. Core Application (Spring Boot Modulith)

**Deployment:** HTTP/SSE App (Spring profile: `app`)
**Responsibilities:**
- Synchronous CRUD + query APIs
- RBAC authorization (Keycloak JWT validation)
- Transactional writes (domain entities + outbox events)
- SSE gateway (subscribes to Redis Stream, pushes to clients)
- Read model exposure (materialized views for queue cards, side-panel bundles)

**Modules (Spring Modulith):**
- **ITSM Module:** Ticket/Incident/ServiceRequest domain, TicketService, RoutingEngine (package: `io.monosense.synergyflow.itsm`)
- **PM Module:** Issue/Sprint/Board domain, IssueService, SprintManager, BurndownCalculator (package: `io.monosense.synergyflow.pm`)
- **Workflow Module:** Approval/RoutingRule/WorkflowState domain, ApprovalEngine, RuleEvaluator, StateMachine (package: `io.monosense.synergyflow.workflow`)
- **Security Module:** Authentication & Authorization (Keycloak, JWT, Batch Auth) (package: `io.monosense.synergyflow.security`)
- **Eventing Module:** Event Infrastructure (Outbox, Event Worker, Search Indexer) (package: `io.monosense.synergyflow.eventing`)
- **SLA Module:** SLA Calculation & Timer Service (package: `io.monosense.synergyflow.sla`)
- **SSE Module:** Server-Sent Events Gateway (package: `io.monosense.synergyflow.sse`)
- **Audit Module:** Audit Logging (package: `io.monosense.synergyflow.audit`)

**Technology Stack:**
- Spring Boot 3.5.x (Java 21 LTS)
- Spring Modulith library for module enforcement
- Spring Data JPA + Hibernate for OLTP
- Spring Security + OAuth2 Resource Server for Keycloak integration
- Spring Web for REST APIs
- Flyway for database migrations
- HikariCP connection pooling (maxPoolSize=50)

**Deployment:**
- 5 replicas (50 users per instance → 250 concurrent capacity)
- Resources: 1 CPU / 2GB RAM requests, 2 CPU / 4GB RAM limits
- Kubernetes Deployment with rolling updates
- Envoy Gateway HTTPRoute for traffic routing
- Health probes: `/actuator/health` (liveness), `/actuator/health/readiness` (readiness)

#### 2. Event Worker (Companion Service)

**Deployment:** Event Worker (Spring profile: `worker`)
**Responsibilities:**
- Polls transactional outbox in FIFO order with gap detection
- Publishes events to Redis Stream (fan-out channel for SSE + indexers)
- Updates materialized read models (`ticket_card`, `queue_row`, `issue_card`, `sprint_summary`)
- Idempotent event processing (deduplication by `{aggregate_id, version}`)

**Technology Stack:**
- Same Spring Boot codebase, different profile
- Spring Integration for outbox polling
- Redisson client for Redis Stream publishing
- Spring Data JPA for read model updates
- Spring Retry for transient failures

**Deployment:**
- 2 replicas (can scale on outbox lag)
- Resources: 0.5 CPU / 1GB RAM requests, 1 CPU / 2GB RAM limits
- Autoscale triggers: outbox_rows_behind (lag), processing_time_ms
- No HTTP ports exposed (background worker)

**Failure Isolation:**
- Event Worker crash → HTTP App continues serving cached reads (ETag 304 Not Modified)
- Outbox backpressure → doesn't steal CPU from HTTP p95 latency

#### 3. Timer/SLA Service (Companion Service)

**Deployment:** Timer/SLA Service (Spring profile: `timer`)
**Responsibilities:**
- Durable job queue with O(1) create/update/cancel operations
- Business calendar evaluator for pause/resume logic (changes `due_at` for holidays, weekends, business hours)
- Idempotent escalation handlers with exponential backoff
- SLA due date calculation (Priority-based: Critical 2h, High 4h, Medium 8h, Low 24h)
- Timer mutations on ticket state changes (priority upgrade → recalculate `due_at`)

**Technology Stack:**
- Same Spring Boot codebase, different profile
- Persistent job queue: `sla_timers` table with B-tree index on `due_at`
- Scheduler: Spring `@Scheduled` with fixed delay (every 5 seconds, scan `due_at <= now()`)
- Business calendar: `business_calendars` table (holidays, business hours per timezone)
- Escalation: Spring Retry with exponential backoff (1s, 2s, 4s, max 3 retries)

**Deployment:**
- 2 replicas (active-passive or sharded by timer buckets: even/odd `ticket_id`)
- Resources: 0.5 CPU / 1GB RAM requests, 1 CPU / 2GB RAM limits
- No HTTP ports exposed (background scheduler)

**Success Criteria:**
- Timer precision drift <1s (NFR18)
- Zero missed escalations (NFR18)
- Timer mutation operations O(1) with p95 <50ms (NFR18)

#### 4. Search Indexer (Companion Service - Optional Merge)

**Deployment:** Search Indexer (Spring profile: `indexer`)
**Responsibilities:**
- Consumes change events from Event Worker (Redis Stream subscriber)
- Maintains OpenSearch inverted index for tickets, issues, comments, KB articles
- Precomputes `related_incident_ids[]` and `related_issue_ids[]` for O(1) side-panel "Related Tickets" lookups
- Versioned documents (drop stale updates by `version`)

**Technology Stack:**
- Same Spring Boot codebase, different profile
- OpenSearch Java client
- Redis Stream consumer (consumer group for fault tolerance)
- Sentence-transformers vector embeddings for similarity (future Phase 2)

**Deployment:**
- 1 replica (CPU-bound workload, scale when needed)
- Resources: 1 CPU / 2GB RAM requests, 2 CPU / 4GB RAM limits
- Can merge into Event Worker at small scale; extract when CPU-bound

**Success Criteria:**
- Search query p95 <300ms (NFR19)
- Index lag p95 <2s from write to searchability (NFR19)
- Correlation precision @k≥5 >0.6 (NFR19)

**Note:** At MVP scale (250 concurrent users, <10K tickets/issues), can start with SQL-based search (`ILIKE`, GIN index) and defer OpenSearch to Phase 2. Validate with spike in Week 3.

---

### Database Design: CQRS-Lite Pattern

**Philosophy:** Hot-read, cool-write workload. Agents/devs read way more than write, and they skim fast. Denormalize for reads, normalize for writes.

#### Primary Datastore: PostgreSQL 16+

**Sizing:**
- 4 vCPU, 16GB RAM, 500GB SSD
- High Availability: Streaming replication (active-passive)
- Connection Pool: HikariCP `maximumPoolSize = 50`

**Schema Strategy:**
- **OLTP Tables (Normalized):** Source of truth for domain writes
- **Read Models (Denormalized):** Materialized views for hot reads, updated by Event Worker

#### OLTP Tables (Normalized - Epic 1 Foundation)

**ITSM OLTP:**
```sql
-- Flyway migration: V1__create_itsm_tables.sql

CREATE TABLE tickets (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7 (time-ordered, RFC 9562)
    ticket_type VARCHAR(20) NOT NULL CHECK (ticket_type IN ('INCIDENT', 'SERVICE_REQUEST')),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    severity VARCHAR(10),
    category VARCHAR(100),
    subcategory VARCHAR(100),
    requester_id UUID NOT NULL,
    assignee_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1, -- Optimistic locking
    CONSTRAINT fk_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_assignee FOREIGN KEY (assignee_id) REFERENCES users(id)
);

CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_assignee ON tickets(assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_tickets_created_at ON tickets(created_at DESC);
CREATE INDEX idx_tickets_updated_at ON tickets(updated_at DESC);

CREATE TABLE incidents (
    ticket_id UUID PRIMARY KEY,
    resolution_notes TEXT,
    resolved_at TIMESTAMP,
    CONSTRAINT fk_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
);

CREATE TABLE service_requests (
    ticket_id UUID PRIMARY KEY,
    request_type VARCHAR(100) NOT NULL,
    approval_status VARCHAR(20) CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    fulfiller_id UUID,
    fulfilled_at TIMESTAMP,
    CONSTRAINT fk_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_fulfiller FOREIGN KEY (fulfiller_id) REFERENCES users(id)
);

CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    ticket_id UUID NOT NULL,
    author_id UUID NOT NULL,
    comment_text TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE INDEX idx_comments_ticket ON ticket_comments(ticket_id, created_at DESC);

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    condition_type VARCHAR(50) NOT NULL, -- 'CATEGORY', 'PRIORITY', 'ROUND_ROBIN'
    condition_value VARCHAR(255),
    target_team_id UUID,
    target_agent_id UUID,
    priority INT NOT NULL DEFAULT 100,
    enabled BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT fk_target_team FOREIGN KEY (target_team_id) REFERENCES teams(id)
);
```

**PM OLTP:**
```sql
-- Flyway migration: V2__create_pm_tables.sql

CREATE TABLE issues (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7 (time-ordered, RFC 9562)
    issue_key VARCHAR(20) NOT NULL UNIQUE, -- e.g., 'DEV-123'
    title VARCHAR(255) NOT NULL,
    description TEXT,
    issue_type VARCHAR(20) NOT NULL CHECK (issue_type IN ('STORY', 'TASK', 'BUG', 'EPIC')),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    story_points INT,
    sprint_id UUID,
    epic_id UUID,
    assignee_id UUID,
    reporter_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id),
    CONSTRAINT fk_epic FOREIGN KEY (epic_id) REFERENCES issues(id),
    CONSTRAINT fk_assignee FOREIGN KEY (assignee_id) REFERENCES users(id),
    CONSTRAINT fk_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)
);

CREATE INDEX idx_issues_status ON issues(status);
CREATE INDEX idx_issues_sprint ON issues(sprint_id) WHERE sprint_id IS NOT NULL;
CREATE INDEX idx_issues_assignee ON issues(assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_issues_created_at ON issues(created_at DESC);

CREATE TABLE sprints (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    sprint_name VARCHAR(100) NOT NULL,
    sprint_goal TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    capacity_points INT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE boards (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    board_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE board_columns (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    board_id UUID NOT NULL,
    column_name VARCHAR(50) NOT NULL,
    position INT NOT NULL,
    wip_limit INT,
    CONSTRAINT fk_board FOREIGN KEY (board_id) REFERENCES boards(id) ON DELETE CASCADE,
    CONSTRAINT unique_board_position UNIQUE (board_id, position)
);

CREATE TABLE issue_links (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    source_issue_id UUID NOT NULL,
    target_issue_id UUID NOT NULL,
    link_type VARCHAR(50) NOT NULL, -- 'BLOCKS', 'RELATES_TO', 'DUPLICATES', 'CHILD_OF'
    CONSTRAINT fk_source FOREIGN KEY (source_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_target FOREIGN KEY (target_issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT unique_link UNIQUE (source_issue_id, target_issue_id, link_type)
);
```

**Shared OLTP:**
```sql
-- Flyway migration: V3__create_shared_tables.sql

CREATE TABLE users (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7 (time-ordered, RFC 9562)
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    keycloak_id VARCHAR(100) UNIQUE, -- Maps to Keycloak user ID
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    role_name VARCHAR(50) NOT NULL UNIQUE -- 'ADMIN', 'ITSM_AGENT', 'DEVELOPER', 'MANAGER', 'EMPLOYEE'
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE teams (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    team_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    request_id UUID NOT NULL, -- References service_requests.ticket_id
    approver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    comment TEXT,
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_approver FOREIGN KEY (approver_id) REFERENCES users(id)
);

CREATE TABLE workflow_states (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7
    aggregate_id UUID NOT NULL, -- ticket_id or issue_id
    aggregate_type VARCHAR(20) NOT NULL, -- 'TICKET' or 'ISSUE'
    current_state VARCHAR(50) NOT NULL,
    allowed_transitions JSONB NOT NULL, -- e.g., ['ASSIGNED', 'CLOSED']
    CONSTRAINT unique_aggregate UNIQUE (aggregate_id, aggregate_type)
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,  -- Hibernate generates UUIDv7 (time-ordered for chronological queries)
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor_id UUID NOT NULL,
    changes JSONB, -- Before/after snapshot
    occurred_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_aggregate ON audit_log(aggregate_id, occurred_at DESC);
CREATE INDEX idx_audit_occurred ON audit_log(occurred_at DESC);

-- Transactional Outbox Pattern
CREATE TABLE outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL, -- 'TICKET', 'ISSUE', 'APPROVAL'
    event_type VARCHAR(100) NOT NULL, -- 'TicketCreated', 'TicketAssigned', 'IssueStateChanged'
    version BIGINT NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT now(),
    payload JSONB NOT NULL,
    processed_at TIMESTAMP, -- NULL = unprocessed, NOT NULL = processed
    CONSTRAINT unique_event UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_outbox_unprocessed ON outbox(id) WHERE processed_at IS NULL;
CREATE INDEX idx_outbox_occurred ON outbox(occurred_at DESC);
```

#### Read Models (Denormalized - Epic 1 Foundation)

**ITSM Read Models:**
```sql
-- Flyway migration: V4__create_read_models.sql

-- Queue Card View (for agent console queue, <200ms p95)
CREATE TABLE ticket_card (
    ticket_id UUID PRIMARY KEY,
    ticket_key VARCHAR(20) NOT NULL, -- Display ID like 'T-1234'
    title VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    assignee_name VARCHAR(255), -- Denormalized for speed
    assignee_avatar_url VARCHAR(500),
    sla_due_at TIMESTAMP, -- Precomputed due date
    sla_risk_level VARCHAR(20), -- 'SAFE', 'WARN', 'BREACH'
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL -- For idempotent updates
);

CREATE INDEX idx_ticket_card_status ON ticket_card(status);
CREATE INDEX idx_ticket_card_assignee ON ticket_card(assignee_name);
CREATE INDEX idx_ticket_card_sla_due ON ticket_card(sla_due_at NULLS LAST);
CREATE INDEX idx_ticket_card_updated ON ticket_card(updated_at DESC);

-- SLA Tracking (for timer service, O(1) lookups)
CREATE TABLE sla_tracking (
    ticket_id UUID PRIMARY KEY,
    priority VARCHAR(20) NOT NULL,
    sla_duration_hours INT NOT NULL, -- Critical: 2, High: 4, Medium: 8, Low: 24
    due_at TIMESTAMP NOT NULL,
    paused BOOLEAN NOT NULL DEFAULT false,
    pause_windows JSONB, -- Array of {start, end} for business calendar pauses
    escalation_count INT NOT NULL DEFAULT 0,
    last_escalated_at TIMESTAMP,
    CONSTRAINT fk_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
);

CREATE INDEX idx_sla_due_at ON sla_tracking(due_at) WHERE NOT paused; -- For scheduler scan

-- Related Incidents (precomputed for O(1) side-panel loads)
CREATE TABLE related_incidents (
    ticket_id UUID PRIMARY KEY,
    related_incident_ids UUID[] NOT NULL, -- Array of related ticket UUIDs
    similarity_scores FLOAT[] NOT NULL, -- Parallel array of similarity scores [0.0-1.0]
    last_updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
);

CREATE INDEX idx_related_updated ON related_incidents(last_updated_at DESC);
```

**PM Read Models:**
```sql
-- Queue Row View (for backlog view, <200ms p95)
CREATE TABLE queue_row (
    issue_id UUID PRIMARY KEY,
    issue_key VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    issue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    story_points INT,
    assignee_name VARCHAR(255),
    reporter_name VARCHAR(255),
    sprint_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_queue_row_status ON queue_row(status);
CREATE INDEX idx_queue_row_sprint ON queue_row(sprint_name);
CREATE INDEX idx_queue_row_priority ON queue_row(priority);

-- Issue Card View (for board cards, <200ms p95)
CREATE TABLE issue_card (
    issue_id UUID PRIMARY KEY,
    issue_key VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    issue_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20),
    story_points INT,
    assignee_avatar_url VARCHAR(500),
    labels JSONB, -- Array of label strings
    board_id UUID,
    column_position INT,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_issue_card_board ON issue_card(board_id, column_position);
CREATE INDEX idx_issue_card_status ON issue_card(status);

-- Sprint Summary (for burndown charts, <200ms p95)
CREATE TABLE sprint_summary (
    sprint_id UUID PRIMARY KEY,
    sprint_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_points INT NOT NULL,
    completed_points INT NOT NULL,
    in_progress_points INT NOT NULL,
    remaining_points INT NOT NULL,
    ideal_burndown JSONB, -- Array of {day, ideal_points}
    actual_burndown JSONB, -- Array of {day, actual_points}
    last_updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE
);
```

**Event Worker Responsibilities:**
- Listen to `TicketCreated`, `TicketAssigned`, `TicketStateChanged` events from outbox
- Update `ticket_card` (denormalized title, assignee name, SLA risk)
- Update `sla_tracking` when priority changes (trigger Timer Service recalculation)
- Update `related_incidents` when new tickets created (trigger Indexer correlation)

---

### UUIDv7 Entity Implementation (Hibernate 7.0)

**See comprehensive guide:** `docs/uuidv7-implementation-guide.md`

#### Entity Example: Ticket

```java
package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @UuidGenerator(style = Style.VERSION_7)  // ← Native UUIDv7 (Hibernate 7.0+)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ticket_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TicketType ticketType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Getters, setters, constructors
}
```

**Key Points:**
- `@UuidGenerator(style = Style.VERSION_7)` - Hibernate 7.0 native support (RFC 9562 compliant)
- Application-side generation (not database DEFAULT)
- Time-ordered UUIDs improve index performance (6.6x faster inserts vs UUIDv4)
- Globally unique across distributed systems

**Alternative for Hibernate 6.x (Custom Generator):**
```java
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedUuidV7  // Custom annotation (requires uuid-creator library)
    private UUID id;

    // ... rest of entity
}
```

See `docs/uuidv7-implementation-guide.md` Section "Implementation Option 2" for Hibernate 6.x custom generator details.

---

### Event-Driven Architecture: Transactional Outbox Pattern

**Philosophy:** Eventual consistency with guaranteed event delivery. Avoid distributed transactions, embrace at-least-once delivery + idempotent reducers.

#### Event Flow:

```
1. HTTP Write Request
   ↓
2. Application Transaction (ACID)
   - Update domain entity (tickets, issues)
   - Insert event to outbox table
   - Commit (both succeed or both fail)
   ↓
3. Event Worker (polls outbox every 100ms)
   - SELECT * FROM outbox WHERE processed_at IS NULL ORDER BY id LIMIT 100
   - Publish each event to Redis Stream (topic: "domain-events")
   - Update read models (ticket_card, queue_row, issue_card, sprint_summary)
   - Mark event processed: UPDATE outbox SET processed_at = now() WHERE id = ?
   ↓
4. Redis Stream Fan-Out
   - SSE Gateway subscribes → pushes to connected clients
   - Search Indexer subscribes → updates OpenSearch
   - Metrics Collector subscribes → updates dashboards (future)
   ↓
5. Client SSE Receiver
   - Receive event: {aggregate_id, event_type, version, payload}
   - Idempotent reducer: if (event.version <= currentState.version) { drop; } else { apply; }
   - UI updates with version-based deduplication
```

#### Outbox Event Schema:

```json
{
  "id": 12345,
  "aggregate_id": "uuid-123",
  "aggregate_type": "TICKET",
  "event_type": "TicketAssigned",
  "version": 5,
  "occurred_at": "2025-10-06T14:23:15Z",
  "payload": {
    "ticket_id": "uuid-123",
    "assignee_id": "uuid-456",
    "assignee_name": "Alice Chen",
    "reason": "Network expertise",
    "assigned_by": "uuid-789"
  },
  "processed_at": "2025-10-06T14:23:16Z"
}
```

#### Redis Stream Configuration:

```yaml
# application-worker.yml
spring:
  redis:
    stream:
      domain-events:
        maxlen: 100000 # Keep last 100K events (24-48h retention at 1K events/hour)
        consumer-group: event-workers
        consumer-name: worker-${HOSTNAME}
        block-millis: 1000
```

#### Idempotent Client Reducer:

```typescript
// frontend/src/store/reducers.ts
const ticketReducer = (state: TicketState, event: DomainEvent) => {
  const currentTicket = state.tickets[event.aggregate_id];

  // Idempotency: Drop stale events by version
  if (currentTicket && event.version <= currentTicket.version) {
    console.log(`Dropping stale event: ${event.event_type} v${event.version}`);
    return state;
  }

  // Apply event
  switch (event.event_type) {
    case 'TicketAssigned':
      return {
        ...state,
        tickets: {
          ...state.tickets,
          [event.aggregate_id]: {
            ...currentTicket,
            assignee_id: event.payload.assignee_id,
            assignee_name: event.payload.assignee_name,
            version: event.version,
            updated_at: event.occurred_at
          }
        }
      };
    // ... other event types
  }
};
```

**Success Criteria:**
- E2E event propagation ≤2s p95 (NFR9)
- Reconnect catch-up <1s (NFR9)
- Zero event loss (transactional outbox guarantees)
- Client deduplication 100% (version-based drop stale)

---

### Authorization: Batch Pattern

**Philosophy:** Auth cost compounds—batch or die. Every list/board card needs permission check; per-row checks nuke tail latency.

#### Batch Authorization Flow:

```
1. User Request: GET /api/itsm/tickets/queue
   ↓
2. Scope-First SQL Query
   - Filter by team/project/role at database level BEFORE per-row checks
   - Example: SELECT * FROM ticket_card WHERE assignee_id IN (SELECT user_id FROM user_teams WHERE team_id IN (:user_teams))
   ↓
3. Batch Authorization Check
   - Extract ticket IDs from query result: [uuid-1, uuid-2, ..., uuid-50]
   - POST /auth/batchCheck { resource_type: 'TICKET', resource_ids: [uuid-1, ..., uuid-50], action: 'READ' }
   - Response: { allowed_ids: [uuid-1, uuid-3, ...], denied_ids: [uuid-2, ...] }
   ↓
4. Filter Results
   - Return only allowed tickets
   - Log denied access attempts to audit_log
```

#### Batch Authorization API:

```java
// backend/src/main/java/io/monosense/synergyflow/security/api/BatchAuthService.java

package io.monosense.synergyflow.security.api;

@Service
public class BatchAuthService {

    private final KeycloakAuthzClient authzClient;
    private final RedisTemplate<String, Set<UUID>> aclCache;

    /**
     * Batch authorization check for N resource IDs
     * Performance target: p95 <3ms for 100 IDs
     */
    public BatchAuthResult checkBatch(String userId, String resourceType, List<UUID> resourceIds, String action) {
        // 1. Check hot ACL cache (per-team capability bitsets)
        Set<UUID> cachedAllowed = aclCache.opsForValue().get(cacheKey(userId, resourceType, action));
        if (cachedAllowed != null) {
            Set<UUID> allowed = resourceIds.stream()
                .filter(cachedAllowed::contains)
                .collect(Collectors.toSet());
            return new BatchAuthResult(allowed, Sets.difference(new HashSet<>(resourceIds), allowed));
        }

        // 2. Fallback: Batch call to Keycloak (parallel requests for chunks of 100)
        List<List<UUID>> chunks = Lists.partition(resourceIds, 100);
        return chunks.parallelStream()
            .map(chunk -> authzClient.checkPermissions(userId, resourceType, chunk, action))
            .reduce(BatchAuthResult::merge)
            .orElse(new BatchAuthResult(Set.of(), new HashSet<>(resourceIds)));
    }

    private String cacheKey(String userId, String resourceType, String action) {
        return String.format("acl:%s:%s:%s", userId, resourceType, action);
    }
}
```

**Success Criteria:**
- Auth overhead <10% of request time (NFR20)
- Batch-check 100 IDs p95 <3ms (NFR20)
- Hot ACL cache hit rate >80%

---

## Implementation Stack

### Backend

**Core Framework:**
- Spring Boot 3.4+ (Java 21 LTS)
- Spring Modulith 1.2+ (module enforcement, event publication)
- Spring Data JPA + **Hibernate 7.0** (native UUIDv7 support - RFC 9562)
- Spring Security 6.x + OAuth2 Resource Server
- Spring Web (REST APIs)
- Flyway 10.x (database migrations)

**UUID Generation:**
- **UUIDv7 Primary Keys** (time-ordered, RFC 9562 compliant)
- Hibernate 7.0 native support via `@UuidGenerator(style = Style.VERSION_7)`
- See: `docs/uuidv7-implementation-guide.md` for comprehensive implementation details
- Performance: 6.6x faster inserts vs UUIDv4, better index locality

**Data Access:**
- PostgreSQL 16+ JDBC Driver
- HikariCP (connection pooling)
- Jackson (JSON serialization)

**Messaging:**
- Redisson 3.x (Redis client for Streams, caching)
- Spring Integration (outbox polling, event processing)

**Search (Optional MVP - Spike Week 3):**
- OpenSearch Java Client 2.x
- OR: PostgreSQL GIN index + `ILIKE` for MVP (<10K records)

**Authentication:**
- Keycloak Java Adapter 23.x (OIDC client)
- Nimbus JOSE+JWT (JWT parsing/validation)

**Observability:**
- Spring Boot Actuator (metrics, health)
- Micrometer + Prometheus (metrics export)
- OpenTelemetry Java Agent (distributed tracing)
- Logback (structured JSON logging)

**Testing:**
- JUnit 5 (unit tests)
- Mockito (mocking)
- Spring Boot Test (integration tests)
- Testcontainers (PostgreSQL, Redis containers)
- ArchUnit (module boundary enforcement)
- WireMock (HTTP mocking for Keycloak)

**Build:**
- Gradle 8.10+ with Kotlin DSL (build tool)
- Gradle `test` (unit) and `integrationTest` (integration) source set
- Dockerfile multi-stage build (JDK 21 → JRE 21 runtime)

### Frontend

**Core Framework:**
- React 18.3+ (UI library)
- TypeScript 5.x (type safety)
- Vite 5.x (build tool, HMR)

**UI Library:**
- Ant Design (AntD) 5.x (component library)
- AntD Icons (icon set)

**State Management:**
- Redux Toolkit 2.x (global state)
- RTK Query (API client with caching)
- Immer (immutable updates)

**Routing:**
- React Router 6.x (client-side routing)

**API Client:**
- Axios 1.x (HTTP client)
- EventSource (SSE client with polyfill)

**Real-Time:**
- Server-Sent Events (SSE) with reconnect cursor
- Idempotent reducers (version-based deduplication)

**Testing:**
- Vitest (unit tests)
- React Testing Library (component tests)
- Playwright (E2E tests)
- axe-core (accessibility tests)

**Build:**
- Vite (bundling, code splitting)
- TypeScript Compiler (type checking)
- ESLint (linting)
- Prettier (code formatting)
- Dockerfile (Nginx static file server)

### Infrastructure

**Orchestration:**
- Kubernetes 1.28+ (on-premises cluster)
- Helm 3.x (package manager)
- Envoy Gateway 1.x (Kubernetes Gateway API)

**Databases:**
- PostgreSQL 16+ (primary datastore)
  - Streaming replication (active-passive HA)
  - pgBackRest (backup/restore)
  - Connection pooling via PgBouncer (optional)

**Caching & Messaging:**
- Redis 7.x (SSE fan-out via Streams, hot ACL cache, rate limiting)
  - Redis Sentinel (HA)
  - AOF persistence (append-only file)

**Search (Optional MVP):**
- OpenSearch 2.x (search/correlation)
  - 1 hot node + 1 replica
  - Write-behind indexing via Event Worker

**Identity:**
- Keycloak 23.x (existing on-prem)
  - OIDC/SAML SSO
  - Custom RBAC roles: Admin, ITSM_Agent, Developer, Manager, Employee

**Observability:**
- Prometheus (metrics collection)
- Grafana (dashboards)
- Jaeger or Tempo (distributed tracing)
- Loki (log aggregation)

**CI/CD:**
- Jenkins or GitHub Actions (build pipeline)
- Docker (containerization)
- Helm (deployment)
- SonarQube or Qodana (SAST)
- OWASP Dependency-Check + Trivy (dependency scanning)
- Gatling (performance testing)

---

## Spring Modulith Module Declarations

### Module Structure & Boundaries

Each application module MUST declare its boundaries using `@ApplicationModule` annotation with explicit allowed dependencies.

**ITSM Module Declaration:**

```java
// backend/src/main/java/io/monosense/synergyflow/itsm/package-info.java

/**
 * ITSM Module - Incident Management and Service Requests
 *
 * Public API:
 * - api/TicketController.java (REST endpoints)
 * - api/AgentConsoleController.java (REST endpoints)
 * - spi/TicketQueryService.java (for other modules to query tickets)
 *
 * Events Published:
 * - TicketCreatedEvent
 * - TicketAssignedEvent
 * - TicketStateChangedEvent
 * - SlaBreachedEvent
 *
 * Dependencies:
 * - security (authentication, authorization)
 * - eventing (outbox pattern, event publishing)
 * - sla (SLA calculation)
 * - sse (real-time updates)
 * - audit (audit logging)
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "ITSM Module",
    allowedDependencies = { "security", "eventing", "sla", "sse", "audit" }
)
package io.monosense.synergyflow.itsm;
```

**PM Module Declaration:**

```java
// backend/src/main/java/io/monosense/synergyflow/pm/package-info.java

/**
 * PM Module - Project Management and Issue Tracking
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "PM Module",
    allowedDependencies = { "security", "eventing", "sse", "audit" }
)
package io.monosense.synergyflow.pm;
```

**Workflow Module Declaration:**

```java
// backend/src/main/java/io/monosense/synergyflow/workflow/package-info.java

/**
 * Workflow Module - Approval Workflows and State Machines
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Workflow Module",
    allowedDependencies = { "itsm.spi", "pm.spi", "security", "eventing", "audit" }
)
package io.monosense.synergyflow.workflow;
```

**Named Interface for Cross-Module Communication:**

```java
// backend/src/main/java/io/monosense/synergyflow/itsm/spi/package-info.java

/**
 * ITSM Service Provider Interface (SPI)
 *
 * This package exposes ticket query APIs for other modules (PM, Workflow).
 * Only interfaces in this package are allowed as cross-module contracts.
 */
@org.springframework.modulith.NamedInterface("spi")
package io.monosense.synergyflow.itsm.spi;
```

**Module Verification Tests:**

```java
// backend/src/test/java/io/monosense/synergyflow/ModularityTests.java

package io.monosense.synergyflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.test.AssertableApplicationModules;

/**
 * Spring Modulith verification tests
 * These tests enforce module boundaries at build time
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);

    @Test
    void verifiesModularStructure() {
        // Verify module boundaries are respected
        AssertableApplicationModules.of(modules).verify();
    }

    @Test
    void verifyNoCyclicDependencies() {
        // Ensure no circular dependencies between modules
        modules.verify();
    }

    @Test
    void writeDocumentationSnippets() {
        // Generate C4 component diagrams
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }
}
```

**Module Dependency Matrix:**

| Module | Allowed Dependencies |
|--------|---------------------|
| itsm | security, eventing, sla, sse, audit |
| pm | security, eventing, sse, audit |
| workflow | itsm.spi, pm.spi, security, eventing, audit |
| security | _(no module dependencies)_ |
| eventing | _(no module dependencies)_ |
| sla | _(no module dependencies)_ |
| sse | eventing |
| audit | _(no module dependencies)_ |

---

## Technical Details

### 1. Transactional Outbox Implementation

**Domain Event Publishing:**

```java
// backend/src/main/java/io/monosense/synergyflow/eventing/api/OutboxWriter.java

package io.monosense.synergyflow.eventing.api;

@Service
@Transactional
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Write domain event to outbox table in same transaction as entity update
     * Guarantees atomicity: both succeed or both fail
     */
    public void publish(DomainEvent event) {
        OutboxEntity outboxEntity = new OutboxEntity();
        outboxEntity.setAggregateId(event.getAggregateId());
        outboxEntity.setAggregateType(event.getAggregateType());
        outboxEntity.setEventType(event.getClass().getSimpleName());
        outboxEntity.setVersion(event.getVersion());
        outboxEntity.setOccurredAt(event.getOccurredAt());
        outboxEntity.setPayload(objectMapper.writeValueAsString(event));

        outboxRepository.save(outboxEntity);
    }
}
```

**Event Worker Polling:**

```java
// backend/src/main/java/io/monosense/synergyflow/eventing/internal/worker/OutboxPoller.java

package io.monosense.synergyflow.eventing.internal.worker;

@Component
@Profile("worker")
class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final RedisStreamPublisher redisPublisher;
    private final ReadModelUpdater readModelUpdater;

    @Scheduled(fixedDelay = 100) // Poll every 100ms
    public void pollAndPublish() {
        List<OutboxEntity> unprocessed = outboxRepository.findUnprocessedBatch(100);

        for (OutboxEntity event : unprocessed) {
            try {
                // 1. Publish to Redis Stream (fan-out for SSE + Indexer)
                redisPublisher.publish("domain-events", event);

                // 2. Update read models (ticket_card, queue_row, issue_card, sprint_summary)
                readModelUpdater.update(event);

                // 3. Mark as processed
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to process outbox event {}", event.getId(), e);
                // Leave unprocessed, will retry on next poll
            }
        }
    }
}
```

**Gap Detection (Prevent Silent Failures):**

```java
// backend/src/main/java/io/monosense/synergyflow/eventing/internal/worker/GapDetector.java

package io.monosense.synergyflow.eventing.internal.worker;

@Component
@Profile("worker")
class GapDetector {

    @Scheduled(fixedRate = 60000) // Check every 1 minute
    public void detectGaps() {
        Long maxProcessedId = outboxRepository.findMaxProcessedId();
        List<Long> gaps = outboxRepository.findGaps(maxProcessedId);

        if (!gaps.isEmpty()) {
            log.error("Outbox gaps detected: {}", gaps);
            alerting.sendAlert("Outbox gaps detected", gaps);
        }
    }
}
```

---

### 2. Timer/SLA Service Implementation

**SLA Calculation:**

```java
// backend/src/main/java/io/monosense/synergyflow/sla/api/SlaCalculator.java

package io.monosense.synergyflow.sla.api;

@Service
public class SlaCalculator {

    private final BusinessCalendar businessCalendar;

    /**
     * Calculate SLA due date based on priority and business calendar
     * Accounts for weekends, holidays, business hours
     */
    public Instant calculateDueAt(Ticket ticket, Instant createdAt) {
        int hours = switch (ticket.getPriority()) {
            case CRITICAL -> 2;
            case HIGH -> 4;
            case MEDIUM -> 8;
            case LOW -> 24;
        };

        // Simple calculation: createdAt + hours (ignoring business calendar for MVP)
        Instant simpleDueAt = createdAt.plus(hours, ChronoUnit.HOURS);

        // Advanced (Phase 2): Apply business calendar pauses
        // return businessCalendar.addBusinessHours(createdAt, hours, ticket.getTimezone());

        return simpleDueAt;
    }
}
```

**Timer Scheduler:**

```java
// backend/src/main/java/io/monosense/synergyflow/sla/internal/timer/TimerScheduler.java

package io.monosense.synergyflow.sla.internal.timer;

@Component
@Profile("timer")
class TimerScheduler {

    private final SlaTrackingRepository slaRepository;
    private final EscalationHandler escalationHandler;

    /**
     * Scan for timers due now or in the past
     * O(1) via B-tree index on due_at
     * Fixed delay: scan every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
    public void scanAndFire() {
        Instant now = Instant.now();

        // Query: SELECT * FROM sla_tracking WHERE due_at <= :now AND paused = false
        // Uses idx_sla_due_at index for fast lookup
        List<SlaTracking> dueTimers = slaRepository.findDueTimers(now);

        for (SlaTracking timer : dueTimers) {
            try {
                escalationHandler.escalate(timer);

                // Update timer for next escalation (e.g., +30 minutes)
                timer.setDueAt(now.plus(30, ChronoUnit.MINUTES));
                timer.incrementEscalationCount();
                slaRepository.save(timer);

            } catch (Exception e) {
                log.error("Failed to escalate timer {}", timer.getTicketId(), e);
                // Retry with exponential backoff (Spring Retry)
            }
        }
    }
}
```

**Success Criteria:**
- Timer precision drift <1s (use `System.nanoTime()` for testing)
- Zero missed escalations (alert if `escalation_count` stuck at 0 for >1 hour)
- Timer mutation O(1) with p95 <50ms (UPDATE single row by PK)

---

### 3. SSE Gateway Implementation

**SSE Controller:**

```java
// backend/src/main/java/io/monosense/synergyflow/sse/api/SseController.java

package io.monosense.synergyflow.sse.api;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final SseConnectionManager connectionManager;
    private final RedisStreamSubscriber redisSubscriber;

    /**
     * SSE endpoint with reconnect cursor support
     * Client sends Last-Event-ID header on reconnect
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
        Principal principal
    ) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout
        String userId = principal.getName();

        // Register connection
        connectionManager.register(userId, emitter, lastEventId);

        // Send missed events since lastEventId (catchup)
        if (lastEventId != null) {
            List<DomainEvent> missedEvents = redisSubscriber.readSince(lastEventId);
            missedEvents.forEach(event -> emitter.send(event));
        }

        // Handle disconnection
        emitter.onCompletion(() -> connectionManager.unregister(userId, emitter));
        emitter.onError(e -> connectionManager.unregister(userId, emitter));
        emitter.onTimeout(() -> connectionManager.unregister(userId, emitter));

        return emitter;
    }
}
```

**Redis Stream Subscriber:**

```java
// backend/src/main/java/io/monosense/synergyflow/sse/internal/RedisStreamSubscriber.java

package io.monosense.synergyflow.sse.internal;

@Component
class RedisStreamSubscriber {

    private final RedissonClient redissonClient;
    private final SseConnectionManager connectionManager;

    @PostConstruct
    public void subscribe() {
        RStream<String, DomainEvent> stream = redissonClient.getStream("domain-events");

        stream.createGroup("sse-gateways"); // Consumer group for fault tolerance

        // Read new events continuously
        stream.readGroup("sse-gateways", "gateway-" + InetAddress.getLocalHost().getHostName())
            .forEach(event -> {
                // Fan out to all connected SSE emitters
                connectionManager.broadcast(event);
            });
    }
}
```

**Success Criteria:**
- E2E event propagation ≤2s p95 (measure from `outbox.occurred_at` to client receipt)
- Reconnect catch-up <1s (measure from reconnect to first event sent)
- 250 concurrent SSE connections sustained for 8 hours without memory leak

---

### 4. Batch Authorization Implementation

**Scope-First SQL:**

```java
// backend/src/main/java/io/monosense/synergyflow/itsm/internal/repository/TicketCardRepository.java

package io.monosense.synergyflow.itsm.internal.repository;

@Repository
interface TicketCardRepository extends JpaRepository<TicketCard, UUID> {

    /**
     * Scope-first query: Filter by team/project/role BEFORE per-row checks
     * Assumes user teams cached in session
     */
    @Query("""
        SELECT tc FROM TicketCard tc
        WHERE tc.assigneeId IN (
            SELECT ut.userId FROM UserTeam ut
            WHERE ut.teamId IN (:userTeams)
        )
        OR tc.status = 'UNASSIGNED'
        ORDER BY tc.sla_due_at ASC NULLS LAST, tc.priority DESC
        """)
    List<TicketCard> findQueueForUser(@Param("userTeams") List<UUID> userTeams, Pageable pageable);
}
```

**Batch Authorization Service (already shown above):**

```java
// backend/src/main/java/io/monosense/synergyflow/security/api/BatchAuthService.java
// See "Technical Approach" section for full implementation
```

**Success Criteria:**
- Auth overhead <10% of total request time (e.g., 20ms auth for 200ms queue query)
- Batch-check 100 IDs p95 <3ms
- Hot ACL cache hit rate >80% (measure via Prometheus counter)

---

## Development Setup

### Prerequisites

- **JDK 21** (Temurin or Oracle)
- **Node.js 20+** (LTS)
- **Docker Desktop** (for local PostgreSQL, Redis, Keycloak, OpenSearch)
- **Gradle 8.10+** (wrapper committed)
- **Git**
- **IDE:** IntelliJ IDEA (recommended) or VS Code

### Local Environment (Docker Compose)

```yaml
# infrastructure/docker-compose.yml

version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: synergyflow
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpass
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev"]
      interval: 5s
      timeout: 3s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: dev
      KC_DB_PASSWORD: devpass
    command: start-dev
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

  opensearch:
    image: opensearchproject/opensearch:2.11.0
    environment:
      discovery.type: single-node
      OPENSEARCH_JAVA_OPTS: "-Xms512m -Xmx512m"
      DISABLE_SECURITY_PLUGIN: "true"
    ports:
      - "9200:9200"
    volumes:
      - opensearch-data:/usr/share/opensearch/data

volumes:
  postgres-data:
  redis-data:
  opensearch-data:
```

### Running Locally

**1. Start Infrastructure:**
```bash
cd infrastructure
docker-compose up -d
```

**2. Wait for Services:**
```bash
# Check health
docker-compose ps

# PostgreSQL ready when: healthy
# Redis ready when: healthy
# Keycloak ready when: http://localhost:8080/health returns 200
```

**3. Configure Keycloak (First Time Only):**
```bash
# Login to Keycloak admin console: http://localhost:8080
# Username: admin, Password: admin

# Create realm: synergyflow
# Create client: synergyflow-backend (OIDC, confidential)
# Create roles: ADMIN, ITSM_AGENT, DEVELOPER, MANAGER, EMPLOYEE
# Create users: agent1@example.com (role: ITSM_AGENT), dev1@example.com (role: DEVELOPER)
# Export realm configuration to infrastructure/keycloak-realm.json
```

**4. Run Database Migrations:**
```bash
cd backend
# Flyway runs automatically on application startup (spring.flyway.enabled=true)
# Optional (if Gradle Flyway plugin is added later):
# ./gradlew flywayMigrate
```

**5. Start Backend (HTTP/SSE App Profile):**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=app'
```

**6. Start Event Worker (Worker Profile):**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=worker'
```

**7. Start Timer Service (Timer Profile):**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=timer'
```

**8. Start Search Indexer (Indexer Profile - Optional):**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=indexer'
```

**9. Start Frontend:**
```bash
cd frontend
npm install
npm run dev
```

**10. Access Application:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8081/api
- Backend Health: http://localhost:8081/actuator/health
- Keycloak: http://localhost:8080
- PostgreSQL: localhost:5432 (psql -h localhost -U dev -d synergyflow)
- Redis: localhost:6379 (redis-cli)
- OpenSearch: http://localhost:9200

---

## Implementation Guide

### Week 1: Project Setup & Database Foundation

**Stories (Sprint 1, Week 1-2):**
1. **Story 1.1:** Initialize monorepo structure
   - Create Git repository with backend/, frontend/, infrastructure/, docs/ folders
   - Configure Gradle Kotlin DSL at repo root (backend build)
   - Initialize single Vite app (monolithic frontend) with internal routing
   - Add .gitignore, README.md, LICENSE

2. **Story 1.2:** Set up Spring Boot modulith skeleton
   - Create `SynergyFlowApplication.java` main class
   - Configure `build.gradle.kts` with Spring Boot 3.5.x, Spring Modulith dependencies
   - Create module packages: itsm/, pm/, workflow/, security/, eventing/, sla/, sse/, audit/
   - Add `package-info.java` for each module with `@ApplicationModule` annotations
   - Create `spi/` packages with `@NamedInterface` annotations for cross-module APIs
   - Add `ModularityTests.java` to verify module boundaries at build time
   - Add ArchUnit test to enforce `internal/` package access rules

3. **Story 1.3:** Configure PostgreSQL with Flyway
   - Ensure Flyway dependencies are present (migrations run on startup)
   - Create migration V1__create_itsm_tables.sql (tickets, incidents, service_requests, ticket_comments, routing_rules)
   - Create migration V2__create_pm_tables.sql (issues, sprints, boards, board_columns, issue_links)
   - Create migration V3__create_shared_tables.sql (users, roles, teams, approvals, workflow_states, audit_log, outbox)
   - Create migration V4__create_read_models.sql (ticket_card, sla_tracking, related_incidents, queue_row, issue_card, sprint_summary)
   - Run migrations locally against Docker Compose PostgreSQL
   - Add integration test with Testcontainers to validate schema

4. **Story 1.4:** Integrate Keycloak OIDC authentication
   - Add `spring-boot-starter-oauth2-resource-server` dependency
   - Configure `SecurityConfig.java` with JWT validation
   - Create `JwtValidator.java` to decode and validate Keycloak tokens
   - Create `RoleMapper.java` to map Keycloak roles to application roles
   - Add integration test: authenticate with Keycloak, validate JWT, check roles
   - Configure RBAC: Admin, ITSM_Agent, Developer, Manager, Employee

**Acceptance Criteria:**
- ✅ Monorepo structure matches source tree diagram
- ✅ `./gradlew build` succeeds (backend builds)
- ✅ `npm install` succeeds (frontend builds)
- ✅ All packages start with `io.monosense.synergyflow`
- ✅ `package-info.java` created for each module with `@ApplicationModule` annotations
- ✅ `ModularityTests.java` passes (verifies module boundaries)
- ✅ ArchUnit test enforces `internal/` package access rules
- ✅ Flyway migrations create all tables in PostgreSQL
- ✅ Authenticated `/actuator/health` endpoint returns 200 OK with JWT token
- ✅ Integration test validates Keycloak JWT and RBAC roles

---

### Week 2: Transactional Outbox + Event Worker

**Stories (Sprint 1, Week 1-2 continued):**

5. **Story 1.5:** Implement transactional outbox pattern
   - Create `OutboxEntity.java` JPA entity
   - Create `OutboxRepository.java` with `findUnprocessedBatch(limit)`
   - Create `OutboxWriter.java` service to publish events
   - Create domain events: `TicketCreated`, `TicketAssigned`, `IssueCreated`, `IssueStateChanged`
   - Add integration test: create ticket → verify outbox entry inserted in same transaction

6. **Story 1.6:** Implement Event Worker (outbox poller)
   - Create `OutboxPoller.java` with `@Scheduled(fixedDelay = 100)`
   - Poll outbox, publish to Redis Stream, mark processed
   - Create `RedisStreamPublisher.java` to publish events
   - Create `ReadModelUpdater.java` stub (will implement in Week 3)
   - Add integration test: insert outbox entry → Event Worker polls → Redis Stream receives event

7. **Story 1.7:** Implement SSE gateway
   - Create `SseController.java` with `/api/sse/events` endpoint
   - Create `SseConnectionManager.java` to manage emitters
   - Create `RedisStreamSubscriber.java` to fan out to SSE emitters
   - Add reconnect cursor support (Last-Event-ID header)
   - Add integration test: connect SSE client → publish event to Redis → verify client receives event within 2s

**Acceptance Criteria:**
- ✅ Ticket creation writes to tickets table + outbox table in single transaction
- ✅ Event Worker polls outbox every 100ms, publishes to Redis Stream
- ✅ SSE client receives events within ≤2s p95
- ✅ SSE reconnect with Last-Event-ID resends missed events <1s

---

### Week 3: CQRS-Lite Read Models + Batch Auth

**Stories (Sprint 2, Week 3-4):**

8. **Story 1.8:** Implement read model updater (ticket_card)
   - Create `TicketCardProjection.java` JPA entity
   - Implement `ReadModelUpdater.update(TicketCreated)` → insert ticket_card
   - Implement `ReadModelUpdater.update(TicketAssigned)` → update ticket_card.assignee_name
   - Add integration test: create ticket → Event Worker updates ticket_card → query ticket_card <200ms

9. **Story 1.9:** Implement read model updater (queue_row, issue_card)
   - Create `QueueRowProjection.java`, `IssueCardProjection.java` JPA entities
   - Implement `ReadModelUpdater.update(IssueCreated)` → insert queue_row, issue_card
   - Implement `ReadModelUpdater.update(IssueStateChanged)` → update issue_card.status
   - Add integration test: create issue → Event Worker updates queue_row, issue_card

10. **Story 1.10:** Implement batch authorization service
    - Create `BatchAuthService.java` with `checkBatch(userId, resourceType, resourceIds, action)`
    - Integrate with Keycloak Authorization Services API
    - Implement hot ACL cache (Redis, per-team capability bitsets)
    - Add integration test: checkBatch(100 IDs) completes p95 <3ms

11. **Story 1.11:** Implement scope-first SQL queries
    - Create `TicketCardRepository.findQueueForUser(userTeams, pageable)`
    - Create `QueueRowRepository.findBacklogForUser(userProjects, pageable)`
    - Add batch authorization filter after scope-first SQL
    - Add integration test: query queue (50 tickets) completes p95 <200ms with auth overhead <10%

**Acceptance Criteria:**
- ✅ Read models (ticket_card, queue_row, issue_card) updated by Event Worker
- ✅ Queue queries complete p95 <200ms with batch auth
- ✅ Batch auth overhead <10% of total request time

---

### Week 4: Timer/SLA Service + CI/CD Pipeline

**Stories (Sprint 2, Week 3-4 continued):**

12. **Story 1.12:** Implement Timer/SLA Service
    - Create `TimerScheduler.java` with `@Scheduled(fixedDelay = 5000)`
    - Create `SlaCalculator.java` to calculate due dates (Priority-based: Critical 2h, High 4h, Medium 8h, Low 24h)
    - Create `EscalationHandler.java` with idempotent escalation logic
    - Create `sla_tracking` table with index on `due_at`
    - Add integration test: create ticket → SLA timer created → escalation fires at due_at

13. **Story 1.13:** Spike: OpenSearch vs PostgreSQL GIN index for search
    - Implement PostgreSQL GIN index + `ILIKE` search for tickets/issues
    - Implement OpenSearch indexer stub (consume events, index documents)
    - Benchmark both: 10K tickets, search query "network failure"
    - Decision: If PostgreSQL search p95 <300ms → defer OpenSearch to Phase 2; else implement OpenSearch

14. **Story 1.14:** Set up CI/CD pipeline (Jenkins or GitHub Actions)
    - Create Jenkinsfile or .github/workflows/ci.yml
    - Build stage: `./gradlew build` (backend), `npm run build` (frontend)
    - Test stage: `./gradlew test integrationTest` (JUnit + Testcontainers)
    - Security stage: SAST (Qodana), dependency scanning (OWASP Dependency-Check + Trivy)
    - Performance gate: Gatling smoke test (50 users, p95 <400ms) - stub for now, implement in Epic 5
    - Deploy stage: Build Docker images, push to registry, deploy to staging (kubectl apply)

15. **Story 1.15:** Create Kubernetes deployment manifests (Helm)
    - Create Helm chart in `infrastructure/helm/`
    - Create templates: app-deployment.yaml (5 replicas), worker-deployment.yaml (2 replicas), timer-deployment.yaml (2 replicas), indexer-deployment.yaml (1 replica)
    - Create service.yaml (ClusterIP for app), httproute.yaml (Envoy Gateway)
    - Create configmap.yaml (application config), secrets.yaml (Keycloak credentials)
    - Deploy to development namespace: `helm install synergyflow ./helm -n synergyflow-dev`
    - Verify pods running, health probes passing

**Acceptance Criteria:**
- ✅ Timer/SLA Service creates timers, escalates at due_at with precision drift <1s
- ✅ Search spike decision made (PostgreSQL GIN or OpenSearch)
- ✅ CI/CD pipeline executes: build → unit tests → SAST → deploy to staging
- ✅ Kubernetes pods deployed via Envoy Gateway with health probes passing
- ✅ Local Docker Compose dev environment matches production stack

**Epic 1 Demo Milestone:**
"Health check deployed to Kubernetes, accessible via Envoy Gateway (`https://synergyflow-dev.company.com/actuator/health`), CI/CD pipeline green, Event Worker processing events, Timer Service firing escalations, read models updating in <2s, batch auth completing in <3ms"

---

## Testing Approach

### Unit Tests (JUnit 5 + Mockito)

**Target Coverage:** >80% for domain logic (ITSM, PM, Workflow modules)

**Examples:**
- `TicketServiceTest.java`: Test ticket creation, assignment, state transitions
- `SlaCalculatorTest.java`: Test SLA due date calculation for all priorities
- `RuleEvaluatorTest.java`: Test routing rule matching logic
- `BatchAuthServiceTest.java`: Test batch authorization with mocked Keycloak

**Run:**
```bash
./gradlew test
```

### Integration Tests (Spring Boot Test + Testcontainers)

**Scope:** Test database interactions, outbox pattern, Event Worker, SSE gateway, batch auth

**Examples:**
- `OutboxIntegrationTest.java`: Create ticket → verify outbox entry + domain entity in single transaction
- `EventWorkerIntegrationTest.java`: Insert outbox entry → Event Worker polls → Redis Stream receives event
- `SseGatewayIntegrationTest.java`: Connect SSE client → publish event → verify client receives within 2s
- `ReadModelIntegrationTest.java`: Create ticket → Event Worker updates ticket_card → query <200ms

**Testcontainers Configuration:**
```java
@SpringBootTest
@Testcontainers
class OutboxIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void testOutboxAtomicity() {
        // Test implementation
    }
}
```

**Run:**
```bash
./gradlew integrationTest
```

### Contract Tests (Spring Cloud Contract - Future Epic 2-5)

**Scope:** Validate API contracts between frontend and backend

**Examples:**
- `TicketApiContractTest.java`: POST /api/itsm/tickets → 201 Created
- `BoardApiContractTest.java`: GET /api/pm/boards/{id}/cards → 200 OK with issue cards

**Deferred to:** Epic 2-5 (when REST APIs implemented)

### E2E Tests (Playwright - Future Epic 2-5)

**Scope:** End-to-end user workflows

**Examples:**
- `AgentConsoleE2E.spec.ts`: Agent creates incident → assigns to team → resolves → MTTR metric updates
- `KanbanBoardE2E.spec.ts`: Developer drags issue from To Do → In Progress → Done → sprint velocity updates

**Deferred to:** Epic 5 (after UI implemented)

### Performance Tests (Gatling - Future Epic 5)

**Scope:** Load testing with 250 concurrent users

**Scenario:**
- 60% agent console operations (queue load, ticket create, ticket assign)
- 30% board interactions (board load, drag-drop, sprint planning)
- 10% approvals (approval queue load, approve/reject)

**Success Criteria:**
- p95 <400ms for all CRUD operations
- p95 <200ms for queue/list operations
- SSE event propagation ≤2s p95

**Deferred to:** Epic 5 (performance gate in CI/CD)

### Accessibility Tests (axe-core + Lighthouse - Future Epic 2-5)

**Scope:** Automated accessibility scanning + manual screen reader testing

**Tools:**
- axe-core (automated WCAG AA scanning)
- Lighthouse CI (accessibility audit in CI/CD)
- Manual testing: NVDA, JAWS, VoiceOver

**Deferred to:** Epic 2-5 (before Epic completion)

---

## Deployment Strategy

### Deployment Pattern: Blue/Green for App, Rolling for Companions

**HTTP/SSE App (Spring profile: app):**
- **Strategy:** Blue/Green deployment
- **Rationale:** Zero-downtime for user-facing traffic
- **Process:**
  1. Deploy new version (green) alongside old version (blue)
  2. Run smoke tests against green
  3. Switch Envoy Gateway HTTPRoute to green
  4. Monitor metrics for 15 minutes
  5. If healthy, terminate blue; else rollback to blue

**Event Worker, Timer Service, Indexer (Companions):**
- **Strategy:** Rolling updates
- **Rationale:** Background workers tolerate brief disruption
- **Process:**
  1. Update Deployment with new image
  2. Kubernetes rolling update (maxUnavailable: 1, maxSurge: 1)
  3. Pods restart gracefully (drain connections, finish processing)

### Kubernetes Deployment (Helm)

**Helm Chart Structure:**
```
infrastructure/helm/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── app-deployment.yaml        # HTTP/SSE App (5 replicas)
    ├── worker-deployment.yaml     # Event Worker (2 replicas)
    ├── timer-deployment.yaml      # Timer/SLA Service (2 replicas)
    ├── indexer-deployment.yaml    # Search Indexer (1 replica)
    ├── service.yaml               # ClusterIP services
    ├── httproute.yaml             # Envoy Gateway HTTPRoute
    ├── configmap.yaml             # Application config
    └── secrets.yaml               # Keycloak credentials
```

**Deployment Commands:**
```bash
# Install (first time)
helm install synergyflow ./helm -n synergyflow-prod

# Upgrade (blue/green for app, rolling for companions)
helm upgrade synergyflow ./helm -n synergyflow-prod --set app.image.tag=v1.2.3

# Rollback (if deployment fails)
helm rollback synergyflow -n synergyflow-prod
```

### Health Probes

**Liveness Probe:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 3
```

**Readiness Probe:**
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 3
```

### Rollback Procedures

**Immediate Rollback Triggers:**
- Error rate >1% (4xx or 5xx responses)
- API latency p95 >400ms sustained for >5 minutes
- Database connection pool exhaustion
- Memory leak (heap usage >90% sustained)

**Rollback Command:**
```bash
# Helm rollback to previous revision
helm rollback synergyflow -n synergyflow-prod

# Or manual rollback
kubectl set image deployment/synergyflow-app synergyflow-app=synergyflow:v1.2.2 -n synergyflow-prod
```

---

## Success Criteria (Epic 1 Acceptance)

### Functional Criteria

✅ **Authenticated health check endpoint:**
- `GET /actuator/health` with JWT token returns 200 OK
- Response includes: `{"status": "UP", "components": {"db": {"status": "UP"}, "redis": {"status": "UP"}}}`

✅ **PostgreSQL schema created:**
- All Flyway migrations executed (V1 through V5)
- Tables created: tickets, incidents, service_requests, issues, sprints, boards, users, roles, approvals, outbox, ticket_card, sla_tracking, queue_row, issue_card
- Indexes created: idx_tickets_status, idx_issues_status, idx_sla_due_at, idx_outbox_unprocessed

✅ **CI/CD pipeline executes:**
- Build stage passes (`./gradlew build`)
- Unit test stage passes (>80% coverage)
- Integration test stage passes (Testcontainers)
- SAST stage passes (Qodana - zero critical/high vulnerabilities)
- Dependency scan stage passes (OWASP + Trivy - zero critical/high CVEs)
- Deploy stage succeeds (Helm install to staging)

✅ **Kubernetes pod deployed:**
- App pod running (5 replicas), Worker pod running (2 replicas), Timer pod running (2 replicas)
- Health probes passing (liveness + readiness)
- Envoy Gateway HTTPRoute routes traffic to app service
- Accessible via `https://synergyflow-dev.company.com/actuator/health`

✅ **Local Docker Compose dev environment:**
- `docker-compose up` starts PostgreSQL, Redis, Keycloak, OpenSearch
- Backend connects to all services
- Flyway migrations run successfully
- Frontend connects to backend API

### Non-Functional Criteria

✅ **Outbox → SSE event propagation ≤2s p95:**
- Measure: Create ticket → outbox insert → Event Worker polls → Redis Stream publish → SSE client receives
- Metric: `event_propagation_latency_seconds_p95 < 2.0`

✅ **Timer precision drift <1s:**
- Measure: Create SLA timer with `due_at = now() + 2 hours` → escalation fires at `due_at ± 1s`
- Metric: `timer_drift_seconds_abs < 1.0`

✅ **Zero missed escalations:**
- Measure: Create 100 SLA timers → all escalate exactly once
- Metric: `missed_escalations_count = 0`

✅ **Batch auth p95 <3ms for 100 IDs:**
- Measure: Call `BatchAuthService.checkBatch(100 ticket IDs)` → response time
- Metric: `batch_auth_latency_ms_p95 < 3.0`

✅ **Read models update <200ms p95:**
- Measure: Create ticket → Event Worker updates ticket_card → query ticket_card
- Metric: `read_model_update_latency_ms_p95 < 200`

---

## Risks and Mitigations

### Risk 1: Timer/SLA Service Precision <1s Challenging

**Impact:** High (NFR18 non-negotiable)
**Probability:** Medium

**Mitigation:**
- **Spike in Week 3:** Prove timer precision with integration test (`@Scheduled` vs Quartz vs custom scheduler)
- **Benchmark:** Create 1,000 timers, measure drift using `System.nanoTime()`
- **Fallback:** If Spring `@Scheduled` drift >1s, switch to Quartz Scheduler with misfire handling
- **Acceptance:** Timer drift must be <1s for Epic 1 completion

### Risk 2: OpenSearch Integration Complexity

**Impact:** Medium (can defer to Phase 2)
**Probability:** Medium

**Mitigation:**
- **Decision Point in Week 3:** Spike PostgreSQL GIN index + `ILIKE` search
- **Benchmark:** 10K tickets, search query "network failure", measure p95 latency
- **Fallback:** If PostgreSQL search p95 <300ms → defer OpenSearch to Phase 2
- **Acceptance:** Search p95 <300ms (either PostgreSQL or OpenSearch)

### Risk 3: SSE Connection Storms on Reconnect

**Impact:** High (could overload Redis)
**Probability:** Low (250 concurrent users, but spiky on deploy/network partition)

**Mitigation:**
- **Throttling:** Implement exponential backoff on client reconnect (1s, 2s, 4s, max 8s)
- **Load test in Epic 5:** Simulate 250 concurrent SSE clients, disconnect all, reconnect simultaneously
- **Monitor:** Redis CPU/memory during reconnect storm
- **Fallback:** If Redis overloads, add Redis Cluster or reduce reconnect catchup window (from 24h to 1h)

### Risk 4: Flyway Migration Failures in Production

**Impact:** High (blocks deployment)
**Probability:** Low (tested in staging)

**Mitigation:**
- **Pre-deployment validation:** Run Flyway migrate against staging database matching production version
- **Dry-run mode:** Flyway `-validateOnMigrate=true` to catch conflicts before applying
- **Rollback script:** For each migration V{n}, create corresponding rollback U{n} (executed manually if needed)
- **Backup:** PostgreSQL continuous WAL archiving (RPO 15 minutes)

### Risk 5: Keycloak Integration Delays

**Impact:** Medium (blocks authentication)
**Probability:** Low (existing on-prem Keycloak)

**Mitigation:**
- **Early integration in Week 1:** Create Keycloak realm, client, roles, test users
- **Mock Keycloak for local dev:** WireMock stub for JWT validation (bypass real Keycloak)
- **Fallback:** If Keycloak unavailable, use in-memory authentication for development (switch to Keycloak for staging/prod)

---

## Next Steps (Post Epic 1)

### Epic 2-ITSM: Core ITSM Module (Weeks 3-10)
- Implement Ticket CRUD APIs (TicketController, TicketService)
- Implement Agent Console UI (Zero-Hunt Console with Loft Layout)
- Implement SLA calculation and routing rules
- Implement metrics dashboard (MTTR, FRT, SLA compliance)

### Epic 3-PM: Core PM Module (Weeks 3-10)
- Implement Issue CRUD APIs (IssueController, IssueService)
- Implement Kanban Board UI (Developer Copilot Board with drag-drop)
- Implement Sprint planning and burndown calculation
- Implement metrics dashboard (velocity, burndown, cycle time)

### Epic 4: Simple Workflow Engine (Weeks 9-14)
- Implement Approval workflows (ApprovalEngine, ApprovalController)
- Implement Routing automation (RuleEvaluator, RoutingRuleController)
- Implement State machine enforcement (StateMachine, WorkflowStateController)
- Implement Workflow configuration UI (admin interface)

### Epic 5: Unified Dashboard & Real-Time Updates (Weeks 13-18)
- Implement Unified Dashboard UI (cross-module overview)
- Implement Real-time activity feed
- Load test: 250 concurrent SSE connections sustained for 8 hours
- Performance gate: Gatling scenarios (60% agent, 30% board, 10% approvals)

---

**Epic 1 Tech Spec Complete**

**Status:** Ready for Development Kickoff (Week 1)
**Next Action:** Team formation (2 BE + 1 Infra + 1 QA) + Sprint 1 planning (Stories 1.1 - 1.7)
**Dependencies:** None (Epic 1 is foundation for all subsequent epics)
