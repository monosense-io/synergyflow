# Review Readiness Checklist

**Purpose:** Developer self-assessment before submitting stories for review
**Scope:** All Epic 2+ stories
**Owner:** Development Team
**Last Updated:** 2025-10-08

---

## Overview

This checklist ensures comprehensive coverage of acceptance criteria, testing, security, performance, and observability requirements before code review submission. Complete each section and provide evidence where indicated.

---

## 1. Acceptance Criteria Coverage

### 1.1 Requirements Traceability
- [ ] **All ACs mapped to implementation**
  - Each acceptance criterion has corresponding code changes
  - Changes are referenced by file path and line numbers in story Dev Notes

- [ ] **Test evidence for every AC**
  - Unit tests: Class/method references documented
  - Integration tests: Test class and scenario documented
  - Manual test evidence: Screenshots or logs attached

- [ ] **Edge cases identified and tested**
  - Null/empty inputs handled
  - Boundary conditions validated
  - Error paths tested

### 1.2 AC Verification Matrix
```markdown
| AC ID | Description | Implementation File(s) | Test Evidence | Status |
|-------|-------------|------------------------|---------------|--------|
| AC-1  | ...         | path/to/file.java:123  | TestClass:L45 | ✅     |
| AC-2  | ...         | path/to/file.java:456  | TestClass:L78 | ✅     |
```

---

## 2. Integration Testing

### 2.1 External System Integration
- [ ] **Testcontainers configured for all external dependencies**
  - PostgreSQL with schema initialization
  - Keycloak (if authentication involved)
  - Redis/other caches
  - Message queues (if applicable)

- [ ] **Integration test scenarios cover:**
  - Happy path end-to-end flows
  - Failure scenarios (connection loss, timeouts)
  - Data consistency across module boundaries
  - Transaction rollback behavior

### 2.2 Test Execution
- [ ] **All integration tests pass locally**
  - Command: `./gradlew integrationTest`
  - No flaky tests (run 3x to verify)

- [ ] **Test logs reviewed for warnings/errors**
  - No unexpected exceptions in logs
  - Resource cleanup verified (no leaked containers)

---

## 3. Idempotency & Consistency

### 3.1 Database-Level Guards
- [ ] **Unique constraints prevent duplicate processing**
  - Unique indexes on business keys
  - Check constraints for invariants

- [ ] **Optimistic locking implemented where required**
  - `@Version` annotations on aggregates
  - Concurrent modification tests added

### 3.2 Replay Safety
- [ ] **Idempotent operations tested**
  - Command handlers can be called multiple times safely
  - Event handlers tolerate duplicate events

- [ ] **Replay tests written and passing**
  - Test: Process same input twice → same outcome
  - Test: Replay events → state matches original

---

## 4. Authorization & Security

### 4.1 Authorization Checks
- [ ] **Filter-before-expose pattern enforced**
  - Repository queries include user/tenant filters
  - No data leakage across tenants/users

- [ ] **Authorization tested at API boundaries**
  - Positive tests: Authorized user can access
  - Negative tests: Unauthorized user blocked (403)
  - Cross-tenant tests: User A cannot access User B's data

### 4.2 Security Best Practices
- [ ] **No credentials in code or version control**
  - Secrets externalized to environment variables
  - `.env` files in `.gitignore`

- [ ] **Input validation implemented**
  - Bean Validation annotations used
  - SQL injection prevented (parameterized queries)
  - XSS prevented (output encoding)

- [ ] **Sensitive data handling**
  - PII logged appropriately (masked/excluded)
  - Audit trail for data access (if required)

---

## 5. Performance

### 5.1 Load Testing
- [ ] **Load tests written for critical paths**
  - Test class: `Load{Feature}Test.java`
  - Scenario: Concurrent users, realistic workload

- [ ] **Performance metrics captured**
  - p50, p95, p99 latencies documented
  - Throughput (requests/second) measured

- [ ] **Performance requirements met**
  - Compare against SLOs defined in story
  - No regressions vs. baseline (if applicable)

### 5.2 Resource Efficiency
- [ ] **N+1 query problems eliminated**
  - Use `@EntityGraph` or JOIN FETCH
  - Query count logged and reviewed

- [ ] **Database indexes present for queries**
  - Indexes on foreign keys
  - Composite indexes for common filters

- [ ] **Connection pooling configured appropriately**
  - HikariCP settings reviewed
  - Pool size matches expected load

---

## 6. Observability

### 6.1 Metrics
- [ ] **Business metrics emitted**
  - Counters for events (e.g., `tickets.created`)
  - Gauges for state (e.g., `tickets.open.count`)

- [ ] **Technical metrics emitted**
  - Latency histograms for key operations
  - Error rates for external calls

- [ ] **Metrics tested**
  - Assertions verify metrics are incremented
  - Metric names follow convention: `{module}.{entity}.{action}`

### 6.2 Structured Logging
- [ ] **Logs use SLF4J with structured context**
  - MDC populated with correlation IDs, user IDs, tenant IDs
  - Log levels appropriate (DEBUG, INFO, WARN, ERROR)

- [ ] **Critical events logged**
  - Domain events published/consumed
  - External API calls (request/response IDs)
  - Authorization failures

- [ ] **No sensitive data in logs**
  - Passwords, tokens, PII excluded or masked
  - Log statements reviewed for data leakage

### 6.3 Tracing
- [ ] **Distributed tracing enabled**
  - `@Observed` annotation on key methods
  - Trace IDs propagate across module boundaries

---

## 7. Documentation

### 7.1 Code Documentation
- [ ] **Public APIs have Javadoc**
  - Classes: Purpose, responsibility, usage examples
  - Methods: Parameters, return values, exceptions
  - Complex logic: Inline comments explaining "why"

- [ ] **Architecture Decision Records (ADRs) updated**
  - New patterns or deviations documented
  - Trade-offs explained

### 7.2 Story Documentation
- [ ] **Dev Notes section complete in story**
  - Implementation summary (what changed, why)
  - File references with line numbers
  - Test evidence linked (class names, methods)
  - Known limitations or follow-up tasks noted

- [ ] **API documentation updated (if applicable)**
  - OpenAPI/Swagger specs regenerated
  - Endpoint descriptions accurate
  - Example requests/responses provided

---

## 8. Code Quality

### 8.1 Static Analysis
- [ ] **Build passes with no warnings**
  - Command: `./gradlew build --warning-mode=all`
  - Checkstyle, SpotBugs, PMD rules satisfied

- [ ] **Test coverage meets threshold**
  - Minimum 80% line coverage for new code
  - Critical paths have 100% coverage

### 8.2 Code Review Preparation
- [ ] **Commit history is clean**
  - Logical commits with clear messages
  - No "WIP" or "fix typo" commits (squash if needed)

- [ ] **Self-review completed**
  - All commented-out code removed
  - No debug print statements
  - Consistent formatting (run formatter)

---

## 9. Pre-Submission Checklist

Before marking story as "Ready for Review":

- [ ] All sections above completed and checked
- [ ] Story status updated: `Ready for Review`
- [ ] Dev Notes section finalized with:
  - Implementation summary
  - Test evidence table (AC → Test mapping)
  - Performance results (if applicable)
  - Screenshots/logs (if applicable)
- [ ] All tests passing locally: `./gradlew clean build test integrationTest`
- [ ] Branch pushed to remote
- [ ] Self-review notes added to story (anything reviewers should focus on)

---

## 10. Review Submission

### Story Update Template
```markdown
## Dev Notes

### Implementation Summary
[Brief description of changes]

### Acceptance Criteria Coverage
| AC ID | Implementation | Test Evidence | Status |
|-------|---------------|---------------|--------|
| ...   | ...           | ...           | ✅     |

### Test Results
- Unit tests: ✅ [X] passed
- Integration tests: ✅ [Y] passed
- Performance: p95=[Z]ms (requirement: [R]ms)

### Review Readiness
- [✅] All checklist items completed
- [✅] Self-review performed
- [✅] Build passing with no warnings
```

---

## Appendix: Quick Reference

### Common Test Patterns
```java
// Idempotency test
@Test
void commandIsIdempotent() {
    var result1 = commandHandler.handle(command);
    var result2 = commandHandler.handle(command);
    assertThat(result1).isEqualTo(result2);
}

// Authorization test (negative)
@Test
void unauthorizedUserCannotAccess() {
    authenticateAs("user-without-permission");
    assertThatThrownBy(() -> service.getResource(id))
        .isInstanceOf(AccessDeniedException.class);
}

// Performance test
@Test
void performanceIsAcceptable() {
    StopWatch watch = new StopWatch();
    watch.start();
    service.performOperation();
    watch.stop();
    assertThat(watch.getTotalTimeMillis()).isLessThan(100);
}
```

### Useful Commands
```bash
# Run all tests with coverage
./gradlew clean build test integrationTest jacocoTestReport

# Check for security vulnerabilities
./gradlew dependencyCheckAnalyze

# Run static analysis
./gradlew check

# View test coverage report
open build/reports/jacoco/test/html/index.html
```

---

**Remember:** This checklist is a quality gate, not a bureaucratic burden. Each item exists to prevent production issues. When in doubt, err on the side of more testing and documentation.
