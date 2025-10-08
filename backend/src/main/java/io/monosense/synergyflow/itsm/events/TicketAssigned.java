package io.monosense.synergyflow.itsm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a ticket is assigned to an agent or user.
 *
 * <p>This event captures the assignment of a ticket to a specific assignee and includes
 * information about who performed the assignment and when it occurred. This event is used
 * to notify other parts of the system about ticket assignment changes, update read models,
 * and trigger any follow-up actions such as notifications or workflow transitions.</p>
 *
 * <p>The event includes the aggregate version to support event ordering and conflict
 * resolution in distributed systems. The assignedAt timestamp represents when the
 * assignment occurred, which may differ from the ticket's updatedAt timestamp if
 * multiple operations occurred in the same transaction.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * TicketAssigned event = new TicketAssigned(
 *     ticket.getId(),
 *     agentId,
 *     assignedBy,
 *     Instant.now(),
 *     ticket.getVersion()
 * );
 *
 * eventPublisher.publish(
 *     event.ticketId(),
 *     "TICKET",
 *     "TicketAssigned",
 *     event.version(),
 *     event.assignedAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 *
 * @param ticketId the unique identifier of the assigned ticket
 * @param assigneeId the unique identifier of the user the ticket is assigned to
 * @param assignedBy the unique identifier of the user who performed the assignment
 * @param assignedAt the timestamp when the assignment occurred
 * @param version the version number of the ticket aggregate after this assignment
 */
public record TicketAssigned(
        UUID ticketId,
        UUID assigneeId,
        UUID assignedBy,
        Instant assignedAt,
        Long version
) {
}
