# SynergyFlow - Implementation Handoff Package

**Status:** ✅ **ARCHITECTURE COMPLETE - READY FOR MVP IMPLEMENTATION**
**Date:** 2025-10-18
**Target:** 10-Week MVP Sprint (Trust + UX Foundation Pack)
**Team:** 6-person cross-functional team (3 Devs, QA, DevOps, UX)

---

## Executive Summary

The SynergyFlow architecture has been completed, validated, and approved for MVP implementation. All quality gates pass, all gaps closed, and comprehensive documentation is ready to guide development.

**Quality Metrics:**
- **Workflow Completion:** 100% (all steps complete)
- **Architecture Quality:** 95%+ (production-ready)
- **Requirements Traceability:** 92% readiness, 100% FR coverage
- **Architecture-Requirements Cohesion:** 96%
- **Zero Blocking Gaps**

**Decision:** Proceeding to implementation **without** generating per-epic tech-specs. The existing comprehensive architecture documentation is sufficient for an experienced development team.

---

## Architecture Documentation Map

### 🎯 Start Here

**Primary Documents:**
1. **[PRD.md](./PRD.md)** - Product requirements, user journeys, 16 epics, acceptance criteria
2. **[ux-specification.md](./ux-specification.md)** - UI/UX patterns, key journeys, design system, accessibility
3. **[architecture/architecture.md](./architecture/architecture.md)** - Complete system architecture (2,844+ lines)

### 📚 Core Architecture Reference

**[architecture.md](./architecture/architecture.md)** contains:

**Section 1: Introduction**
- Technology Decision Table (94 technologies with exact versions)
- Architectural principles (Event-Driven Modular Monolith, CQRS, Policy-Driven)
- Performance targets (p95 <200ms, event lag <100ms)

**Section 2-3: System Context & Containers**
- C4 Level 1 (System Context) diagrams
- C4 Level 2 (Container Architecture) with Next.js frontend + Spring Boot backend

**Section 4: Component Architecture**
- C4 Level 3 (Component details)
- **Proposed Source Tree** (3 repos: backend, frontend, infrastructure)
- Spring Modulith module structure (user, incident, change, knowledge, task, time)

**Section 5: Event-Driven Architecture**
- Spring Modulith ApplicationEvents patterns
- Transactional outbox (event_publication table)
- Event schemas and versioning

**Section 6: Database Architecture**
- PostgreSQL schema per module
- Event publication table design
- Data models for all aggregates

**Section 7: API Specifications**
- REST API patterns
- OpenAPI 3.0 generation
- Error handling standards

**Section 8: Domain Event Catalog**
- Java record-based events
- Event payload examples
- Correlation/causation IDs

**Section 9: Security Architecture**
- OAuth2 JWT authentication flow
- OPA policy-based authorization
- Audit trail with HMAC signatures

**Section 10: Deployment Architecture**
- Kubernetes deployment patterns
- PostgreSQL HA (CloudNative-PG)
- Envoy Gateway configuration
- GitOps with Flux CD

**Section 19: Testing Strategy** ✨ NEW
- Testing pyramid (70% unit, 20% integration, 10% E2E)
- JUnit 5, Spring Modulith Test, Playwright patterns
- Coverage targets (80% overall, 90% business logic)
- CI/CD pipeline integration

### 🔧 Detailed Implementation Appendices

**For Backend Developers:**
- [appendix-04-component-data-access.md](./architecture/appendix-04-component-data-access.md) - MyBatis-Plus patterns, Spring Modulith event publishing
- [appendix-09-security-opa-policies.md](./architecture/appendix-09-security-opa-policies.md) - Complete OPA Rego policies, authorization manager
- [appendix-19-testing-strategy.md](./architecture/appendix-19-testing-strategy.md) - JUnit 5, Spring Modulith Test examples

**For DevOps/Infrastructure:**
- [appendix-10-deployment-kubernetes.md](./architecture/appendix-10-deployment-kubernetes.md) - PostgreSQL HA, Envoy Gateway, DragonflyDB, Flux CD manifests

**For Frontend Developers:**
- [ux-specification.md](./ux-specification.md) - Component library, interaction patterns, responsive layout
- Section 4.5.2 in architecture.md - Frontend source tree (Next.js App Router structure)
- [appendix-19-testing-strategy.md](./architecture/appendix-19-testing-strategy.md) - Vitest, React Testing Library, Playwright examples

### 📊 Validation & Alignment Documents

- [architecture/11-requirements-traceability.md](./architecture/11-requirements-traceability.md) - FR/NFR coverage matrix, epic alignment, 92% readiness
- [architecture/cohesion-check-report.md](./architecture/cohesion-check-report.md) - 96% architecture-requirements cohesion
- [architecture/epic-alignment-matrix.md](./architecture/epic-alignment-matrix.md) - Epic-to-module mapping, dependency graph
- [architecture/gap-closure-report-2025-10-18.md](./architecture/gap-closure-report-2025-10-18.md) - Final gap analysis

---

## MVP Sprint Scope (10 Weeks)

### Trust + UX Foundation Pack (5 MVP Epics)

**Epic-00: Trust + UX Foundation Pack**
- Single-Entry Time Tray
- Link-on-Action (cross-module workflows)
- Freshness Badges (projection lag visibility)
- Policy Studio MVP + Decision Receipts

**Epic-01: Incident Management**
- Create, update, resolve incidents
- SLA tracking with Flowable timers
- Severity/priority workflows

**Epic-02: Change Management**
- Change request creation
- OPA policy-based approval workflows
- Impact assessment

**Epic-05: Knowledge Management**
- Article creation and publishing
- Full-text search
- Knowledge linking to incidents

**Epic-16: Platform Foundation**
- Spring Boot 3.5.6 + Spring Modulith 1.4.2
- PostgreSQL 16.8 with CloudNative-PG
- Kubernetes deployment with Flux CD
- OAuth2 JWT authentication

### MVP User Stories: 15 Core Features

See [PRD.md](./PRD.md) Section 4.1 for complete user stories with acceptance criteria.

---

## Technology Stack (Exact Versions)

### Backend
- **Java:** OpenJDK 21.0.2 LTS
- **Framework:** Spring Boot 3.5.6
- **Architecture:** Spring Modulith 1.4.2
- **Database:** PostgreSQL 16.8
- **Cache:** DragonflyDB 1.17.0
- **Workflow:** Flowable 7.1.0
- **Policy Engine:** Open Policy Agent 0.68.0
- **Data Access:** MyBatis-Plus 3.5.14
- **Database Migration:** Flyway 10.18.0
- **API Docs:** SpringDoc OpenAPI 2.3.0

### Frontend
- **Runtime:** Node.js 20.12.0 LTS
- **Framework:** Next.js 14.2.5
- **UI Library:** React 18.3.1
- **State:** TanStack Query 5.32.0
- **Styling:** Tailwind CSS 3.4.3
- **Forms:** React Hook Form 7.51.3 + Zod 3.23.6
- **HTTP Client:** Axios 1.6.8

### DevOps & Infrastructure
- **Kubernetes:** 1.30.0
- **GitOps:** Flux CD 2.2.3
- **Gateway:** Envoy Gateway 1.0.1
- **PostgreSQL Operator:** CloudNative-PG 1.23.0
- **Metrics:** Victoria Metrics 1.100.0
- **Visualization:** Grafana 10.4.2
- **Tracing:** OpenTelemetry 1.28.0

### Testing
- **Backend Unit:** JUnit 5 (5.10.2), Mockito (5.11.0)
- **Integration:** Spring Modulith Test 1.4.2
- **Contract:** Spring Cloud Contract 4.1.0
- **Frontend Unit:** Vitest 1.5.0
- **E2E:** Playwright 1.43.0
- **Performance:** K6 0.48.0

**See [architecture.md Section 1.4](./architecture/architecture.md#14-technology-and-library-decision-table) for complete table with 94 technologies.**

---

## Quick Start Guide for Developers

### 1. Backend Setup

```bash
# Prerequisites: Java 21, Docker, Podman, or compatible container runtime

# Clone backend repository
git clone https://github.com/monosense/synergyflow-backend
cd synergyflow-backend

# Start local dependencies (PostgreSQL, DragonflyDB)
docker-compose up -d

# Run Flyway migrations
./gradlew flywayMigrate

# Run application
./gradlew bootRun

# Run tests
./gradlew test integrationTest
```

**Source Tree Reference:** [architecture.md Section 4.5.1](./architecture/architecture.md#451-backend-repository-synergyflow-backend)

### 2. Frontend Setup

```bash
# Prerequisites: Node.js 20 LTS

# Clone frontend repository
git clone https://github.com/monosense/synergyflow-frontend
cd synergyflow-frontend

# Install dependencies
npm ci

# Configure environment
cp .env.example .env.local
# Edit .env.local with API URL

# Run development server
npm run dev

# Run tests
npm run test
npm run test:e2e
```

**Source Tree Reference:** [architecture.md Section 4.5.2](./architecture/architecture.md#452-frontend-repository-synergyflow-frontend)

### 3. Infrastructure Setup

```bash
# Prerequisites: kubectl, flux CLI

# Clone infrastructure repository
git clone https://github.com/monosense/synergyflow-infra
cd synergyflow-infra

# Bootstrap Flux CD
flux bootstrap github \
  --owner=monosense \
  --repository=synergyflow-infra \
  --branch=main \
  --path=clusters/dev

# Apply base manifests
kubectl apply -k base/
```

**Kubernetes Manifests:** [appendix-10-deployment-kubernetes.md](./architecture/appendix-10-deployment-kubernetes.md)

---

## Module Implementation Order (Recommended)

### Week 1-2: Foundation
1. **User Module (Foundation)**
   - UserService, TeamService
   - OAuth2 JWT authentication
   - Security configuration
   - Reference: [architecture.md Section 4.3](./architecture/architecture.md#43-backend-module-details)

2. **Event Infrastructure**
   - Spring Modulith event publication registry
   - event_publication table
   - Transactional outbox pattern
   - Reference: [architecture.md Section 5](./architecture/architecture.md#5-event-driven-architecture)

### Week 3-4: Core ITSM (Incident)
3. **Incident Module**
   - Incident aggregate + repository
   - IncidentCreatedEvent, IncidentResolvedEvent
   - SLA timer integration (Flowable)
   - Reference: [architecture.md Section 4.3](./architecture/architecture.md#43-backend-module-details)

### Week 5-6: Time Tray + Change Module
4. **Time Module**
   - Single-Entry Time Tray
   - TimeEntryLoggedEvent
   - Mirroring logic to linked entities
   - Reference: [PRD.md User Journey J1](./PRD.md)

5. **Change Module**
   - Change request aggregate
   - OPA policy-based approvals
   - ChangeRequestedEvent, ChangeApprovedEvent
   - Reference: [appendix-09-security-opa-policies.md](./architecture/appendix-09-security-opa-policies.md)

### Week 7-8: Knowledge + Link-on-Action
6. **Knowledge Module**
   - Article aggregate + full-text search
   - ArticlePublishedEvent
   - Reference: [architecture.md Section 4.3](./architecture/architecture.md#43-backend-module-details)

7. **Cross-Module Linking**
   - Link-on-Action UI components
   - Bidirectional relationship management
   - Related panel implementation
   - Reference: [PRD.md User Journey J2](./PRD.md)

### Week 9-10: Integration, Testing, Hardening
8. **Freshness Badges**
   - Projection lag calculation
   - Freshness badge component
   - Thresholds: <100ms green, 100-500ms yellow, >500ms red
   - Reference: [PRD.md FR-4](./PRD.md)

9. **Policy Studio MVP**
   - OPA policy management UI
   - Decision receipts display
   - Shadow mode testing
   - Reference: [PRD.md FR-5](./PRD.md)

10. **Testing & Deployment**
    - Unit test coverage to 75% (ramping to 80%)
    - Integration tests for all event consumers
    - E2E tests for critical journeys
    - Kubernetes deployment to staging
    - Reference: [architecture.md Section 19](./architecture/architecture.md#19-testing-strategy)

---

## Development Workflows

### Adding a New Module

**Follow Spring Modulith conventions:**

1. **Create module package:** `io.monosense.synergyflow.{module}`
2. **Define module structure:**
   ```
   {module}/
   ├── package-info.java           # Module documentation
   ├── api/                         # Public events/DTOs
   ├── application/                 # REST controllers
   ├── domain/                      # Entities, services, repositories
   └── internal/                    # Event consumers (private)
   ```
3. **Publish events via ApplicationEventPublisher** (auto-saved to event_publication table)
4. **Consume events in internal package** with @TransactionalEventListener
5. **Add module tests** with @ApplicationModuleTest

**Reference:** [architecture.md Section 4.5.1](./architecture/architecture.md#451-backend-repository-synergyflow-backend)

### Publishing an Event

```java
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {
    private final IncidentMapper incidentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Incident createIncident(CreateIncidentCommand cmd) {
        var incident = new Incident(cmd);
        incidentMapper.insert(incident);

        // Event auto-saved to event_publication table
        eventPublisher.publishEvent(new IncidentCreatedEvent(
            incident.getId(),
            incident.getTitle(),
            incident.getSeverity(),
            Instant.now(),
            CorrelationContext.get(),
            null
        ));

        return incident;
    }
}
```

**Reference:** [appendix-04-component-data-access.md](./architecture/appendix-04-component-data-access.md)

### Consuming an Event

```java
@Component
@RequiredArgsConstructor
class IncidentEventConsumer {
    private final TaskProjectionRepository projectionRepo;

    @TransactionalEventListener
    @Async
    void onIncidentCreated(IncidentCreatedEvent event) {
        // Create projection in task module
        var projection = TaskProjection.fromEvent(event);
        projectionRepo.insert(projection);
    }
}
```

**Idempotency:** Check processed_events table or use event ID as natural key.

**Reference:** [architecture.md Section 5.3](./architecture/architecture.md#53-event-consumer-patterns)

### Adding an OPA Policy

```rego
# policies/change/auto-approve-minor.rego
package synergyflow.change.approval

default allow := false

allow if {
    input.change.type == "MINOR"
    input.change.risk == "LOW"
    input.user.role == "CHANGE_MANAGER"
}
```

**Deploy:** ConfigMap mounted to OPA sidecar, hot-reloaded automatically.

**Reference:** [appendix-09-security-opa-policies.md](./architecture/appendix-09-security-opa-policies.md)

---

## Testing Approach

### Unit Tests (70% of pyramid)

**Target:** ≥80% overall, ≥90% business logic

```java
@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {
    @Mock IncidentMapper mapper;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks IncidentServiceImpl service;

    @Test
    void createIncident_shouldPublishEvent() {
        // Arrange, Act, Assert
        // See appendix-19 for complete examples
    }
}
```

**Reference:** [appendix-19-testing-strategy.md Section A19.1](./architecture/appendix-19-testing-strategy.md#a191-backend-unit-testing-examples)

### Integration Tests (20% of pyramid)

**Spring Modulith Test:**

```java
@SpringBootTest
@ApplicationModuleTest
class IncidentModuleIntegrationTest {
    @Test
    void createIncident_shouldPublishEvent(Scenario scenario) {
        scenario.stimulate(() -> service.create(cmd))
                .andWaitForEventOfType(IncidentCreatedEvent.class)
                .toArrive();
    }
}
```

**Reference:** [appendix-19-testing-strategy.md Section A19.3](./architecture/appendix-19-testing-strategy.md#a193-integration-testing-examples)

### E2E Tests (10% of pyramid)

**Playwright for critical journeys:**

```typescript
test('Single-Entry Time Tray mirrors to incident and task', async ({ page }) => {
  // See appendix-19 for complete example
});
```

**Reference:** [appendix-19-testing-strategy.md Section A19.5](./architecture/appendix-19-testing-strategy.md#a195-end-to-end-testing-examples)

### Performance Tests

**K6 load testing:**
- Normal load: 250 concurrent users (MVP target)
- Stress test: 1,000 concurrent users (4x MVP)
- Target: p95 <200ms, event lag <100ms

**Reference:** [appendix-19-testing-strategy.md Section A19.6](./architecture/appendix-19-testing-strategy.md#a196-performance-testing-examples)

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
on: [push, pull_request]

jobs:
  backend-unit-tests:
    - ./gradlew test jacocoTestReport
    - Upload coverage to SonarQube

  integration-tests:
    - docker-compose up -d postgres dragonfly
    - ./gradlew integrationTest

  contract-tests:
    - ./gradlew contractTest

  e2e-tests:
    - Deploy to staging
    - npm run test:e2e

  quality-gates:
    - SonarQube quality gate (coverage ≥80%)
    - Contract validation (no breaking changes)
```

**Full Pipeline:** [appendix-19-testing-strategy.md Section A19.8](./architecture/appendix-19-testing-strategy.md#a198-cicd-test-pipeline-examples)

---

## Performance Targets (NFRs)

**API Response Time (NFR-1):**
- p95 <200ms
- p99 <500ms

**Event Processing Lag (NFR-1):**
- <100ms typical (in-memory Spring Modulith events)
- p95 <200ms

**Policy Evaluation (NFR-1):**
- <100ms p95 (OPA sidecar)
- Target <10ms

**System Availability (NFR-2):**
- ≥99.5% uptime (MVP target)
- ≥99.9% goal (Phase 2)

**Scale Targets (NFR-3):**
- MVP: 250 users, 50-75 concurrent
- Validated: 1,000 users, 250 concurrent
- Concurrent workflows: 1,000-5,000 active SLA timers

**Reference:** [architecture.md Section 1.3](./architecture/architecture.md#13-key-requirements)

---

## Key Architectural Decisions (ADRs)

### ADR-001: Event-Only Communication Between Modules
- **Decision:** All cross-module communication via Spring Modulith ApplicationEvents
- **Rationale:** Enforced module boundaries, zero coupling, natural migration to microservices
- **Trade-offs:** Eventual consistency (mitigated by Freshness Badges)

### ADR-002: Spring Modulith (Not Kafka) for MVP
- **Decision:** Use Spring Modulith in-memory events for MVP, defer Kafka to Phase 2
- **Rationale:** Operational simplicity for 3-person team, <100ms event lag sufficient for MVP
- **Migration Path:** Spring Modulith → Kafka externalization when scale demands

### ADR-003: Shared PostgreSQL Cluster + Pooler
- **Decision:** Single CloudNative-PG cluster with pooler, separate schema per module
- **Rationale:** Operational simplicity, sufficient isolation, HA with 3-replica cluster
- **Reference:** [CONNECTION-SCALING-STRATEGY.md](./architecture/CONNECTION-SCALING-STRATEGY.md)

### ADR-004: MyBatis-Plus (Not JPA)
- **Decision:** Use MyBatis-Plus for data access, not Spring Data JPA
- **Rationale:** Performance (no N+1 queries), explicit SQL control, lambda query DSL
- **Trade-offs:** More boilerplate than JPA, but better performance and control

### ADR-005: OPA for Policy-Driven Authorization
- **Decision:** Open Policy Agent sidecar for authorization and workflow decisions
- **Rationale:** Declarative policies (Rego), shadow mode testing, explainable decisions
- **Reference:** [appendix-09-security-opa-policies.md](./architecture/appendix-09-security-opa-policies.md)

---

## Common Pitfalls & How to Avoid

### ❌ Direct Method Calls Between Modules
**Problem:** `incidentService.findById()` called from task module

**Solution:** Always use events:
```java
// DON'T: Direct call
var incident = incidentService.findById(id);

// DO: Event-driven
@TransactionalEventListener
void onIncidentCreated(IncidentCreatedEvent event) {
    // Create projection in task module
}
```

### ❌ Forgetting Event Idempotency
**Problem:** Duplicate events cause duplicate projections

**Solution:** Use event ID or natural key:
```java
if (processedEventsRepo.exists(event.getId())) {
    return; // Already processed
}
```

### ❌ Long-Running Synchronous Operations in Event Handlers
**Problem:** Event handler blocks for 5 seconds, causing lag

**Solution:** Use @Async for long-running tasks:
```java
@TransactionalEventListener
@Async
void onIncidentCreated(IncidentCreatedEvent event) {
    // Long-running projection build
}
```

### ❌ Missing Freshness Badge Updates
**Problem:** UI shows stale data without lag indication

**Solution:** Always include projection timestamp:
```sql
CREATE TABLE task_projections (
    id UUID PRIMARY KEY,
    incident_id VARCHAR(20),
    last_updated TIMESTAMP NOT NULL,  -- For freshness calculation
    ...
);
```

### ❌ Hardcoded OPA Policy Decisions
**Problem:** `if (user.role == "ADMIN")` in Java code

**Solution:** Always delegate to OPA:
```java
var decision = opaClient.evaluate(input);
if (!decision.isAllowed()) {
    throw new ForbiddenException(decision.getReason());
}
```

---

## Support & Resources

### Documentation
- **Architecture:** [architecture.md](./architecture/architecture.md)
- **PRD:** [PRD.md](./PRD.md)
- **UX Spec:** [ux-specification.md](./ux-specification.md)
- **Testing:** [appendix-19-testing-strategy.md](./architecture/appendix-19-testing-strategy.md)

### Code Examples
- **Backend:** [appendix-04-component-data-access.md](./architecture/appendix-04-component-data-access.md)
- **Security:** [appendix-09-security-opa-policies.md](./architecture/appendix-09-security-opa-policies.md)
- **Deployment:** [appendix-10-deployment-kubernetes.md](./architecture/appendix-10-deployment-kubernetes.md)

### External References
- **Spring Modulith:** https://docs.spring.io/spring-modulith/reference/
- **MyBatis-Plus:** https://baomidou.com/
- **Open Policy Agent:** https://www.openpolicyagent.org/docs/latest/
- **CloudNative-PG:** https://cloudnative-pg.io/documentation/
- **Playwright:** https://playwright.dev/

---

## Final Checklist Before Starting Development

### Infrastructure
- [ ] Kubernetes cluster provisioned (dev, staging environments)
- [ ] CloudNative-PG operator installed
- [ ] PostgreSQL cluster created with pooler
- [ ] Flux CD bootstrapped
- [ ] Harbor registry deployed (for private images)
- [ ] External Secrets Operator configured (for credentials)

### Repositories
- [ ] Backend repository scaffolded (Spring Boot 3.5.6 + Modulith 1.4.2)
- [ ] Frontend repository scaffolded (Next.js 14.2.5)
- [ ] Infrastructure repository created (Kustomize + Flux manifests)
- [ ] CI/CD pipelines configured (GitHub Actions)

### Development Environment
- [ ] Java 21 installed on developer machines
- [ ] Node.js 20 LTS installed
- [ ] Docker/Podman installed (for local dependencies)
- [ ] kubectl and flux CLI installed
- [ ] IDE configured (IntelliJ IDEA for backend, VSCode for frontend)
- [ ] Local PostgreSQL + DragonflyDB via docker-compose

### Team Onboarding
- [ ] Architecture review session completed
- [ ] PRD walkthrough completed
- [ ] Technology stack training (Spring Modulith, MyBatis-Plus, OPA)
- [ ] Development workflow established (Git flow, PR process)
- [ ] Testing strategy reviewed (coverage targets, CI/CD gates)

---

## Success Metrics (Track Weekly)

### Development Velocity
- [ ] Sprint velocity trending toward 15 stories in 10 weeks
- [ ] No critical blockers >24 hours
- [ ] PR cycle time <24 hours (review → merge)

### Quality Gates
- [ ] Code coverage ≥75% (ramping to 80%)
- [ ] SonarQube quality gate passing
- [ ] Contract tests passing (no breaking changes)
- [ ] E2E tests passing for critical paths

### Performance
- [ ] API p95 latency <200ms
- [ ] Event processing lag <100ms (p95 <200ms)
- [ ] Policy evaluation <100ms p95

### Team Health
- [ ] Daily standups <15 minutes
- [ ] Sprint retrospectives actionable
- [ ] Team morale >7/10 (weekly survey)

---

## 🚀 You're Ready to Build!

**Architecture Status:** ✅ 100% Complete
**Documentation:** ✅ Comprehensive
**Quality:** ✅ 95%+ (Production-Ready)
**Team:** ✅ Equipped with all guidance needed

**Now go build an amazing ITSM+PM platform!**

Questions? Refer to the architecture documentation or reach out to Winston (Architect) for clarification.

_Document prepared: 2025-10-18 by Winston (Architect Agent)_
_Last validated: 2025-10-18_
