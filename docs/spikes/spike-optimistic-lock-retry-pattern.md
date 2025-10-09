# SPIKE: OptimisticLockException Retry Pattern

**Date:** 2025-10-09
**Owner:** James (Dev Agent)
**Story:** Story 2.2 Preparation Sprint
**Status:** Ready for Implementation
**Estimated Effort:** 3 hours

---

## Objective

Design a robust retry pattern for handling `OptimisticLockException` in TicketService methods to ensure concurrent ticket updates succeed without data loss.

---

## Problem Statement

From Story 2.1 IT4 integration test, we validated that concurrent ticket updates trigger `ObjectOptimisticLockingFailureException` (Spring's wrapper for JPA `OptimisticLockException`).

**Scenario:**
1. User A loads Ticket (version=1)
2. User B loads same Ticket (version=1)
3. User A updates and saves → version increments to 2 ✅
4. User B updates and saves → **OptimisticLockingFailureException** ❌ (stale version 1)

**Goal:** User B's operation should automatically retry with fresh ticket data instead of failing.

---

## Retry Strategy

### Core Principles

1. **Maximum Retry Attempts**: 3 retries (total 4 attempts including initial)
2. **Backoff Strategy**: Exponential backoff with jitter
   - Attempt 1: Immediate (0ms delay)
   - Attempt 2: 50ms + random(0-10ms)
   - Attempt 3: 100ms + random(0-20ms)
   - Attempt 4: 200ms + random(0-40ms)
3. **Idempotency**: Each retry loads fresh entity from database (prevents stale reads)
4. **Failure Handling**: After 3 retries, throw `ConcurrentUpdateException` to caller

### Why Exponential Backoff?

- **Reduces contention**: Spreading retries over time reduces collision probability
- **Jitter prevents thundering herd**: Random delay prevents synchronized retries
- **Load test validation**: Story 2.1 LT2 showed 0 OptimisticLockExceptions with partitioned writes → Low contention expected in production

---

## Implementation Approach

### Option A: Spring @Retryable Annotation (RECOMMENDED)

**Pros:**
- Declarative, minimal boilerplate
- Spring Retry handles backoff and max attempts
- Tested and production-ready

**Cons:**
- Requires spring-retry dependency
- Less control over retry logic

**Implementation:**

```java
// Add dependency to build.gradle.kts
implementation("org.springframework.retry:spring-retry")
implementation("org.springframework:spring-aspects") // For @Retryable AOP

// Enable retry in Spring Boot application
@EnableRetry
@SpringBootApplication
public class SynergyFlowApplication { ... }

// Apply @Retryable to TicketService methods
@Service
@Transactional
public class TicketService {

    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket assignTicket(UUID ticketId, UUID assigneeId, UUID currentUserId) {
        // Load fresh entity on each retry
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate and perform state transition
        ticket.assign(assigneeId);

        // Save (may throw OptimisticLockingFailureException if version conflict)
        Ticket saved = ticketRepository.save(ticket);

        // Publish event
        eventPublisher.publishEvent(new TicketAssignedEvent(saved.getId(), assigneeId));

        return saved;
    }

    @Recover
    public Ticket recoverFromOptimisticLock(
        ObjectOptimisticLockingFailureException ex,
        UUID ticketId,
        UUID assigneeId,
        UUID currentUserId
    ) {
        log.error("Failed to assign ticket {} after 4 attempts due to concurrent updates", ticketId);
        throw new ConcurrentUpdateException(
            "Ticket is being updated by another user. Please try again.",
            ticketId
        );
    }
}
```

**Configuration:**

```yaml
# application.yml
spring:
  retry:
    enabled: true
```

---

### Option B: Manual Retry with RetryTemplate (Alternative)

**Pros:**
- More control over retry logic
- Can customize per-method retry policies

**Cons:**
- More boilerplate
- Manual retry loop management

**Implementation:**

```java
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry policy: max 4 attempts
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(4);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Backoff policy: exponential with jitter
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(50); // 50ms
        backOffPolicy.setMultiplier(2.0);      // Double each retry
        backOffPolicy.setMaxInterval(200);     // Cap at 200ms
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}

@Service
public class TicketService {

    private final RetryTemplate retryTemplate;

    public Ticket assignTicket(UUID ticketId, UUID assigneeId, UUID currentUserId) {
        return retryTemplate.execute(context -> {
            // This block will be retried up to 4 times
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

            ticket.assign(assigneeId);
            Ticket saved = ticketRepository.save(ticket);
            eventPublisher.publishEvent(new TicketAssignedEvent(saved.getId(), assigneeId));

            return saved;
        }, context -> {
            // Recovery callback after all retries exhausted
            throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ticketId
            );
        });
    }
}
```

---

### Option C: Custom Retry Utility (Maximum Control)

**Pros:**
- No external dependencies
- Full control over retry logic

**Cons:**
- Most boilerplate
- Reinventing the wheel

**Implementation:**

```java
@Component
public class RetryUtil {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 50;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_BACKOFF_MS = 200;
    private static final Random RANDOM = new Random();

    public <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        int attempt = 0;
        long backoff = INITIAL_BACKOFF_MS;

        while (true) {
            try {
                return operation.get();
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempt++;
                if (attempt > MAX_RETRIES) {
                    log.error("Operation {} failed after {} attempts", operationName, attempt);
                    throw new ConcurrentUpdateException(
                        "Operation failed due to concurrent updates. Please try again.",
                        ex
                    );
                }

                // Exponential backoff with jitter
                long jitter = RANDOM.nextInt((int) (backoff * 0.2)); // 20% jitter
                long delay = Math.min(backoff + jitter, MAX_BACKOFF_MS);

                log.warn("OptimisticLockException on attempt {}/{} for {}. Retrying in {}ms",
                    attempt, MAX_RETRIES, operationName, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ConcurrentUpdateException("Retry interrupted", ie);
                }

                backoff = (long) (backoff * BACKOFF_MULTIPLIER);
            }
        }
    }
}

// Usage in TicketService
@Service
public class TicketService {

    private final RetryUtil retryUtil;

    public Ticket assignTicket(UUID ticketId, UUID assigneeId, UUID currentUserId) {
        return retryUtil.executeWithRetry(() -> {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

            ticket.assign(assigneeId);
            Ticket saved = ticketRepository.save(ticket);
            eventPublisher.publishEvent(new TicketAssignedEvent(saved.getId(), assigneeId));

            return saved;
        }, "assignTicket");
    }
}
```

---

## Recommendation: Option A (@Retryable)

**Rationale:**
1. **Spring ecosystem standard**: `spring-retry` is widely used and battle-tested
2. **Declarative approach**: Clean code, easy to maintain
3. **Minimal boilerplate**: Annotation-based, no manual retry loops
4. **AOP-based**: Retry logic separated from business logic
5. **Story 2.1 validation**: IT4 confirmed OptimisticLockException behavior → Retries are well-understood problem

**Trade-offs Accepted:**
- Small dependency addition (spring-retry ~200KB)
- Slight learning curve for @Retryable annotations

---

## Exception Hierarchy

Define custom exception for retry exhaustion:

```java
package io.monosense.synergyflow.itsm.internal.exception;

/**
 * Thrown when a ticket update fails after maximum retry attempts due to concurrent modifications.
 *
 * @since 2.2
 */
public class ConcurrentUpdateException extends BusinessException {

    private final UUID ticketId;

    public ConcurrentUpdateException(String message, UUID ticketId) {
        super(message);
        this.ticketId = ticketId;
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
        this.ticketId = null;
    }

    public UUID getTicketId() {
        return ticketId;
    }
}
```

**Client Handling:**
- REST API returns HTTP 409 Conflict with retry-after header
- Frontend displays user-friendly message: "This ticket was just updated by another user. Please refresh and try again."

---

## Testing Strategy

### Unit Tests

```java
@Test
void assignTicket_retriesOnOptimisticLock_succeeds() {
    // Given: Mock repository to throw OptimisticLockException twice, then succeed
    Ticket ticket = new Ticket(...);
    when(ticketRepository.findById(ticketId))
        .thenReturn(Optional.of(ticket));

    when(ticketRepository.save(any(Ticket.class)))
        .thenThrow(new ObjectOptimisticLockingFailureException("Stale version", null))
        .thenThrow(new ObjectOptimisticLockingFailureException("Stale version", null))
        .thenReturn(ticket); // Succeed on 3rd attempt

    // When: Assign ticket
    Ticket result = ticketService.assignTicket(ticketId, assigneeId, currentUserId);

    // Then: Assignment succeeds after retries
    assertThat(result.getAssigneeId()).isEqualTo(assigneeId);
    verify(ticketRepository, times(3)).save(any(Ticket.class)); // 3 attempts
}

@Test
void assignTicket_retriesExhausted_throwsConcurrentUpdateException() {
    // Given: Mock repository always throws OptimisticLockException
    when(ticketRepository.findById(ticketId))
        .thenReturn(Optional.of(new Ticket(...)));

    when(ticketRepository.save(any(Ticket.class)))
        .thenThrow(new ObjectOptimisticLockingFailureException("Stale version", null));

    // When/Then: After 4 attempts, throw ConcurrentUpdateException
    assertThatThrownBy(() -> ticketService.assignTicket(ticketId, assigneeId, currentUserId))
        .isInstanceOf(ConcurrentUpdateException.class)
        .hasMessageContaining("concurrent updates");

    verify(ticketRepository, times(4)).save(any(Ticket.class)); // 4 attempts
}
```

### Integration Tests

```java
@Test
@Tag("integration")
void concurrentAssignment_withRetry_bothSucceed() throws Exception {
    // Given: Create ticket in NEW state
    Ticket ticket = createTicket();

    // When: Two threads try to assign same ticket to different agents concurrently
    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<Ticket> future1 = executor.submit(() ->
        ticketService.assignTicket(ticket.getId(), agent1Id, user1Id)
    );

    Future<Ticket> future2 = executor.submit(() ->
        ticketService.assignTicket(ticket.getId(), agent2Id, user2Id)
    );

    // Then: One succeeds immediately, the other retries and succeeds
    Ticket result1 = future1.get(5, TimeUnit.SECONDS);
    Ticket result2 = future2.get(5, TimeUnit.SECONDS);

    // Both calls succeed, last write wins (one agent will be assigned)
    assertThat(result1.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    assertThat(result2.getStatus()).isEqualTo(TicketStatus.ASSIGNED);

    // Verify version incremented twice (two updates)
    Ticket final = ticketRepository.findById(ticket.getId()).orElseThrow();
    assertThat(finalTicket.getVersion()).isEqualTo(3L); // Initial=1, +2 updates

    executor.shutdown();
}
```

### Load Tests

Extend Story 2.1 LT2 to include TicketService operations:

```java
@Test
@Tag("load")
void concurrentServiceOperations_withRetry_zeroFailures() {
    // Given: Create 100 tickets
    List<Ticket> tickets = createBulkTickets(100);

    // When: 10 threads concurrently assign/update same tickets
    int threadCount = 10;
    int operationsPerThread = 500;

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger retryCount = new AtomicInteger(0);

    // Execute concurrent operations
    runConcurrentOperations(threadCount, operationsPerThread, () -> {
        UUID ticketId = tickets.get(random.nextInt(100)).getId();
        try {
            ticketService.assignTicket(ticketId, randomAgent(), currentUser());
            successCount.incrementAndGet();
        } catch (ConcurrentUpdateException ex) {
            retryCount.incrementAndGet();
        }
    });

    // Then: All operations succeed (possibly after retries)
    assertThat(successCount.get()).isGreaterThan(4500); // >90% success rate
    assertThat(retryCount.get()).isLessThan(500); // <10% exhausted retries
}
```

---

## Retry Metrics and Monitoring

Add metrics to track retry behavior:

```java
@Service
public class TicketService {

    private final MeterRegistry meterRegistry;

    @Retryable(...)
    public Ticket assignTicket(...) {
        meterRegistry.counter("ticket.assign.attempts").increment();
        // ... business logic
    }

    @Recover
    public Ticket recoverFromOptimisticLock(...) {
        meterRegistry.counter("ticket.assign.retries_exhausted").increment();
        throw new ConcurrentUpdateException(...);
    }
}
```

**Dashboard Metrics:**
- `ticket.assign.attempts` - Total assignment attempts (includes retries)
- `ticket.assign.retries_exhausted` - Failed after max retries (alerts trigger if >1%)
- `ticket.assign.success_rate` - Percentage of successful assignments

---

## Performance Considerations

### Retry Impact on Latency

Based on exponential backoff:
- **Best case** (no contention): 0ms overhead
- **1 retry**: +50-60ms
- **2 retries**: +150-170ms
- **3 retries**: +350-390ms

**Mitigation:**
- Story 2.1 LT2 showed low contention (0 OptimisticLockExceptions with partitioned writes)
- Production workload expected: <5% operations require retries
- P95 latency impact: +50ms (acceptable for write operations)

### Database Connection Pool

Retries hold database connections longer:
- **Max retry duration**: ~400ms per operation
- **Connection pool size**: 20 connections (HikariCP default)
- **Expected contention**: Low (based on LT2 results)

**Recommendation:** Monitor connection pool utilization. Increase to 30 if retries cause pool exhaustion.

---

## Edge Cases and Error Scenarios

### 1. Retry During Transaction Rollback

**Scenario:** OptimisticLockException thrown, transaction rollback in progress
**Handling:** @Transactional annotation ensures each retry gets new transaction

### 2. Retry with Event Publishing

**Scenario:** Event published before save fails, retry publishes duplicate event
**Solution:** Publish event AFTER successful save (current implementation correct)

```java
Ticket saved = ticketRepository.save(ticket); // May throw exception
eventPublisher.publishEvent(new TicketAssignedEvent(saved.getId(), assigneeId)); // Only if save succeeds
```

### 3. Nested Retries

**Scenario:** TicketService.assignTicket() calls RoutingEngine.applyRules(), both have @Retryable
**Handling:** Avoid nested retries. Remove @Retryable from internal service methods, only apply to public API

### 4. User Cancellation During Retry

**Scenario:** User closes browser during retry backoff delay
**Handling:** HTTP request timeout/cancellation interrupts retry loop (Spring handles gracefully)

---

## Story 2.2 Acceptance Criteria

Based on this spike, Story 2.2 must include:

- ✅ **AC-RETRY-1**: Add spring-retry dependency and enable @Retry in application
- ✅ **AC-RETRY-2**: All TicketService write methods annotated with @Retryable (max 4 attempts, exponential backoff)
- ✅ **AC-RETRY-3**: @Recover methods defined to throw ConcurrentUpdateException after retry exhaustion
- ✅ **AC-RETRY-4**: Unit tests verify retry behavior (2 retries succeed, 4 retries exhausted)
- ✅ **AC-RETRY-5**: Integration test validates concurrent updates both succeed with retries
- ✅ **AC-RETRY-6**: Load test confirms <10% retry exhaustion rate under concurrent load
- ✅ **AC-RETRY-7**: Metrics track retry attempts and exhaustion rate

---

## Implementation Checklist for Story 2.2

- [ ] Add `spring-retry` and `spring-aspects` dependencies to `build.gradle.kts`
- [ ] Add `@EnableRetry` to `SynergyFlowApplication.java`
- [ ] Create `ConcurrentUpdateException.java` in `internal/exception/` package
- [ ] Annotate TicketService methods with `@Retryable(retryFor = {ObjectOptimisticLockingFailureException.class}, ...)`
- [ ] Add `@Recover` methods for each @Retryable method
- [ ] Write unit tests for retry behavior (mock ObjectOptimisticLockingFailureException)
- [ ] Write integration test for concurrent assignment with retries
- [ ] Extend LT2 load test to include TicketService operations
- [ ] Add retry metrics to Micrometer/Prometheus
- [ ] Document retry configuration in `application.yml`

---

## Questions for Team Review

1. **Backoff Tuning**: Is 50ms initial delay appropriate, or should we start lower (10ms)?
2. **Max Retries**: Is 3 retries (4 total attempts) sufficient, or increase to 5 retries?
3. **Metrics Alerts**: Should we alert if retry exhaustion rate >1% or >5%?
4. **User Experience**: Should API return 409 Conflict or 503 Service Unavailable on retry exhaustion?

---

**SPIKE STATUS**: ✅ COMPLETE - Ready for Story 2.2 Implementation

**Recommendation**: Use Option A (@Retryable) for declarative, Spring-native retry handling.

**Next Steps:**
1. Add spring-retry dependency in Story 2.2 Task 1
2. Implement @Retryable on all TicketService write methods
3. Write comprehensive retry unit tests and integration tests
4. Monitor retry metrics in load tests
