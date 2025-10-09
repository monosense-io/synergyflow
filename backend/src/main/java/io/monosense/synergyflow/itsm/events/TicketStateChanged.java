package io.monosense.synergyflow.itsm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a ticket's status changes.
 *
 * <p>This event captures state transitions in the ticket lifecycle, such as
 * moving from NEW to ASSIGNED, IN_PROGRESS to RESOLVED, etc. It includes both
 * the previous and new status to enable event sourcing, audit logging, and
 * state machine validation in downstream consumers.</p>
 *
 * <p>This event is published for all state transitions including:</p>
 * <ul>
 *   <li>Unassignment (ASSIGNED → NEW)</li>
 *   <li>Start work (ASSIGNED → IN_PROGRESS)</li>
 *   <li>Pause work (IN_PROGRESS → ASSIGNED)</li>
 *   <li>Resolution (IN_PROGRESS → RESOLVED)</li>
 *   <li>Closure (RESOLVED → CLOSED)</li>
 *   <li>Reopening (RESOLVED/CLOSED → NEW)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * TicketStateChanged event = new TicketStateChanged(
 *     ticket.getId(),
 *     oldStatus.name(),
 *     newStatus.name(),
 *     ticket.getUpdatedAt(),
 *     ticket.getVersion()
 * );
 *
 * eventPublisher.publish(
 *     event.ticketId(),
 *     "TICKET",
 *     "TicketStateChanged",
 *     event.version(),
 *     event.updatedAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 2.2
 *
 * @param ticketId the unique identifier of the ticket
 * @param oldStatus the previous status before the transition
 * @param newStatus the new status after the transition
 * @param updatedAt the timestamp when the state change occurred
 * @param version the version number of the ticket aggregate after this change
 */
public record TicketStateChanged(
        UUID ticketId,
        String oldStatus,
        String newStatus,
        Instant updatedAt,
        Long version
) {
}
