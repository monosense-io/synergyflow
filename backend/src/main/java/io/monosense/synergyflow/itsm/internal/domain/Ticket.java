package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import static org.hibernate.annotations.UuidGenerator.Style;

/**
 * Ticket aggregate root representing IT Service Management tickets.
 *
 * <p>This JPA entity serves as the base class for all ITSM tickets in the domain model.
 * It uses UUIDv7 for time-ordered primary keys, optimistic locking for concurrency control,
 * and JPA inheritance to support specialized ticket types (Incident, ServiceRequest).</p>
 *
 * <p><strong>Inheritance Strategy:</strong> SINGLE_TABLE chosen for performance optimization.
 * This strategy uses a single database table with a discriminator column to distinguish
 * between Ticket, Incident, and ServiceRequest entities. This approach provides:</p>
 * <ul>
 *   <li>Faster polymorphic queries (single table scan vs joins)</li>
 *   <li>Simplified read model projections for ticket queues</li>
 *   <li>Better performance for read-heavy ITSM workflows (ticket lists, dashboards, SSE updates)</li>
 * </ul>
 * <p>Trade-off: Some nullable columns for subclass-specific fields, but this is acceptable
 * given the read-heavy nature of Epic 2 ITSM features.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Maintain ticket state and business invariants</li>
 *   <li>Provide time-ordered UUIDv7 primary keys for better index locality</li>
 *   <li>Enforce optimistic locking to prevent lost updates</li>
 *   <li>Manage lifecycle timestamps via JPA callbacks</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
@Entity
@Table(name = "tickets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ticket_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

    /**
     * UUIDv7 primary key providing time-ordered identifiers.
     *
     * <p>UUIDv7 includes a 48-bit Unix timestamp prefix enabling chronological ordering
     * and improved database index locality. Hibernate 7.0+ generates these automatically.</p>
     */
    @Id
    @UuidGenerator(style = Style.VERSION_7)
    private UUID id;

    /**
     * Discriminator field for JPA inheritance.
     *
     * <p>Automatically populated by JPA based on the entity subclass.
     * Values: INCIDENT, SERVICE_REQUEST</p>
     */
    @Column(name = "ticket_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private TicketType ticketType;

    /**
     * Brief descriptive title of the ticket (max 500 characters).
     */
    @NotBlank(message = "Ticket title is required")
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * Detailed description of the issue or request.
     *
     * <p>Stored as TEXT column for large content support.</p>
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Current lifecycle status of the ticket.
     *
     * <p>Valid states: NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED</p>
     */
    @NotNull(message = "Ticket status is required")
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    /**
     * Priority level indicating urgency and impact.
     *
     * <p>Valid levels: CRITICAL, HIGH, MEDIUM, LOW</p>
     * <p>Nullable to support incidents created without initial priority (Story 2.3).
     * Priority can be added later via updatePriority(), which triggers SLA tracking.</p>
     */
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    /**
     * Category classification for routing and reporting.
     *
     * <p>Examples: "Network", "Hardware", "Software", "Access Management"</p>
     */
    @Column(length = 100)
    private String category;

    /**
     * UUID of the user who created this ticket.
     */
    @NotNull(message = "Requester ID is required")
    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    /**
     * UUID of the agent assigned to handle this ticket.
     *
     * <p>Nullable until ticket is assigned.</p>
     */
    @Column(name = "assignee_id")
    private UUID assigneeId;

    /**
     * Optimistic locking version field.
     *
     * <p>JPA automatically increments this on each update. Initial value is 1.</p>
     */
    @Version
    private Long version = 1L;

    /**
     * Timestamp when this ticket was first persisted.
     *
     * <p>Set automatically by {@link #onCreate()} callback. Immutable after creation.</p>
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the last update to this ticket.
     *
     * <p>Set automatically by {@link #onCreate()} and {@link #onUpdate()} callbacks.</p>
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Counter tracking how many times this ticket has been reopened.
     *
     * <p>Incremented each time a ticket transitions from RESOLVED or CLOSED back to NEW.
     * Used for reporting and quality metrics.</p>
     */
    @Column(name = "reopen_count")
    private Integer reopenCount = 0;

    /**
     * Creates a new ticket with the specified details.
     *
     * <p>Initializes a ticket with required fields. The ID is generated automatically
     * by Hibernate using UUIDv7. Timestamps are managed by lifecycle callbacks.</p>
     *
     * @param title the descriptive title of the ticket (required)
     * @param description detailed description of the issue or request
     * @param ticketType the type of ticket (INCIDENT or SERVICE_REQUEST)
     * @param status initial status (typically NEW)
     * @param priority priority level (CRITICAL, HIGH, MEDIUM, or LOW)
     * @param category category classification for routing
     * @param requesterId the UUID of the user creating the ticket (required)
     * @param assigneeId the UUID of the assigned agent (nullable)
     * @since 2.1
     */
    public Ticket(String title, String description, TicketType ticketType, TicketStatus status,
                  Priority priority, String category, UUID requesterId, UUID assigneeId) {
        this.title = title;
        this.description = description;
        this.ticketType = ticketType;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.requesterId = requesterId;
        this.assigneeId = assigneeId;
    }

    /**
     * JPA lifecycle callback invoked before entity persistence.
     *
     * <p>Sets both {@code createdAt} and {@code updatedAt} to the current instant.</p>
     *
     * @since 2.1
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA lifecycle callback invoked before entity updates.
     *
     * <p>Updates {@code updatedAt} to the current instant. {@code createdAt} remains immutable.</p>
     *
     * @since 2.1
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Explicitly increments the version field.
     *
     * <p>Used for testing optimistic locking behavior or forcing version bumps
     * without other field changes. In normal operation, JPA manages the version
     * automatically.</p>
     *
     * @since 2.1
     */
    public void incrementVersion() {
        if (this.version == null) {
            this.version = 0L;
        }
        this.version++;
    }

    /**
     * Assigns this ticket to a specific agent.
     *
     * <p>Sets the assignee ID. The {@code updatedAt} timestamp will be updated
     * automatically by the {@link #onUpdate()} callback when the entity is saved.</p>
     *
     * @param assigneeId the UUID of the agent to assign (required)
     * @since 2.1
     */
    public void assignTo(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    /**
     * Updates the ticket status.
     *
     * <p>Changes the lifecycle state. The {@code updatedAt} timestamp will be updated
     * automatically by the {@link #onUpdate()} callback when the entity is saved.</p>
     *
     * @param status the new status (required)
     * @since 2.1
     */
    public void updateStatus(TicketStatus status) {
        this.status = status;
    }

    /**
     * Clears the ticket assignment.
     *
     * <p>Sets the assignee ID to null, removing the current assignment.
     * Used during unassignment and reopen operations.</p>
     *
     * @since 2.2
     */
    public void clearAssignment() {
        this.assigneeId = null;
    }

    /**
     * Updates the priority level of this ticket.
     *
     * <p>Changes the priority classification. The {@code updatedAt} timestamp
     * will be updated automatically by the {@link #onUpdate()} callback.</p>
     *
     * @param priority the new priority level (required)
     * @since 2.2
     */
    public void updatePriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Updates the category classification of this ticket.
     *
     * <p>Changes the category for routing and reporting. The {@code updatedAt}
     * timestamp will be updated automatically by the {@link #onUpdate()} callback.</p>
     *
     * @param category the new category (can be null)
     * @since 2.2
     */
    public void updateCategory(String category) {
        this.category = category;
    }

    /**
     * Updates the title of this ticket.
     *
     * <p>Changes the descriptive title. The {@code updatedAt} timestamp
     * will be updated automatically by the {@link #onUpdate()} callback.</p>
     *
     * @param title the new title (required)
     * @since 2.2
     */
    public void updateTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the description of this ticket.
     *
     * <p>Changes the detailed description. The {@code updatedAt} timestamp
     * will be updated automatically by the {@link #onUpdate()} callback.</p>
     *
     * @param description the new description (can be null)
     * @since 2.2
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * Increments the reopen counter.
     *
     * <p>Called when a ticket is reopened from RESOLVED or CLOSED state.
     * Initializes the counter to 0 if null before incrementing.</p>
     *
     * @since 2.2
     */
    public void incrementReopenCount() {
        if (this.reopenCount == null) {
            this.reopenCount = 0;
        }
        this.reopenCount++;
    }
}
