# ADR-0008: Flowable for Workflow Orchestration

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Backend Team

---

## Context

SynergyFlow requires workflow orchestration for:
- **SLA timers**: 4-hour incident resolution SLA, pre-breach notifications at 80% threshold
- **Approval workflows**: Change approval routing (manual, policy-driven, CAB)
- **Escalation flows**: SLA breach auto-escalation to manager
- **Multi-step processes**: Service request fulfillment workflows

Requirements:
- **BPMN 2.0 support**: Visual workflow modeling for business analysts
- **Embedded in Spring Boot**: No separate workflow server
- **Durable state**: Workflow state persists across application restarts
- **Timer support**: Schedule jobs for SLA tracking, pre-breach notifications
- **Async execution**: Non-blocking job executor for background tasks

Options:
1. **Flowable** (BPMN 2.0 engine, Spring Boot embedded)
2. **Camunda** (BPMN 2.0 engine, separate server or embedded)
3. **Temporal** (durable execution platform)
4. **Custom workflow** (Quartz Scheduler + state machine)

---

## Decision

**We will use Flowable 7.1.0 embedded in Spring Boot, NOT Camunda or Temporal.**

### Rationale

**1. Embedded in Spring Boot (Zero Operational Overhead)**
```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter</artifactId>
    <version>7.1.0</version>
</dependency>
```
- **No separate server**: Flowable runs in Spring Boot application process
- **Automatic autoconfiguration**: Spring Boot manages Flowable lifecycle
- **Shared database**: Flowable tables (`ACT_*`) in same PostgreSQL database

**2. BPMN 2.0 Visual Modeler**
- **Visual workflow design**: Business analysts can design workflows without coding
- **Flowable Modeler**: Web-based BPMN modeler (optional)
- **Code-first option**: Developers can write workflows in Java (FluentAPI)

**3. Timer-Based SLA Tracking**
```xml
<timerEventDefinition>
  <timeDuration>PT4H</timeDuration> <!-- 4 hours -->
</timerEventDefinition>
```
- **BPMN timers**: Schedule SLA deadlines, pre-breach notifications
- **Async job executor**: 4-10 threads, 100 queue size
- **Durable state**: Timer state persists in database, survives restarts

**4. Durable Workflow State**
- **Database persistence**: All workflow state in `ACT_RU_EXECUTION`, `ACT_RU_TASK` tables
- **Survives restarts**: Application restart doesn't lose workflow state
- **Cluster support**: Multiple backend replicas can execute workflows (async job executor lock)

**5. Spring Integration**
```java
@Autowired
private RuntimeService runtimeService;

runtimeService.startProcessInstanceByKey("incident-sla-timer", variables);
```
- **Spring-managed beans**: Inject Flowable services into Spring components
- **Transaction management**: Flowable participates in Spring `@Transactional`
- **Event integration**: Flowable can publish Spring ApplicationEvents

**6. Lightweight (No Microservices Overhead)**
- **Shared database**: No separate Flowable database, uses SynergyFlow PostgreSQL
- **In-process execution**: No network calls to workflow server
- **Resource efficient**: Minimal overhead (async job executor threads only)

---

## Consequences

### Positive

✅ **Embedded**: Zero operational overhead, no separate workflow server
✅ **BPMN 2.0**: Visual workflow modeling, business analyst friendly
✅ **Timer support**: SLA tracking, pre-breach notifications, escalation flows
✅ **Durable state**: Survives application restarts, cluster-safe
✅ **Spring integration**: Autowired services, transaction participation

### Negative

⚠️ **Database tables**: 20+ Flowable tables (`ACT_*`) in shared database
⚠️ **Learning curve**: Team must learn BPMN 2.0 modeling
⚠️ **Complex workflows**: Visual modeler can become complex for 20+ step workflows

---

## Alternatives Considered

### Alternative 1: Camunda

**Rejected because**: Requires separate Camunda server (or Camunda Cloud), more operational overhead, similar features to Flowable.

**When to use**: Need Camunda Cockpit (advanced workflow monitoring UI), enterprise support required.

### Alternative 2: Temporal

**Rejected because**: Requires separate Temporal server (Go-based), gRPC communication overhead, learning curve for Temporal API.

**When to use**: Need for durable execution (long-running workflows spanning days/weeks), microservices orchestration.

### Alternative 3: Quartz Scheduler + Custom State Machine

**Rejected because**: Manual state machine implementation, no visual modeling, no BPMN standard, harder to maintain.

**When to use**: Very simple timer-based tasks (daily backups, cleanup jobs), no workflow complexity.

---

## Validation

- **SLA timer accuracy**: Measure timer precision (target ±5 seconds)
- **Workflow performance**: Measure workflow start/complete latency (target <100ms)
- **Durable state**: Test application restart during workflow execution

---

## References

- **Flowable Documentation**: https://www.flowable.com/open-source/docs/
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md)
- **Architecture**: [docs/architecture/6-components.md](../architecture/6-components.md)
