# SPIKE: Ticket State Machine Definition

**Date:** 2025-10-09
**Owners:** Winston (Architect) + Sarah (PO)
**Story:** Story 2.2 Preparation Sprint
**Status:** 🔴 CRITICAL PATH - Must complete before Story 2.2
**Estimated Effort:** 4 hours

---

## Objective

Define the complete ticket state machine with valid state transitions, validation rules, and business constraints for implementation in TicketService (Story 2.2).

---

## Ticket States (TicketStatus Enum)

Based on Story 2.1 implementation:

```java
public enum TicketStatus {
    NEW,           // Initial state when ticket created
    ASSIGNED,      // Ticket has been assigned to an agent
    IN_PROGRESS,   // Agent is actively working on the ticket
    RESOLVED,      // Issue has been resolved, awaiting confirmation
    CLOSED         // Ticket is closed and archived
}
```

---

## State Transition Rules

### Valid State Transitions

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

### Transition Matrix

| From State   | To State     | Action        | Required Fields | Validation Rules |
|--------------|--------------|---------------|-----------------|------------------|
| NEW          | ASSIGNED     | assign()      | assigneeId      | assigneeId must not be null, must be valid user |
| NEW          | IN_PROGRESS  | ❌ INVALID    | -               | Must assign first |
| ASSIGNED     | IN_PROGRESS  | startWork()   | -               | assigneeId must match current user |
| ASSIGNED     | NEW          | unassign()    | -               | Clear assigneeId |
| IN_PROGRESS  | RESOLVED     | resolve()     | resolutionNotes | resolutionNotes required (min 10 chars) |
| IN_PROGRESS  | ASSIGNED     | pauseWork()   | -               | Allowed for work interruption |
| RESOLVED     | CLOSED       | close()       | -               | Optional: Customer confirmation |
| RESOLVED     | NEW          | reopen()      | reopenReason    | reopenReason required (min 10 chars) |
| CLOSED       | NEW          | reopen()      | reopenReason    | reopenReason required, requires approval |
| CLOSED       | ❌ Any other | ❌ INVALID    | -               | Closed is terminal (except reopen) |

---

## Business Rules

### Rule 1: Assignment Rules
- **BR-1.1**: Only NEW tickets can be assigned for the first time
- **BR-1.2**: ASSIGNED tickets can be reassigned to different agent (state stays ASSIGNED)
- **BR-1.3**: IN_PROGRESS tickets can be reassigned (triggers automatic pauseWork() → assign())
- **BR-1.4**: assigneeId must reference valid user in users table
- **BR-1.5**: Unassigning a ticket (assigneeId = null) moves state back to NEW

### Rule 2: Work Progression Rules
- **BR-2.1**: Only ASSIGNED tickets can transition to IN_PROGRESS
- **BR-2.2**: startWork() must be called by the assigned agent (assigneeId == currentUserId)
- **BR-2.3**: IN_PROGRESS tickets can be paused back to ASSIGNED (work interruption)
- **BR-2.4**: Only IN_PROGRESS tickets can be resolved

### Rule 3: Resolution Rules
- **BR-3.1**: resolve() requires resolutionNotes (minimum 10 characters)
- **BR-3.2**: For Incident subtype: resolvedAt timestamp set to Instant.now()
- **BR-3.3**: For ServiceRequest subtype: fulfilledAt timestamp set to Instant.now()
- **BR-3.4**: Priority cannot be changed once ticket is RESOLVED or CLOSED

### Rule 4: Closure Rules
- **BR-4.1**: Only RESOLVED tickets can be closed (normal flow)
- **BR-4.2**: Closing directly from IN_PROGRESS is NOT allowed (must resolve first)
- **BR-4.3**: CLOSED is a terminal state (no further updates except reopen)

### Rule 5: Reopen Rules
- **BR-5.1**: RESOLVED tickets can be reopened to NEW with reopenReason
- **BR-5.2**: CLOSED tickets can be reopened to NEW with reopenReason + approval
- **BR-5.3**: Reopen clears resolutionNotes and resolvedAt/fulfilledAt timestamps
- **BR-5.4**: Reopen increments reopenCount field (track reopen frequency)

---

## State-Specific Field Requirements

### NEW State
- **Required**: title, description, status=NEW, priority, requesterId, createdAt
- **Optional**: category, assigneeId (null until assigned)
- **Forbidden**: resolutionNotes, resolvedAt, fulfilledAt

### ASSIGNED State
- **Required**: All NEW fields + assigneeId (not null)
- **Business Rule**: Auto-assign via RoutingEngine if category matches routing rule

### IN_PROGRESS State
- **Required**: All ASSIGNED fields
- **Business Rule**: SLA countdown starts (or continues from ASSIGNED)

### RESOLVED State
- **Required**: All IN_PROGRESS fields + resolutionNotes
- **Incident**: resolvedAt = Instant.now()
- **ServiceRequest**: fulfilledAt = Instant.now()

### CLOSED State
- **Required**: All RESOLVED fields
- **Forbidden**: Any updates except reopen operation
- **Business Rule**: SLA tracking stops, metrics (MTTR, FRT) calculated

---

## Exception Handling

### InvalidStateTransitionException

Thrown when attempting invalid state transition:

```java
public class InvalidStateTransitionException extends BusinessException {
    public InvalidStateTransitionException(TicketStatus from, TicketStatus to) {
        super(String.format("Invalid state transition: %s → %s", from, to));
    }
}
```

**Example Scenarios:**
- NEW → RESOLVED (must go through ASSIGNED → IN_PROGRESS)
- CLOSED → ASSIGNED (closed is terminal, must reopen first)
- IN_PROGRESS → CLOSED (must resolve first)

### MissingRequiredFieldException

Thrown when required fields missing for state transition:

```java
public class MissingRequiredFieldException extends BusinessException {
    public MissingRequiredFieldException(String field, String operation) {
        super(String.format("Required field '%s' missing for operation: %s", field, operation));
    }
}
```

**Example Scenarios:**
- assign() without assigneeId
- resolve() without resolutionNotes
- reopen() without reopenReason

### UnauthorizedOperationException

Thrown when user lacks permission for operation:

```java
public class UnauthorizedOperationException extends BusinessException {
    public UnauthorizedOperationException(String operation, UUID userId) {
        super(String.format("User %s not authorized for operation: %s", userId, operation));
    }
}
```

**Example Scenarios:**
- startWork() called by user who is not the assignee
- close() called by requester (only agents can close)

---

## TicketService Method Signatures

Based on state machine, TicketService must implement:

```java
public interface TicketService {

    // CREATE operation (NEW state)
    Ticket createTicket(CreateTicketCommand cmd);

    // ASSIGN operation (NEW → ASSIGNED)
    Ticket assignTicket(UUID ticketId, UUID assigneeId, UUID currentUserId);

    // UNASSIGN operation (ASSIGNED → NEW)
    Ticket unassignTicket(UUID ticketId, UUID currentUserId);

    // REASSIGN operation (ASSIGNED/IN_PROGRESS → ASSIGNED)
    Ticket reassignTicket(UUID ticketId, UUID newAssigneeId, UUID currentUserId);

    // START_WORK operation (ASSIGNED → IN_PROGRESS)
    Ticket startWork(UUID ticketId, UUID currentUserId);

    // PAUSE_WORK operation (IN_PROGRESS → ASSIGNED)
    Ticket pauseWork(UUID ticketId, UUID currentUserId);

    // RESOLVE operation (IN_PROGRESS → RESOLVED)
    Ticket resolveTicket(UUID ticketId, String resolutionNotes, UUID currentUserId);

    // CLOSE operation (RESOLVED → CLOSED)
    Ticket closeTicket(UUID ticketId, UUID currentUserId);

    // REOPEN operation (RESOLVED/CLOSED → NEW)
    Ticket reopenTicket(UUID ticketId, String reopenReason, UUID currentUserId);

    // UPDATE operations (metadata changes without state change)
    Ticket updatePriority(UUID ticketId, Priority newPriority, UUID currentUserId);
    Ticket updateCategory(UUID ticketId, String newCategory, UUID currentUserId);
}
```

---

## Implementation Guidelines for Story 2.2

### Step 1: Create State Transition Validator

```java
@Component
class TicketStateTransitionValidator {

    public void validateTransition(TicketStatus from, TicketStatus to) {
        if (!isValidTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }

    private boolean isValidTransition(TicketStatus from, TicketStatus to) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.of(
        NEW, Set.of(ASSIGNED),
        ASSIGNED, Set.of(NEW, IN_PROGRESS),
        IN_PROGRESS, Set.of(ASSIGNED, RESOLVED),
        RESOLVED, Set.of(NEW, CLOSED),
        CLOSED, Set.of(NEW)  // Reopen only
    );
}
```

### Step 2: Implement TicketService Methods

Each service method must:
1. **Load ticket with optimistic lock**: `Ticket ticket = repository.findById(id).orElseThrow()`
2. **Validate current state**: Check ticket.getStatus() allows requested transition
3. **Validate required fields**: Check all required fields present
4. **Perform state transition**: Update ticket.setStatus(newStatus)
5. **Update metadata**: Set timestamps, clear fields, increment counters
6. **Save with version check**: `repository.save(ticket)` (auto-increment version)
7. **Publish event**: `eventPublisher.publishEvent(new TicketStateChangedEvent(...))`
8. **Handle OptimisticLockException**: Retry up to 3 times with exponential backoff

### Step 3: Add Unit Tests

For each state transition:
- ✅ Valid transition succeeds
- ❌ Invalid transition throws InvalidStateTransitionException
- ❌ Missing required field throws MissingRequiredFieldException
- ❌ Unauthorized user throws UnauthorizedOperationException

### Step 4: Add Integration Tests

- **IT-SM-1**: Full lifecycle (NEW → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED)
- **IT-SM-2**: Reopen flow (RESOLVED → NEW, CLOSED → NEW)
- **IT-SM-3**: Concurrent state change with OptimisticLockException retry
- **IT-SM-4**: Invalid transitions rejected with proper exceptions

---

## Incident-Specific Extensions

Incidents have additional severity field:

### Severity Escalation Rules
- **SE-1**: CRITICAL incidents auto-escalate if not ASSIGNED within 30 minutes
- **SE-2**: HIGH incidents auto-escalate if not ASSIGNED within 1 hour
- **SE-3**: Severity can be upgraded (MEDIUM → HIGH) but not downgraded once IN_PROGRESS

---

## ServiceRequest-Specific Extensions

ServiceRequests have approval workflow:

### Approval Flow (Parallel to State Machine)
```
[CREATE] → approvalStatus = PENDING
[MANAGER_APPROVE] → approvalStatus = APPROVED → Allow ASSIGN
[MANAGER_REJECT] → approvalStatus = REJECTED → Auto CLOSE with rejection reason
```

**SR-1**: ServiceRequests cannot be ASSIGNED until approvalStatus = APPROVED
**SR-2**: Rejected ServiceRequests move directly to CLOSED (skip normal flow)
**SR-3**: approvalStatus is independent of TicketStatus (orthogonal state)

---

## Metrics Impact

State transitions trigger metrics updates:

| Transition     | Metric Impact |
|----------------|---------------|
| NEW → ASSIGNED | FRT (First Response Time) = assignedAt - createdAt |
| IN_PROGRESS → RESOLVED | Resolution Time = resolvedAt - createdAt |
| RESOLVED → CLOSED | MTTR (Mean Time To Resolution) calculation |
| Any → Any | Update tickets_by_status dashboard count |

---

## ADR Reference

This spike should be formalized as:
- **ADR-010: Ticket State Machine and Transition Rules**

Location: `docs/architecture/adr/ADR-010-ticket-state-machine.md`

---

## Acceptance Criteria for Story 2.2

Based on this spike, Story 2.2 must include:

- ✅ AC-SM-1: TicketService implements all 10 state transition methods
- ✅ AC-SM-2: TicketStateTransitionValidator validates all transitions per matrix
- ✅ AC-SM-3: Invalid transitions throw InvalidStateTransitionException with clear message
- ✅ AC-SM-4: Required field validation throws MissingRequiredFieldException
- ✅ AC-SM-5: Unauthorized operations throw UnauthorizedOperationException
- ✅ AC-SM-6: State transitions publish TicketStateChangedEvent to outbox
- ✅ AC-SM-7: Integration tests cover full lifecycle + reopen + invalid transitions
- ✅ AC-SM-8: Incident severity escalation rules implemented
- ✅ AC-SM-9: ServiceRequest approval workflow integrated with state machine
- ✅ AC-SM-10: Metrics calculations triggered on state transitions

---

## Questions for PO/Architect Review

1. **Reopen Approval**: Should CLOSED → NEW require manager approval, or is reopenReason sufficient?
2. **Direct Closure**: Should we allow ASSIGNED/IN_PROGRESS → CLOSED for "duplicate" or "spam" tickets?
3. **Severity Escalation**: Should escalation be automated (Story 2.2) or manual (deferred to Story 2.5)?
4. **Approval Workflow**: Should ServiceRequest approval be in Story 2.2 or split to Story 2.4?

---

**SPIKE STATUS**: ✅ COMPLETE - Ready for Story 2.2 Implementation

**Next Steps:**
1. Review state machine with Sarah (PO) - confirm business rules
2. Create ADR-010 formalizing this design
3. Use transition matrix to generate TicketService unit test cases
4. Implement TicketStateTransitionValidator as first Story 2.2 task
