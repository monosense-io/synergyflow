# ADR-010: Ticket State Machine and Transition Rules

**Date:** 2025-10-09
**Status:** Accepted
**Authors:** Winston (Architect), Sarah (PO)
**Story:** Story 2.2 - Ticket Service Layer
**Related Spike:** `docs/spikes/spike-ticket-state-machine.md`

---

## Context

ITSM ticket lifecycle requires well-defined state transitions to ensure data integrity, business rule enforcement, and audit compliance. Without a formal state machine, invalid transitions could occur (e.g., NEW → RESOLVED skipping assignment), leading to incomplete workflows and poor metrics.

**Problem:**
- Tickets have 5 states (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)
- Not all state transitions are valid (e.g., cannot resolve a ticket that hasn't been worked on)
- Different ticket types (Incident, ServiceRequest) have additional constraints
- Concurrent updates require optimistic locking and retry logic
- State changes must publish events for metrics, notifications, and workflows

**Requirements:**
- Enforce valid state transitions per business rules
- Validate required fields for each transition (e.g., resolutionNotes required for resolve)
- Support reopen workflow (RESOLVED/CLOSED → NEW)
- Distinguish between Incident severity escalation and ServiceRequest approval workflow
- Maintain auditability (who made the transition, when, why)

---

## Decision

Implement a **finite state machine** for ticket lifecycle with strict validation:

### State Transition Matrix

```
┌─────────────────────────────────────────────────────────────┐
│                   TICKET STATE MACHINE                       │
└─────────────────────────────────────────────────────────────┘

    [CREATE]
       │
       ▼
     ┌───┐
     │NEW│ ◄──────────────────────────┐
     └───┘                            │
       │                              │ [REOPEN]
       │ [ASSIGN]                     │
       ▼                              │
  ┌────────┐                          │
  │ASSIGNED│                          │
  └────────┘                          │
       │                              │
       │ [START_WORK]                 │
       ▼                              │
┌────────────┐                        │
│IN_PROGRESS │                        │
└────────────┘                        │
       │                              │
       │ [RESOLVE]                    │
       ▼                              │
  ┌──────────┐                        │
  │ RESOLVED │ ───────────────────────┘
  └──────────┘
       │
       │ [CLOSE]
       ▼
   ┌────────┐
   │ CLOSED │ (Terminal State)
   └────────┘
```

### Valid Transitions

| From State   | To State     | Action        | Required Fields | Authorization |
|--------------|--------------|---------------|-----------------|---------------|
| NEW          | ASSIGNED     | assign()      | assigneeId      | Any agent     |
| ASSIGNED     | NEW          | unassign()    | -               | Assignee or manager |
| ASSIGNED     | IN_PROGRESS  | startWork()   | -               | Assignee only |
| IN_PROGRESS  | ASSIGNED     | pauseWork()   | -               | Assignee only |
| IN_PROGRESS  | RESOLVED     | resolve()     | resolutionNotes | Assignee only |
| RESOLVED     | CLOSED       | close()       | -               | Any agent     |
| RESOLVED     | NEW          | reopen()      | reopenReason    | Requester or agent |
| CLOSED       | NEW          | reopen()      | reopenReason    | Agent only (with approval) |

### Implementation Components

1. **TicketStateTransitionValidator** - Validates all transitions
   - Maintains valid transition map: `Map<TicketStatus, Set<TicketStatus>>`
   - Throws `InvalidStateTransitionException` for invalid transitions
   - Validates required fields (throws `MissingRequiredFieldException`)
   - Validates authorization (throws `UnauthorizedOperationException`)

2. **TicketService State Transition Methods**:
   - `assignTicket(ticketId, assigneeId, currentUserId)`
   - `unassignTicket(ticketId, currentUserId)`
   - `reassignTicket(ticketId, newAssigneeId, currentUserId)`
   - `startWork(ticketId, currentUserId)`
   - `pauseWork(ticketId, currentUserId)`
   - `resolveTicket(ticketId, resolutionNotes, currentUserId)`
   - `closeTicket(ticketId, currentUserId)`
   - `reopenTicket(ticketId, reopenReason, currentUserId)`

3. **Business Rules Enforcement**:
   - **BR-1**: Only NEW tickets can be assigned for the first time
   - **BR-2**: Only ASSIGNED tickets can transition to IN_PROGRESS
   - **BR-3**: startWork() must be called by assigned agent (assigneeId == currentUserId)
   - **BR-4**: resolve() requires resolutionNotes (minimum 10 characters)
   - **BR-5**: CLOSED is terminal (only reopen operation allowed)
   - **BR-6**: Reopen from CLOSED requires approval (future enhancement)

4. **Event Publishing**:
   - State transitions publish `TicketStateChangedEvent` to transactional outbox
   - Events include: ticketId, oldStatus, newStatus, changedBy, changedAt, reason

5. **Incident-Specific Extensions**:
   - Severity field (CRITICAL, HIGH, MEDIUM, LOW)
   - Severity escalation rules (auto-escalate if not assigned within SLA)
   - resolvedAt timestamp set on RESOLVED state

6. **ServiceRequest-Specific Extensions**:
   - Approval workflow (PENDING → APPROVED → allow assignment)
   - Rejected requests move directly to CLOSED
   - fulfilledAt timestamp set on RESOLVED state

---

## Consequences

### Pros

✅ **Data Integrity**: Invalid state transitions prevented at service layer (fail-fast)
✅ **Auditability**: All state changes logged with who/when/why via events
✅ **Testability**: State machine rules isolated in validator (easy to unit test)
✅ **Extensibility**: New states or transitions added to transition map without refactoring
✅ **Business Alignment**: State machine matches ITIL best practices for ITSM workflows
✅ **Metrics Accuracy**: Clean state transitions ensure accurate MTTR, FRT calculations
✅ **Reopen Support**: Tickets can be reopened with reason tracking (customer feedback loop)

### Cons

⚠️ **Implementation Complexity**: 10+ service methods, 20+ test cases for all transitions
⚠️ **Performance Overhead**: Validator called on every state transition (negligible ~1ms)
⚠️ **Approval Workflow Gap**: ServiceRequest approval requires orthogonal state (approvalStatus) separate from TicketStatus—adds complexity
⚠️ **Reopen from CLOSED**: Requires approval workflow (not implemented in Story 2.2, deferred to Story 2.5)

### Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Edge cases missed (e.g., concurrent state changes) | Medium | Comprehensive integration tests (IT-SM-1 to IT-SM-4) + optimistic locking with retries |
| ServiceRequest approval complicates state machine | Medium | Treat approvalStatus as orthogonal state, validate before allowing assignment |
| Reopen abuse (users repeatedly reopen tickets) | Low | Track reopenCount field, add business rule to limit reopens (future enhancement) |
| Invalid transitions in API layer bypass validation | High | Enforce validation in service layer (not controller), controller calls service methods only |

---

## Alternatives Considered

### Alternative 1: No State Machine (Ad-Hoc Transitions)

**Approach:** Allow any state transition, validate at API layer only

**Rejected Because:**
- ❌ Data integrity issues (invalid states in database)
- ❌ Difficult to test (no single source of truth)
- ❌ Duplicate validation logic across API/Service layers
- ❌ No protection against direct repository access

### Alternative 2: Database Check Constraints

**Approach:** Enforce valid transitions with PostgreSQL check constraints

**Rejected Because:**
- ❌ Business rules too complex for SQL (authorization, required fields)
- ❌ Poor error messages (database constraint violations)
- ❌ Difficult to test without database
- ❌ Migration complexity (altering constraints on production tables)

### Alternative 3: Spring State Machine Framework

**Approach:** Use Spring State Machine library

**Rejected Because:**
- ❌ Overkill for simple 5-state machine
- ❌ Additional dependency and learning curve
- ❌ Performance overhead (framework abstractions)
- ✅ **Custom validator is simpler and sufficient**

---

## Implementation Notes

### Exception Hierarchy

```java
BusinessException (base)
├── InvalidStateTransitionException (NEW → RESOLVED)
├── MissingRequiredFieldException (resolve without resolutionNotes)
├── UnauthorizedOperationException (non-assignee calls startWork)
└── ConcurrentUpdateException (OptimisticLockException after retries)
```

### Testing Strategy

**Unit Tests:**
- All valid transitions succeed (8 transitions × 2 test cases = 16 tests)
- All invalid transitions throw InvalidStateTransitionException (20+ edge cases)
- Required field validation (4 tests)
- Authorization validation (3 tests)

**Integration Tests:**
- IT-SM-1: Full lifecycle (NEW → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED)
- IT-SM-2: Reopen flow (RESOLVED → NEW, CLOSED → NEW)
- IT-SM-3: Concurrent state change with OptimisticLockException retry
- IT-SM-4: Invalid transitions rejected with proper exceptions

**Load Tests:**
- Extend Story 2.1 LT2 to include TicketService state transitions
- Verify <10% retry exhaustion under concurrent load

---

## References

- **Spike Document**: `docs/spikes/spike-ticket-state-machine.md` (complete specification)
- **Story**: Story 2.2 - Ticket Service Layer
- **Related ADRs**:
  - ADR-002: UUIDv7 (time-ordered ticket IDs)
  - ADR-009: Integration Tests with Testcontainers
- **ITIL Framework**: Incident Management lifecycle (Logged → In Progress → Resolved → Closed)
- **Epic 2 Tech Spec**: `docs/epics/epic-2-itsm-tech-spec.md` (ITSM module architecture)

---

## Review History

| Date       | Reviewer | Decision | Notes |
|------------|----------|----------|-------|
| 2025-10-09 | Winston  | Approved | State machine aligns with Spring Modulith architecture |
| 2025-10-09 | Sarah    | Approved | Business rules match ITSM requirements |
| 2025-10-09 | James    | Approved | Implementation plan clear, ready for Story 2.2 |
| 2025-10-09 | Murat    | Approved | Test strategy comprehensive |

---

**Status:** ✅ **ACCEPTED** - Ready for implementation in Story 2.2
