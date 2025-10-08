# Distributed Systems Development Checklist

**Version:** 1.0
**Created:** 2025-10-08
**Owner:** Architecture Team
**Purpose:** Prevent critical review findings and reduce iteration cycles for distributed system implementations

---

## Background

This checklist addresses the 6 most common critical findings identified during Epic 1 Stories 1.5-1.7 code reviews. These patterns represent systemic issues in distributed systems development that can be prevented through disciplined application of this checklist during implementation and review.

**Mandatory Usage:** Apply this checklist to ALL stories involving:
- Event-driven architectures
- Distributed transactions
- Real-time data streaming (SSE, WebSocket)
- Asynchronous processing
- Inter-service communication
- Distributed data consistency

---

## 1. Idempotency

### ✅ Checklist Items

- [ ] **Database-level idempotency enforcement**
  - All INSERT operations use database constraints (UNIQUE, PRIMARY KEY) or `ON CONFLICT` clauses
  - No application-level "check-then-insert" patterns (race condition vulnerable)
  - UPDATE operations include WHERE clauses with version numbers, timestamps, or state constraints
  - Example: `UPDATE outbox SET status = 'PROCESSING' WHERE id = ? AND status = 'PENDING'`

- [ ] **Idempotency keys for external operations**
  - HTTP clients include idempotency tokens in headers (e.g., `Idempotency-Key`)
  - Event handlers use event IDs as natural idempotency keys
  - Duplicate event detection uses database constraints, not in-memory caches

- [ ] **State machine enforcement**
  - All status transitions validated at database level
  - Invalid state transitions rejected by WHERE clause failures
  - Audit trail captures attempted vs. successful transitions

### 🚫 Anti-Patterns to Avoid

```java
// ❌ WRONG: Race condition between check and insert
if (!repository.existsByEventId(eventId)) {
    repository.save(new ProcessedEvent(eventId));
}

// ✅ CORRECT: Database constraint enforcement
try {
    repository.save(new ProcessedEvent(eventId));
} catch (DuplicateKeyException e) {
    // Already processed, safe to ignore
    log.debug("Event {} already processed", eventId);
}
```

---

## 2. Protocol Compliance

### ✅ Checklist Items

- [ ] **SSE/WebSocket specification adherence**
  - Event IDs follow `{domain}-{timestamp}-{sequence}` format for replay
  - `Last-Event-ID` header processed on reconnection
  - Event types use hierarchical naming: `ticket.created`, `ticket.status_changed`
  - Data field contains valid JSON (no raw strings)

- [ ] **Reconnection mechanisms**
  - Server stores missed events for configurable window (default: 5 minutes)
  - Client sends `Last-Event-ID` on reconnect
  - Server replays missed events in order before resuming live stream
  - Graceful degradation if event window exceeded (client full refresh)

- [ ] **HTTP/2 and keep-alive**
  - SSE connections use HTTP/2 or HTTP/1.1 with `Connection: keep-alive`
  - Heartbeat/ping messages every 30 seconds to prevent proxy timeouts
  - Timeout configuration: read timeout > ping interval

### 📋 Implementation Reference

```java
// Event ID generation
String eventId = String.format("%s-%d-%d",
    domain,
    Instant.now().toEpochMilli(),
    sequenceGenerator.next()
);

// SSE event structure
SseEmitter.SseEventBuilder event = SseEmitter.event()
    .id(eventId)
    .name("ticket.status_changed")
    .data(payload, MediaType.APPLICATION_JSON)
    .reconnectTime(5000L);
```

---

## 3. Authorization Patterns

### ✅ Checklist Items

- [ ] **Filter-before-broadcast architecture**
  - Authorization filters applied BEFORE adding to broadcast queues
  - No sensitive data reaches unauthorized channels
  - Filtering logic uses same authorization service as REST APIs

- [ ] **Per-user event streams**
  - Each SSE connection tagged with authenticated user context
  - Events filtered by user permissions before transmission
  - No reliance on client-side filtering

- [ ] **Attribute-based access control (ABAC)**
  - Events carry minimal context (IDs only, not full payloads)
  - Receiver performs fresh authorization check if full data needed
  - Time-bound tokens for event stream access

### 🔒 Security Pattern

```java
// ✅ CORRECT: Filter before broadcast
public void broadcastTicketUpdate(TicketEvent event) {
    userSessions.forEach((userId, emitter) -> {
        if (authService.canAccessTicket(userId, event.getTicketId())) {
            emitter.send(event);
        }
    });
}

// ❌ WRONG: Broadcasting then expecting client filtering
public void broadcastTicketUpdate(TicketEvent event) {
    allEmitters.forEach(emitter -> emitter.send(event));
    // Relies on client to ignore unauthorized events
}
```

---

## 4. Performance Validation

### ✅ Checklist Items

- [ ] **Load testing in acceptance criteria**
  - Every story with async/event features includes performance AC
  - Minimum requirements:
    - Concurrent users: 100 (or 10x expected peak)
    - Event throughput: 1000 events/second
    - Response time: p95 < 200ms, p99 < 500ms
    - Memory stability: No leaks over 1-hour test

- [ ] **Benchmarking setup**
  - Performance tests automated in CI/CD (can be tagged for selective runs)
  - Tools: JMeter, Gatling, or K6 for load generation
  - Metrics collection: Prometheus + Grafana or Micrometer

- [ ] **Resource limits configured**
  - Connection pools sized appropriately (DB, HTTP clients)
  - Thread pools use bounded queues (reject policy documented)
  - Event buffer sizes prevent unbounded memory growth

### 📊 Load Test Template

```yaml
# Example K6 script parameters
scenarios:
  sse_streaming:
    executor: ramping-vus
    startVUs: 0
    stages:
      - duration: 2m, target: 50  # Ramp up
      - duration: 5m, target: 100 # Sustain
      - duration: 2m, target: 0   # Ramp down

thresholds:
  http_req_duration: ['p95<200', 'p99<500']
  http_req_failed: ['rate<0.01']
```

---

## 5. Concurrency

### ✅ Checklist Items

- [ ] **Thread-safe data structures**
  - Use `ConcurrentHashMap`, `CopyOnWriteArrayList`, or `Collections.synchronized*`
  - No manual synchronization unless unavoidable (prefer higher-level constructs)
  - Document synchronization strategy in class Javadoc

- [ ] **Non-blocking I/O**
  - Async HTTP clients (WebClient, AsyncHttpClient) for external calls
  - CompletableFuture or Project Reactor for async orchestration
  - Never block virtual/platform threads with `.get()` or `.block()`

- [ ] **Reactive streams backpressure**
  - Flux/Mono publishers honor subscriber demand
  - Backpressure strategy documented (buffer, drop, error)
  - Bounded buffers prevent memory exhaustion

### ⚡ Concurrency Patterns

```java
// ✅ Thread-safe session management
private final ConcurrentHashMap<String, SseEmitter> sessions = new ConcurrentHashMap<>();

public void addSession(String userId, SseEmitter emitter) {
    sessions.put(userId, emitter);
    emitter.onCompletion(() -> sessions.remove(userId));
    emitter.onTimeout(() -> sessions.remove(userId));
}

// ✅ Non-blocking event processing
@Async("eventExecutor")
public CompletableFuture<Void> processEvent(DomainEvent event) {
    return CompletableFuture.runAsync(() -> {
        // Processing logic
    }, eventExecutor);
}
```

---

## 6. Retry & Backoff

### ✅ Checklist Items

- [ ] **Exponential backoff implementation**
  - Retry delays: 1s, 2s, 4s, 8s, 16s (max 5 attempts typical)
  - Jitter added to prevent thundering herd: `delay * (0.5 + random(0.5))`
  - Maximum retry duration configured (e.g., 2 minutes total)

- [ ] **Circuit breaker pattern**
  - Use Resilience4j or similar library
  - Configuration:
    - Failure threshold: 50% over 10 requests
    - Wait duration in open state: 30 seconds
    - Half-open state: Allow 3 test requests
  - Circuit breaker per downstream dependency

- [ ] **Dead letter queue (DLQ)**
  - Failed events moved to DLQ after max retries
  - DLQ processing: manual review, automated replay, or alert
  - Retention policy: 7 days minimum for troubleshooting

### 🔄 Retry Configuration Example

```java
@Bean
public RetryTemplate outboxRetryTemplate() {
    return RetryTemplate.builder()
        .maxAttempts(5)
        .exponentialBackoff(1000, 2.0, 32000)
        .retryOn(TransientDataAccessException.class)
        .traversingCauses()
        .build();
}

@Bean
public CircuitBreaker eventGatewayCircuitBreaker() {
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .slidingWindowSize(10)
        .build();

    return CircuitBreaker.of("eventGateway", config);
}
```

---

## Application Workflow

### Development Phase

1. **Story Kickoff**
   - Review checklist with entire team
   - Identify which sections apply to the story
   - Add relevant checklist items to story acceptance criteria

2. **Implementation**
   - Developer self-reviews using checklist before creating PR
   - Unit tests validate checklist requirements (e.g., idempotency tests)

3. **Code Review**
   - Reviewer explicitly verifies each applicable checklist item
   - PR template includes: "Distributed Systems Checklist: [Applied/Not Applicable]"

4. **Testing**
   - QA runs load tests defined in performance validation section
   - Chaos engineering tests (optional): Network delays, service failures

### Epic 2 Story 2.1 Success Criteria

- [ ] Checklist applied during sprint planning
- [ ] All applicable items addressed in implementation
- [ ] Code review completed with zero checklist violations
- [ ] Performance tests pass defined thresholds
- [ ] Reduced review iterations: ≤2 rounds (vs. 3-4 in Epic 1)

---

## Lessons Learned from Epic 1

| Finding | Story | Root Cause | Checklist Prevention |
|---------|-------|------------|---------------------|
| Race condition in outbox processing | 1.5 | Application-level duplicate check | Section 1: DB-level idempotency |
| SSE reconnection data loss | 1.6 | No Last-Event-ID handling | Section 2: Protocol compliance |
| Unauthorized event exposure | 1.6 | No pre-broadcast filtering | Section 3: Authorization patterns |
| Memory leak in SSE connections | 1.6 | Unbounded session map | Section 5: Concurrency |
| Event processing failures unhandled | 1.7 | No retry mechanism | Section 6: Retry & backoff |
| Performance degradation under load | 1.7 | No load testing in AC | Section 4: Performance validation |

---

## Maintenance

- **Review Cycle:** Quarterly or after each epic
- **Updates:** Add new patterns from post-mortems and retrospectives
- **Ownership:** Architecture team maintains, all teams contribute
- **Metrics:** Track review iteration reduction and production incident correlation

---

## Quick Reference Card

```
🎯 Before PR Submission:
□ Idempotency: DB constraints, not app logic
□ Protocol: Event IDs, reconnection, proper format
□ AuthZ: Filter before broadcast
□ Performance: Load test passing
□ Concurrency: Thread-safe structures
□ Retry: Exponential backoff + circuit breaker

🔍 During Code Review:
□ Check anti-patterns section
□ Verify test coverage for concurrency
□ Confirm performance AC exists
□ Review error handling paths

🚀 Before Story Completion:
□ Load tests automated
□ DLQ monitoring configured
□ Runbook updated for new failure modes
```

---

**Document Status:** Active
**Next Review:** After Epic 2 Story 2.3 completion
**Feedback:** Submit improvements via architecture review meeting or PR to this document
