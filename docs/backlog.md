# Engineering Backlog

This backlog collects cross-cutting or future action items that emerge from reviews and planning.

Routing guidance:

- Use this file for non-urgent optimizations, refactors, or follow-ups that span multiple stories/epics.
- Must-fix items to ship a story belong in that story’s `Tasks / Subtasks`.
- Same-epic improvements may also be captured under the epic Tech Spec `Post-Review Follow-ups` section.

| Date | Story | Epic | Type | Severity | Owner | Status | Notes |
| ---- | ----- | ---- | ---- | -------- | ----- | ------ | ----- |
| 2025-10-09 | 2.2 | 2 | Bug/TechDebt | HIGH | Backend Engineer | Open | **Spring Retry Exception Wrapping** - Refactor business validation logic to execute BEFORE @Retryable methods. Fixes 8 failing IT tests (IT-SM-3/4/5, IT-META-3). Extract validation to non-retryable methods. Files: TicketService.java:232-940. Effort: 2h. Blocker for Story 2.3. |
| 2025-10-09 | 2.2 | 2 | Security | MEDIUM | Backend Engineer | Open | **Add Audit Logging** - Integrate audit logging for sensitive operations (assign, resolve, reopen) to meet SOC2/GDPR compliance. Use @EventListener for audit events. Files: TicketService.java (all state transition methods). Effort: 1-2h. |
| 2025-10-09 | 2.2 | 2 | Enhancement | MEDIUM | Backend Engineer | Open | **Add @Cacheable to findById()** - Enable caching for TicketQueryServiceImpl.findById() to reduce DB load. Reduces p95 latency from ~50ms to ~5ms. Files: TicketQueryServiceImpl.java, application.yml. Effort: 15min. AC-11. |
| 2025-10-09 | 2.2 | 2 | TechDebt | LOW | QA Engineer | Open | **Execute Load Tests** - Run load tests with ./gradlew test -DincludeTags=load and document p95 latencies, retry exhaustion rates. Validates performance targets before production. Files: TicketServiceLoadTest.java. Effort: 30min. AC-24. |
| 2025-10-09 | 2.2 | 2 | Enhancement | LOW | Backend Engineer | Open | **Add Database Indexes** - Create V9 Flyway migration with indexes on (status), (assignee_id), (requester_id), (created_at DESC), composite (status, priority). Improves query perf from O(n) to O(log n). Effort: 15min. |
| 2025-10-09 | 2.2 | 2 | Refactoring | MEDIUM | Backend Engineer | Open | **Refactor Large Service Class** - Extract TicketAssignmentService, TicketResolutionService, TicketMetadataService from 1200-line TicketService.java. Improves maintainability. Effort: 2-3h. Defer to future refactoring story. |
