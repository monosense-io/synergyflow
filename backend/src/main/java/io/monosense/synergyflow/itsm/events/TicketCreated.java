package io.monosense.synergyflow.itsm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a ticket is created.
 *
 * <p>This event captures the complete state of a newly created ticket and is published
 * to notify other parts of the system about ticket creation. It contains all essential
 * ticket information needed for downstream processing and read model updates.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * TicketCreated event = new TicketCreated(
 *     ticket.getId(),
 *     ticket.getTicketNumber(),
 *     ticket.getTitle(),
 *     ticket.getStatus(),
 *     ticket.getPriority(),
 *     ticket.getRequesterId(),
 *     ticket.getCreatedAt(),
 *     ticket.getUpdatedAt(),
 *     ticket.getVersion()
 * );
 *
 * eventPublisher.publish(
 *     event.ticketId(),
 *     "TICKET",
 *     "TicketCreated",
 *     event.version(),
 *     event.createdAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.1.0
 *
 * @param ticketId the unique identifier of the created ticket
 * @param ticketNumber the human-readable ticket number
 * @param title the descriptive title of the ticket
 * @param status the initial status of the ticket (typically "OPEN")
 * @param priority the priority level of the ticket
 * @param requesterId the unique identifier of the user who created the ticket
 * @param createdAt the timestamp when the ticket was created
 * @param updatedAt the timestamp when the ticket was last updated (same as createdAt for new tickets)
 * @param version the optimistic locking version of the ticket (typically 1L for new tickets)
 */
public record TicketCreated(
        UUID ticketId,
        String ticketNumber,
        String title,
        String status,
        String priority,
        UUID requesterId,
        UUID assigneeId,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
