# Description, Context and Goals

## Project Description

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

## Deployment Intent

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

## Context

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

## Strategic Goals

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
