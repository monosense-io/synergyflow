package io.monosense.synergyflow.eventing.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API for publishing domain events to the transactional outbox.
 *
 * <p>This interface provides the contract for publishing domain events in an event-driven
 * architecture. Events are persisted in the same database transaction as aggregate writes,
 * ensuring reliable event delivery and maintaining consistency between command and query
 * sides. The outbox pattern prevents lost events and enables eventual consistency.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Publish domain events within the same transaction as aggregate state changes</li>
 *   <li>Ensure event ordering and versioning for aggregates</li>
 *   <li>Support duplicate event detection and prevention</li>
 *   <li>Enable reliable asynchronous event delivery</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class TicketService {
 *     private final EventPublisher eventPublisher;
 *
 *     public void createTicket(String title, UUID requesterId) {
 *         // Create and save ticket
 *         Ticket ticket = new Ticket(title, requesterId);
 *         ticket = ticketRepository.save(ticket);
 *
 *         // Publish event in the same transaction
 *         TicketCreated event = new TicketCreated(ticket.getId(), title, requesterId);
 *         ObjectNode payload = objectMapper.valueToTree(event);
 *
 *         eventPublisher.publish(
 *             ticket.getId(),
 *             "TICKET",
 *             "TicketCreated",
 *             ticket.getVersion(),
 *             ticket.getCreatedAt(),
 *             payload
 *         );
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
public interface EventPublisher {

    /**
     * Publishes a domain event to the transactional outbox.
     *
     * <p>This method persists domain events in the same database transaction as aggregate state changes,
     * implementing the transactional outbox pattern for reliable event delivery. The event will be
     * processed asynchronously by worker services and published to message brokers for downstream
     * consumers.
     *
     * <p><strong>Transaction Behavior:</strong>
     * <ul>
     *   <li>Events are persisted in the caller's transaction context</li>
     *   <li>If the transaction commits, events are guaranteed to be stored</li>
     *   <li>If the transaction rolls back, events are also rolled back</li>
     *   <li>Ensures consistency between domain state and published events</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>DuplicateEventException: Thrown when the same event is published twice</li>
     *   <li>DataAccessException: Thrown for database-related issues</li>
     *   <li>IllegalArgumentException: Thrown for invalid parameters</li>
     * </ul>
     *
     * <p><strong>Event Ordering:</strong>
     * <p>Events are ordered by aggregate version to ensure proper sequencing for consumers.
     * The version number should be incremented for each state change to maintain event
     * ordering and enable idempotent processing.
     *
     * @param aggregateId the unique identifier of the aggregate that produced the event
     * @param aggregateType the type name of the aggregate (e.g., "TICKET", "ISSUE", "PROJECT")
     * @param eventType the specific type of the event (e.g., "TicketCreated", "IssueStateChanged", "ProjectCompleted")
     * @param version the version number of the aggregate after this event (must be monotonically increasing)
     * @param occurredAt the timestamp when the event occurred in the domain (should be the actual event time, not publish time)
     * @param payload the event payload containing domain-specific data as JSON
     * @throws DuplicateEventException if an event with the same (aggregateId, version) already exists
     * @throws DataAccessException if database persistence fails
     * @throws IllegalArgumentException if any required parameter is null or invalid
     */
    void publish(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Long version,
            Instant occurredAt,
            JsonNode payload
    );
}
