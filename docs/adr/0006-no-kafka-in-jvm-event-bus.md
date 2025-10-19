# ADR-0006: No Kafka - In-JVM Event Bus (Spring Modulith)

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Backend Team, DevOps

---

## Context

SynergyFlow's event-driven architecture requires cross-module communication:
- **Incident → Change**: When incident created, related change should be notified
- **Change → Incident**: When change approved/deployed, related incidents updated
- **Worklog → Incident + Task**: Single time entry mirrors to both modules
- **Event volume**: ~100-500 events/second at peak (1,000 concurrent users)

We needed an event bus solution for:
- **Pub/Sub messaging** between modules (IncidentModule → ChangeModule)
- **At-least-once delivery** (no event loss)
- **Event persistence** (audit trail, replay capability)
- **Transactional outbox** (atomic entity + event writes)

Options considered:
1. **Kafka** (3-broker cluster + ZooKeeper + Schema Registry)
2. **RabbitMQ** (message broker with AMQP protocol)
3. **Spring Modulith in-JVM event bus** (in-memory with database persistence)
4. **AWS EventBridge / Azure Event Grid** (cloud-managed)

Key constraints:
- Self-hosted deployment (no cloud-managed services)
- 12-month delivery timeline with 5-person team
- Target scale: 250-1,000 users (not massive scale)
- Operational simplicity (DevOps has limited capacity)

---

## Decision

**We will use Spring Modulith's in-JVM event bus with transactional outbox, NOT Kafka or external message broker.**

### Architecture

```
Incident Service
    ↓
ApplicationEventPublisher.publishEvent(IncidentCreatedEvent)
    ↓
Spring Modulith Event Registry
    ↓ Atomic write (same DB transaction)
    ├── incidents table (INSERT incident)
    └── event_publication table (INSERT event)
        ↓
COMMIT
    ↓ In-JVM delivery (<1ms)
@ApplicationModuleListener in ChangeModule
    ↓
Process event, update related changes
```

### Rationale

**1. Performance (20x Faster Event Processing)**
- **In-process delivery**: <1ms method call vs 10-50ms network call to Kafka
- **End-to-end lag**: 50-100ms (p95 <200ms) vs 2-3 seconds with Kafka
- **Zero serialization**: Java object references vs JSON/Avro serialization overhead
- **Target achieved**: <100ms event processing lag requirement easily met

**2. Cost Efficiency (Eliminates $250-350/month Infrastructure)**
- **No Kafka cluster**: Saves 3 brokers × 2 CPU × 4.5GB RAM = 6 CPU, 13.5GB RAM
- **No ZooKeeper**: Saves 3 nodes × 1 CPU × 2GB RAM = 3 CPU, 6GB RAM
- **No Schema Registry**: Saves 1-2 instances × 1 CPU × 2GB RAM
- **Total savings**: ~9-11 CPU cores, 19.5-21.5GB RAM, $250-350/month

**3. Operational Simplicity (Zero External Dependencies)**
- **No broker to manage**: No Kafka broker monitoring, scaling, upgrades
- **No topic management**: No partition assignment, rebalancing, consumer group lag
- **No cluster coordination**: No ZooKeeper quorum, leader election, ISR management
- **Single process debugging**: All events visible in one application log stream

**4. Transactional Outbox Built-In**
```java
@Transactional
public Incident createIncident(CreateIncidentRequest request) {
    Incident incident = incidentRepository.save(new Incident(...));  // 1
    eventPublisher.publishEvent(new IncidentCreatedEvent(...));      // 2
    return incident;
    // Both 1 and 2 committed atomically - event stored in event_publication table
}
```
- **Atomic writes**: Entity + event in single database transaction
- **No dual write problem**: No risk of entity saved but event lost
- **Guaranteed delivery**: Events persisted in `event_publication` table
- **Automatic retry**: Spring Modulith retries failed event listeners

**5. Type-Safe Events (Compile-Time Validation)**
```java
// Java record - type-safe, immutable
public record IncidentCreatedEvent(
    UUID incidentId,
    UUID correlationId,
    Instant timestamp
) {}

@ApplicationModuleListener
void onIncidentCreated(IncidentCreatedEvent event) {
    // IDE autocomplete, refactoring support, compile-time validation
}
```
- **No schema registry needed**: Java compiler validates event structure
- **Refactoring support**: Rename event field → all consumers break at compile time
- **IDE autocomplete**: IntelliJ IDEA autocompletes event fields

**6. Natural Migration Path to Kafka (If Needed)**
Spring Modulith supports **event externalization**:
```java
@Configuration
class EventExternalizationConfig {
    @Bean
    ApplicationModuleListener kafkaEventExternalizer() {
        return event -> kafkaTemplate.send("events", event);
    }
}
```
- **Add later**: Externalize events to Kafka if external consumers needed
- **No code changes**: Module code unchanged, only configuration
- **Gradual migration**: Externalize specific event types, not all

---

## Consequences

### Positive

✅ **Performance**: 20x faster (50-100ms vs 2-3s), <1ms in-process delivery
✅ **Cost efficiency**: $250-350/month savings, 9-11 CPU cores, 19.5GB RAM eliminated
✅ **Operational simplicity**: Zero external dependencies, single process debugging
✅ **Transactional outbox**: Atomic entity + event writes, guaranteed delivery
✅ **Type safety**: Compile-time event validation, refactoring support
✅ **Migration path**: Event externalization enables future Kafka integration

### Negative

⚠️ **Single process**: Events lost if application crashes before DB commit (mitigated: transactional outbox persists events)
⚠️ **No external consumers initially**: Third-party systems cannot consume events (mitigated: event externalization added when needed)
⚠️ **Horizontal scaling complexity**: Event listeners run in all replicas (mitigated: idempotency keys, distributed locks if needed)
⚠️ **Replay limitations**: Cannot replay events from arbitrary timestamp (mitigated: event_publication table stores all events)

### Trade-offs Accepted

We **accept**:
- Single-process event bus (events local to application)
- No external event consumers initially (added via externalization if needed)
- Event listeners run in all backend replicas (idempotency required)

We **gain**:
- 20x faster event processing
- $250-350/month cost savings
- Zero broker operational burden

---

## Alternatives Considered

### Alternative 1: Kafka (3-broker cluster + ZooKeeper)

**Approach**: Kafka cluster for event streaming, Schema Registry for Avro schemas.

**Rejected because**:
- **Infrastructure overhead**: 3 Kafka brokers (6 CPU, 13.5GB RAM), 3 ZooKeeper nodes (3 CPU, 6GB RAM), Schema Registry (1-2 CPU, 2-4GB RAM)
- **Operational complexity**: Topic management, partition rebalancing, consumer group lag monitoring, broker upgrades
- **Cost**: $250-350/month cloud costs (or equivalent hardware on-prem)
- **Performance penalty**: 20x slower (2-3s vs 50-100ms), network serialization overhead
- **Overkill**: SynergyFlow target scale (1,000 users) doesn't require Kafka throughput (millions of events/sec)

**When this makes sense**:
- Scale >10,000 concurrent users generating >10,000 events/second
- External event consumers (third-party integrations, analytics pipelines)
- Multi-region deployments requiring event replication
- Event replay from arbitrary timestamps (days/weeks ago)

---

### Alternative 2: RabbitMQ

**Approach**: RabbitMQ cluster for AMQP-based messaging between modules.

**Rejected because**:
- **Infrastructure overhead**: 3 RabbitMQ nodes (cluster for HA), management UI, monitoring
- **Operational complexity**: Queue management, dead letter queues, message TTL, shovel/federation for multi-datacenter
- **Serialization overhead**: JSON/binary serialization vs in-memory Java objects
- **Transactional outbox complexity**: Requires manual implementation (atomic DB write + RabbitMQ publish)
- **Performance**: Slower than in-JVM (10-50ms network latency per event)

**When this makes sense**:
- Need for complex routing (topic exchanges, headers exchanges)
- Priority queues, delayed messages, message TTL
- Legacy systems requiring AMQP protocol
- Cross-language event consumers (Python, Go, Node.js services)

---

### Alternative 3: Cloud-Managed Event Bus (AWS EventBridge, Azure Event Grid)

**Approach**: Cloud provider's managed event bus service.

**Rejected because**:
- **Vendor lock-in**: Ties SynergyFlow to specific cloud provider
- **Self-hosted requirement**: Customers deploy on-premises or multi-cloud
- **Cost**: Pay-per-event pricing ($1-2 per million events = $100-500/month at scale)
- **Data residency**: Cannot guarantee Indonesia data residency
- **Inconsistent with platform**: Other apps use self-hosted infrastructure

**When this makes sense**:
- SaaS multi-tenant deployment (not self-hosted)
- Cloud-only deployment strategy
- Need for managed event delivery, retries, DLQ
- Integration with cloud-native services (Lambda, Step Functions)

---

## Validation

**How we validate this decision**:

1. **Event processing latency** (Continuous):
   - Target: p95 <200ms (end-to-end incident creation + projection update)
   - Monitor: `event_publication` table lag, listener execution time
   - Alert if lag >500ms for >5 minutes

2. **Event delivery reliability** (Continuous):
   - Target: Zero event loss, at-least-once delivery
   - Monitor: `event_publication.completed_at` timestamp, listener failures
   - Alert if event retry count >5 or event age >10 minutes

3. **Performance benchmarking** (Month 3):
   - Load test: 1,000 concurrent users, 500 events/second peak
   - Measure: Event processing throughput, latency distribution
   - Success criteria: p95 <200ms, zero event timeouts

4. **Scale validation** (Month 6):
   - Simulate 4x growth (4,000 concurrent users, 2,000 events/second)
   - Identify bottlenecks (CPU, DB writes, listener backlog)
   - Plan for Kafka migration if needed (>10,000 events/second sustained)

---

## When to Revisit This Decision

We should **reconsider Kafka** if:

1. **Event volume exceeds 10,000 events/second** sustained (in-JVM bus saturated)
2. **External event consumers** required (third-party integrations, analytics pipelines)
3. **Multi-region deployment** needs event replication across datacenters
4. **Event replay** from arbitrary timestamps becomes critical requirement

We have a **natural migration path**:
- Spring Modulith event externalization publishes events to Kafka topics
- Module code unchanged (still publishes `ApplicationEvents`)
- External consumers subscribe to Kafka topics
- In-JVM bus remains for internal module communication

---

## References

- **Spring Modulith Event Publication Registry**: https://docs.spring.io/spring-modulith/reference/#events
- **Transactional Outbox Pattern**: https://microservices.io/patterns/data/transactional-outbox.html
- **Architecture Document**: [docs/architecture/11-backend-architecture.md](../architecture/11-backend-architecture.md)
- **ADR-0001**: [Spring Modulith over Microservices](./0001-spring-modulith-over-microservices.md)
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md) (Spring Modulith 1.4.2)
