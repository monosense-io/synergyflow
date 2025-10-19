# ADR-0001: Spring Modulith over Microservices

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Engineering Team

---

## Context

SynergyFlow is an integrated ITSM+PM platform requiring:
- **Event-driven architecture** for cross-module communication (Incident ↔ Change ↔ Task)
- **Module isolation** to prevent tight coupling between domains
- **Transactional consistency** for core workflows (e.g., incident creation + event publication must be atomic)
- **Small team efficiency** (5-person team: 3 backend, 1 frontend, 1 DevOps)
- **Operational simplicity** for self-hosted deployment

We needed to choose between:
1. **Microservices architecture** with separate services per domain
2. **Modular monolith** with enforced module boundaries
3. **Traditional layered monolith** without module isolation

Key constraints:
- Target scale: 250-1,000 users (not massive scale requiring microservices)
- Self-hosted Kubernetes deployment (customers manage infrastructure)
- 12-month delivery timeline with small team
- Need for cross-module transactions and data consistency

---

## Decision

**We will use Spring Modulith 1.4.2 as a modular monolith architecture, NOT microservices.**

### Rationale

**1. Operational Simplicity (Critical for 5-Person Team)**
- Single deployable artifact (one JAR file)
- No distributed system complexity (no service mesh, API gateway between services, circuit breakers)
- Simplified debugging (single process, single log stream)
- No distributed tracing overhead (OpenTelemetry optional, not mandatory)
- Reduced DevOps burden (1 application to deploy vs 7-10 microservices)

**2. Performance (20x Faster Event Processing)**
- **In-process communication**: <1ms method calls vs 10-50ms HTTP/gRPC calls
- **In-JVM event bus**: 50-100ms end-to-end event processing vs 2-3 seconds with Kafka
- **No network latency**: Zero serialization/deserialization overhead for internal events
- **Target met**: <100ms event processing lag (p95 <200ms) easily achievable

**3. Cost Efficiency (Eliminates $250-350/month Infrastructure)**
- **No Kafka cluster**: Saves 3 brokers × 2 CPU × 4.5GB RAM = 6 CPU, 13.5GB RAM
- **No ZooKeeper**: Saves 3 nodes × 1 CPU × 2GB RAM = 3 CPU, 6GB RAM
- **No Schema Registry**: Saves 1-2 instances × 1 CPU × 2GB RAM
- **Total savings**: ~9-11 CPU cores, 19.5-21.5GB RAM, $250-350/month cloud costs

**4. Development Velocity (5-Person Team Can Deliver in 12 Months)**
- Single codebase, single repo, single build
- Refactoring across modules is trivial (IDE refactoring tools work)
- No API versioning between services (internal APIs are Java interfaces)
- Shared test infrastructure (Testcontainers for integration tests)
- Faster onboarding for new developers (one codebase to learn)

**5. Module Isolation Equivalent to Microservices**
- **Spring Modulith enforces boundaries**: Modules cannot directly access each other's internals
- **Compile-time verification**: `@ApplicationModule` annotations + verification tests
- **Event-driven integration**: Modules communicate via `ApplicationEvents` (same pattern as microservices)
- **Canonical IDs propagate**: Incident ID, Change ID, Task ID flow across module boundaries via events
- **Later migration path**: Spring Modulith event externalization enables Kafka migration if needed

**6. Transactional Consistency (Critical for ITSM Data Integrity)**
- **Atomic writes**: Entity + event publication in single database transaction (transactional outbox pattern)
- **No eventual consistency complexity**: Change approval + related incident update can be transactional
- **No distributed transactions**: Avoids 2-phase commit, saga patterns, compensation logic
- **Data integrity guarantee**: Zero state loss, zero orphaned records

---

## Consequences

### Positive

✅ **Operational simplicity**: Single deployable artifact, simplified debugging, reduced infrastructure
✅ **Performance**: 20x faster event processing (50-100ms vs 2-3s), <1ms in-process communication
✅ **Cost efficiency**: $250-350/month savings, 9-11 CPU cores, 19.5GB RAM eliminated
✅ **Development velocity**: 5-person team can deliver complete product in 12 months
✅ **Module isolation**: Spring Modulith enforces boundaries equivalent to microservices
✅ **Transactional consistency**: Atomic entity + event writes, no distributed transactions
✅ **Migration path**: Event externalization enables future Kafka migration if scale requires

### Negative

⚠️ **Single deployment unit**: Cannot deploy modules independently (mitigated: rolling updates, blue-green deployments)
⚠️ **Shared database**: All modules in one PostgreSQL database (mitigated: separate schemas per module, connection pooling)
⚠️ **Vertical scaling only initially**: Horizontal scaling requires load balancer + session affinity (mitigated: stateless application, JWT auth)
⚠️ **Module discipline required**: Developers must respect module boundaries (mitigated: Spring Modulith verification tests in CI)

### Trade-offs Accepted

We **accept**:
- Single deployment unit (all modules deploy together)
- Shared database with schema-per-module isolation
- Initial vertical scaling (horizontal scaling added later if needed)

We **gain**:
- 20x faster event processing
- $250-350/month cost savings
- 5-person team can deliver in 12 months
- Zero distributed system complexity

---

## Alternatives Considered

### Alternative 1: Microservices with Kafka

**Approach**: Separate Spring Boot services for Incident, Change, Task, User modules with Kafka for event streaming.

**Rejected because**:
- **Operational complexity**: Requires Kafka (3 brokers + ZooKeeper), API gateway, service mesh, distributed tracing
- **Infrastructure overhead**: 9-11 additional CPU cores, 19.5GB RAM, $250-350/month
- **Team burden**: 5-person team cannot manage 7-10 microservices effectively
- **Performance penalty**: 20x slower event processing (2-3s vs 50-100ms)
- **Development velocity**: Cross-service refactoring requires API versioning, coordination

**When this makes sense**:
- Team size >20 engineers
- Scale >10,000 concurrent users
- Need for independent service deployment by separate teams
- External event consumers require real-time streaming

---

### Alternative 2: Traditional Layered Monolith

**Approach**: Standard Spring Boot application with Controller → Service → Repository layers, no module boundaries.

**Rejected because**:
- **No module isolation**: Any service can call any other service, leading to tight coupling
- **Scalability bottleneck**: Cannot later migrate to microservices without full rewrite
- **Event-driven architecture missing**: No first-class event support, harder to implement cross-module workflows
- **Technical debt accumulation**: Becomes "big ball of mud" over time

**When this makes sense**:
- Very small applications (1-2 domain entities)
- Short-lived projects (3-6 months)
- No plan for future scale or modularization

---

### Alternative 3: Microservices with REST-Only Communication

**Approach**: Microservices without Kafka, using synchronous REST calls between services.

**Rejected because**:
- **Synchronous coupling**: Service A directly calls Service B, creating tight runtime coupling
- **Cascading failures**: If Change service is down, Incident service cannot create related changes
- **Retry complexity**: Requires circuit breakers, retries, fallback logic
- **Performance**: Network calls add 10-50ms latency per hop
- **No event-driven benefits**: Loses event sourcing, audit trail, temporal decoupling

**When this makes sense**:
- Simple CRUD applications with minimal cross-service interaction
- Very stable service boundaries (no future refactoring expected)

---

## Validation

**How we validate this decision**:

1. **Performance benchmarking** (Month 2):
   - Target: Event processing lag <100ms (p95 <200ms)
   - Measure: `event_publication` table lag, end-to-end incident creation + projection update
   - Success criteria: ≥95% of events processed in <100ms

2. **Module isolation testing** (Continuous):
   - Spring Modulith verification tests in CI pipeline
   - Fail build if module boundaries violated
   - Manual architecture reviews every sprint

3. **Load testing** (Month 6):
   - Target: 1,000 concurrent users
   - Measure: API latency p95 <200ms, throughput >500 req/sec
   - Success criteria: Single instance handles 1,000 users without degradation

4. **Operational metrics** (Production):
   - Mean Time To Deploy (MTTD) <10 minutes
   - Mean Time To Recovery (MTTR) <30 minutes
   - Zero state loss incidents

---

## When to Revisit This Decision

We should **reconsider microservices** if:

1. **Scale exceeds 10,000 concurrent users** and single-instance vertical scaling is exhausted
2. **Team grows to >20 engineers** requiring independent module ownership and deployment
3. **External event consumers** (third-party integrations) require real-time event streaming via Kafka
4. **Regulatory requirements** mandate physical separation of certain modules

We have a **natural migration path**:
- Spring Modulith event externalization can publish events to Kafka
- Module boundaries already defined and enforced
- Refactor modules into separate services incrementally (strangler fig pattern)

---

## References

- **Spring Modulith Documentation**: https://docs.spring.io/spring-modulith/reference/
- **Architecture Document**: [docs/architecture/11-backend-architecture.md](../architecture/11-backend-architecture.md)
- **PRD Section**: "Architecture Philosophy - Modular Monolith" (docs/PRD.md, lines 36-43)
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md) (Spring Modulith 1.4.2)
