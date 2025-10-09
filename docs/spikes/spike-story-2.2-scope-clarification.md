# SPIKE: Story 2.2 Scope Clarification

**Date:** 2025-10-09
**Owners:** Sarah (PO) + Bob (SM)
**Epic:** Epic 2 - Core ITSM Module
**Status:** 🔴 CRITICAL PATH - Must complete before Sprint Planning
**Estimated Effort:** 1 hour

---

## Objective

Define the precise scope boundary for Story 2.2 to determine:
1. **What's IN scope**: Service layer only OR service + API layer?
2. **What's OUT of scope**: Deferred to Stories 2.3+
3. **Story points estimation**: Based on finalized scope
4. **Sprint allocation**: 1 sprint or 2 sprints?

---

## Context: Epic 2 Component Breakdown

From Epic 2 Tech Spec, the ITSM backend has multiple layers:

### Already Complete (Story 2.1)
✅ **Domain Layer**: Ticket, Incident, ServiceRequest entities
✅ **Repository Layer**: TicketRepository, IncidentRepository, ServiceRequestRepository, TicketCommentRepository, RoutingRuleRepository
✅ **Database Schema**: V6 (SINGLE_TABLE), V7 (comments/routing)
✅ **Testing Infrastructure**: Testcontainers, unit tests, integration tests, load tests

### Remaining Epic 2 Components

**Service Layer** (`internal/service/`):
- TicketService (CRUD, state transitions, event publishing)
- TicketQueryServiceImpl (SPI implementation)
- RoutingEngine (auto-assign logic)
- SuggestedResolutionEngine (AI-powered suggestions)
- CompositeSidePanelService (aggregate context data)
- MetricsService (MTTR, FRT, SLA calculations)

**API Layer** (`api/`):
- TicketController (REST endpoints)
- AgentConsoleController (composite endpoints)
- MetricsController (dashboard APIs)

**Event Layer** (`events/`):
- TicketCreatedEvent
- TicketAssignedEvent
- TicketStateChangedEvent
- SlaBreachedEvent

---

## Scope Options Analysis

### Option A: Service Layer ONLY (Recommended)

**Scope:**
- ✅ TicketService with all state transition methods (assign, startWork, resolve, close, reopen)
- ✅ TicketQueryServiceImpl (SPI implementation)
- ✅ TicketStateTransitionValidator
- ✅ @Retryable optimistic lock handling
- ✅ Event publishing to transactional outbox
- ✅ State machine tests (unit + integration)
- ✅ Concurrent update tests with retries
- ❌ REST API controllers (deferred to Story 2.3)
- ❌ RoutingEngine (deferred to Story 2.4)
- ❌ SuggestedResolutionEngine (deferred to Story 2.5+)

**Story Points Estimate:** 10-12 points

**Pros:**
- ✅ Clear separation of concerns (service logic independent of API transport)
- ✅ Allows parallel development (Story 2.3 REST API can start immediately after 2.2)
- ✅ Manageable scope for 1 sprint (2 weeks)
- ✅ All spike work directly applicable (state machine, retry pattern, SPI)
- ✅ Foundation ready for multiple API styles (REST, GraphQL, gRPC)

**Cons:**
- ⚠️ No demo-able feature (no UI can call service yet)
- ⚠️ Testing requires service-level integration tests (not API endpoint tests)

**Completion Criteria:**
- TicketService implements 10+ state transition methods
- All methods have @Retryable with retry tests
- SPI contract implemented and tested
- Events published to outbox on state changes
- State machine validation enforced
- Load tests validate concurrent updates with retries

---

### Option B: Service Layer + Basic REST API

**Scope:**
- ✅ All from Option A (TicketService + SPI)
- ✅ TicketController with basic CRUD endpoints:
  - POST /api/itsm/tickets (create)
  - GET /api/itsm/tickets/{id} (read)
  - PUT /api/itsm/tickets/{id}/assign (assign)
  - PUT /api/itsm/tickets/{id}/start-work (start work)
  - PUT /api/itsm/tickets/{id}/resolve (resolve)
  - PUT /api/itsm/tickets/{id}/close (close)
  - PUT /api/itsm/tickets/{id}/reopen (reopen)
- ✅ Request/Response DTOs
- ✅ Controller integration tests (MockMvc or RestAssured)
- ❌ AgentConsoleController (deferred to Story 2.6)
- ❌ RoutingEngine (deferred to Story 2.4)

**Story Points Estimate:** 15-18 points

**Pros:**
- ✅ Demo-able feature (can test via Postman/Swagger)
- ✅ Frontend can start consuming APIs immediately
- ✅ API contract validated early
- ✅ Full vertical slice (DB → Service → API)

**Cons:**
- ⚠️ Larger scope (may require 2 sprints)
- ⚠️ API design decisions may slow service layer implementation
- ⚠️ Risk of scope creep (temptation to add filtering, pagination, etc.)
- ⚠️ Testing overhead (both service + API integration tests)

**Completion Criteria:**
- All from Option A, plus:
- REST API endpoints functional with 2xx/4xx responses
- OpenAPI/Swagger spec generated
- Controller tests verify request validation and error handling
- Security annotations (@PreAuthorize) applied

---

### Option C: Full Ticket Lifecycle (Service + API + Routing)

**Scope:**
- ✅ All from Option B (Service + Basic API)
- ✅ RoutingEngine (auto-assign based on rules)
- ✅ Routing rule evaluation logic
- ✅ Round-robin assignment strategy
- ❌ SuggestedResolutionEngine (deferred to Story 2.5+)
- ❌ CompositeSidePanelService (deferred to Story 2.6)

**Story Points Estimate:** 20-25 points

**Pros:**
- ✅ Complete ticket lifecycle demo (create → auto-assign → resolve → close)
- ✅ High business value (routing is core ITSM feature)

**Cons:**
- ⚠️ Very large scope (2 sprints minimum)
- ⚠️ RoutingEngine is complex (category matching, priority rules, round-robin)
- ⚠️ High risk of incomplete delivery
- ⚠️ Violates "small, incremental stories" principle

**Not Recommended** - Too large for single story

---

## Recommendation: Option A (Service Layer Only)

### Rationale

1. **Sprint Velocity**: Story 2.1 was 8 points and took full sprint. Service layer is more complex → 10-12 points is realistic for 2-week sprint.

2. **Spike Alignment**: All 3 preparation spikes (state machine, retry pattern, SPI) are service-layer focused. Option A directly implements spike designs.

3. **Parallel Development**: Splitting service/API enables:
   - Story 2.2 (Service): Dev Agent focuses on business logic
   - Story 2.3 (API): Different dev can start API layer once 2.2 service is done
   - Stories can overlap by 1-2 days (API work starts before 2.2 fully complete)

4. **Risk Mitigation**: Smaller stories reduce risk of incomplete delivery. Service layer is foundation—must be solid.

5. **Testing Strategy**: Service-level integration tests are more valuable than API endpoint tests. Tests validate business logic directly, not HTTP routing.

### Story Sequencing (Recommended)

```
Story 2.1: Domain Entities + Repositories (8 pts) ✅ COMPLETE
  └─ Deliverable: Ticket, Incident, ServiceRequest, Repositories, DB schema

Story 2.2: Ticket Service Layer (10 pts) ← CURRENT SCOPE
  └─ Deliverable: TicketService with state transitions, SPI impl, event publishing
  └─ Duration: 1 sprint (2 weeks)

Story 2.3: Ticket REST API (8 pts)
  └─ Deliverable: TicketController with CRUD endpoints, DTOs, Swagger spec
  └─ Duration: 1 sprint
  └─ Can START during Story 2.2 week 2 (parallel work)

Story 2.4: Routing Engine (10 pts)
  └─ Deliverable: Auto-assignment based on routing rules
  └─ Duration: 1 sprint

Story 2.5: Comments & History (6 pts)
  └─ Deliverable: Add comment API, ticket history tracking
  └─ Duration: 0.5 sprint

Story 2.6: Agent Console Backend (12 pts)
  └─ Deliverable: AgentConsoleController, CompositeSidePanelService, queue APIs
  └─ Duration: 1 sprint
```

**Epic 2 Total**: 54 points backend (Story 2.1-2.6), leaving ~106 points for SLA, metrics, frontend

---

## Story 2.2 Scope Definition (FINAL)

### IN SCOPE

**TicketService (Core Business Logic):**
1. **Create Ticket**: `createTicket(CreateTicketCommand)` → Ticket
   - Validate command fields
   - Create Ticket or Incident or ServiceRequest entity
   - Set initial status = NEW
   - Publish TicketCreatedEvent to outbox
   - Return created ticket

2. **State Transition Methods** (per State Machine Spike):
   - `assignTicket(ticketId, assigneeId, currentUserId)` → NEW → ASSIGNED
   - `unassignTicket(ticketId, currentUserId)` → ASSIGNED → NEW
   - `reassignTicket(ticketId, newAssigneeId, currentUserId)` → ASSIGNED/IN_PROGRESS → ASSIGNED
   - `startWork(ticketId, currentUserId)` → ASSIGNED → IN_PROGRESS
   - `pauseWork(ticketId, currentUserId)` → IN_PROGRESS → ASSIGNED
   - `resolveTicket(ticketId, resolutionNotes, currentUserId)` → IN_PROGRESS → RESOLVED
   - `closeTicket(ticketId, currentUserId)` → RESOLVED → CLOSED
   - `reopenTicket(ticketId, reopenReason, currentUserId)` → RESOLVED/CLOSED → NEW

3. **Metadata Update Methods**:
   - `updatePriority(ticketId, newPriority, currentUserId)`
   - `updateCategory(ticketId, newCategory, currentUserId)`
   - `updateTitle(ticketId, newTitle, currentUserId)`
   - `updateDescription(ticketId, newDescription, currentUserId)`

**TicketStateTransitionValidator:**
- Validate all state transitions per state machine matrix
- Throw InvalidStateTransitionException for invalid transitions
- Throw MissingRequiredFieldException for missing fields
- Throw UnauthorizedOperationException for unauthorized actions

**Retry Pattern:**
- Add spring-retry dependency
- @EnableRetry in application
- @Retryable on all TicketService write methods
- @Recover methods throw ConcurrentUpdateException after exhaustion
- Exponential backoff: 50ms, 100ms, 200ms with jitter

**TicketQueryService SPI:**
- Define TicketQueryService interface in `spi/` package
- Create TicketSummaryDTO and TicketSearchCriteria DTOs
- Implement TicketQueryServiceImpl in `internal/service/`
- @NamedInterface annotation for cross-module access

**Event Publishing:**
- Publish TicketCreatedEvent on ticket creation
- Publish TicketAssignedEvent on assignment
- Publish TicketStateChangedEvent on status/priority change
- All events go to transactional outbox (Epic 1 pattern)

**Exception Hierarchy:**
- Create `internal/exception/` package
- Define ConcurrentUpdateException
- Define InvalidStateTransitionException
- Define MissingRequiredFieldException
- Define UnauthorizedOperationException
- Define TicketNotFoundException

**Testing:**
- Unit tests for TicketService (all 10+ methods)
- Unit tests for TicketStateTransitionValidator (valid/invalid transitions)
- Unit tests for retry behavior (mock OptimisticLockException)
- Integration tests for state machine (IT-SM-1 to IT-SM-4)
- Integration tests for concurrent updates with retries
- SPI contract tests (verify cross-module query methods)
- Extend Story 2.1 LT2 to include service operations

---

### OUT OF SCOPE (Deferred to Later Stories)

❌ **REST API Controllers** (Story 2.3)
- TicketController endpoints
- Request/Response DTOs for API
- OpenAPI/Swagger spec
- Controller integration tests (MockMvc)

❌ **Routing Engine** (Story 2.4)
- Auto-assignment logic
- Routing rule evaluation
- Round-robin strategy
- Team workload balancing

❌ **Comments Service** (Story 2.5)
- Add comment to ticket
- Internal vs public comments
- Comment history

❌ **Agent Console APIs** (Story 2.6)
- AgentConsoleController
- CompositeSidePanelService
- Queue APIs with filtering/sorting
- Suggested resolution APIs

❌ **SLA Calculation** (Story 2.7+)
- SLA countdown logic
- Breach detection
- Escalation rules

❌ **Metrics Service** (Story 2.8+)
- MTTR calculation
- FRT calculation
- SLA compliance rate

---

## Story 2.2 Acceptance Criteria (Draft)

Based on finalized scope:

### Functional ACs

**AC-1**: TicketService implements 10+ state transition methods following state machine spike
**AC-2**: All state transitions validated by TicketStateTransitionValidator
**AC-3**: Invalid transitions throw InvalidStateTransitionException with clear message
**AC-4**: Required field validation throws MissingRequiredFieldException
**AC-5**: Unauthorized operations throw UnauthorizedOperationException
**AC-6**: All TicketService write methods annotated with @Retryable (exponential backoff)
**AC-7**: Retry exhaustion throws ConcurrentUpdateException after 4 attempts
**AC-8**: State transitions publish appropriate events to transactional outbox
**AC-9**: TicketQueryService SPI implemented with @NamedInterface annotation
**AC-10**: TicketSummaryDTO exposes ticket data for cross-module queries

### Testing ACs

**AC-11**: Unit tests cover all TicketService methods (>90% coverage)
**AC-12**: Unit tests verify retry behavior (2 retries succeed, 4 retries exhausted)
**AC-13**: Integration tests validate full ticket lifecycle (NEW → CLOSED)
**AC-14**: Integration tests validate reopen flow (RESOLVED → NEW, CLOSED → NEW)
**AC-15**: Integration tests validate concurrent updates with retries
**AC-16**: SPI contract tests verify all query methods
**AC-17**: Load test extends LT2 to include TicketService operations (<10% retry exhaustion)

### Non-Functional ACs

**AC-18**: Spring Modulith ModularityTests pass (SPI accessible, internals not accessible)
**AC-19**: All service methods have Javadoc with @since 2.2 tag
**AC-20**: Metrics track retry attempts and exhaustion rate

---

## Task Breakdown for Story 2.2

### Task 1: Setup & Dependencies (2 hours)
- [ ] Add spring-retry and spring-aspects dependencies to build.gradle.kts
- [ ] Add @EnableRetry to SynergyFlowApplication.java
- [ ] Create `internal/exception/` package with exception classes
- [ ] Create `spi/` package structure

### Task 2: State Machine Validator (3 hours)
- [ ] Implement TicketStateTransitionValidator per spike
- [ ] Add valid transition matrix (Map<TicketStatus, Set<TicketStatus>>)
- [ ] Implement validateTransition() method
- [ ] Unit tests for all valid/invalid transitions (20+ test cases)

### Task 3: TicketService - Create & Basic Operations (6 hours)
- [ ] Implement createTicket() with event publishing
- [ ] Implement updateTitle(), updateDescription(), updatePriority(), updateCategory()
- [ ] Add @Retryable annotations with backoff configuration
- [ ] Unit tests for create and update methods
- [ ] Integration test for ticket creation with event in outbox

### Task 4: TicketService - State Transitions (8 hours)
- [ ] Implement assignTicket(), unassignTicket(), reassignTicket()
- [ ] Implement startWork(), pauseWork()
- [ ] Implement resolveTicket(), closeTicket(), reopenTicket()
- [ ] Each method validates transition, updates state, publishes event
- [ ] Unit tests for all transition methods (valid cases)
- [ ] Unit tests for invalid transitions (exception cases)

### Task 5: Retry Pattern Integration (4 hours)
- [ ] Add @Recover methods for each @Retryable method
- [ ] Configure retry metrics (Micrometer counters)
- [ ] Unit tests mock OptimisticLockException and verify retries
- [ ] Integration test for concurrent assignment with retries

### Task 6: SPI Implementation (4 hours)
- [ ] Define TicketQueryService interface in spi/ package
- [ ] Create TicketSummaryDTO record
- [ ] Create TicketSearchCriteria record with builder
- [ ] Implement TicketQueryServiceImpl in internal/service/
- [ ] SPI contract tests verify all query methods

### Task 7: Integration Tests (6 hours)
- [ ] IT-SM-1: Full lifecycle test (NEW → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED)
- [ ] IT-SM-2: Reopen flow test (RESOLVED → NEW, CLOSED → NEW)
- [ ] IT-SM-3: Concurrent state change with retry
- [ ] IT-SM-4: Invalid transition exceptions
- [ ] IT-SM-5: SPI cross-module query test

### Task 8: Load Tests & Documentation (3 hours)
- [ ] Extend LT2 to include TicketService operations
- [ ] Verify <10% retry exhaustion under concurrent load
- [ ] Create ADR-010: Ticket State Machine
- [ ] Update module-boundaries.md with SPI usage examples
- [ ] Story completion review and cleanup

**Total Estimated Effort:** 36 hours (~1.5 weeks with buffer) → **10 story points**

---

## Sprint Planning Recommendation

### Sprint Allocation

**Story 2.2**: 1 sprint (2 weeks)
- Week 1: Tasks 1-4 (setup, validator, create/update, state transitions)
- Week 2: Tasks 5-8 (retry, SPI, integration tests, load tests, docs)

**Velocity Check:**
- Story 2.1: 8 points, 8 tasks, completed in 1 sprint ✅
- Story 2.2: 10 points, 8 tasks, estimated 1 sprint (with 20% buffer)

### Parallel Work Opportunity

**Week 2 of Story 2.2**: TicketService methods stable
- **New Story 2.3** can start: Define REST API DTOs and TicketController skeleton
- Frontend team can start mocking APIs based on SPI contract

---

## Decision Summary

### RECOMMENDED SCOPE: Option A - Service Layer Only

**Story Points:** 10 points
**Sprint Allocation:** 1 sprint (2 weeks)
**Deliverables:**
- ✅ TicketService with 10+ state transition methods
- ✅ TicketStateTransitionValidator with state machine enforcement
- ✅ @Retryable optimistic lock handling with exponential backoff
- ✅ TicketQueryService SPI for cross-module queries
- ✅ Event publishing to transactional outbox
- ✅ Comprehensive testing (unit, integration, load)

**Next Story:** Story 2.3 - Ticket REST API (8 points)

---

## Questions for Final Approval

**For Sarah (PO):**
1. ✅ Approve Option A (Service Layer Only) as Story 2.2 scope?
2. ✅ Agree Story 2.3 (REST API) provides demo-able feature for stakeholders?
3. ✅ Accept 10 story points estimate for Story 2.2?

**For Bob (SM):**
1. ✅ Confirm 1 sprint allocation (2 weeks) for 10 points?
2. ✅ Approve task breakdown (8 tasks, 36 hours estimated)?
3. ✅ Plan Story 2.3 to start in week 2 of Story 2.2 (parallel work)?

**For Winston (Architect):**
1. ✅ Validate SPI contract design aligns with Spring Modulith architecture?
2. ✅ Confirm state machine spike covers all Epic 2 requirements?

---

**SPIKE STATUS**: ✅ COMPLETE - Ready for Sprint Planning

**Recommended Decision**: **Option A - Service Layer Only (10 points, 1 sprint)**

**Next Steps:**
1. PO/SM approve recommended scope
2. Create Story 2.2 in backlog with 10 story points
3. Schedule sprint planning for Story 2.2
4. Begin Task 1 setup once sprint starts
