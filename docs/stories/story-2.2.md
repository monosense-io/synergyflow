# Story 2.2: TicketService CRUD Operations

Status: Draft

## Story

As a Development Team,
I want a TicketService with CRUD operations, state transition validation, and event publishing,
so that we can manage ticket lifecycle with enforced business rules and event-driven updates to read models.

## Acceptance Criteria

### Functional Requirements

1. **AC1**: Create `TicketService.java` at `backend/src/main/java/io/monosense/synergyflow/itsm/internal/service/TicketService.java` with:
   - `@Service` annotation for Spring dependency injection
   - `@Transactional` annotation at class level for transaction management
   - Package-private visibility (internal service package)
   - Dependencies injected via constructor: `TicketRepository`, `OutboxWriter`

2. **AC2**: Implement `createTicket(CreateTicketRequest request)` method that:
   - Creates new `Ticket` entity from request DTO
   - Sets initial values: `status = NEW`, `createdAt = Instant.now()`, `updatedAt = Instant.now()`, `version = 1L`
   - Persists ticket via `TicketRepository.save()`
   - Publishes `TicketCreatedEvent` to outbox with fields: ticketId, title, ticketType, status, priority, requesterId, createdAt, version
   - Returns persisted `Ticket` entity

3. **AC3**: Implement `assignTicket(UUID ticketId, UUID assigneeId, String assignmentReason)` method that:
   - Loads ticket by ID, throws `TicketNotFoundException` if not found
   - Updates `assigneeId`, sets `status = ASSIGNED`, updates `updatedAt`, increments version
   - Persists updated ticket
   - Publishes `TicketAssignedEvent` to outbox with fields: ticketId, assigneeId, assignmentReason, updatedAt, version
   - Returns updated `Ticket` entity

4. **AC4**: Implement `updateStatus(UUID ticketId, TicketStatus newStatus)` method that:
   - Loads ticket by ID, throws `TicketNotFoundException` if not found
   - Validates state transition via `isValidTransition(currentStatus, newStatus)`, throws `InvalidStateTransitionException` if invalid
   - Updates `status`, updates `updatedAt`, increments version
   - If transitioning to `RESOLVED` and ticket is `Incident`, sets `resolvedAt = Instant.now()`
   - Persists updated ticket
   - Publishes `TicketStateChangedEvent` to outbox with fields: ticketId, oldStatus, newStatus, updatedAt, version
   - Returns updated `Ticket` entity

5. **AC5**: Implement `isValidTransition(TicketStatus from, TicketStatus to)` private method enforcing state transition rules (FR-WF-6):
   - `NEW` → `ASSIGNED` or `CLOSED` (allowed)
   - `ASSIGNED` → `IN_PROGRESS` or `CLOSED` (allowed)
   - `IN_PROGRESS` → `RESOLVED` or `ASSIGNED` (allowed, can reassign)
   - `RESOLVED` → `CLOSED` (allowed, no reopening from RESOLVED)
   - `CLOSED` → (no transitions allowed)
   - All other transitions → false

6. **AC6**: Implement `addComment(UUID ticketId, AddCommentRequest request)` method that:
   - Loads ticket by ID to verify existence, throws `TicketNotFoundException` if not found
   - Creates new `TicketComment` entity with fields: ticketId, author, commentText, isInternal, createdAt
   - Persists comment via `TicketCommentRepository.save()`
   - Returns persisted `TicketComment` entity
   - Note: Comment events are NOT published in this story (deferred to later requirements)

7. **AC7**: Create DTO classes in `backend/src/main/java/io/monosense/synergyflow/itsm/api/dto/`:
   - `CreateTicketRequest` with fields: title (String, @NotBlank), description (String, @NotBlank), ticketType (TicketType, @NotNull), priority (Priority, @NotNull), category (String), requesterId (UUID, @NotNull)
   - `AssignTicketRequest` with fields: assigneeId (UUID, @NotNull), assignmentReason (String)
   - `UpdateStatusRequest` with fields: newStatus (TicketStatus, @NotNull)
   - `AddCommentRequest` with fields: author (UUID, @NotNull), commentText (String, @NotBlank), isInternal (boolean)

8. **AC8**: Create event classes in `backend/src/main/java/io/monosense/synergyflow/itsm/events/`:
   - `TicketCreatedEvent` with fields: ticketId, title, ticketType, status, priority, requesterId, createdAt, version
   - `TicketAssignedEvent` with fields: ticketId, assigneeId, assignmentReason, updatedAt, version
   - `TicketStateChangedEvent` with fields: ticketId, oldStatus, newStatus, updatedAt, version
   - All event classes implement `DomainEvent` interface (from Epic 1)

9. **AC9**: Create custom exceptions in `backend/src/main/java/io/monosense/synergyflow/itsm/internal/exception/`:
   - `TicketNotFoundException` extending `RuntimeException` with constructor taking UUID ticketId
   - `InvalidStateTransitionException` extending `RuntimeException` with constructor taking TicketStatus from, TicketStatus to

### Integration Tests (Testcontainers)

**REQUIRED**: Integration tests using Testcontainers for PostgreSQL database verification.

1. **Test: Create ticket successfully**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: CreateTicketRequest with valid fields (title="Test Incident", ticketType=INCIDENT, priority=HIGH, requesterId=valid-uuid)
   - Expected behavior: Ticket persisted to database with id, createdAt, updatedAt populated; TicketCreatedEvent published to outbox table
   - Verification: Query tickets table for new ticket; query outbox table for TicketCreatedEvent with matching ticketId

2. **Test: Assign ticket successfully**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: Pre-created ticket with status=NEW; assigneeId=valid-uuid; assignmentReason="Auto-assigned to Network Team"
   - Expected behavior: Ticket updated with assigneeId, status=ASSIGNED, version incremented; TicketAssignedEvent published to outbox
   - Verification: Query tickets table for updated ticket; verify version incremented; query outbox for TicketAssignedEvent

3. **Test: Update status with valid transition succeeds**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: Pre-created ticket with status=ASSIGNED
   - Expected behavior: updateStatus(ticketId, IN_PROGRESS) succeeds; ticket status updated, version incremented; TicketStateChangedEvent published
   - Verification: Query tickets table for status=IN_PROGRESS, version=3; query outbox for event with oldStatus=ASSIGNED, newStatus=IN_PROGRESS

4. **Test: Update status with invalid transition throws exception**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: Pre-created ticket with status=CLOSED
   - Expected behavior: updateStatus(ticketId, IN_PROGRESS) throws InvalidStateTransitionException; no database changes; no event published
   - Verification: Verify exception thrown; query tickets table confirms status still CLOSED and version unchanged; outbox has no new events

5. **Test: Transition to RESOLVED sets resolvedAt for Incident**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: Pre-created Incident with status=IN_PROGRESS, resolvedAt=null
   - Expected behavior: updateStatus(ticketId, RESOLVED) succeeds; incident.resolvedAt set to current timestamp
   - Verification: Query incidents table for resolvedAt != null and status=RESOLVED

6. **Test: Add comment successfully**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: Pre-created ticket; AddCommentRequest with author=valid-uuid, commentText="Customer confirmed issue resolved", isInternal=false
   - Expected behavior: Comment persisted to ticket_comments table with id, ticketId, author, commentText, isInternal, createdAt
   - Verification: Query ticket_comments table for new comment; verify all fields match request

7. **Test: Create ticket fails when ticket not found (assign, update, comment operations)**
   - Container setup: PostgreSQL (Testcontainers)
   - Test data: Non-existent ticketId (random UUID not in database)
   - Expected behavior: assignTicket, updateStatus, addComment all throw TicketNotFoundException
   - Verification: Verify exception thrown with correct message; no database changes

### Load Tests (@Tag("load"))

**REQUIRED**: Load tests to verify performance under concurrent ticket operations.

1. **Load test: Concurrent ticket creation**
   - Performance target: 100 tickets/second, response time p95 < 400ms (per NFR1)
   - Test duration: 2 minutes sustained load (12,000 total tickets created)
   - Success criteria: p95 latency < 400ms, p99 < 800ms (per NFR1), zero database errors, all tickets persisted with valid UUIDs

2. **Load test: Concurrent ticket assignment**
   - Performance target: 200 assignments/second, response time p95 < 400ms
   - Test duration: 1 minute sustained load (12,000 assignments across 1,000 pre-created tickets)
   - Success criteria: p95 latency < 400ms, zero OptimisticLockExceptions (version conflicts handled with retry), all assignments persisted

## Tasks / Subtasks

- [ ] **Task 1: Create TicketService core implementation** (AC1, AC2, AC3, AC4, AC6)
  - [ ] 1.1: Create TicketService.java class with @Service and @Transactional annotations in internal/service package
  - [ ] 1.2: Add constructor with TicketRepository, TicketCommentRepository, OutboxWriter dependencies
  - [ ] 1.3: Implement createTicket method: entity creation, persistence, event publishing
  - [ ] 1.4: Implement assignTicket method: load ticket, update assignment, publish event
  - [ ] 1.5: Implement updateStatus method: load ticket, validate transition, update status, handle Incident.resolvedAt, publish event
  - [ ] 1.6: Implement addComment method: create comment entity, persist via TicketCommentRepository

- [ ] **Task 2: Implement state transition validation** (AC5)
  - [ ] 2.1: Create isValidTransition private method with switch statement for FR-WF-6 rules
  - [ ] 2.2: Add validation check in updateStatus before persisting changes
  - [ ] 2.3: Create InvalidStateTransitionException with constructor taking (from, to) statuses

- [ ] **Task 3: Implement event publishing** (AC8)
  - [ ] 3.1: Create TicketCreatedEvent class in events package with all required fields
  - [ ] 3.2: Create TicketAssignedEvent class in events package
  - [ ] 3.3: Create TicketStateChangedEvent class in events package
  - [ ] 3.4: Ensure all events implement DomainEvent interface from Epic 1
  - [ ] 3.5: Publish events via OutboxWriter.publish() in each service method

- [ ] **Task 4: Create DTO classes** (AC7)
  - [ ] 4.1: Create CreateTicketRequest DTO in api/dto package with Bean Validation annotations (@NotBlank, @NotNull)
  - [ ] 4.2: Create AssignTicketRequest DTO with assigneeId and assignmentReason
  - [ ] 4.3: Create UpdateStatusRequest DTO with newStatus field
  - [ ] 4.4: Create AddCommentRequest DTO with author, commentText, isInternal fields

- [ ] **Task 5: Create custom exceptions** (AC9)
  - [ ] 5.1: Create TicketNotFoundException in internal/exception package with UUID parameter
  - [ ] 5.2: Create InvalidStateTransitionException with from/to status parameters and meaningful message

- [ ] **Task 6: Write integration tests with Testcontainers** (AC6, AC7 integration tests)
  - [ ] 6.1: Set up TicketServiceIntegrationTest class with @SpringBootTest and Testcontainers PostgreSQL
  - [ ] 6.2: Test createTicket → verify ticket persisted and TicketCreatedEvent in outbox
  - [ ] 6.3: Test assignTicket → verify assignment updated and TicketAssignedEvent in outbox
  - [ ] 6.4: Test updateStatus with valid transitions (NEW→ASSIGNED, ASSIGNED→IN_PROGRESS, IN_PROGRESS→RESOLVED, RESOLVED→CLOSED) → all succeed
  - [ ] 6.5: Test updateStatus with invalid transitions (CLOSED→IN_PROGRESS, NEW→RESOLVED, etc.) → throw InvalidStateTransitionException
  - [ ] 6.6: Test updateStatus to RESOLVED for Incident → verify resolvedAt timestamp set
  - [ ] 6.7: Test addComment → verify comment persisted to ticket_comments table
  - [ ] 6.8: Test TicketNotFoundException for all operations with non-existent ticketId

- [ ] **Task 7: Write load tests** (AC6, AC7 load tests)
  - [ ] 7.1: Create TicketServiceLoadTest with @Tag("load") annotation
  - [ ] 7.2: Implement concurrent ticket creation test: 100 tickets/sec for 2 minutes with p95 < 400ms assertion
  - [ ] 7.3: Implement concurrent ticket assignment test: 200 assignments/sec for 1 minute with p95 < 400ms assertion
  - [ ] 7.4: Add metrics collection for latency percentiles (p50, p95, p99) and error rate

## Dev Notes

### Architecture Patterns and Constraints

**Service Layer Pattern** [Source: docs/epics/epic-2-itsm-tech-spec.md#5-internal-service-layer]:
- TicketService is package-private (internal visibility) in `itsm.internal.service` package
- Business logic encapsulated in service layer, not exposed directly to API layer
- API layer (TicketController, future Story 2.5) will delegate to this service

**Event-Driven Architecture** [Source: docs/epics/epic-2-itsm-tech-spec.md#2-ticket-crud-service]:
- All state-changing operations publish domain events via OutboxWriter (Epic 1 Story 1.5)
- Events enable eventual consistency for read models (ticket_card, queue_row projections)
- Event schema includes version field for optimistic concurrency tracking

**State Machine Enforcement** [Source: docs/epics/epic-2-itsm-tech-spec.md#2-ticket-crud-service lines 402-411]:
- FR-WF-6 state transition rules enforced via isValidTransition method
- Prevents invalid flows: cannot reopen RESOLVED tickets, cannot skip IN_PROGRESS for incident resolution
- CLOSED is terminal state (no outbound transitions)

**Optimistic Locking** [Source: docs/architecture/data-model.md, Story 2.1]:
- Ticket entity has @Version field for concurrency control
- Service layer calls incrementVersion() on each update
- JPA will throw OptimisticLockException if version mismatch (concurrent update detected)

### Project Structure Notes

**Alignment with unified project structure**:
- `backend/src/main/java/io/monosense/synergyflow/itsm/internal/service/TicketService.java` (package-private service)
- `backend/src/main/java/io/monosense/synergyflow/itsm/api/dto/` (public DTOs for API layer)
- `backend/src/main/java/io/monosense/synergyflow/itsm/events/` (public events for cross-module boundary)
- `backend/src/main/java/io/monosense/synergyflow/itsm/internal/exception/` (package-private exceptions)

**Dependencies from previous stories**:
- Story 2.1: Ticket, Incident, ServiceRequest, TicketComment entities with repositories (✅ completed)
- Story 1.5: OutboxWriter for transactional event publishing (✅ completed from Epic 1)

**Deferred integrations** (implemented in later stories):
- SlaCalculator integration: Story 2.3 (SLA calculation on ticket creation/update)
- RoutingEngine integration: Story 2.4 (auto-assignment based on routing rules)
- TicketController REST API: Story 2.5 (exposes TicketService via HTTP endpoints)

### Testing Strategy

**Integration Tests**:
- Container dependencies: PostgreSQL 16 (Testcontainers with @ServiceConnection)
- Test scope: TicketService → TicketRepository/TicketCommentRepository → PostgreSQL database + OutboxWriter → outbox table
- Data setup approach: Use @BeforeEach to create test tickets; @AfterEach clears database (rollback transaction or DELETE FROM tickets)
- Key scenarios covered:
  1. Happy path: create → assign → update status → add comment (full lifecycle)
  2. State transition validation: test all valid and invalid transitions per FR-WF-6
  3. Event publishing: verify outbox table contains expected events after each operation
  4. Incident-specific logic: verify resolvedAt timestamp set when transitioning to RESOLVED
  5. Error handling: verify TicketNotFoundException and InvalidStateTransitionException thrown correctly

**Load Tests**:
- Performance baseline: Target p95 < 400ms per NFR1 (ticket CRUD operations)
- Test environment: Testcontainers PostgreSQL with 2 CPU, 4GB RAM limits; simulate 50 concurrent users
- Monitoring approach: Collect latency percentiles (p50, p95, p99), throughput (ops/sec), error rate
- Acceptance thresholds:
  - Ticket creation: 100 tickets/sec, p95 < 400ms, p99 < 800ms
  - Ticket assignment: 200 assignments/sec, p95 < 400ms

**Unit Tests**:
- Coverage expectations: >80% coverage for TicketService methods
- Mocking strategy: Mock TicketRepository, OutboxWriter to isolate service logic; test state transition validation in isolation

### References

- [Source: docs/epics/epic-2-itsm-tech-spec.md#2-ticket-crud-service] - TicketService implementation details (lines 265-413)
- [Source: docs/epics/epic-2-itsm-tech-spec.md#implementation-guide-week-3-4-sprint-3] - Story 2.2 breakdown (lines 1395-1400)
- [Source: docs/product/prd.md#FR-ITSM-1] - Incident CRUD requirements
- [Source: docs/product/prd.md#FR-ITSM-2] - Service Request CRUD requirements
- [Source: docs/architecture/data-model.md#tickets-table] - Ticket entity schema
- [Source: Story 2.1] - Ticket domain entities and repositories (completed)

## Change Log

| Date       | Version | Description   | Author     |
| ---------- | ------- | ------------- | ---------- |
| 2025-10-09 | 0.1     | Initial draft | monosense  |

## Dev Agent Record

### Context Reference

<!-- Path(s) to story context XML/JSON will be added here by context workflow -->

### Agent Model Used

Claude 3.5 Sonnet (claude-sonnet-4-5-20250929)

### Debug Log References

### Completion Notes List

### File List
