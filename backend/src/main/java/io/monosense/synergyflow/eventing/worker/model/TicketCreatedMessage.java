package io.monosense.synergyflow.eventing.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Event message representing the creation of a new IT service management ticket.
 *
 * <p>This immutable record captures all essential information about a newly created ticket
 * that needs to be propagated through the event-driven architecture. It serves as the
 * canonical data structure for ticket creation events consumed by read model updaters,
 * notification services, and other downstream systems.</p>
 *
 * <p><strong>Event Flow:</strong>
 * <ol>
 *   <li>Ticket is created via domain service or API endpoint</li>
 *   <li>TicketCreatedMessage is published to the outbox table</li>
 *   <li>Worker processes the message and updates read models</li>
 *   <li>Notification services may send alerts or emails</li>
 *   <li>Analytics systems may record metrics</li>
 * </ol>
 *
 * <p><strong>Data Integrity:</strong>
 * <ul>
 *   <li>All fields are immutable after creation</li>
 *   <li>Unknown JSON properties are ignored for forward compatibility</li>
 *   <li>Timestamps represent actual event occurrence time</li>
 *   <li>Null values are allowed for optional information</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create a ticket creation event
 * TicketCreatedMessage event = new TicketCreatedMessage(
 *     ticketId,
 *     "TICKET-001",
 *     "Login issue reported",
 *     "OPEN",
 *     "HIGH",
 *     requesterId,
 *     "John Doe",
 *     "john.doe@company.com",
 *     Instant.now(),
 *     Instant.now()
 * );
 *
 * // Process the event in read model updater
 * public void handleTicketCreated(TicketCreatedMessage message) {
 *     TicketCardEntity entity = mapper.toTicketCard(message, 1L);
 *     repository.save(entity);
 * }
 * }</pre>
 *
 * @param ticketId the unique identifier of the created ticket
 * @param ticketNumber the human-readable ticket number (e.g., "TICKET-001")
 * @param title the descriptive title of the ticket
 * @param status the initial status of the ticket (typically "OPEN")
 * @param priority the priority level (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")
 * @param requesterId the unique identifier of the user who created the ticket
 * @param requesterName the full name of the ticket requester
 * @param requesterEmail the email address of the ticket requester
 * @param createdAt the timestamp when the ticket was created
 * @param updatedAt the timestamp when the ticket was last updated (same as createdAt for new tickets)
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see io.monosense.synergyflow.eventing.readmodel.TicketCardEntity
 * @see io.monosense.synergyflow.eventing.readmodel.mapping.TicketCardMapper
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketCreatedMessage(
        UUID ticketId,
        String ticketNumber,
        String title,
        String status,
        String priority,
        UUID requesterId,
        UUID assigneeId,
        String requesterName,
        String requesterEmail,
        Instant createdAt,
        Instant updatedAt
) {
}
