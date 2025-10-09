package io.monosense.synergyflow.itsm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit event emitted when a ticket is reopened.
 *
 * <p>This event captures the reopening of resolved or closed tickets, recording who
 * reopened the ticket, why it was reopened, and the historical reopen count. Reopening
 * is a sensitive operation that indicates the original resolution was insufficient or
 * incorrect, making it critical for quality tracking and process improvement.</p>
 *
 * <p>The event includes the reopen count to track tickets that are repeatedly reopened,
 * which may indicate systemic issues, inadequate initial resolutions, or recurring
 * problems. This metric is valuable for quality assurance, agent performance evaluation,
 * and identifying training opportunities.</p>
 *
 * <p>The reopen reason is mandatory and provides context for why the ticket needed to
 * be reopened. This documentation is essential for compliance, quality audits, and
 * understanding patterns of resolution failures.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * TicketReopenAudit event = new TicketReopenAudit(
 *     ticket.getId(),
 *     currentUserId,
 *     reopenReason,
 *     ticket.getReopenCount(),
 *     Instant.now(),
 *     ticket.getVersion()
 * );
 *
 * eventPublisher.publish(
 *     event.ticketId(),
 *     "TICKET",
 *     "TicketReopenAudit",
 *     event.version(),
 *     event.reopenedAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 2.2
 *
 * @param ticketId the unique identifier of the reopened ticket
 * @param reopenedBy the unique identifier of the user who reopened the ticket
 * @param reopenReason the documentation explaining why the ticket was reopened
 * @param reopenCount the total number of times this ticket has been reopened (after this operation)
 * @param reopenedAt the timestamp when the ticket was reopened
 * @param version the version number of the ticket aggregate after reopening
 */
public record TicketReopenAudit(
        UUID ticketId,
        UUID reopenedBy,
        String reopenReason,
        Integer reopenCount,
        Instant reopenedAt,
        Long version
) {
}
