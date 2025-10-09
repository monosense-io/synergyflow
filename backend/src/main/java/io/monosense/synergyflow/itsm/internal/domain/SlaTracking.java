package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import static org.hibernate.annotations.UuidGenerator.Style;

/**
 * SLA (Service Level Agreement) tracking entity for incident tickets.
 *
 * <p>This entity tracks priority-based SLA deadlines for INCIDENT tickets in the ITSM domain.
 * Each incident can have at most one SLA tracking record, enforced by a unique constraint
 * on {@code ticketId}. Service requests do not receive SLA tracking as they follow approval
 * workflows instead.</p>
 *
 * <p><strong>SLA Policy:</strong></p>
 * <ul>
 *   <li>CRITICAL: 2 hours</li>
 *   <li>HIGH: 4 hours</li>
 *   <li>MEDIUM: 8 hours</li>
 *   <li>LOW: 24 hours</li>
 * </ul>
 *
 * <p><strong>Lifecycle:</strong></p>
 * <ul>
 *   <li>Created when an incident is created with a priority</li>
 *   <li>Recalculated when incident priority changes (from original {@code createdAt})</li>
 *   <li>Frozen when incident reaches RESOLVED or CLOSED status (preserves historical accuracy)</li>
 *   <li>Cascade deleted when the associated ticket is deleted</li>
 * </ul>
 *
 * <p><strong>Concurrency:</strong> Uses optimistic locking with {@code @Version} to prevent
 * lost updates during concurrent priority changes. The {@code TicketService} handles retries
 * via {@code @Retryable} when {@code OptimisticLockException} occurs.</p>
 *
 * @author monosense
 * @since 2.3
 */
@Entity
@Table(
    name = "sla_tracking",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sla_tracking_ticket_id",
        columnNames = "ticket_id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlaTracking {

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
     * Foreign key reference to the associated ticket.
     *
     * <p>Points to {@code tickets.id}. Unique constraint ensures one SLA record per ticket.
     * Foreign key constraint with {@code ON DELETE CASCADE} ensures SLA record is deleted
     * when the ticket is deleted.</p>
     */
    @NotNull(message = "Ticket ID is required for SLA tracking")
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    /**
     * Priority level at the time of SLA calculation.
     *
     * <p>Determines the SLA duration:
     * CRITICAL = 2h, HIGH = 4h, MEDIUM = 8h, LOW = 24h.
     * Updated when ticket priority changes.</p>
     */
    @NotNull(message = "Priority is required for SLA calculation")
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    /**
     * SLA deadline timestamp.
     *
     * <p>Calculated as {@code ticket.createdAt + priority-based duration}.
     * When priority changes, recalculated from the <strong>original</strong> ticket creation
     * time (not current time) to preserve fairness.</p>
     *
     * <p>Example: If ticket created 1h ago as LOW (24h SLA), then escalated to CRITICAL (2h SLA),
     * the deadline is {@code ticket.createdAt + 2h}, which is already in the past (breached).</p>
     */
    @NotNull(message = "SLA due date is required")
    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    /**
     * Indicates whether the SLA has been breached.
     *
     * <p>TRUE if {@code Instant.now() > dueAt}. Updated by a future breach detection
     * background job (not implemented in Story 2.3 MVP). Default is FALSE.</p>
     */
    @Column(nullable = false)
    private boolean breached = false;

    /**
     * Optimistic locking version field.
     *
     * <p>JPA automatically increments this on each update. Initial value is 0 per JPA convention.
     * Concurrent priority changes trigger {@code OptimisticLockException}, handled by
     * {@code TicketService} retry logic.</p>
     */
    @Version
    private Long version;

    /**
     * Timestamp when this SLA tracking record was first persisted.
     *
     * <p>Set automatically by {@link #onCreate()} callback. Immutable after creation.</p>
     */
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the last update to this SLA tracking record.
     *
     * <p>Set automatically by {@link #onCreate()} and {@link #onUpdate()} callbacks.</p>
     */
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Creates a new SLA tracking record for an incident ticket.
     *
     * <p>All fields are required. The ID is generated automatically by Hibernate using UUIDv7.
     * Timestamps are managed by lifecycle callbacks.</p>
     *
     * @param ticketId the UUID of the associated incident ticket (required)
     * @param priority the priority level determining SLA duration (required)
     * @param dueAt the calculated SLA deadline (required)
     * @since 2.3
     */
    public SlaTracking(UUID ticketId, Priority priority, Instant dueAt) {
        this.ticketId = ticketId;
        this.priority = priority;
        this.dueAt = dueAt;
    }

    /**
     * JPA lifecycle callback invoked before entity persistence.
     *
     * <p>Sets both {@code createdAt} and {@code updatedAt} to the current instant.</p>
     *
     * @since 2.3
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
     * @since 2.3
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Updates the SLA deadline and priority.
     *
     * <p>Called by {@code TicketService.updatePriority()} when ticket priority changes.
     * Recalculates {@code dueAt} from the original ticket creation time, not current time.</p>
     *
     * @param newPriority the new priority level (required)
     * @param newDueAt the recalculated SLA deadline (required)
     * @since 2.3
     */
    public void updateSla(Priority newPriority, Instant newDueAt) {
        this.priority = newPriority;
        this.dueAt = newDueAt;
    }

    /**
     * Marks this SLA as breached.
     *
     * <p>Called by a future breach detection background job when {@code Instant.now() > dueAt}.
     * Not implemented in Story 2.3 MVP.</p>
     *
     * @since 2.3
     */
    public void markBreached() {
        this.breached = true;
    }
}
