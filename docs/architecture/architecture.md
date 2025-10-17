# SynergyFlow System Architecture

**Version:** 1.0
**Date:** 2025-10-17
**Author:** Winston (Architect)
**Status:** Draft for Technical Review

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [System Context (C4 Level 1)](#2-system-context-c4-level-1)
3. [Container Architecture (C4 Level 2)](#3-container-architecture-c4-level-2)
4. [Component Architecture (C4 Level 3)](#4-component-architecture-c4-level-3)
5. [Event-Driven Architecture](#5-event-driven-architecture)
6. [Database Architecture](#6-database-architecture)
7. [API Specifications](#7-api-specifications)
8. [Event Schemas](#8-event-schemas)
9. [Security Architecture](#9-security-architecture)
10. [Deployment Architecture](#10-deployment-architecture)
11. [Workflow Orchestration](#11-workflow-orchestration)
12. [Integration Patterns](#12-integration-patterns)
13. [Scalability and Performance](#13-scalability-and-performance)
14. [Disaster Recovery](#14-disaster-recovery)
15. [Observability](#15-observability)
16. [Architectural Decisions](#16-architectural-decisions)
17. [Resource Sizing](#17-resource-sizing)
18. [Testing Strategy](#18-testing-strategy)

---

## 1. Introduction

### 1.1 Purpose

This document defines the comprehensive system architecture for **SynergyFlow**, a unified ITSM+PM platform with intelligent workflow automation. It serves as the technical blueprint for the 10-week MVP implementation and provides guidance for future scalability to 1,000+ users.

### 1.2 Architectural Principles

**1. Event-Driven Modular Monolith**
- All cross-module communication via Spring Modulith ApplicationEvents
- No direct database access or method calls between modules (except Foundation)
- Transactional outbox pattern ensures reliable event delivery
- Events stored in database, delivered asynchronously with automatic retry

**2. Spring Modulith Architecture**
- Single deployable artifact with enforced module boundaries
- Built-in event publication registry for durable messaging
- Operational simplicity for 3-person team (zero message broker infrastructure)
- Natural migration path to microservices via event externalization

**3. CQRS with Eventual Consistency**
- Write side: Commands update aggregate roots in PostgreSQL
- Read side: Event consumers build query-optimized projections
- Freshness badges make projection lag visible to users (<100ms typical lag)
- Event publication table provides transactional guarantees

**4. Policy-Driven Automation**
- Open Policy Agent (OPA) for authorization and workflow decisions
- Every decision generates explainable receipt
- Shadow mode testing before production rollout

**5. Boring Technology for Stability**
- Spring Boot 3.5.6, Spring Modulith 1.4.2, PostgreSQL 16.8
- Proven at scale, mature ecosystems, strong community support
- Innovation reserved for genuine competitive advantages
- Simplicity over premature optimization (YAGNI principle)

### 1.3 Key Requirements

**Performance Targets:**
- API response time: p95 <200ms, p99 <500ms
- Event processing lag: <100ms end-to-end (p95 <200ms, in-memory event bus)
- Policy evaluation: <100ms p95 (target <10ms)
- System availability: ≥99.5% uptime

**Scale Targets:**
- MVP: 250 users, 50-75 concurrent
- Validated: 1,000 users, 250 concurrent
- Concurrent workflows: 1,000-5,000 active SLA timers

**Operational:**
- Zero workflow state loss
- Zero data loss or corruption
- Zero schema breaking changes
- Graceful degradation under load

### 1.4 Technology and Library Decision Table

This table documents all technology selections with specific versions and rationale aligned with PRD requirements.

| Category | Technology | Version | Rationale | PRD Alignment |
|----------|-----------|---------|-----------|---------------|
| **Core Stack** |
| Runtime | Java (OpenJDK) | 21.0.2 LTS | LTS support until 2029, virtual threads for high concurrency, modern language features | NFR-1 (Performance), NFR-3 (Scalability) |
| Framework | Spring Boot | 3.5.6 | Latest production release, virtual threads support, CDS optimization, improved startup performance | NFR-2 (Reliability), NFR-9 (Maintainability) |
| Architecture | Spring Modulith | 1.4.2 | Latest GA release (July 2025), enforced module boundaries, event lifecycle improvements, enhanced testing, observability integration | ADR-001 (Event-Only Communication), NFR-7 (Operational Simplicity) |
| Database | PostgreSQL | 16.8 | ACID compliance, JSONB support, full-text search, mature replication, proven at scale | NFR-2 (Reliability), NFR-4 (Data Integrity) |
| Cache | DragonflyDB | 1.17.0 | Redis-compatible, 25x faster than Redis, 80% lower memory footprint, native multi-threading | ADR-011 (Cache Store), NFR-1 (Performance) |
| Workflow | Flowable | 7.1.0 | BPMN 2.0 engine, SLA timer support, audit trail, Spring Boot integration, visual designer | FR-2 (SLA Tracking), FR-6 (Workflow Automation) |
| Policy Engine | Open Policy Agent | 0.68.0 | Declarative policy language (Rego), shadow mode testing, explainable decisions, 100+ connectors | FR-23 (Role-Based Access), NFR-8 (Security) |
| Database Migration | Flyway | 10.18.0 | Version-controlled schema evolution, repeatable migrations, rollback support | ADR-009 (Schema Evolution), NFR-9 (Maintainability) |
| Data Access | MyBatis-Plus | 3.5.14 | Enhanced MyBatis toolkit with lambda queries, automatic CRUD, type-safe DSL, native Spring Boot 3.x support | NFR-1 (Performance), NFR-9 (Maintainability) |
| MyBatis-Plus Starter | mybatis-plus-spring-boot3-starter | 3.5.14 | Spring Boot 3.x auto-configuration, simplified integration, automatic mapper scanning | NFR-9 (Maintainability) |
| MyBatis-Plus JSqlParser | mybatis-plus-jsqlparser | 5.0 | Required for pagination, tenant, dynamic table name, and SQL interception plugins (v3.5.9+) | NFR-1 (Performance), NFR-8 (Security) |
| **API and Documentation** |
| API Documentation | SpringDoc OpenAPI | 2.3.0 | Automatic API docs from Spring controllers, OpenAPI 3.0 spec generation, Spring Boot 3.5.6 native | FR-33 (API Integration), NFR-9 (Maintainability) |
| API UI | Swagger UI | 2.3.0 (bundled) | Interactive API testing, OAuth2 integration, real-time schema validation | FR-33 (API Integration) |
| Validation | Jakarta Bean Validation | 3.0.2 | Declarative validation annotations, i18n support, custom validators | NFR-4 (Data Integrity) |
| **Frontend Stack** |
| Runtime | Node.js | 20.12.0 LTS | LTS support until April 2026, native ESM, performance improvements | NFR-1 (Performance) |
| Framework | Next.js | 14.2.5 | React Server Components, App Router, TypeScript first, API routes, built-in optimization | FR-12 (Self-Service Portal), NFR-1 (Performance) |
| UI Library | React | 18.3.1 | Virtual DOM, hooks, concurrent rendering, strong ecosystem | NFR-9 (Maintainability) |
| State Management | TanStack Query | 5.32.0 | Server state synchronization, automatic caching, optimistic updates, offline support | FR-12 (Self-Service Portal), NFR-1 (Performance) |
| Styling | Tailwind CSS | 3.4.3 | Utility-first CSS, tree-shaking, design system consistency, fast development | NFR-9 (Maintainability) |
| Forms | React Hook Form | 7.51.3 | Performant validation, TypeScript support, Zod integration | NFR-1 (Performance) |
| Schema Validation | Zod | 3.23.6 | TypeScript-first schema validation, type inference, composable schemas | NFR-4 (Data Integrity) |
| HTTP Client | Axios | 1.6.8 | Interceptors for auth, request/response transformations, timeout handling | FR-33 (API Integration) |
| **DevOps and Infrastructure** |
| Orchestrator | Kubernetes | 1.30.0 | Industry standard, declarative config, auto-scaling, self-healing | NFR-3 (Scalability), NFR-2 (Reliability) |
| GitOps | Flux CD | 2.2.3 | Git-based deployments, automated reconciliation, drift detection, Helm support | NFR-9 (Maintainability), NFR-2 (Reliability) |
| Gateway | Envoy Gateway | 1.0.1 | Kubernetes Gateway API native, JWT validation, rate limiting, observability | ADR-010 (Gateway Adoption), NFR-8 (Security) |
| PostgreSQL Operator | CloudNative-PG | 1.23.0 | Kubernetes-native, automated backups, replication, rolling updates | ADR-003 (HA Posture), NFR-2 (Reliability) |
| Certificate Manager | cert-manager | 1.15.0 | Automated TLS certificate provisioning and renewal, Let's Encrypt integration | NFR-8 (Security) |
| **Observability** |
| Metrics | Victoria Metrics | 1.100.0 | Prometheus-compatible, 10x storage compression, fast queries, low resource usage | NFR-6 (Monitoring), NFR-3 (Cost Efficiency) |
| Visualization | Grafana | 10.4.2 | Multi-source dashboards, alerting, annotations, RBAC | NFR-6 (Monitoring) |
| Tracing | OpenTelemetry | 1.28.0 | Vendor-neutral standard, automatic instrumentation, distributed tracing | NFR-6 (Monitoring), NFR-1 (Performance Debugging) |
| Logging | Pino | 8.21.0 | JSON structured logging, 5x faster than Winston, low overhead | NFR-6 (Monitoring), NFR-1 (Performance) |
| **Security** |
| Authentication | OAuth2/OIDC | 2.1 spec | Industry standard, JWT tokens, refresh tokens, PKCE flow | FR-11 (SSO), NFR-8 (Security) |
| JWT Library | Spring Security OAuth2 Resource Server | 6.2.0 (Spring Boot 3.5.6) | RS256 signature validation, automatic claims extraction, scope enforcement | NFR-8 (Security) |
| Secrets Management | Kubernetes Secrets | Native | Encrypted at rest, RBAC-controlled, external secrets operator integration | NFR-8 (Security) |
| **Testing** |
| Unit Testing (Backend) | JUnit 5 | 5.10.2 | Modern assertions, parameterized tests, nested tests, lifecycle hooks | NFR-5 (Test Coverage ≥80%) |
| Integration Testing | Spring Modulith Test | 1.4.2 | Module isolation, enhanced event verification, improved scenario testing, test fixtures | NFR-5 (Test Coverage) |
| Mocking | Mockito | 5.11.0 | Behavior verification, argument matchers, spy support | NFR-5 (Test Coverage) |
| Contract Testing | Spring Cloud Contract | 4.1.0 | Producer-driven contracts, stub generation, API evolution safety | ADR-002 (Event Contract Versioning) |
| Unit Testing (Frontend) | Vitest | 1.5.0 | Fast execution, ESM native, TypeScript support, Jest-compatible API | NFR-5 (Test Coverage ≥80%) |
| E2E Testing | Playwright | 1.43.0 | Cross-browser, auto-wait, trace viewer, mobile emulation | NFR-5 (Test Coverage) |
| **Build and Packaging** |
| Backend Build | Gradle | 8.7.0 | Fast incremental builds, dependency management, multi-project support | NFR-9 (Maintainability) |
| Frontend Build | Next.js Compiler (SWC) | 14.2.5 (bundled) | 17x faster than Babel, tree-shaking, code splitting, minification | NFR-1 (Performance) |
| Container Runtime | Podman / Docker | 5.0+ / 26.0+ | OCI-compatible, rootless mode, BuildKit support | NFR-8 (Security) |

**Version Management Policy:**
- **LTS versions preferred**: Java 21 LTS (until 2029), Node.js 20 LTS (until 2026)
- **Major version stability**: Stay on current major versions for 12+ months before upgrading
- **Security patches**: Applied within 7 days of release for critical CVEs
- **Dependency updates**: Monthly review cycle, automated via Renovate Bot
- **Breaking changes**: Requires ADR documentation and gradual migration strategy

---

## 2. System Context (C4 Level 1)

### 2.1 Overview

SynergyFlow is a unified platform that combines ITSM (Incident, Problem, Change, Knowledge Management) and PM (Project, Task, Sprint Management) capabilities with intelligent workflow automation.

### 2.2 System Context Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          System Context                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐                                                   │
│  │ IT Agents    │                                                   │
│  │ (Primary)    │──────────────┐                                    │
│  └──────────────┘              │                                    │
│                                │                                    │
│  ┌──────────────┐              │                                    │
│  │ Project      │              │                                    │
│  │ Managers     │──────────────┤                                    │
│  │ (Secondary)  │              │                                    │
│  └──────────────┘              │                                    │
│                                 ▼                                   │
│  ┌──────────────┐      ┌────────────────┐                           │
│  │ End Users    │      │  SynergyFlow   │                           │
│  │ (Self-       │─────▶│   Platform     │                           │
│  │  Service)    │      │                │                           │
│  └──────────────┘      │ • ITSM Module  │                           │
│                        │ • PM Module    │                           │
│  ┌──────────────┐      │ • Time Track   │      ┌───────────────┐    │
│  │ External     │      │ • Workflows    │◀────▶│ Identity      │    │
│  │ Monitoring   │◀────▶│ • Policies     │      │ Provider      │    │
│  │ (DataDog)    │      └────────────────┘      │ (OAuth2/OIDC) │    │
│  └──────────────┘              │               └───────────────┘    │
│                                │                                    │
│  ┌──────────────┐              │                ┌───────────────┐   │
│  │ Communication│              │                │ External CMDB │   │
│  │ Platforms    │◀─────────────┼───────────────▶│ (Optional)    │   │
│  │ (Slack/Teams)│              │                └───────────────┘   │
│  └──────────────┘              │                                    │
│                                │                ┌───────────────┐   │
│                                └───────────────▶│ CI/CD         │   │
│                                                 │ Pipelines     │   │
│                                                 └───────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 External Actors

**Primary Users:**
- **IT Service Desk Agents**: Create/manage incidents, changes, knowledge articles
- **Project Managers**: Manage projects, tasks, sprints, backlogs
- **End Users**: Submit requests, search knowledge base, track ticket status

**External Systems:**
- **Identity Provider**: Keycloak, Auth0, Okta for OAuth2/OIDC authentication
- **Monitoring Tools**: DataDog, New Relic, Prometheus for metrics collection
- **Communication Platforms**: Slack, Microsoft Teams for notifications
- **External CMDB**: ServiceNow CMDB, Device42 for CI discovery (optional)
- **CI/CD Pipelines**: GitHub Actions, GitLab CI for deployment webhooks

### 2.4 Key Interactions

1. **User Authentication**: Users authenticate via external IdP (OAuth2), SynergyFlow validates JWT tokens
2. **Incident Management**: Agents create incidents, system tracks SLAs, publishes events
3. **Cross-Module Workflows**: Link-on-action creates related entities (incident → change, incident → task)
4. **Time Tracking**: Single-entry time tray mirrors time logs to incidents and tasks
5. **Policy Automation**: OPA evaluates policies, generates decision receipts for all automated actions
6. **External Notifications**: System publishes events to Slack/Teams for alerts
7. **Monitoring Integration**: Prometheus-compatible metrics exported for external monitoring tools

---

## 3. Container Architecture (C4 Level 2)

### 3.1 Overview

SynergyFlow uses a modular monolith architecture with event-driven integration between modules. All containers deployed on Kubernetes with high availability.

### 3.2 Container Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Container Architecture                            │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌─────────────────┐                                                      │
│  │   Web Browser   │                                                      │
│  └────────┬────────┘                                                      │
│           │ HTTPS                                                         │
│           ▼                                                               │
│  ┌──────────────────────────────────────────────────────────────────┐     │
│  │              Envoy Gateway (API Gateway)                         │     │
│  │  • JWT validation (RS256)                                        │     │
│  │  • Rate limiting (100 req/min per user)                          │     │
│  │  • CORS configuration                                            │     │
│  │  • Routing: /api/* → Backend, / → Frontend                       │     │
│  └────────┬───────────────────────────────┬─────────────────────────┘     │
│           │ HTTP                          │ HTTP                          │
│           ▼                               ▼                               │
│  ┌────────────────────┐         ┌──────────────────────────┐              │
│  │ Frontend App       │         │  Backend Application     │              │
│  │ (Next.js 14.2.5)      │         │  (Spring Boot 3.5.6)       │              │
│  │                    │         │                          │              │
│  │ • React 18.3.1        │         │  Modules:                │              │
│  │ • TypeScript       │         │  • Incident              │◀───┐         │
│  │ • Tailwind CSS     │         │  • Change                │    │         │
│  │ • TanStack Query   │         │  • Knowledge             │    │         │
│  └────────────────────┘         │  • Task                  │    │         │
│                                 │  • Time Tracking         │    │         │
│                                 │  • User Management       │    │         │
│                                 │                          │    │         │
│                                 │  ┌─────────────────┐     │    │         │
│                                 │  │ OPA Sidecar     │     │    │         │
│                                 │  │ (localhost:8181)│ ◀───┼────┘         │
│                                 │  │ • Policy eval   │     │              │
│                                 │  │ • <10ms latency │     │              │
│                                 │  └─────────────────┘     │              │
│                                 └──────┬───────────────────┘              │
│                                        │                                  │
│                                        │ JDBC                             │
│                                        ▼                                  │
│  ┌──────────────────────────────────────────────────────────────┐         │
│  │         Database Access (Shared Cluster Pattern)             │         │
│  │  ┌────────────────────────────────────────────────────┐     │         │
│  │  │  PgBouncer Pooler (cnpg-system namespace)          │     │         │
│  │  │  • 3 instances, transaction mode                   │     │         │
│  │  │  • Connection pooling (max 1000 clients → 50 DB)   │     │         │
│  │  └──────────────────┬─────────────────────────────────┘     │         │
│  │                     │ Cross-cluster service call            │         │
│  │                     ▼                                        │         │
│  │  ┌────────────────────────────────────────────────────┐     │         │
│  │  │  Shared PostgreSQL (infra cluster)                 │     │         │
│  │  │  • 3 instances, PostgreSQL 16.8                    │     │         │
│  │  │  • Database: synergyflow                           │     │         │
│  │  │  • Schemas: users, incidents, changes, knowledge,  │     │         │
│  │  │    tasks, audit, workflows, event_publication      │     │         │
│  │  └────────────────────────────────────────────────────┘     │         │
│  └──────────────────────────────────────────────────────────────┘         │
│                                                                           │
│  ┌─────────────────┐    ┌──────────────────┐                              │
│  │  DragonflyDB    │    │  Vault           │                              │
│  │  (Cache)        │    │  (Secrets Mgmt)  │                              │
│  │  • 2 replicas   │    │  • ExternalSecret│                              │
│  └─────────────────┘    └──────────────────┘                              │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────┐          │
│  │         Observability Stack                                 │          │
│  │  • Victoria Metrics (metrics storage)                       │          │
│  │  • Grafana (dashboards)                                     │          │
│  │  • OpenTelemetry (tracing)                                  │          │
│  └─────────────────────────────────────────────────────────────┘          │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Container Responsibilities

**Frontend Application (Next.js)**
- Technology: Next.js 14.2.5, React 18.3.1, TypeScript, Tailwind CSS
- Responsibilities:
  - Server-side rendering (SSR) for initial page load
  - Client-side routing and state management
  - REST API calls to backend via Envoy Gateway
  - Real-time UI updates via polling or SSE (future: WebSocket)
- Replicas: 3 (HA)
- Resource: 500m CPU, 512MB RAM per replica

**API Gateway (Envoy)**
- Technology: Envoy Gateway with Kubernetes Gateway API
- Responsibilities:
  - JWT validation using IdP JWKS endpoint
  - Rate limiting per authenticated user
  - CORS policy enforcement
  - Request routing: /api/* → backend, / → frontend
  - TLS termination
- Replicas: 3 (HA)
- Resource: 1 CPU, 1GB RAM per replica

**Backend Application (Spring Boot)**
- Technology: Spring Boot 3.5.6, Java 21.0.2 LTS, Spring Modulith 1.4.2
- Responsibilities:
  - Business logic for all modules (incident, change, knowledge, task, time)
  - REST API endpoints (OpenAPI 3.0)
  - Internal event publication via ApplicationEventPublisher
  - Event consumption and projection building (@ApplicationModuleListener)
  - Transactional outbox pattern (event_publication table)
  - Flowable workflow execution (embedded)
  - OPA policy evaluation (via sidecar)
- Replicas: 3 (HA), scales to 10
- Resource: 500m-2000m CPU, 1.5-2GB RAM per replica

**OPA Sidecar**
- Technology: Open Policy Agent 0.68+
- Responsibilities:
  - Authorization policy evaluation
  - Decision receipt generation
  - Sub-10ms latency (localhost communication)
- Co-located: 1 sidecar per backend pod
- Resource: 100m-500m CPU, 128-256MB RAM per sidecar

**PostgreSQL Database Access (Shared Cluster + Pooler)**
- Technology: Shared PostgreSQL 16.8 cluster with CloudNative-PG operator + PgBouncer pooler
- Responsibilities:
  - Primary data store for all SynergyFlow modules
  - Event publication registry (transactional outbox pattern)
  - Connection pooling and multiplexing via PgBouncer
  - Schema-level isolation for module boundaries
  - Leverage shared cluster's HA and backup infrastructure
- Pooler Instances: 3 (high availability)
- Database: synergyflow within shared-postgres cluster (infra cluster)
- Schemas: users, incidents, changes, knowledge, tasks, audit, workflows, event_publication
- Pooler Resource: 500m-2000m CPU, 256-512MB RAM per pooler instance
- Pattern: Consistent with other platform applications (gitlab, harbor, keycloak, mattermost)

**DragonflyDB (Cache)**
- Technology: Shared DragonflyDB cluster in dragonfly-system namespace
- Responsibilities:
  - User profile caching (1-hour TTL)
  - Policy decision caching (10-minute TTL)
  - Query result caching (30-second TTL)
- Deployment: Uses existing shared cluster (3 replicas in cluster mode)
- Resource: No dedicated resources (shared infrastructure)
- Pattern: Consistent with other platform applications (gitlab, harbor, keycloak, mattermost)

**1Password (Secrets Management)**
- Technology: 1Password with External Secrets Operator (ESO)
- Responsibilities:
  - Database credentials storage
  - JWT signing keys storage
  - Automatic secret synchronization
- Deployment: Shared infrastructure namespace
- Resource: 100m CPU, 128MB RAM (ESO operator)

**Victoria Metrics + Grafana (Observability)**
- Technology: Victoria Metrics Operator, Grafana 10.4.2
- Responsibilities:
  - Metrics collection (15s scrape interval)
  - Time-series storage (30-day retention)
  - Dashboard visualization
  - Alert rule evaluation
- Resource: 2 CPU, 4GB RAM (combined)

---

_End of Part 1. Continue to next section..._

## 4. Component Architecture (C4 Level 3)

### 4.1 Overview

The backend application is organized as a Spring Modulith with six primary modules. Each module has its own bounded context, PostgreSQL schema, and communicates with other modules exclusively via events.

### 4.2 Backend Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Backend Application (Spring Boot)                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌────────────────────────────────────────────────────────────────┐    │
│  │              Foundation Module (Shared)                         │    │
│  │  • UserService, TeamService                                     │    │
│  │  • SecurityConfig (OAuth2 Resource Server)                      │    │
│  │  • CorrelationIdFilter                                          │    │
│  │  • Common DTOs, exceptions                                      │    │
│  └────────────────────────────────────────────────────────────────┘    │
│                                    │                                     │
│         ┌──────────────────────────┼──────────────────────────┐        │
│         │                          │                          │        │
│         ▼                          ▼                          ▼        │
│  ┌──────────────┐         ┌───────────────┐        ┌──────────────┐  │
│  │   Incident   │         │    Change     │        │  Knowledge   │  │
│  │    Module    │         │    Module     │        │    Module    │  │
│  ├──────────────┤         ├───────────────┤        ├──────────────┤  │
│  │              │         │               │        │              │  │
│  │ REST API:    │         │ REST API:     │        │ REST API:    │  │
│  │ - Controller │         │ - Controller  │        │ - Controller │  │
│  │              │         │               │        │              │  │
│  │ Domain:      │         │ Domain:       │        │ Domain:      │  │
│  │ - Service    │         │ - Service     │        │ - Service    │  │
│  │ - Repository │         │ - Repository  │        │ - Repository │  │
│  │ - Entity     │         │ - Entity      │        │ - Entity     │  │
│  │              │         │               │        │              │  │
│  │ Events:      │         │ Events:       │        │ Events:      │  │
│  │ - Publisher  │         │ - Publisher   │        │ - Publisher  │  │
│  │ - Consumer   │         │ - Consumer    │        │ - Consumer   │  │
│  │              │         │               │        │              │  │
│  │ Projection:  │         │ Projection:   │        │ Search:      │  │
│  │ - task_proj  │         │ - incident_pr │        │ - Full-text  │  │
│  └──────┬───────┘         └───────┬───────┘        └──────┬───────┘  │
│         │                         │                       │           │
│         │    ┌────────────────────┼───────────────────────┘           │
│         │    │                    │                                   │
│         ▼    ▼                    ▼                                   │
│  ┌──────────────┐         ┌───────────────┐        ┌──────────────┐  │
│  │     Task     │         │     Time      │        │    Audit     │  │
│  │   Module     │         │   Tracking    │        │    Module    │  │
│  │   (PM)       │         │    Module     │        │              │  │
│  ├──────────────┤         ├───────────────┤        ├──────────────┤  │
│  │              │         │               │        │              │  │
│  │ REST API:    │         │ REST API:     │        │ REST API:    │  │
│  │ - Controller │         │ - Controller  │        │ - Controller │  │
│  │ - Project    │         │               │        │              │  │
│  │ - Task       │         │ Domain:       │        │ Domain:      │  │
│  │ - Sprint     │         │ - TimeEntry   │        │ - AuditLog   │  │
│  │              │         │ - Mirror      │        │ - Decision   │  │
│  │ Domain:      │         │   Service     │        │   Receipt    │  │
│  │ - Service    │         │               │        │              │  │
│  │ - Repository │         │ Events:       │        │ Events:      │  │
│  │              │         │ - Consumer    │        │ - Consumer   │  │
│  │ Events:      │         │               │        │   (All)      │  │
│  │ - Publisher  │         └───────────────┘        └──────────────┘  │
│  │ - Consumer   │                                                     │
│  │              │                                                     │
│  │ Projection:  │                                                     │
│  │ - incident_pr│                                                     │
│  └──────────────┘                                                     │
│                                                                         │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │          Spring Modulith Event Publication Registry            │  │
│  │  • Transactional outbox pattern (event_publication table)      │  │
│  │  • Asynchronous event delivery to listeners                    │  │
│  │  • Automatic retry with exponential backoff                    │  │
│  │  • At-least-once delivery guarantee                            │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │          Flowable Engine (Embedded)                             │  │
│  │  • Process engine for BPMN workflows                            │  │
│  │  • Async job executor (4-10 threads)                            │  │
│  │  • Timer management for SLA tracking                            │  │
│  │  • State persistence in PostgreSQL                              │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Module Descriptions

**Foundation Module**
- **Purpose**: Shared infrastructure and cross-cutting concerns
- **Components**:
  - `UserService`: User CRUD, authentication integration
  - `TeamService`: Team management, multi-team support
  - `SecurityConfig`: OAuth2 Resource Server configuration
  - `CorrelationIdFilter`: Request tracing across all modules
  - `ExceptionHandler`: Global error handling (RFC 7807)
- **Database Schema**: `synergyflow_users`
- **Dependencies**: None (foundation layer)

**Incident Module**
- **Purpose**: Incident lifecycle management with SLA tracking
- **Components**:
  - `IncidentController`: REST API endpoints
  - `IncidentService`: Business logic, SLA calculation
  - `IncidentRepository`: PostgreSQL data access
  - `Incident` entity: Aggregate root
  - `SLATimerManager`: Flowable process integration
  - Event publishers: `IncidentCreatedEvent`, `IncidentResolvedEvent`
  - Event consumers: `TaskCreatedEventConsumer` (builds task projections)
- **Database Schema**: `synergyflow_incidents`
- **Tables**: incidents, incident_comments, incident_attachments, incident_worklogs, task_projections
- **Dependencies**: Foundation module, Flowable engine

**Change Module**
- **Purpose**: Change request management with approval workflows
- **Components**:
  - `ChangeController`: REST API endpoints
  - `ChangeService`: Business logic, risk assessment
  - `ChangeRepository`: PostgreSQL data access
  - `Change` entity: Aggregate root
  - `ApprovalWorkflowManager`: Flowable integration
  - Event publishers: `ChangeRequestedEvent`, `ChangeApprovedEvent`, `ChangeDeployedEvent`
  - Event consumers: `IncidentCreatedEventConsumer` (builds incident projections)
- **Database Schema**: `synergyflow_changes`
- **Tables**: changes, change_approvals, change_calendar, incident_projections
- **Dependencies**: Foundation module, Flowable engine

**Knowledge Module**
- **Purpose**: Knowledge base with versioning and full-text search
- **Components**:
  - `KnowledgeController`: REST API endpoints
  - `ArticleService`: Business logic, version management
  - `ArticleRepository`: PostgreSQL data access
  - `Article` entity: Aggregate root with version history
  - `SearchService`: Full-text search using PostgreSQL tsvector
  - Event publishers: `ArticlePublishedEvent`
- **Database Schema**: `synergyflow_knowledge`
- **Tables**: articles, article_versions, article_tags, article_ratings
- **Dependencies**: Foundation module

**Task Module (PM)**
- **Purpose**: Project and task management for PM functionality
- **Components**:
  - `TaskController`, `ProjectController`: REST API endpoints
  - `TaskService`, `ProjectService`: Business logic
  - `TaskRepository`, `ProjectRepository`, `SprintRepository`: Data access
  - `Task`, `Project`, `Sprint` entities
  - Event publishers: `TaskCreatedEvent`, `TaskAssignedEvent`, `TaskCompletedEvent`
  - Event consumers: `IncidentCreatedEventConsumer` (builds incident projections)
- **Database Schema**: `synergyflow_tasks`
- **Tables**: projects, tasks, sprints, task_sprint, task_worklogs, incident_projections
- **Dependencies**: Foundation module

**Time Tracking Module**
- **Purpose**: Single-entry time logging that mirrors to incidents and tasks
- **Components**:
  - `TimeController`: REST API endpoints
  - `TimeEntryService`: Business logic, mirroring logic
  - Event consumers: `IncidentCreatedEventConsumer`, `TaskCreatedEventConsumer`
  - Event publishers: `TimeEntryLoggedEvent`
- **Database Schema**: Shared with foundation
- **Tables**: time_entries
- **Dependencies**: Foundation module, Incident module (for projection), Task module (for projection)

**Audit Module**
- **Purpose**: Centralized audit logging and policy decision tracking
- **Components**:
  - `AuditController`: Query API for audit logs
  - `AuditService`: Audit log persistence
  - `DecisionReceiptService`: Policy decision recording
  - Event consumers: All events (universal consumer for audit trail)
- **Database Schema**: `synergyflow_audit`
- **Tables**: audit_events, policy_decisions
- **Dependencies**: Foundation module

### 4.4 Communication Patterns

**Intra-Module Communication** (within same module):
- Direct method calls (synchronous)
- No events needed
- Example: `IncidentController` → `IncidentService` → `IncidentRepository`

**Inter-Module Communication** (between modules):
- Spring Modulith ApplicationEvents (internal event bus)
- Stored in event_publication table (transactional outbox)
- Asynchronous delivery via @ApplicationModuleListener
- At-least-once delivery with automatic retry
- Example: Incident module publishes `IncidentCreatedEvent` → Task module consumes

**Module Boundary Enforcement**:
- Spring Modulith compile-time validation
- No direct imports between modules (except Foundation)
- ArchUnit tests verify boundaries
- Example violation: `TaskService` cannot directly call `IncidentService`

### 4.5 Proposed Source Tree

This section documents the complete directory structure for all three repositories in the SynergyFlow project.

#### 4.5.1 Backend Repository (synergyflow-backend)

```
synergyflow-backend/
├── build.gradle.kts                    # Gradle build configuration
├── settings.gradle.kts                 # Multi-project setup
├── gradle.properties                   # Gradle properties (versions, flags)
├── gradlew, gradlew.bat               # Gradle wrapper scripts
├── .editorconfig                      # Code style configuration
├── .gitignore
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/io/monosense/synergyflow/
│   │   │   ├── SynergyFlowApplication.java         # Spring Boot main class
│   │   │   ├── package-info.java                   # Module documentation
│   │   │   │
│   │   │   ├── user/                               # Foundation Module (shared)
│   │   │   │   ├── package-info.java
│   │   │   │   ├── api/                           # Public API (DTOs, events)
│   │   │   │   │   ├── UserCreatedEvent.java
│   │   │   │   │   ├── TeamCreatedEvent.java
│   │   │   │   │   └── UserDTO.java
│   │   │   │   ├── application/                   # REST controllers
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── TeamController.java
│   │   │   │   │   └── WhoAmIController.java
│   │   │   │   ├── domain/                        # Domain logic
│   │   │   │   │   ├── User.java                  # Entity
│   │   │   │   │   ├── Team.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── TeamService.java
│   │   │   │   │   ├── UserMapper.java           # MyBatis-Plus BaseMapper
│   │   │   │   │   └── TeamMapper.java
│   │   │   │   └── infrastructure/                # Cross-cutting concerns
│   │   │   │       ├── SecurityConfig.java        # OAuth2 Resource Server
│   │   │   │       ├── JwtAuthoritiesConverter.java
│   │   │   │       ├── CorrelationIdFilter.java
│   │   │   │       └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   ├── incident/                          # Incident Module
│   │   │   │   ├── package-info.java
│   │   │   │   ├── api/                           # Public events/DTOs
│   │   │   │   │   ├── IncidentCreatedEvent.java
│   │   │   │   │   ├── IncidentResolvedEvent.java
│   │   │   │   │   └── IncidentDTO.java
│   │   │   │   ├── application/                   # REST controllers
│   │   │   │   │   └── IncidentController.java
│   │   │   │   ├── domain/                        # Domain logic
│   │   │   │   │   ├── Incident.java              # Entity (with @TableName, @TableId)
│   │   │   │   │   ├── IncidentMapper.java        # MyBatis-Plus BaseMapper interface
│   │   │   │   │   ├── IncidentService.java       # Business logic (extends IService)
│   │   │   │   │   ├── IncidentServiceImpl.java   # Service implementation (extends ServiceImpl)
│   │   │   │   │   └── SLATimerManager.java
│   │   │   │   └── internal/                      # Event consumers (private)
│   │   │   │       └── TaskCreatedEventConsumer.java
│   │   │   │
│   │   │   ├── change/                            # Change Module
│   │   │   │   ├── package-info.java
│   │   │   │   ├── api/
│   │   │   │   │   ├── ChangeRequestedEvent.java
│   │   │   │   │   ├── ChangeApprovedEvent.java
│   │   │   │   │   └── ChangeDTO.java
│   │   │   │   ├── application/
│   │   │   │   │   └── ChangeController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Change.java
│   │   │   │   │   ├── ChangeService.java
│   │   │   │   │   ├── ChangeMapper.java        # MyBatis-Plus BaseMapper
│   │   │   │   │   └── ApprovalWorkflowManager.java
│   │   │   │   └── internal/
│   │   │   │       └── IncidentCreatedEventConsumer.java
│   │   │   │
│   │   │   ├── knowledge/                         # Knowledge Module
│   │   │   │   ├── package-info.java
│   │   │   │   ├── api/
│   │   │   │   │   ├── ArticlePublishedEvent.java
│   │   │   │   │   └── ArticleDTO.java
│   │   │   │   ├── application/
│   │   │   │   │   ├── KnowledgeController.java
│   │   │   │   │   └── SearchController.java
│   │   │   │   └── domain/
│   │   │   │       ├── Article.java
│   │   │   │       ├── ArticleService.java
│   │   │   │       ├── ArticleMapper.java        # MyBatis-Plus BaseMapper
│   │   │   │       └── SearchService.java
│   │   │   │
│   │   │   ├── task/                              # Task Module (PM)
│   │   │   │   ├── package-info.java
│   │   │   │   ├── api/
│   │   │   │   │   ├── TaskCreatedEvent.java
│   │   │   │   │   ├── TaskCompletedEvent.java
│   │   │   │   │   └── TaskDTO.java
│   │   │   │   ├── application/
│   │   │   │   │   ├── TaskController.java
│   │   │   │   │   ├── ProjectController.java
│   │   │   │   │   └── SprintController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Task.java
│   │   │   │   │   ├── Project.java
│   │   │   │   │   ├── Sprint.java
│   │   │   │   │   ├── TaskService.java
│   │   │   │   │   ├── ProjectService.java
│   │   │   │   │   ├── TaskMapper.java           # MyBatis-Plus BaseMapper
│   │   │   │   │   ├── ProjectMapper.java
│   │   │   │   │   └── SprintMapper.java
│   │   │   │   └── internal/
│   │   │   │       └── IncidentCreatedEventConsumer.java
│   │   │   │
│   │   │   ├── time/                              # Time Tracking Module
│   │   │   │   ├── package-info.java
│   │   │   │   ├── api/
│   │   │   │   │   ├── TimeEntryLoggedEvent.java
│   │   │   │   │   └── TimeEntryDTO.java
│   │   │   │   ├── application/
│   │   │   │   │   └── TimeController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── TimeEntry.java
│   │   │   │   │   ├── TimeEntryService.java
│   │   │   │   │   └── MirrorService.java         # Mirrors to incident/task
│   │   │   │   └── internal/
│   │   │   │       ├── IncidentCreatedEventConsumer.java
│   │   │   │       └── TaskCreatedEventConsumer.java
│   │   │   │
│   │   │   ├── audit/                             # Audit Module
│   │   │   │   ├── package-info.java
│   │   │   │   ├── application/
│   │   │   │   │   └── AuditController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   ├── DecisionReceipt.java
│   │   │   │   │   ├── AuditService.java
│   │   │   │   │   └── AuditMapper.java          # MyBatis-Plus BaseMapper
│   │   │   │   └── internal/
│   │   │   │       └── UniversalEventConsumer.java # Listens to all events
│   │   │   │
│   │   │   └── workflow/                          # Workflow Module (Flowable)
│   │   │       ├── package-info.java
│   │   │       ├── FlowableConfig.java
│   │   │       └── processes/                     # BPMN process definitions
│   │   │           ├── sla-timer.bpmn20.xml
│   │   │           └── approval-workflow.bpmn20.xml
│   │   │
│   │   └── resources/
│   │       ├── application.yml                    # Main configuration
│   │       ├── application-dev.yml                # Dev profile
│   │       ├── application-prod.yml               # Production profile
│   │       ├── db/migration/                      # Flyway migrations
│   │       │   ├── V001__initial_schema.sql
│   │       │   ├── V002__add_change_module.sql
│   │       │   └── V003__add_knowledge_module.sql
│   │       └── openapi/                           # OpenAPI specs (SpringDoc)
│   │           └── openapi-config.yml
│   │
│   └── test/
│       └── java/io/monosense/synergyflow/
│           ├── ArchitectureTests.java             # Module boundary tests
│           ├── ModulithStructureTests.java        # Spring Modulith tests
│           ├── testsupport/                       # Test utilities
│           │   ├── containers/                    # Testcontainers
│           │   │   ├── PostgreSQLContainer.java
│           │   │   ├── KafkaContainer.java
│           │   │   └── RedisContainer.java
│           │   ├── events/
│           │   │   └── EventTestHelper.java       # Event assertion utilities
│           │   └── time/
│           │       └── TestClock.java             # Fixed clock for tests
│           ├── user/
│           │   ├── UserServiceIntegrationTest.java
│           │   └── application/
│           │       └── WhoAmIControllerTest.java
│           ├── incident/
│           │   ├── IncidentServiceIntegrationTest.java
│           │   └── application/
│           │       └── IncidentControllerTest.java
│           └── integration/                       # Cross-module integration tests
│               └── IncidentToTaskEventFlowTest.java
│
├── config/                                        # External configuration files
│   ├── opa/                                       # OPA policy files
│   │   ├── authorization.rego
│   │   └── routing.rego
│   └── flowable/                                  # Additional BPMN diagrams
│
└── docs/
    ├── api/                                       # API documentation
    │   └── openapi.yaml                          # Generated OpenAPI spec
    └── diagrams/                                  # Architecture diagrams
        └── component-diagram.puml
```

#### 4.5.2 Frontend Repository (synergyflow-frontend)

```
synergyflow-frontend/
├── package.json                        # Dependencies and scripts
├── package-lock.json                   # Locked dependency versions
├── next.config.mjs                     # Next.js configuration
├── tsconfig.json                       # TypeScript configuration
├── tailwind.config.ts                  # Tailwind CSS configuration
├── postcss.config.mjs                  # PostCSS configuration
├── .eslintrc.json                      # ESLint rules
├── .prettierrc                         # Prettier formatting
├── vitest.config.ts                    # Vitest unit test config
├── playwright.config.ts                # Playwright E2E test config
├── .gitignore
├── README.md
│
├── public/                             # Static assets
│   ├── favicon.ico
│   ├── logo.svg
│   └── images/
│
├── src/
│   ├── app/                            # Next.js App Router
│   │   ├── layout.tsx                  # Root layout
│   │   ├── page.tsx                    # Home page
│   │   ├── globals.css                 # Global styles
│   │   ├── favicon.ico
│   │   │
│   │   ├── (auth)/                     # Auth route group
│   │   │   ├── login/
│   │   │   │   └── page.tsx
│   │   │   └── callback/
│   │   │       └── page.tsx
│   │   │
│   │   ├── (app)/                      # Authenticated app routes
│   │   │   ├── layout.tsx              # App shell with nav/sidebar
│   │   │   ├── dashboard/
│   │   │   │   └── page.tsx
│   │   │   ├── incidents/
│   │   │   │   ├── page.tsx            # List view
│   │   │   │   ├── [id]/
│   │   │   │   │   └── page.tsx        # Detail view
│   │   │   │   └── new/
│   │   │   │       └── page.tsx        # Create form
│   │   │   ├── changes/
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx
│   │   │   ├── knowledge/
│   │   │   │   ├── page.tsx            # Search and browse
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx        # Article view
│   │   │   ├── tasks/
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx
│   │   │   ├── projects/
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx
│   │   │   └── time/
│   │   │       └── page.tsx            # Time tracking tray
│   │   │
│   │   └── api/                        # API routes (for BFF pattern)
│   │       └── auth/
│   │           └── [...nextauth]/
│   │               └── route.ts        # NextAuth.js handler
│   │
│   ├── components/                     # Reusable React components
│   │   ├── core/                       # Core app components
│   │   │   ├── Header.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── ThemeToggle.tsx
│   │   │   └── SessionIndicator.tsx
│   │   ├── incidents/
│   │   │   ├── IncidentCard.tsx
│   │   │   ├── IncidentForm.tsx
│   │   │   └── IncidentStatusBadge.tsx
│   │   ├── knowledge/
│   │   │   ├── ArticleCard.tsx
│   │   │   └── SearchBar.tsx
│   │   └── ui/                         # shadcn/ui components
│   │       ├── button.tsx
│   │       ├── card.tsx
│   │       ├── dialog.tsx
│   │       └── input.tsx
│   │
│   ├── lib/                            # Utilities and configurations
│   │   ├── api-client.ts               # Axios client with interceptors
│   │   ├── auth.ts                     # NextAuth.js configuration
│   │   ├── i18n.ts                     # Internationalization
│   │   ├── query-provider.tsx          # TanStack Query setup
│   │   └── utils.ts                    # Helper functions
│   │
│   ├── hooks/                          # Custom React hooks
│   │   ├── use-incidents.ts            # TanStack Query hooks
│   │   ├── use-auth.ts
│   │   └── use-toast.ts
│   │
│   ├── types/                          # TypeScript types
│   │   ├── incident.ts
│   │   ├── change.ts
│   │   ├── article.ts
│   │   └── api.ts                      # API response types
│   │
│   └── schemas/                        # Zod validation schemas
│       ├── incident.schema.ts
│       ├── change.schema.ts
│       └── auth.schema.ts
│
├── tests/                              # Test files
│   ├── unit/                           # Vitest unit tests
│   │   └── lib/
│   │       └── api-client.test.ts
│   └── e2e/                            # Playwright E2E tests
│       ├── auth.spec.ts
│       ├── incidents.spec.ts
│       └── knowledge.spec.ts
│
└── scripts/                            # Build/deployment scripts
    ├── check-bundle-size.js
    └── generate-types.js               # Generate types from OpenAPI
```

#### 4.5.3 Infrastructure Repository (synergyflow-infra)

```
synergyflow-infra/
├── README.md
├── .gitignore
│
├── base/                               # Base Kubernetes manifests
│   ├── namespace.yaml
│   ├── backend/
│   │   ├── deployment.yaml             # Backend StatefulSet/Deployment
│   │   ├── service.yaml                # ClusterIP Service
│   │   ├── configmap.yaml              # Application configuration
│   │   ├── hpa.yaml                    # HorizontalPodAutoscaler
│   │   └── servicemonitor.yaml         # Prometheus monitoring
│   ├── frontend/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── hpa.yaml
│   ├── database/
│   │   ├── pooler.yaml                 # PgBouncer pooler (CloudNative-PG)
│   │   └── database.yaml               # Database resource (shared cluster)
│   ├── cache/
│   │   └── dragonfly-client.yaml       # Connection config (shared cluster)
│   └── gateway/
│       ├── gateway.yaml                # Envoy Gateway resource
│       ├── gatewayclass.yaml           # Gateway class
│       ├── httproute.yaml              # HTTP routing rules
│       ├── jwt-provider.yaml           # JWT authentication config
│       └── rate-limit.yaml             # Rate limiting policy
│
├── overlays/                           # Kustomize overlays per environment
│   ├── dev/
│   │   ├── kustomization.yaml
│   │   ├── configmap-patch.yaml        # Dev-specific configs
│   │   ├── replicas-patch.yaml         # Lower replica count
│   │   └── ingress-patch.yaml          # dev.synergyflow.local
│   ├── staging/
│   │   ├── kustomization.yaml
│   │   ├── configmap-patch.yaml
│   │   └── ingress-patch.yaml          # staging.synergyflow.io
│   └── production/
│       ├── kustomization.yaml
│       ├── configmap-patch.yaml
│       ├── replicas-patch.yaml         # Production scale
│       ├── ingress-patch.yaml          # synergyflow.io
│       └── pdb.yaml                    # PodDisruptionBudget
│
├── gitops/                             # GitOps configurations
│   ├── flux/                           # Flux CD
│   │   ├── gitrepository.yaml          # Git source
│   │   ├── kustomization-dev.yaml      # Dev Kustomization
│   │   ├── kustomization-staging.yaml
│   │   └── kustomization-prod.yaml
│   └── argocd/                         # Argo CD (alternative)
│       ├── application-dev.yaml
│       ├── application-staging.yaml
│       └── application-prod.yaml
│
├── observability/                      # Monitoring stack
│   ├── victoria-metrics/
│   │   ├── vmagent.yaml                # Metrics scraper
│   │   ├── vmstorage.yaml              # Time-series storage
│   │   └── vmselect.yaml               # Query interface
│   ├── grafana/
│   │   ├── deployment.yaml
│   │   ├── dashboards/
│   │   │   ├── synergyflow-overview.json
│   │   │   ├── spring-modulith.json
│   │   │   └── database-performance.json
│   │   └── datasources.yaml
│   └── opentelemetry/
│       └── collector.yaml              # OTEL Collector
│
├── security/                           # Security configurations
│   ├── networkpolicies/
│   │   ├── backend-network-policy.yaml
│   │   ├── frontend-network-policy.yaml
│   │   └── database-network-policy.yaml
│   └── secrets/
│       └── external-secrets.yaml       # External Secrets Operator config
│
├── policies/                           # OPA policies
│   ├── gateway/
│   │   └── authorization.rego          # Gateway-level policies
│   └── conftest/
│       └── policy.rego                 # Infrastructure validation
│
└── scripts/
    ├── deploy.sh                       # Deployment helper script
    ├── rollback.sh                     # Rollback script
    └── validate-manifests.sh           # Pre-deployment validation
```

**Source Tree Conventions**:

1. **Backend (Java/Spring Boot + MyBatis-Plus)**:
   - Package-by-feature within each module (Spring Modulith convention)
   - `api/` package: Public contracts (events, DTOs) - can be imported by other modules
   - `application/` package: REST controllers (entry points)
   - `domain/` package: Business logic (services, mappers, entities)
     - Entities: POJOs with `@TableName`, `@TableId`, `@TableField` annotations
     - Mappers: Interfaces extending `BaseMapper<T>` (no implementation needed)
     - Services: Interfaces extending `IService<T>` (optional, for service layer pattern)
     - ServiceImpl: Classes extending `ServiceImpl<Mapper, Entity>` (implements IService)
   - `internal/` package: Private implementation (event consumers) - cannot be imported by other modules
   - `infrastructure/` package: Cross-cutting concerns (security, filters, MyBatis-Plus config)

2. **Frontend (Next.js)**:
   - App Router convention (`app/` directory)
   - Route groups: `(auth)` for public routes, `(app)` for authenticated routes
   - Colocation: Keep components close to where they're used
   - Shared components in `components/` directory
   - Type-safe API client with generated types from OpenAPI spec

3. **Infrastructure (Kubernetes)**:
   - Base manifests in `base/` directory
   - Kustomize overlays for environment-specific configuration
   - GitOps manifests for Flux CD or Argo CD
   - Separate directories for observability, security, and policies

4. **Testing Structure**:
   - Backend: Tests mirror production structure (`src/test/java/...`)
   - Frontend: `tests/unit/` for Vitest, `tests/e2e/` for Playwright
   - Test utilities in dedicated packages (`testsupport/`, `test-utils/`)

**MyBatis-Plus Code Pattern Example**:

```java
// 1. Entity (domain/Incident.java)
@TableName("incidents")
public class Incident {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("incident_id")
    private String incidentId;

    private String title;
    private Priority priority;

    @Version  // Optimistic locking
    private Integer version;

    @TableLogic  // Soft delete
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

// 2. Mapper (domain/IncidentMapper.java)
@Mapper
public interface IncidentMapper extends BaseMapper<Incident> {
    // No implementation needed! BaseMapper provides:
    // - insert(), deleteById(), updateById(), selectById()
    // - selectList(), selectPage(), selectCount()
    // Custom queries with @Select, @Update, @Delete, @Insert

    @Select("SELECT * FROM incidents WHERE status = #{status} AND priority = #{priority}")
    List<Incident> findByStatusAndPriority(@Param("status") String status, @Param("priority") Priority priority);
}

// 3. Service Interface (domain/IncidentService.java) - OPTIONAL
public interface IncidentService extends IService<Incident> {
    // IService provides: save(), saveBatch(), getById(), list(), page()
    // Add custom business methods here
    Incident createIncident(CreateIncidentCommand command);
}

// 4. Service Implementation pattern - publishes events atomically
@Service
public class IncidentServiceImpl extends ServiceImpl<IncidentMapper, Incident> implements IncidentService {
    @Transactional
    public Incident createIncident(CreateIncidentCommand command) {
        this.save(incident);  // MyBatis-Plus inherited method
        eventPublisher.publishEvent(new IncidentCreatedEvent(incident));  // Atomic with save
        return incident;
    }
}

// 5. Lambda Query pattern (type-safe, no SQL strings)
List<Incident> results = incidentService.lambdaQuery()
    .eq(Incident::getPriority, Priority.HIGH)
    .page(new Page<>(1, 20));  // Page 1, size 20
```

**See Also:** [appendix-04-component-data-access.md](./appendix-04-component-data-access.md) for:
- Full service implementation examples
- Complete MyBatis-Plus configuration
- Lambda query patterns and usage

---

_End of Part 2. Continue to Event-Driven Architecture..._

## 5. Event-Driven Architecture

### 5.1 Overview

SynergyFlow uses a CQRS (Command Query Responsibility Segregation) pattern with event-driven integration between modules. All cross-module communication happens via Spring Modulith ApplicationEvents with transactional outbox pattern for guaranteed delivery.

**Key Principles**:
- Events stored in `event_publication` table during write transaction (atomicity)
- Asynchronous delivery via Spring Modulith's built-in event publication registry
- @ApplicationModuleListener for type-safe event consumption
- Automatic retry with exponential backoff for failed processing
- At-least-once delivery semantics (idempotent consumers required)

### 5.2 CQRS Pattern

**Write Side (Command)**:
1. REST API receives command (e.g., POST /api/v1/incidents)
2. Controller validates request, extracts user context from JWT
3. OPA policy evaluation (authorization check)
4. Service layer executes business logic
5. Entity persisted to PostgreSQL (aggregate root)
6. Domain event published via ApplicationEventPublisher
7. Event stored in `event_publication` table (same transaction)
8. Transaction commits (ACID guarantees)

**Read Side (Query)**:
1. Spring Modulith delivers event to @ApplicationModuleListener
2. Consumer builds/updates projection in local PostgreSQL schema
3. Tracks processing timestamp for freshness calculation
4. Event marked as completed in `event_publication` table
5. Query API reads from projections (not aggregate roots)

### 5.3 Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                 Spring Modulith Event Flow Architecture              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. Write Command                                                    │
│  ┌────────────┐                                                     │
│  │ POST /api/ │                                                     │
│  │ incidents  │                                                     │
│  └──────┬─────┘                                                     │
│         │                                                            │
│         ▼                                                            │
│  ┌──────────────────────────────────────────────────────┐           │
│  │         Incident Module (JVM Process)                │           │
│  │  ┌───────────────────────────────────────────────┐   │           │
│  │  │ @Transactional                                │   │           │
│  │  │                                               │   │           │
│  │  │  2. Persist Aggregate                         │   │           │
│  │  │  ┌──────────────────────────────────────┐    │   │           │
│  │  │  │ incidentRepository.save(incident)    │    │   │           │
│  │  │  └──────────────────────────────────────┘    │   │           │
│  │  │                                               │   │           │
│  │  │  3. Publish Event                             │   │           │
│  │  │  ┌──────────────────────────────────────┐    │   │           │
│  │  │  │ eventPublisher.publishEvent(         │    │   │           │
│  │  │  │   new IncidentCreatedEvent(...)      │    │   │           │
│  │  │  │ )                                     │    │   │           │
│  │  │  └──────────────────────────────────────┘    │   │           │
│  │  │                                               │   │           │
│  │  │  4. Spring Modulith Interceptor               │   │           │
│  │  │  ┌──────────────────────────────────────┐    │   │           │
│  │  │  │ INSERT INTO event_publication (      │    │   │           │
│  │  │  │   id, event_type, serialized_event,  │    │   │           │
│  │  │  │   publication_date                   │    │   │           │
│  │  │  │ ) VALUES (...)                       │    │   │           │
│  │  │  └──────────────────────────────────────┘    │   │           │
│  │  │                                               │   │           │
│  │  │  5. Transaction Commit (both writes atomic)   │   │           │
│  │  └───────────────────────────────────────────────┘   │           │
│  └──────────────────────────────────────────────────────┘           │
│                                                                       │
│  6. Async Event Delivery (Spring Modulith background thread)        │
│  ┌──────────────────────────────────────────────────────┐           │
│  │ EventPublicationRegistry.processIncomplete()         │           │
│  │ • Polls event_publication table                      │           │
│  │ • Delivers to @ApplicationModuleListener methods     │           │
│  │ • Retries failed deliveries (exponential backoff)    │           │
│  └──────────────────────────────────────────────────────┘           │
│           │                                                          │
│           │ 7. In-JVM event delivery (<1ms latency)                 │
│           ▼                                                          │
│  ┌──────────────────────────────────────────────────────┐           │
│  │         Task Module (same JVM Process)                │           │
│  │  ┌───────────────────────────────────────────────┐   │           │
│  │  │ @ApplicationModuleListener                    │   │           │
│  │  │ @Transactional                                │   │           │
│  │  │ public void handle(IncidentCreatedEvent evt) {│   │           │
│  │  │   // Build projection                         │   │           │
│  │  │   projectionRepo.save(                        │   │           │
│  │  │     new IncidentProjection(                   │   │           │
│  │  │       evt.incidentId,                         │   │           │
│  │  │       evt.title,                              │   │           │
│  │  │       Instant.now() // last_updated           │   │           │
│  │  │     )                                          │   │           │
│  │  │   );                                           │   │           │
│  │  │ }                                              │   │           │
│  │  └───────────────────────────────────────────────┘   │           │
│  └──────────────────────────────────────────────────────┘           │
│           │                                                          │
│           │ 8. Mark event completed                                 │
│           ▼                                                          │
│  ┌──────────────────────────────────────────────────────┐           │
│  │ UPDATE event_publication                             │           │
│  │ SET completion_date = NOW()                          │           │
│  │ WHERE id = ?                                         │           │
│  └──────────────────────────────────────────────────────┘           │
│                                                                       │
│  9. Query with freshness badge                                      │
│  ┌────────────┐                                                     │
│  │ GET /api/  │                                                     │
│  │ tasks/     │                                                     │
│  │ TASK-890/  │                                                     │
│  │ relations  │                                                     │
│  └──────┬─────┘                                                     │
│         │                                                            │
│         ▼                                                            │
│  ┌────────────────────────┐                                         │
│  │ Response:              │                                         │
│  │ {                      │                                         │
│  │   relatedIncidents: [  │                                         │
│  │     {                  │                                         │
│  │       id: "INC-1234",  │                                         │
│  │       title: "...",    │                                         │
│  │       lastUpdated: "..." │                                       │
│  │     }                  │                                         │
│  │   ],                   │                                         │
│  │   freshnessLag: 0.05s  │  # 50ms (typical <100ms)               │
│  │ }                      │                                         │
│  └────────────────────────┘                                         │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.4 Spring Modulith Event Structure

All domain events are POJOs (Plain Old Java Objects) serialized as JSON in the `event_publication` table:

**Event Lifecycle Pattern** (Spring Modulith 1.4.2 improvements):

1. **Definition**: Java record with immutable fields, correlation ID for tracing
2. **Publishing**: `ApplicationEventPublisher.publishEvent()` within `@Transactional` method (stored in `event_publication` table atomically)
   - Spring Modulith 1.4.2 revamps event publication lifecycle for better performance
3. **Consumption**: `@ApplicationModuleListener` method in consumer module, separate transaction, idempotent handling
   - Enhanced testing support in 1.4.2 for event assertions
4. **Projection**: Consumer builds query-optimized view with freshness tracking
   - Improved observability integration for event metrics in 1.4.2

```java
// 1. Event (immutable record in incident.api package)
public record IncidentCreatedEvent(String incidentId, String title, ..., String correlationId) {}

// 2. Publisher (incident module)
@Transactional
public Incident create(Command cmd) {
    Incident incident = repo.save(new Incident(cmd));
    eventPublisher.publishEvent(new IncidentCreatedEvent(...));  // Stored in event_publication
    return incident;
}

// 3. Consumer (task module internal package)
@ApplicationModuleListener
public void on(IncidentCreatedEvent event) {
    projectionRepo.save(new IncidentProjection(event));  // Build read-optimized view
}
```

Full implementation in backend repository (see Section 4.5.1).

**event_publication Table Structure**:
```sql
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,       -- Fully qualified class name
    serialized_event TEXT NOT NULL,          -- JSON serialization
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP,               -- NULL until processed
    listener_id VARCHAR(255)                 -- Identifies the listener
);

CREATE INDEX idx_event_publication_completion
    ON event_publication(completion_date)
    WHERE completion_date IS NULL;
```

**Stored Event Example** (JSON in serialized_event column):
```json
{
  "incidentId": "INC-1234",
  "title": "Production API returning 500 errors",
  "description": "API gateway reporting 500 errors since 14:00",
  "priority": "HIGH",
  "severity": "S1",
  "assignedTo": "user-456",
  "slaDeadline": "2025-10-17T18:00:00Z",
  "createdAt": "2025-10-17T14:00:00Z",
  "createdBy": "user-789",
  "correlationId": "cor-abc-123"
}
```

### 5.5 Projection Lag and Freshness Badges

**Projection Lag Calculation**:
```sql
-- In projection table
CREATE TABLE incident_projections (
  id UUID PRIMARY KEY,
  incident_id VARCHAR(50),
  incident_title VARCHAR(255),
  last_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Freshness query
SELECT
  incident_id,
  incident_title,
  EXTRACT(EPOCH FROM (NOW() - last_updated)) AS freshness_lag_seconds
FROM incident_projections
WHERE incident_id = 'INC-1234';

-- Example result: freshness_lag_seconds = 0.087 (87ms)
```

**Freshness Badge Thresholds** (Spring Modulith in-memory event bus):
- **GREEN** (<200ms): Data is fresh, high confidence (typical p50 <50ms, p95 <100ms)
- **YELLOW** (200ms-1s): Slight lag, acceptable for all operations
- **RED** (>1s): Significant lag, indicates system overload or event processing failure

**Performance Characteristics**:
- **Typical lag**: 50-100ms (p95)
- **Best case**: <10ms (in-memory event delivery)
- **Worst case**: <500ms (under load with retry)

**UI Display**:
```
Related Tasks: TASK-890, TASK-891
[●] Data current as of 87ms ago
```

**Comparison to External Message Broker**:
- Spring Modulith in-JVM: 50-100ms typical lag
- Kafka-based: 2-3s typical lag (network + serialization overhead)
- **20x faster** for MVP use case

### 5.6 Event Delivery Guarantees

**At-Least-Once Delivery**:
- Spring Modulith stores events in `event_publication` table during write transaction
- Background thread polls incomplete events and delivers to listeners
- Failed deliveries automatically retried with exponential backoff
- Event marked complete only after successful listener execution

**Idempotency Pattern**:
```java
@Service
public class TaskIncidentProjectionService {

    private final IncidentProjectionRepository projectionRepository;
    private final ProcessedEventRepository processedEventRepository;

    @ApplicationModuleListener
    @Transactional
    public void on(IncidentCreatedEvent event) {
        // Idempotency check using event natural key
        String idempotencyKey = "incident:" + event.incidentId();

        if (processedEventRepository.existsByKey(idempotencyKey)) {
            log.debug("Event already processed for {}, skipping", event.incidentId());
            return;
        }

        // Build projection
        IncidentProjection projection = new IncidentProjection(
            event.incidentId(),
            event.title(),
            event.priority().name(),
            Instant.now()
        );
        projectionRepository.save(projection);

        // Mark as processed
        processedEventRepository.save(
            new ProcessedEvent(idempotencyKey, Instant.now())
        );

        log.info("Built incident projection for {}", event.incidentId());
    }
}
```

**Processed Events Tracking**:
```sql
CREATE TABLE processed_events (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events(processed_at);

-- Cleanup old entries (optional, run weekly)
DELETE FROM processed_events
WHERE processed_at < NOW() - INTERVAL '30 days';
```

**Retry Configuration**:
```java
@Configuration
public class EventPublicationConfiguration {

    @Bean
    public CompletionMode completionMode() {
        return CompletionMode.builder()
            .on(Throwable.class)
            .retry(5)  // Retry up to 5 times
            .exponentialBackoff(Duration.ofSeconds(2), 2.0)  // 2s, 4s, 8s, 16s, 32s
            .build();
    }
}
```

### 5.7 Event Schema Evolution

**Backward Compatibility Rules**:
- Can add optional fields (null-safe) without breaking consumers
- Can add new event types without affecting existing consumers
- Cannot remove fields (breaks old consumers still reading them)
- Cannot change field types (Jackson deserialization fails)
- Use separate event types for major breaking changes

**Example Evolution**:
```java
// v1.0.0 - Initial event
public record IncidentCreatedEvent(
    String incidentId,
    String title,
    Priority priority
) {}

// v1.1.0 - Add optional field (backward compatible)
public record IncidentCreatedEvent(
    String incidentId,
    String title,
    Priority priority,
    List<String> tags  // New field, consumers handle null gracefully
) {}

// v2.0.0 - Breaking change (new event type recommended)
public record IncidentCreatedEventV2(
    String incidentId,
    String title,
    Priority priority,
    List<String> tags,
    IncidentMetadata metadata  // Complex new field
) {}
```

**Migration Strategy for Breaking Changes**:
1. Publish both old and new event types during migration period
2. Consumers gradually update to handle new event type
3. Deprecate old event type after all consumers migrated
4. Remove old event publishers

**Spring Modulith Event Type Resolution**:
- Uses fully qualified class name for event type identification
- JSON deserialization via Jackson ObjectMapper
- Type-safe compilation (no schema drift at runtime)
- Refactoring tools (IDE) detect breaking changes automatically

### 5.8 Architectural Decision: Spring Modulith vs External Message Broker

**Decision**: Use Spring Modulith's built-in event publication registry instead of external message broker (Kafka) for MVP.

**Context**:
SynergyFlow requires event-driven integration between modules (Incident, Change, Task, etc.) for CQRS projections and cross-module communication. Two primary options were evaluated:
1. **Spring Modulith**: In-JVM event bus with transactional outbox pattern
2. **Apache Kafka**: External distributed message broker

**Decision Rationale**:

**Favors Spring Modulith** (chosen for MVP):
- ✅ **No external system integration**: All modules in single JVM, no cross-service communication needed
- ✅ **Resource efficiency**: Eliminates 6 CPU cores, 13.5GB RAM, $250-350/month for Kafka infrastructure
- ✅ **Performance**: 50-100ms event lag vs 2-3s with Kafka (20x faster)
- ✅ **Operational simplicity**: Zero message broker infrastructure for 3-person team
- ✅ **Transactional guarantees**: Events stored atomically with aggregate in same database transaction
- ✅ **Type safety**: Java records with compile-time validation, IDE refactoring support
- ✅ **Development velocity**: No schema registry, no Avro serialization complexity
- ✅ **Natural migration path**: Can externalize events to Kafka later via Spring Modulith event externalization

**When Kafka Would Be Needed** (future, post-MVP):
- ❌ External microservices consuming events
- ❌ Cross-system integration (different deployment boundaries)
- ❌ Strict event ordering across multiple pods (partition key strategy)
- ❌ Event replay requirements for analytics
- ❌ Multi-datacenter replication
- ❌ Independent scaling of event consumers

**Current SynergyFlow Architecture**:
- Single modular monolith (no microservices)
- All modules deployed together (shared fate)
- 250 users MVP (scales to 1,000 users with 10 backend replicas)
- CQRS projections consumed within same JVM
- No external systems consuming events

**Migration Path** (if needed in future):
1. Enable Spring Modulith event externalization
2. Deploy Kafka cluster
3. Configure event externalizer to publish to Kafka topics
4. Existing @ApplicationModuleListener code unchanged
5. External systems consume from Kafka
6. Internal modules continue using in-JVM events

**Conclusion**: Spring Modulith provides all required event-driven capabilities for MVP with significantly lower complexity and cost. Kafka can be added incrementally if/when external event consumers are needed.

---

## 6. Database Architecture

### 6.1 Overview

SynergyFlow uses PostgreSQL 16.8 with CloudNative-PG operator. Each module has its own schema, enforcing module boundaries at the database level.

### 6.2 Schema Organization

```
PostgreSQL Database: synergyflow
├── synergyflow_users (Foundation Module)
├── synergyflow_incidents (Incident Module)
├── synergyflow_changes (Change Module)
├── synergyflow_knowledge (Knowledge Module)
├── synergyflow_tasks (Task Module)
├── synergyflow_audit (Audit Module)
├── synergyflow_workflows (Flowable Embedded Engine)
└── event_publication (Spring Modulith Transactional Outbox)
```

**event_publication Schema** (Spring Modulith):
```sql
-- Event publication table (transactional outbox pattern)
CREATE TABLE event_publication (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(512) NOT NULL,        -- Fully qualified class name
    serialized_event TEXT NOT NULL,          -- JSON serialization
    publication_date TIMESTAMP NOT NULL DEFAULT NOW(),
    completion_date TIMESTAMP,               -- NULL until successfully processed
    listener_id VARCHAR(512)                 -- Identifies consuming listener
);

-- Indexes for efficient polling and cleanup
CREATE INDEX idx_event_publication_completion
    ON event_publication(completion_date)
    WHERE completion_date IS NULL;  -- Partial index for incomplete events

CREATE INDEX idx_event_publication_date
    ON event_publication(publication_date DESC);

-- Processed events tracking (for idempotency)
CREATE TABLE processed_events (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events(processed_at);
```

### 6.3 Core Tables

**synergyflow_users Schema**:
```sql
-- Users table
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,
  team_id UUID,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_team FOREIGN KEY (team_id) REFERENCES teams(id)
);

-- Teams table
CREATE TABLE teams (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_team_id ON users(team_id);
```

**synergyflow_incidents Schema**:
```sql
-- Incidents table (aggregate root)
CREATE TABLE incidents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_id VARCHAR(50) UNIQUE NOT NULL,  -- INC-1234
  title VARCHAR(255) NOT NULL,
  description TEXT,
  priority VARCHAR(20) NOT NULL,  -- LOW, MEDIUM, HIGH, CRITICAL
  severity VARCHAR(10) NOT NULL,  -- S1, S2, S3, S4
  status VARCHAR(20) NOT NULL,    -- NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
  assigned_to UUID,
  sla_deadline TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMP,
  closed_at TIMESTAMP,
  created_by UUID NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_assigned_to FOREIGN KEY (assigned_to) REFERENCES synergyflow_users.users(id),
  CONSTRAINT fk_created_by FOREIGN KEY (created_by) REFERENCES synergyflow_users.users(id)
);

-- Comments table
CREATE TABLE incident_comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_id UUID NOT NULL,
  user_id UUID NOT NULL,
  comment_text TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES synergyflow_users.users(id)
);

-- Worklogs table
CREATE TABLE incident_worklogs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_id UUID NOT NULL,
  user_id UUID NOT NULL,
  time_spent_minutes INT NOT NULL,
  description TEXT,
  logged_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES synergyflow_users.users(id)
);

-- Projection table (read model from task events)
CREATE TABLE task_projections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id VARCHAR(50) NOT NULL,
  task_title VARCHAR(255),
  incident_id UUID NOT NULL,
  last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_incidents_incident_id ON incidents(incident_id);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_assigned_to ON incidents(assigned_to);
CREATE INDEX idx_incidents_sla_deadline ON incidents(sla_deadline);
CREATE INDEX idx_incident_comments_incident_id ON incident_comments(incident_id);
CREATE INDEX idx_incident_worklogs_incident_id ON incident_worklogs(incident_id);
CREATE INDEX idx_task_projections_incident_id ON task_projections(incident_id);
```

**synergyflow_tasks Schema**:
```sql
-- Projects table
CREATE TABLE projects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id VARCHAR(50) UNIQUE NOT NULL,  -- PROJ-1
  name VARCHAR(255) NOT NULL,
  description TEXT,
  owner_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_owner FOREIGN KEY (owner_id) REFERENCES synergyflow_users.users(id)
);

-- Tasks table (aggregate root)
CREATE TABLE tasks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id VARCHAR(50) UNIQUE NOT NULL,  -- TASK-890
  project_id UUID NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL,  -- NOT_STARTED, IN_PROGRESS, DONE
  assigned_to UUID,
  story_points INT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  version INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
  CONSTRAINT fk_assigned_to FOREIGN KEY (assigned_to) REFERENCES synergyflow_users.users(id)
);

-- Sprints table
CREATE TABLE sprints (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sprint_id VARCHAR(50) UNIQUE NOT NULL,  -- SPRINT-12
  project_id UUID NOT NULL,
  name VARCHAR(255) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  goal TEXT,
  CONSTRAINT fk_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Task-Sprint mapping (many-to-many)
CREATE TABLE task_sprint (
  task_id UUID NOT NULL,
  sprint_id UUID NOT NULL,
  PRIMARY KEY (task_id, sprint_id),
  CONSTRAINT fk_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
  CONSTRAINT fk_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE
);

-- Projection table (read model from incident events)
CREATE TABLE incident_projections (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  incident_id VARCHAR(50) NOT NULL,
  incident_title VARCHAR(255),
  task_id UUID NOT NULL,
  last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_tasks_task_id ON tasks(task_id);
CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to);
CREATE INDEX idx_incident_projections_task_id ON incident_projections(task_id);
```

**synergyflow_audit Schema**:
```sql
-- Audit events table (append-only)
CREATE TABLE audit_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type VARCHAR(50) NOT NULL,
  entity_id VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL,
  user_id UUID NOT NULL,
  correlation_id VARCHAR(100) NOT NULL,
  occurred_at TIMESTAMP NOT NULL DEFAULT NOW(),
  payload_json JSONB NOT NULL,
  signature VARCHAR(255) NOT NULL,  -- HMAC-SHA256 signature
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES synergyflow_users.users(id)
);

-- Policy decisions table
CREATE TABLE policy_decisions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  policy_name VARCHAR(100) NOT NULL,
  policy_version VARCHAR(20) NOT NULL,
  input_data JSONB NOT NULL,
  decision JSONB NOT NULL,
  reason TEXT,
  evaluated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_occurred_at ON audit_events(occurred_at DESC);
CREATE INDEX idx_policy_decisions_policy_name ON policy_decisions(policy_name);
CREATE INDEX idx_policy_decisions_evaluated_at ON policy_decisions(evaluated_at DESC);
```

### 6.4 Connection Pooling

**HikariCP Configuration**:
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 1800000
      pool-name: SynergyFlowHikariPool
```

**Capacity Planning**:
- 3 backend replicas × 20 connections = 60 total connections
- PostgreSQL `max_connections`: 200 (leaves room for admin, monitoring)
- Scale to 10 replicas: 10 × 20 = 200 connections (still within limit)

### 6.5 Migration Strategy

**Flyway or Liquibase** for schema versioning:
```
migrations/
├── V001__create_users_schema.sql
├── V002__create_incidents_schema.sql
├── V003__create_tasks_schema.sql
├── V004__add_incident_projections.sql
└── V005__add_audit_tables.sql
```

**Migration Execution**:
- Run on application startup (Spring Boot auto-configuration)
- Versioned migrations (V001, V002, etc.)
- Checksum validation prevents tampering
- Rollback strategy: restore from backup, replay migrations

---

_End of Part 3. Document continues..._

## 7. API Specifications

### 7.1 Overview

All REST APIs follow OpenAPI 3.0 specification with consistent patterns for authentication, pagination, error handling, and versioning.

### 7.2 API Design Principles

**RESTful Conventions**:
- Resource-based URLs: `/api/v1/incidents/{id}`
- HTTP methods: GET (read), POST (create), PATCH (update), DELETE (delete)
- Idempotent operations: GET, PUT, DELETE
- Non-idempotent: POST

**Request/Response Format**:
- Content-Type: `application/json`
- Character encoding: UTF-8
- Date format: ISO 8601 (2025-10-17T14:00:00Z)

**API Versioning**:
- URL-based versioning: `/api/v1/`, `/api/v2/`
- Major version changes: breaking changes
- Minor version changes: backward-compatible additions

### 7.3 Core API Endpoints

**API Documentation**: All REST APIs are automatically documented using **SpringDoc OpenAPI 2.3.0** with interactive Swagger UI available at `/api-docs` (OpenAPI spec) and `/swagger-ui.html` (interactive documentation). Controllers use `@Operation`, `@ApiResponse`, and `@Schema` annotations for rich documentation.

**API Modules**:

| Module | Base Path | Key Resources | Purpose |
|--------|-----------|---------------|---------|
| Incident | `/api/v1/incidents` | incidents, comments, worklogs | Incident lifecycle management with SLA tracking |
| Change | `/api/v1/changes` | changes, approvals, calendar | Change request management with approval workflows |
| Knowledge | `/api/v1/knowledge` | articles, search, ratings | Knowledge base with versioning and full-text search |
| Task | `/api/v1/tasks`, `/api/v1/projects` | projects, tasks, sprints | Project and task management (PM functionality) |
| Time | `/api/v1/time` | time-entries, mirrors | Single-entry time tracking with mirroring to incidents/tasks |
| User | `/api/v1/users`, `/api/v1/teams` | users, teams, preferences | User and team management (Foundation module) |
| Audit | `/api/v1/audit` | audit-logs, policy-decisions | Audit trail and policy decision tracking |

**REST API Pattern** (example: Incident CRUD):

```http
POST   /api/v1/incidents              # Create incident → 201 Created
GET    /api/v1/incidents              # List incidents (paginated) → 200 OK
GET    /api/v1/incidents/{id}         # Get incident detail → 200 OK
PATCH  /api/v1/incidents/{id}         # Update incident → 200 OK
DELETE /api/v1/incidents/{id}         # Delete incident → 204 No Content

POST   /api/v1/incidents/{id}/comments  # Add comment → 201 Created
GET    /api/v1/incidents/{id}/relationships  # Get related entities (HATEOAS) → 200 OK
```

**Common Request Headers**:
- `Authorization: Bearer {jwt-token}` (required for all authenticated endpoints)
- `Content-Type: application/json`
- `X-Correlation-Id: {uuid}` (for request tracing across modules)
- `Accept-Language: en-US` (for i18n support)

**Common Response Patterns**:
- **Success**: `200 OK` (read/update), `201 Created` (create), `204 No Content` (delete)
- **Errors**: RFC 7807 Problem Details format (see Section 7.4)
- **Pagination**: `limit`, `offset`, `total` fields (see Section 7.5)
- **HATEOAS**: `_links` object with related resource URLs
- **Freshness Badges**: `freshnessLag` field for projection-based queries (seconds since event)

**Security**:
- All endpoints require valid JWT token (except `/api/health`, `/api/metrics`)
- OPA policy evaluation for authorization (see Section 9.3)
- Rate limiting: 100 requests/minute per user (enforced by Envoy Gateway)
- CORS: Configured for `https://*.synergyflow.io` domains

**SpringDoc Configuration Example**:

```java
@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incident", description = "Incident management API")
public class IncidentController {
    
    @PostMapping
    @Operation(summary = "Create incident", description = "Creates new incident with SLA tracking")
    @ApiResponse(responseCode = "201", description = "Incident created successfully")
    public ResponseEntity<IncidentDTO> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        // Implementation
    }
}
```

**Interactive API Documentation**:
- **Swagger UI**: `https://api.synergyflow.io/swagger-ui.html` - Interactive API testing with OAuth2 authentication
- **OpenAPI Spec**: `https://api.synergyflow.io/api-docs` - Machine-readable OpenAPI 3.0 specification
- **ReDoc**: `https://api.synergyflow.io/redoc` - Alternative documentation UI with better readability

For complete request/response schemas, authentication flows, and example payloads, refer to the Swagger UI or OpenAPI specification.


### 7.4 Error Handling (RFC 7807)

All errors follow RFC 7807 Problem Details format:

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.synergyflow.io/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Invalid priority value: 'SUPER_HIGH'. Valid values: LOW, MEDIUM, HIGH, CRITICAL",
  "instance": "/api/v1/incidents",
  "correlationId": "cor-abc-123",
  "errors": [
    {
      "field": "priority",
      "message": "Invalid enum value"
    }
  ]
}
```

**Common Error Codes**:
- `400 Bad Request`: Invalid input, validation errors
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: Insufficient permissions (OPA policy denied)
- `404 Not Found`: Resource not found
- `409 Conflict`: Optimistic locking conflict (version mismatch)
- `422 Unprocessable Entity`: Business rule violation
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Unexpected server error
- `503 Service Unavailable`: Service temporarily unavailable

### 7.5 Pagination

**Offset-Based Pagination**:
```
GET /api/v1/incidents?limit=20&offset=40

Response:
{
  "items": [...],
  "total": 150,
  "limit": 20,
  "offset": 40,
  "hasMore": true
}
```

**Cursor-Based Pagination** (future, for real-time feeds):
```
GET /api/v1/incidents?limit=20&cursor=eyJpZCI6InV1aWQtMTIzIn0

Response:
{
  "items": [...],
  "nextCursor": "eyJpZCI6InV1aWQtMTQ0In0",
  "hasMore": true
}
```

---

## 8. Domain Event Catalog

### 8.1 Overview

Spring Modulith events are Java records stored in the `event_publication` table. All events are serialized as JSON and delivered in-process to @ApplicationModuleListener methods.

**Event Naming Conventions**:
- Past tense verbs (e.g., `IncidentCreated`, not `CreateIncident`)
- Entity-centric (e.g., `IncidentResolvedEvent`, not `ResolutionEvent`)
- Package follows module structure (`io.monosense.synergyflow.{module}`)

**Event Characteristics**:
- Immutable (Java records)
- Serializable to JSON (Jackson)
- Type-safe (compile-time validation)
- Self-documenting (field names and types explicit)

### 8.2 Incident Module Events

**IncidentCreatedEvent**:
```java
package io.monosense.synergyflow.incident;

import java.time.Instant;

/**
 * Published when a new incident is created.
 * Consumed by: Task module (projection), Audit module, Time module (projection)
 */
public record IncidentCreatedEvent(
    String incidentId,      // INC-1234
    String title,
    String description,
    Priority priority,      // LOW, MEDIUM, HIGH, CRITICAL
    Severity severity,      // S1, S2, S3, S4
    String assignedTo,      // Nullable
    Instant slaDeadline,
    Instant createdAt,
    String createdBy,
    String correlationId
) {
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Severity { S1, S2, S3, S4 }
}
```

**IncidentResolvedEvent**:
```java
package io.monosense.synergyflow.incident;

import java.time.Instant;

/**
 * Published when an incident is resolved.
 * Consumed by: Task module (update projection), Audit module
 */
public record IncidentResolvedEvent(
    String incidentId,
    String resolvedBy,
    Instant resolvedAt,
    String resolutionNotes,
    String correlationId
) {}
```

**IncidentStatusChangedEvent**:
```java
package io.monosense.synergyflow.incident;

import java.time.Instant;

/**
 * Published when incident status changes (e.g., ASSIGNED → IN_PROGRESS).
 * Consumed by: Task module (update projection), Audit module
 */
public record IncidentStatusChangedEvent(
    String incidentId,
    String oldStatus,
    String newStatus,
    String changedBy,
    Instant changedAt,
    String correlationId
) {}
```

### 8.3 Change Module Events

**ChangeRequestedEvent**:
```java
package io.monosense.synergyflow.change;

import java.time.Instant;
import java.util.List;

/**
 * Published when a change request is created.
 * Consumed by: Incident module (projection), Audit module
 */
public record ChangeRequestedEvent(
    String changeId,          // CHANGE-890
    String title,
    Risk risk,                // LOW, MEDIUM, HIGH, EMERGENCY
    List<String> impactedServices,
    String requestedBy,
    Instant requestedAt,
    String relatedIncidentId, // Nullable
    String correlationId
) {
    public enum Risk { LOW, MEDIUM, HIGH, EMERGENCY }
}
```

**ChangeApprovedEvent**:
```java
package io.monosense.synergyflow.change;

import java.time.Instant;

/**
 * Published when a change request is approved.
 * Consumed by: Incident module (update projection), Audit module
 */
public record ChangeApprovedEvent(
    String changeId,
    String approvedBy,
    Instant approvedAt,
    String approvalReason,
    String correlationId
) {}
```

### 8.4 Task Module Events

**TaskCreatedEvent**:
```java
package io.monosense.synergyflow.task;

import java.time.Instant;

/**
 * Published when a new task is created.
 * Consumed by: Incident module (projection), Time module (projection), Audit module
 */
public record TaskCreatedEvent(
    String taskId,            // TASK-890
    String projectId,
    String title,
    String assignedTo,        // Nullable
    Instant createdAt,
    String relatedIncidentId, // Nullable
    String relatedChangeId,   // Nullable
    String correlationId
) {}
```

**TaskCompletedEvent**:
```java
package io.monosense.synergyflow.task;

import java.time.Instant;

/**
 * Published when a task is marked as done.
 * Consumed by: Incident module (update projection), Audit module
 */
public record TaskCompletedEvent(
    String taskId,
    String completedBy,
    Instant completedAt,
    String correlationId
) {}
```

### 8.5 Time Tracking Module Events

**TimeEntryLoggedEvent**:
```java
package io.monosense.synergyflow.time;

import java.time.Instant;
import java.util.List;

/**
 * Published when time is logged.
 * Consumed by: Incident module (update worklogs), Task module (update worklogs), Audit module
 */
public record TimeEntryLoggedEvent(
    String timeEntryId,
    String userId,
    int timeSpentMinutes,
    String description,
    List<TargetEntity> targetEntities,
    Instant loggedAt,
    String correlationId
) {
    public record TargetEntity(
        String entityType,  // "incident" or "task"
        String entityId     // "INC-1234" or "TASK-890"
    ) {}
}
```

### 8.6 Knowledge Module Events

**ArticlePublishedEvent**:
```java
package io.monosense.synergyflow.knowledge;

import java.time.Instant;
import java.util.List;

/**
 * Published when a knowledge article is published.
 * Consumed by: Audit module, Search indexer (future)
 */
public record ArticlePublishedEvent(
    String articleId,      // KB-0042
    String title,
    List<String> tags,
    String publishedBy,
    Instant publishedAt,
    String correlationId
) {}
```

### 8.7 Event Type Registry

Spring Modulith automatically tracks event types using fully qualified class names:

```sql
-- Example: event_publication table content
SELECT event_type, COUNT(*) as count
FROM event_publication
WHERE completion_date IS NULL
GROUP BY event_type;

-- Result:
-- event_type                                                        | count
-- -----------------------------------------------------------------+-------
-- io.monosense.synergyflow.incident.IncidentCreatedEvent          | 3
-- io.monosense.synergyflow.task.TaskCreatedEvent                  | 7
-- io.monosense.synergyflow.time.TimeEntryLoggedEvent              | 12
```

**Type-Safe Event Handling**:
- Java compiler validates event field access
- IDE autocomplete for event properties
- Refactoring tools update all event usages
- No runtime schema drift

---

_End of Part 4. Continue to Security Architecture..._

## 9. Security Architecture

### 9.1 Authentication Flow (OAuth2 + JWT)

**Authentication Sequence**:
```
1. User → Browser: Navigate to app.synergyflow.io
2. Frontend → IdP: Redirect to IdP login (OAuth2 authorization endpoint)
3. User → IdP: Enter credentials
4. IdP → Frontend: Return JWT token (RS256 signed)
5. Frontend: Store JWT in memory (not localStorage)
6. Frontend → Gateway: API request with Authorization: Bearer {jwt}
7. Gateway → IdP: Validate JWT signature (JWKS endpoint)
8. Gateway → Backend: Forward request with validated claims
9. Backend: Extract user context from JWT claims
```

**JWT Token Structure**:
```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-id-1"
  },
  "payload": {
    "sub": "user-456",
    "email": "alice@example.com",
    "name": "Alice Smith",
    "roles": ["AGENT", "INCIDENT_MANAGER"],
    "teams": ["team-support", "team-ops"],
    "iss": "https://idp.example.com",
    "aud": "synergyflow-api",
    "exp": 1697554800,
    "iat": 1697551200
  },
  "signature": "..."
}
```

**JWT Validation**:
- Algorithm: RS256 (asymmetric)
- Signature verification: IdP public key (JWKS endpoint)
- Expiration check: `exp` claim < current time
- Audience check: `aud` == "synergyflow-api"
- Issuer check: `iss` == configured IdP URL

### 9.2 Authorization Flow (OPA Policy Engine)

**Authorization Sequence**:
```
1. Backend: Intercept secured endpoint
2. Backend: Build OPA input from JWT claims + resource context
3. Backend → OPA: POST localhost:8181/v1/data/synergyflow/authorization/allow
4. OPA: Evaluate Rego policy bundle
5. OPA → Backend: Return decision {allow: true/false, reason: "..."}
6. Backend: Log decision to audit pipeline
7. Backend: Proceed or reject (403 Forbidden)
```

**OPA Policy Pattern** (Rego):
```rego
package synergyflow.authorization
default allow := false
allow if {
    input.action == "update"
    input.resource.ownerId == input.user.id  # Owner can update
}
# Full policy bundle: see appendix
```

**OPA Authorization Manager Pattern** (Spring Security):
```java
@Component
public class OpaAuthorizationManager implements AuthorizationManager<MethodInvocation> {
    public AuthorizationDecision check(Supplier<Authentication> auth, MethodInvocation invocation) {
        OpaResponse response = opaClient.postForObject(
            "http://localhost:8181/v1/data/synergyflow/authorization/decision",
            Map.of("input", buildInput(auth, invocation)),  OpaResponse.class);
        auditService.logPolicyDecision(response.getDecision());
        return new AuthorizationDecision(response.isAllow());
    }
}
```

**See Also:** [appendix-09-security-opa-policies.md](./appendix-09-security-opa-policies.md) for:
- Complete OPA policy bundle
- Full AuthorizationManager implementation
- Decision receipt structures
- Shadow mode testing procedures

### 9.3 Audit Pipeline

**Audit Event Structure**:
```sql
INSERT INTO audit_events (
  entity_type,
  entity_id,
  action,
  user_id,
  correlation_id,
  occurred_at,
  payload_json,
  signature
) VALUES (
  'incident',
  'INC-1234',
  'update',
  'user-456',
  'cor-abc-123',
  '2025-10-17T14:00:00Z',
  '{"changes": {"status": {"from": "NEW", "to": "ASSIGNED"}}}',
  'hmac-sha256-signature'
);
```

**Tamper-Proof Logging**:
```java
public String generateSignature(AuditEvent event, String secretKey) {
    String payload = String.format("%s:%s:%s:%s:%s",
        event.getEntityType(),
        event.getEntityId(),
        event.getAction(),
        event.getUserId(),
        event.getOccurredAt()
    );
    
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));
    byte[] signature = hmac.doFinal(payload.getBytes());
    
    return Base64.getEncoder().encodeToString(signature);
}
```

**Audit Query API**:
```
GET /api/v1/audit/events?entityType=incident&entityId=INC-1234&startDate=2025-10-01

Response:
{
  "events": [
    {
      "id": "audit-1",
      "entityType": "incident",
      "entityId": "INC-1234",
      "action": "create",
      "userId": "user-456",
      "occurredAt": "2025-10-17T14:00:00Z",
      "payload": {...},
      "signature": "..."
    },
    {
      "id": "audit-2",
      "action": "update",
      "occurredAt": "2025-10-17T14:15:00Z",
      ...
    }
  ]
}
```

### 9.4 Secrets Management

**1Password Integration** (ExternalSecrets Operator):
```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: synergyflow-db-credentials
  namespace: synergyflow
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: onepassword-backend
    kind: SecretStore
  target:
    name: synergyflow-db-secret
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: synergyflow-production
      property: username
  - secretKey: password
    remoteRef:
      key: synergyflow-production
      property: password
```

**Secret Usage** (Application):
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres-cluster:5432/synergyflow
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Kubernetes Pod**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: synergyflow-backend
spec:
  template:
    spec:
      containers:
      - name: backend
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: synergyflow-db-secret
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: synergyflow-db-secret
              key: password
```

### 9.5 API Gateway Security (Envoy)

**JWT Validation** (SecurityPolicy):
```yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: SecurityPolicy
metadata:
  name: jwt-validation
  namespace: synergyflow
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: api-route
  jwt:
    providers:
    - name: keycloak
      issuer: https://idp.example.com/realms/synergyflow
      audiences:
      - synergyflow-api
      remoteJWKS:
        uri: https://idp.example.com/realms/synergyflow/protocol/openid-connect/certs
        timeout: 5s
      claimToHeaders:
      - header: X-User-Id
        claim: sub
      - header: X-User-Roles
        claim: roles
```

**Rate Limiting** (BackendTrafficPolicy):
```yaml
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: BackendTrafficPolicy
metadata:
  name: rate-limit
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: api-route
  rateLimit:
    type: Local
    local:
      rules:
      - limit:
          requests: 100
          unit: Minute
        clientSelectors:
        - headers:
          - name: X-User-Id
            type: Distinct
```

---

## 10. Deployment Architecture

### 10.1 Kubernetes Deployment Overview

**Namespace Structure**:
- `synergyflow`: SynergyFlow application (backend, frontend, DragonflyDB cache)
- `cnpg-system`: Shared PostgreSQL cluster for platform applications (including SynergyFlow)
- `monitoring`: Observability stack (Victoria Metrics, Grafana)
- `flux-system`: GitOps management

**High Availability Strategy**:
- Multi-zone deployment (3 availability zones)
- Pod anti-affinity (spread across zones)
- Automatic failover for stateful components
- Graceful shutdown (60s termination grace period)
- Rolling updates (maxSurge=1, maxUnavailable=0)

### 10.2 Application Deployment

**Backend Deployment** (`synergyflow-infra/base/backend/deployment.yaml`):

Key deployment patterns:
- **High Availability**: 3 replicas with pod anti-affinity across zones
- **Rolling Updates**: Zero-downtime deployments (maxSurge=1, maxUnavailable=0)
- **Resource Management**: 500m-2000m CPU, 1.5-2Gi RAM per pod
- **Health Checks**: Liveness (60s delay, 10s interval) and Readiness (30s delay, 5s interval) probes via Spring Boot Actuator
- **Graceful Shutdown**: 60s termination grace period with 10s preStop hook
- **Sidecar Pattern**: OPA container (100m-500m CPU, 128-256Mi RAM) for policy evaluation
- **Secrets Management**: Database credentials from Kubernetes Secrets via External Secrets Operator
- **Observability**: Prometheus metrics, OpenTelemetry tracing, JSON structured logging

Deployment structure:
```yaml
Deployment: synergyflow-backend
├── Replicas: 3 (scales to 10 under load via HPA)
├── Containers:
│   ├── backend (Spring Boot app)
│   │   ├── Image: harbor.synergyflow.io/synergyflow/backend:1.0.0
│   │   ├── Port: 8080 (HTTP)
│   │   └── Probes: /actuator/health/{liveness,readiness}
│   └── opa-sidecar (Policy engine)
│       ├── Image: openpolicyagent/opa:0.68.0
│       └── Port: 8181 (localhost policy evaluation)
└── Volumes: opa-policies (ConfigMap)
```

Full manifest available in infrastructure repository (see Section 4.5.3).

**Frontend Deployment Pattern**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: synergyflow-frontend
spec:
  replicas: 3  # High availability
  template:
    spec:
      containers:
      - name: frontend
        image: harbor.synergyflow.io/synergyflow/frontend:1.0.0
        resources: {requests: {cpu: 250m, memory: 512Mi}, limits: {cpu: 500m, memory: 1Gi}}
# Full manifest: see appendix-10-deployment-kubernetes.md
```

**Spring Modulith Configuration** (`src/main/resources/application.yml`):

Key configuration patterns:
- **Spring Modulith**: Event publication registry with JDBC-based transactional outbox, explicitly-annotated module detection
- **Database**: PostgreSQL via CloudNative-PG pooler (20 max connections, 10 min idle), Hikari connection pool, Flyway migrations
- **MyBatis-Plus**: Automatic mapper scanning, lambda query support, pagination/optimistic locking/multi-tenant plugins
- **Cache**: DragonflyDB shared cluster (Lettuce client, 8 max connections, 2s timeout)
- **Workflow**: Flowable async executor (4-10 threads, 100 queue size)
- **Event Processing**: 5s completion check interval, 5m timeout, 5 retries with 2x backoff
- **Secrets**: Database and cache credentials from Kubernetes Secrets via environment variables

Configuration structure:
```yaml
spring.modulith:          # Transactional outbox + module boundaries
spring.datasource:        # PostgreSQL pooler connection
mybatis-plus.global-config: # MyBatis-Plus global configuration
mybatis-plus.configuration: # MyBatis configuration (map-underscore-to-camel-case)
spring.data.redis:        # DragonflyDB shared cluster
flowable.async-executor:  # BPMN workflow processing (4-10 threads)
synergyflow.events:       # Event retry/backoff configuration
```

Full configuration in backend repository (see Section 4.5.1).

**MyBatis-Plus Plugins Configuration Pattern**:

MyBatis-Plus 3.5.9+ requires plugins for pagination, locking, and security. Configuration pattern:

```java
@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
```

**See Also:** [appendix-04-component-data-access.md](./appendix-04-component-data-access.md) for:
- Multi-tenant configuration
- Illegal SQL prevention
- Full plugin configuration reference

**Plugin Details**:

| Plugin | Purpose | Requirement | Configuration |
|--------|---------|-------------|---------------|
| PaginationInnerInterceptor | Automatic pagination for `selectPage()` | mybatis-plus-jsqlparser 5.0+ | DbType, maxLimit |
| OptimisticLockerInnerInterceptor | Optimistic locking via `@Version` field | None | Auto-detects @Version |
| TenantLineInnerInterceptor | Multi-tenant row-level isolation | TenantLineHandler | tenant_id injection |
| DataPermissionInterceptor | Data permission filtering (RBAC) | Custom DataPermissionHandler | Role-based filtering |
| DynamicTableNameInnerInterceptor | Dynamic table name replacement | TableNameHandler | Table name mapping |
| DataChangeRecorderInnerInterceptor | Data change audit trail | None | Auto-records changes |
| IllegalSQLInnerInterceptor | Detect dangerous SQL (e.g., `UNION`, nested subqueries) | None | Default rules |
| BlockAttackInnerInterceptor | Block full-table update/delete without WHERE | None | Auto-blocks attacks |

**Gradle Dependencies** (`build.gradle.kts`):
```kotlin
dependencies {
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.14")
    implementation("com.baomidou:mybatis-plus-jsqlparser:5.0")  // Required for v3.5.9+
}
```

**Spring Modulith Module Structure**:
```java
// Main Application
@SpringBootApplication
@EnableModulith
public class SynergyFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(SynergyFlowApplication.class, args);
    }
}

// Module definition (e.g., IncidentModule)
@org.springframework.modulith.ApplicationModule
package io.monosense.synergyflow.incident;

// Event publication configuration
@Configuration
public class EventPublicationConfiguration {

    @Bean
    public CompletionMode completionMode() {
        return CompletionMode.builder()
            .on(Throwable.class)
            .retry(5)
            .exponentialBackoff(Duration.ofSeconds(2), 2.0)
            .build();
    }
}
```

### 10.3 Stateful Components

**PostgreSQL Database Access** (Shared Cluster + PgBouncer Pooler):

SynergyFlow uses the shared multi-tenant PostgreSQL cluster (`shared-postgres` in `cnpg-system` namespace) via a dedicated PgBouncer pooler for connection management. This follows the established platform pattern used by all other applications (gitlab, harbor, keycloak, mattermost).

**Architecture Pattern**:
- **Shared Cluster**: `shared-postgres` (PostgreSQL 16.8, 3 instances, 2-4 CPU, 8-16Gi memory)
- **Managed Role**: `synergyflow_app` with connection limit of 50
- **PgBouncer Pooler**: `synergyflow-pooler` (3 instances, transaction mode)
- **Database**: `synergyflow` with 7 schemas (users, incidents, changes, knowledge, tasks, audit, workflows)

**PgBouncer Pooler Configuration Pattern**:
```yaml
apiVersion: postgresql.cnpg.io/v1
kind: Pooler
metadata:
  name: synergyflow-pooler
spec:
  cluster:
    name: shared-postgres
  instances: 3
  pgbouncer:
    poolMode: transaction  # For Spring Boot + Flowable
    parameters:
      default_pool_size: "50"
      max_client_conn: "1000"
# Full config: see appendix-10-deployment-kubernetes.md
```

**Connection String**:
```
jdbc:postgresql://synergyflow-pooler-rw.cnpg-system.svc.cluster.local:5432/synergyflow
```

**Schema Organization** (within synergyflow database):
```
synergyflow database
├── synergyflow_users (Foundation)
├── synergyflow_incidents
├── synergyflow_changes
├── synergyflow_knowledge
├── synergyflow_tasks
├── synergyflow_audit
├── synergyflow_workflows (Flowable)
└── event_publication (Spring Modulith Outbox)
```

**Monitoring**:
- **Pooler metrics**: Via PodMonitor on port 9127
  - `pgbouncer_stats_queries_total` - Query throughput
  - `pgbouncer_stats_client_connections_waiting` - Connection queue depth
  - `pgbouncer_pools_server_connections` - Backend connection usage
- **PostgreSQL metrics**: From shared-postgres cluster via port 9187
  - `cnpg_pg_replication_lag` - Replication lag in bytes
  - `cnpg_pg_stat_database_*` - Database statistics (filtered by synergyflow database)
  - `cnpg_backends_*` - Connection pool statistics
  - `cnpg_up` - Cluster health status

**Benefits of Shared Cluster Pattern**:
- **Resource efficiency**: 50% CPU savings, 87% memory savings vs dedicated cluster
- **Operational simplicity**: Leverage existing backup, monitoring, and upgrade procedures
- **Platform consistency**: Same pattern as gitlab, harbor, keycloak, mattermost
- **Multi-cluster architecture**: Database services in infra cluster, applications in apps cluster

**DragonflyDB Cache** (Shared Cluster):

SynergyFlow uses the existing shared DragonflyDB cluster deployed in the `dragonfly-system` namespace, following the same multi-tenant pattern as PostgreSQL.

**Existing Infrastructure** (Deployed via GitOps):
- **Cluster**: `dragonfly` StatefulSet in `dragonfly-system` namespace
- **Replicas**: 3 (cluster mode for high availability)
- **Service**: `dragonfly.dragonfly-system.svc.cluster.local:6379`
- **Authentication**: Password-protected via `dragonfly-auth` secret
- **Storage**: Persistent volumes for data durability

**SynergyFlow Connection Configuration**:
```yaml
spring:
  data:
    redis:
      host: dragonfly.dragonfly-system.svc.cluster.local
      port: 6379
      password: ${DRAGONFLY_PASSWORD}  # From external secret
      database: 0  # Dedicated database index for SynergyFlow
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

**Usage Patterns**:
- User profile caching (1-hour TTL)
- Policy decision caching (10-minute TTL)
- Query result caching (30-second TTL)
- Session data (optional, if not using sticky sessions)

**Benefits of Shared Cache Pattern**:
- **Resource efficiency**: No dedicated cache infrastructure required
- **Operational simplicity**: Leverage existing monitoring and backup
- **Platform consistency**: Same multi-tenant pattern as GitLab, Harbor, Keycloak, Mattermost
- **Database isolation**: Each application uses dedicated database index

### 10.4 GitOps with Flux CD

**GitRepository Pattern**:
```yaml
apiVersion: source.toolkit.fluxcd.io/v1
kind: GitRepository
metadata:
  name: synergyflow
  namespace: flux-system
spec:
  interval: 1m
  url: https://github.com/monosense/synergyflow-infra
  ref:
    branch: main
# Full config: see appendix-10-deployment-kubernetes.md
```

**Kustomization Pattern**:
```yaml
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: synergyflow-production
spec:
  interval: 5m
  path: ./overlays/production
  sourceRef:
    kind: GitRepository
    name: synergyflow
# Full config with health checks: see appendix-10
```
  - apiVersion: postgresql.cnpg.io/v1
    kind: Cluster
    name: synergyflow-db
    namespace: synergyflow
```

**Deployment Process**:
1. Developer pushes code to GitHub
2. CI/CD builds Docker image, pushes to Harbor
3. CI/CD updates Kustomize overlay with new image tag
4. Flux detects git change, applies to cluster
5. Kubernetes performs rolling update
6. Health checks validate new pods
7. Old pods terminated after new pods ready

---

_End of Part 5. Continue to remaining sections..._
