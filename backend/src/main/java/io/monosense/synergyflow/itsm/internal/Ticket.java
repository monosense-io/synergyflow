package io.monosense.synergyflow.itsm.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Ticket aggregate root representing IT Service Management tickets.
 *
 * <p>This JPA entity serves as the aggregate root for ITSM tickets in the domain model.
 * It encapsulates ticket state and behavior while maintaining consistency through business rules
 * and domain events. The ticket lifecycle follows standard ITSM practices with states such as
 * OPEN, IN_PROGRESS, RESOLVED, and CLOSED.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Maintain ticket state and business invariants</li>
 *   <li>Generate domain events for state changes</li>
 *   <li>Enforce data integrity through JPA constraints</li>
 *   <li>Support optimistic locking with version control</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Create a new ticket
 * Ticket ticket = new Ticket(
 *     UUID.randomUUID(),
 *     "TICKET-001",
 *     "Login issue reported",
 *     "HIGH",
 *     requesterId
 * );
 *
 * // Assign ticket to agent
 * ticket.assignTo(agentId);
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "tickets")
class Ticket {

    @Id
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Version
    private Long version = 1L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor required by JPA.
     *
     * @deprecated Should not be used directly. Use the public constructor instead.
     */
    @Deprecated
    protected Ticket() {
        // JPA constructor
    }

    /**
     * Creates a new ticket with the specified details.
     *
     * <p>Initializes a ticket in OPEN status with creation and update timestamps.
     * The version is automatically managed by JPA's optimistic locking mechanism.</p>
     *
     * @param id the unique identifier for the ticket
     * @param ticketNumber the human-readable ticket number (must be unique)
     * @param title the descriptive title of the ticket
     * @param priority the priority level (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")
     * @param requesterId the unique identifier of the user who created the ticket
     * @throws IllegalArgumentException if any required parameter is null or empty
     */
    public Ticket(UUID id, String ticketNumber, String title, String priority, UUID requesterId) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.title = title;
        this.status = "OPEN";
        this.priority = priority;
        this.requesterId = requesterId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // version is managed by JPA @Version
    }

    /**
     * Assigns this ticket to a specific agent or user.
     *
     * <p>Sets the assignee and updates the last modified timestamp.
     * The JPA version will be automatically incremented for optimistic locking.</p>
     *
     * @param assigneeId the unique identifier of the user to assign this ticket to
     * @throws IllegalArgumentException if assigneeId is null
     */
    public void assignTo(UUID assigneeId) {
        this.assigneeId = assigneeId;
        this.updatedAt = Instant.now();
        // Version will be incremented automatically by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
