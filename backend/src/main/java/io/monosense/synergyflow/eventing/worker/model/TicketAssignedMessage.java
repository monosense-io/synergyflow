package io.monosense.synergyflow.eventing.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Event message representing the assignment of a ticket to an agent or user.
 *
 * <p>This immutable record captures assignment information that needs to be propagated
 * through the event-driven architecture. It serves as the canonical data structure for
 * ticket assignment events consumed by read model updaters, notification services,
 * queue management systems, and workload balancing services.</p>
 *
 * <p><strong>Assignment Process:</strong>
 * <ol>
 *   <li>Ticket is assigned to an agent via manual assignment or auto-routing</li>
 *   <li>TicketAssignedMessage is published to the outbox table</li>
 *   <li>Worker processes the message and updates read models</li>
 *   <li>Notification services alert the assigned agent</li>
 *   <li>Queue management systems update work assignments</li>
 * </ol>
 *
 * <p><strong>Impact on System:</strong>
 * <ul>
 *   <li>Read models: Updates assignment information in ticket card and queue entities</li>
 *   <li>Notifications: Triggers email/SMS alerts to the assigned agent</li>
 *   <li>Workload: Updates agent workload calculations</li>
 *   <li>Queue Management: Reorganizes ticket queues based on new assignment</li>
 *   <li>SLA: May trigger SLA calculations based on assignment time</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create a ticket assignment event
 * TicketAssignedMessage event = new TicketAssignedMessage(
 *     ticketId,
 *     agentId,
 *     managerId,
 *     "Jane Smith",
 *     "jane.smith@company.com",
 *     Instant.now(),
 *     2L
 * );
 *
 * // Process the event in read model updater
 * public void handleTicketAssigned(TicketAssignedMessage message) {
 *     TicketCardEntity entity = repository.findByTicketId(message.ticketId());
 *     if (entity != null) {
 *         mapper.applyAssignment(entity, message);
 *         repository.save(entity);
 *     }
 * }
 * }</pre>
 *
 * @param ticketId the unique identifier of the assigned ticket
 * @param assigneeId the unique identifier of the user the ticket is assigned to
 * @param assignedBy the unique identifier of the user who made the assignment
 * @param assigneeName the full name of the assigned agent
 * @param assigneeEmail the email address of the assigned agent
 * @param assignedAt the timestamp when the assignment occurred
 * @param version the aggregate version for optimistic locking
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see io.monosense.synergyflow.eventing.readmodel.TicketCardEntity
 * @see io.monosense.synergyflow.eventing.readmodel.mapping.TicketCardMapper
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketAssignedMessage(
        UUID ticketId,
        UUID assigneeId,
        UUID assignedBy,
        String assigneeName,
        String assigneeEmail,
        Instant assignedAt,
        Long version
) {
}
