package io.monosense.synergyflow.itsm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit event emitted when a ticket is resolved.
 *
 * <p>This event captures the resolution of tickets, recording who resolved the ticket,
 * when it was resolved, and the resolution notes provided. This audit trail is critical
 * for compliance, quality assurance, and service level agreement (SLA) tracking.</p>
 *
 * <p>Resolution is a sensitive operation that marks a ticket as complete and requires
 * mandatory resolution notes documenting the solution. This event enables reconstruction
 * of resolution history, analysis of resolution patterns, and validation that proper
 * documentation standards were followed.</p>
 *
 * <p>The event includes the complete resolution notes to ensure the audit trail contains
 * sufficient information for compliance reviews and quality audits. The resolvedBy field
 * captures who performed the resolution, which is typically the assigned agent but may
 * differ in cases of supervisor intervention or automated resolution.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * TicketResolutionAudit event = new TicketResolutionAudit(
 *     ticket.getId(),
 *     currentUserId,
 *     resolutionNotes,
 *     Instant.now(),
 *     ticket.getVersion()
 * );
 *
 * eventPublisher.publish(
 *     event.ticketId(),
 *     "TICKET",
 *     "TicketResolutionAudit",
 *     event.version(),
 *     event.resolvedAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 2.2
 *
 * @param ticketId the unique identifier of the resolved ticket
 * @param resolvedBy the unique identifier of the user who resolved the ticket
 * @param resolutionNotes the documentation describing how the ticket was resolved
 * @param resolvedAt the timestamp when the ticket was resolved
 * @param version the version number of the ticket aggregate after resolution
 */
public record TicketResolutionAudit(
        UUID ticketId,
        UUID resolvedBy,
        String resolutionNotes,
        Instant resolvedAt,
        Long version
) {
}
