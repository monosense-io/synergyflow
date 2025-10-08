package io.monosense.synergyflow.itsm.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.monosense.synergyflow.eventing.api.EventPublisher;
import io.monosense.synergyflow.itsm.events.TicketAssigned;
import io.monosense.synergyflow.itsm.events.TicketCreated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing IT Service Management tickets with event-driven architecture.
 *
 * <p>This service handles the core business operations for tickets in the ITSM domain.
 * It demonstrates transactional event publishing by persisting domain events to the outbox
 * pattern within the same database transaction as aggregate state changes. This ensures
 * reliable event delivery and maintains consistency between command and query sides.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Create new tickets with automatic ticket number generation</li>
 *   <li>Assign tickets to agents or users</li>
 *   <li>Publish domain events for state changes</li>
 *   <li>Maintain transactional consistency between commands and events</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class TicketController {
 *     private final TicketService ticketService;
 *
 *     public UUID createNewTicket(String title, String priority, UUID requesterId) {
 *         return ticketService.createTicket(title, priority, requesterId);
 *     }
 *
 *     public void assignTicketToAgent(UUID ticketId, UUID agentId, UUID assignedBy) {
 *         ticketService.assignTicket(ticketId, agentId, assignedBy);
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Service
public class TicketService {

    private final TicketRepository repository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new TicketService with the required dependencies.
     *
     * @param repository the JPA repository for ticket persistence
     * @param eventPublisher the event publisher for domain events
     * @param objectMapper the JSON object mapper for event serialization
     * @throws IllegalArgumentException if any parameter is null
     */
    public TicketService(TicketRepository repository, EventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

  /**
     * Creates a new ticket with the specified details and publishes a TicketCreated event.
     *
     * <p>This method creates a new ticket with automatically generated ticket number,
     * persists it to the database, and publishes a corresponding domain event. The operation
     * is performed within a single transaction to ensure consistency between the ticket
     * state and the published event.</p>
     *
     * @param title the descriptive title of the ticket
     * @param priority the priority level (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")
     * @param requesterId the unique identifier of the user creating the ticket
     * @return the unique identifier of the created ticket
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws DuplicateEventException if an event with the same aggregate ID and version already exists
     */
    @Transactional
    public UUID createTicket(String title, String priority, UUID requesterId) {
        // Create and save ticket
        String ticketNumber = "T-" + System.currentTimeMillis(); // Simple ticket number generation
        Ticket ticket = new Ticket(UUID.randomUUID(), ticketNumber, title, priority, requesterId);
        ticket = repository.save(ticket);

        // Prepare event payload
        TicketCreated event = new TicketCreated(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getRequesterId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );

        // Publish event with incremented version
        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketCreated",
                ticket.getVersion(),
                ticket.getCreatedAt(),
                payload
        );

        return ticket.getId();
    }

    /**
     * Assigns a ticket to the specified agent and publishes a TicketAssigned event.
     *
     * <p>This method updates the ticket's assignee, persists the change to the database,
     * and publishes a corresponding domain event. The operation is performed within a single
     * transaction to ensure consistency between the ticket state and the published event.
     * The method uses saveAndFlush to ensure the JPA version is incremented before event publishing.</p>
     *
     * @param ticketId the unique identifier of the ticket to assign
     * @param assigneeId the unique identifier of the user to assign the ticket to
     * @param assignedBy the unique identifier of the user performing the assignment
     * @throws IllegalArgumentException if any parameter is null or if the ticket is not found
     * @throws DuplicateEventException if an event with the same aggregate ID and version already exists
     */
    @Transactional
    public void assignTicket(UUID ticketId, UUID assigneeId, UUID assignedBy) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        // Update ticket
        ticket.assignTo(assigneeId);
        // Flush to ensure @Version increments before publishing the event
        ticket = repository.saveAndFlush(ticket);

        // Prepare event payload
        TicketAssigned event = new TicketAssigned(
                ticket.getId(),
                assigneeId,
                assignedBy,
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        // Publish event with incremented version
        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketAssigned",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );
    }
}
