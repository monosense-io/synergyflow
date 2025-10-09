package io.monosense.synergyflow.itsm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit event emitted when a ticket assignment operation occurs.
 *
 * <p>This event captures all assignment-related operations including assignment,
 * unassignment, and reassignment of tickets. It provides a complete audit trail
 * for sensitive operations that change ticket ownership, enabling compliance
 * tracking, security auditing, and operational analysis.</p>
 *
 * <p>The event includes both previous and current assignee information to enable
 * full reconstruction of the assignment history. For unassignment operations,
 * the currentAssigneeId will be null. For initial assignments, previousAssigneeId
 * will be null.</p>
 *
 * <p>Supported operations:</p>
 * <ul>
 *   <li>ASSIGN - Initial assignment of an unassigned ticket</li>
 *   <li>UNASSIGN - Removal of assignment from a ticket</li>
 *   <li>REASSIGN - Direct transfer from one assignee to another</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * TicketAssignmentAudit event = new TicketAssignmentAudit(
 *     ticket.getId(),
 *     newAssigneeId,
 *     previousAssigneeId,
 *     currentUserId,
 *     "REASSIGN",
 *     Instant.now(),
 *     ticket.getVersion()
 * );
 *
 * eventPublisher.publish(
 *     event.ticketId(),
 *     "TICKET",
 *     "TicketAssignmentAudit",
 *     event.version(),
 *     event.occurredAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 2.2
 *
 * @param ticketId the unique identifier of the ticket
 * @param currentAssigneeId the assignee after the operation (null for UNASSIGN)
 * @param previousAssigneeId the assignee before the operation (null for initial ASSIGN)
 * @param performedBy the unique identifier of the user who performed the operation
 * @param operation the type of assignment operation (ASSIGN, UNASSIGN, REASSIGN)
 * @param occurredAt the timestamp when the operation occurred
 * @param version the version number of the ticket aggregate after this operation
 */
public record TicketAssignmentAudit(
        UUID ticketId,
        UUID currentAssigneeId,
        UUID previousAssigneeId,
        UUID performedBy,
        String operation,
        Instant occurredAt,
        Long version
) {
}
