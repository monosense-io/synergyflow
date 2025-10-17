# Project Workflow Analysis

**Date:** 2025-10-17
**Project:** SynergyFlow
**Analyst:** monosense

## Assessment Results

### Project Classification

- **Project Type:** Web Application (Full-stack platform)
- **Project Level:** Level 4 (Platform/Ecosystem)
- **Instruction Set:** instructions-lg.md (Large project PRD + Epics + Architect handoff)

### Scope Summary

- **Brief Description:** Unified ITSM+PM platform with intelligent workflow automation. Feature parity with ManageEngine ServiceDesk Plus (ITSM) + JIRA (PM), enhanced with event-driven architecture, policy-based automation (OPA), and workflow orchestration (Flowable 7.x). Target: 250-1,000 users, small to mid-market organizations.
- **Estimated Stories:** 40+ stories (MVP: 15 core features across 6 epics, Full: 16 epics total)
- **Estimated Epics:** 16 epics (Epic-01 Incident/Problem through Epic-16 Deployment/Ops)
- **Timeline:** 10-week MVP for Trust + UX Foundation Pack, 6-12 months for Phase 2, 1-2 years long-term vision

### Context

- **Greenfield/Brownfield:** Greenfield (New project with initial backend/frontend bootstrap completed: stories 16.06, 16.07, 16.08, 16.09)
- **Existing Documentation:** Product Brief (comprehensive, 1,238 lines covering Executive Summary, Problem Statement, Solution, Target Users, Goals, MVP Scope, Technical Architecture, Financial Impact, Risks, Post-MVP Vision)
- **Team Size:** 6-person cross-functional team (3 Developers: Platform/ITSM/PM specialists + QA + DevOps + UX)
- **Deployment Intent:** Self-hosted Kubernetes deployment initially, Indonesia data residency focus, SaaS evolution later

## Recommended Workflow Path

### Primary Outputs

1. **PRD (Product Requirements Document)** - Hybrid approach:
   - MVP scope (Trust + UX Foundation Pack) with full implementation detail
   - Phase 2 and long-term features at strategic level
   - Target: Implementation-ready for 10-week sprint + strategic context for full platform

2. **Epic Breakdown Document** - Detailed epic descriptions:
   - All 16 epics with user stories, acceptance criteria, dependencies
   - MVP epics (Epic-01, 02, 03, 05, 16) in full detail
   - Phase 2 epics at high level for architect planning

3. **Architect Handoff Package** (via 3-solutioning workflow):
   - System architecture document
   - Technical specifications per epic
   - Integration patterns and event contracts
   - Infrastructure requirements

### Workflow Sequence

**Phase 1: PRD Creation (Current Step)**
1. Load instructions-lg.md (Level 4 PRD instructions)
2. Generate PRD from Product Brief with hybrid detail level
3. Create Epic breakdown document
4. Validate PRD against checklist

**Phase 2: Architecture & Technical Design** (Recommended next step)
1. Invoke 3-solutioning workflow for architect assessment
2. Generate system architecture document
3. Create technical specifications for MVP epics
4. Define event contracts and API specifications

**Phase 3: Story Creation** (After architecture)
1. Create detailed user stories for MVP epics
2. Define acceptance criteria and test scenarios
3. Estimate story points and dependencies
4. Generate sprint planning backlog

### Next Actions

1. ✅ Load PRD template (prd-template.md)
2. ✅ Generate PRD sections from Product Brief
3. ✅ Save PRD with hybrid detail level
4. ✅ Load epics template (epics-template.md)
5. ✅ Generate epic breakdown document
6. ✅ Validate PRD against checklist
7. ⏭️ Handoff to Architect for 3-solutioning workflow

## Special Considerations

**MVP Focus (10-Week Sprint):**
- Trust + UX Foundation Pack: 15 core features
- Event Passport Implementation (CloudEvents + Avro)
- Single-Entry Time Tray
- Link-on-Action (Cross-Module Workflows)
- Freshness Badges (Projection Lag Visibility)
- Policy Studio MVP + Decision Receipts
- Core ITSM modules: Incident, Change, Knowledge
- Core PM modules: Project, Task, Sprint
- Foundation infrastructure: Kafka, Schema Registry, Flowable, OPA, PostgreSQL

**Technical Complexity Factors:**
- Event-driven architecture with eventual consistency
- Policy-based automation requiring explainability
- Workflow orchestration with timer-based SLA tracking
- Schema evolution and backward compatibility
- Multi-module integration patterns
- High availability and observability requirements

**Deferral Strategy:**
- Phase 2: Impact Orchestrator (self-optimizing workflows), Self-Healing Engine, Work Graph Intelligence, Advanced CMDB
- Long-term: Fully autonomous operations, conversational workflow OS, real-time business impact engine

**Risk Mitigation:**
- Flowable timer scalability validated through load testing
- OPA policy complexity managed via shadow mode testing
- Event-driven operational complexity addressed via GitOps and monitoring
- Schema evolution protected by Confluent Schema Registry

## Technical Preferences Captured

**Backend Stack:**
- Language: Java 21+
- Framework: Spring Boot 3.5+
- Architecture: Spring Modulith 1.4+ (modular monolith with event-driven modules)

**Data Tier:**
- Primary Database: PostgreSQL 16 (CloudNative-PG operator for HA)
- Cache: DragonflyDB (Redis-compatible)
- Event Backbone: Apache Kafka 3.8+ (Strimzi operator, KRaft mode)
- Schema Registry: Confluent Community Edition 7.8+

**Workflow and Automation:**
- Workflow Engine: Flowable 7.1.0+ (embedded mode)
- Policy Engine: Open Policy Agent (OPA) 0.68+ (sidecar mode)
- Event Pattern: CloudEvents 1.0 + Avro schema-first

**Frontend Stack:**
- Framework: Next.js 14+ (React 18+, TypeScript)
- UI Library: Shadcn/ui + Tailwind CSS
- State Management: TanStack Query (React Query) for server state
- Authentication: OAuth2/JWT (NextAuth.js)

**Infrastructure:**
- Container Platform: Kubernetes (cloud-agnostic)
- GitOps: Flux CD with Kustomize overlays
- Monitoring: Victoria Metrics + Grafana
- Secrets Management: Vault (External Secrets Operator)
- Image Registry: Harbor (private registry, deployed)

**Architecture Patterns:**
- Event-Driven Integration: No shared database, Kafka event backbone
- CQRS: Command-Query Responsibility Segregation with projection lag visibility
- Event Passport: CloudEvents envelope + Avro payload with schema versioning
- High Availability: 3-replica PostgreSQL, 3-broker Kafka, 3-replica application pods

---

_This analysis serves as the routing decision for the adaptive PRD workflow and will be referenced by future orchestration workflows._
