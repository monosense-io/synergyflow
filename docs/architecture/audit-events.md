# Audit Events for ITSM Ticket Operations

## Overview

Audit events provide a comprehensive audit trail for sensitive ticket operations, enabling compliance tracking, security auditing, and operational analysis. These events are published to the transactional outbox alongside business events, ensuring reliable delivery and maintaining consistency.

## Purpose

- **Compliance**: Meet regulatory requirements for tracking who performed what actions and when
- **Security**: Detect and investigate unauthorized or suspicious activity
- **Quality Assurance**: Track patterns of resolution failures through reopen audits
- **Operational Analysis**: Understand workload distribution and assignment patterns

## Event Types

### 1. TicketAssignmentAudit

Tracks all assignment-related operations (assign, unassign, reassign).

**Event Schema:**
```java
public record TicketAssignmentAudit(
    UUID ticketId,              // Ticket being assigned
    UUID currentAssigneeId,     // Assignee after operation (null for UNASSIGN)
    UUID previousAssigneeId,    // Assignee before operation (null for initial ASSIGN)
    UUID performedBy,           // User who performed the operation
    String operation,           // Operation type: ASSIGN, UNASSIGN, or REASSIGN
    Instant occurredAt,         // Timestamp of the operation
    Long version                // Ticket version after operation
)
```

**Operations:**
- **ASSIGN**: Initial assignment of an unassigned ticket
  - `currentAssigneeId`: Set to new assignee
  - `previousAssigneeId`: null
- **UNASSIGN**: Removal of assignment from a ticket
  - `currentAssigneeId`: null
  - `previousAssigneeId`: Set to previous assignee
- **REASSIGN**: Direct transfer from one assignee to another
  - `currentAssigneeId`: Set to new assignee
  - `previousAssigneeId`: Set to previous assignee

**Use Cases:**
- Track workload distribution across agents
- Identify tickets that are frequently reassigned
- Audit assignment changes for security compliance
- Analyze assignment patterns for capacity planning

### 2. TicketResolutionAudit

Tracks ticket resolution operations.

**Event Schema:**
```java
public record TicketResolutionAudit(
    UUID ticketId,              // Ticket being resolved
    UUID resolvedBy,            // User who resolved the ticket
    String resolutionNotes,     // Documentation of the resolution
    Instant resolvedAt,         // Timestamp of resolution
    Long version                // Ticket version after resolution
)
```

**Use Cases:**
- Verify that proper documentation standards are followed
- Track resolution time metrics
- Audit resolution quality for training purposes
- Comply with documentation requirements for audits

### 3. TicketReopenAudit

Tracks ticket reopen operations.

**Event Schema:**
```java
public record TicketReopenAudit(
    UUID ticketId,              // Ticket being reopened
    UUID reopenedBy,            // User who reopened the ticket
    String reopenReason,        // Documentation explaining why ticket was reopened
    Integer reopenCount,        // Total number of times ticket has been reopened
    Instant reopenedAt,         // Timestamp of reopen operation
    Long version                // Ticket version after reopen
)
```

**Use Cases:**
- Identify tickets with repeated reopen cycles (quality issues)
- Track patterns of inadequate initial resolutions
- Evaluate agent performance and training needs
- Detect systemic issues causing recurring problems

## Implementation Details

### Publishing Mechanism

Audit events are published to the transactional outbox using the `EventPublisher` interface, ensuring:
- **Atomicity**: Audit events are persisted in the same transaction as the ticket state change
- **Reliability**: Events are guaranteed to be delivered via the outbox pattern
- **Ordering**: Events maintain chronological order via `occurred_at` timestamp

### Integration Points

Audit events are published in the following TicketService methods:

| Method | Audit Event Type | Operation |
|--------|-----------------|-----------|
| `assignTicketInternal()` | TicketAssignmentAudit | ASSIGN |
| `unassignTicket()` | TicketAssignmentAudit | UNASSIGN |
| `reassignTicket()` | TicketAssignmentAudit | REASSIGN |
| `resolveTicketInternal()` | TicketResolutionAudit | N/A |
| `reopenTicketInternal()` | TicketReopenAudit | N/A |

### Database Schema

Audit events are stored in the `outbox` table with the following relevant fields:

```sql
CREATE TABLE outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,      -- Always "TICKET"
    aggregate_id UUID NOT NULL,                -- Ticket ID
    event_type VARCHAR(100) NOT NULL,          -- Event type (e.g., "TicketAssignmentAudit")
    event_payload JSONB NOT NULL,              -- Event data
    version BIGINT NOT NULL,                   -- Ticket version
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processing_error TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT idx_outbox_idempotency UNIQUE (aggregate_id, event_type, version)
);
```

**Note:** The unique constraint on `(aggregate_id, event_type, version)` allows multiple event types to be published for the same aggregate and version, which is essential for audit events that are published alongside business events.

## Query Examples

### Find all assignment changes for a ticket

```sql
SELECT
    event_payload->>'operation' as operation,
    event_payload->>'previousAssigneeId' as from_assignee,
    event_payload->>'currentAssigneeId' as to_assignee,
    event_payload->>'performedBy' as performed_by,
    occurred_at
FROM outbox
WHERE aggregate_id = '<ticket-id>'
  AND event_type = 'TicketAssignmentAudit'
ORDER BY occurred_at;
```

### Find tickets with multiple reopens

```sql
SELECT
    aggregate_id as ticket_id,
    COUNT(*) as reopen_count,
    ARRAY_AGG(event_payload->>'reopenReason' ORDER BY occurred_at) as reopen_reasons
FROM outbox
WHERE event_type = 'TicketReopenAudit'
GROUP BY aggregate_id
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC;
```

### Audit trail for a specific ticket

```sql
SELECT
    event_type,
    event_payload,
    occurred_at
FROM outbox
WHERE aggregate_id = '<ticket-id>'
  AND event_type IN ('TicketAssignmentAudit', 'TicketResolutionAudit', 'TicketReopenAudit')
ORDER BY occurred_at;
```

## Security Considerations

1. **Immutability**: Audit events are immutable once written to the outbox
2. **Tamper Protection**: Events include version numbers to detect tampering
3. **Access Control**: Audit event data should be restricted to authorized personnel
4. **Retention**: Audit events should be retained according to compliance requirements

## Future Enhancements

Potential improvements to the audit system:

1. **Audit Event Streaming**: Stream audit events to a dedicated audit log system (e.g., Splunk, ELK)
2. **Real-time Alerts**: Configure alerts for suspicious patterns (e.g., excessive reassignments)
3. **Audit Dashboard**: Build UI for visualizing audit trails and patterns
4. **Data Retention Policies**: Implement automated archival and purging of old audit events
5. **Additional Audit Events**: Expand to cover more operations (e.g., priority changes, comment additions)

## References

- [ADR-010: Ticket State Machine](./adr/ADR-010-ticket-state-machine.md)
- [Module Boundaries](./module-boundaries.md)
- [Story 2.2 Technical Specification](../stories/story-2.2-REVISED.md)
