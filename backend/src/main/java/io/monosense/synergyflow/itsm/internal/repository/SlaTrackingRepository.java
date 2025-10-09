package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.SlaTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for SLA tracking persistence.
 *
 * <p>Provides database access operations for SLA tracking records using UUIDv7 as the primary key.
 * Each SLA tracking record is associated with exactly one incident ticket via a unique constraint
 * on {@code ticketId}.</p>
 *
 * <p>Custom query methods support:
 * <ul>
 *   <li>Finding SLA record by ticket ID (O(1) lookup via indexed {@code ticket_id} column)</li>
 *   <li>Finding breached SLAs (for future breach detection background jobs)</li>
 * </ul>
 *
 * <p><strong>Lifecycle:</strong> SLA tracking records are created when an incident ticket
 * with a priority is created, recalculated when priority changes, and cascade deleted
 * when the associated ticket is deleted (foreign key {@code ON DELETE CASCADE}).</p>
 *
 * <p>Public visibility required for injection from other internal packages.</p>
 *
 * @author monosense
 * @since 2.3
 */
public interface SlaTrackingRepository extends JpaRepository<SlaTracking, UUID> {

    /**
     * Finds the SLA tracking record for a specific ticket.
     *
     * <p>Uses an indexed lookup on {@code ticket_id} column for O(1) performance.
     * Returns empty Optional if no SLA record exists for the ticket (e.g., service request,
     * or incident created without priority).</p>
     *
     * @param ticketId the UUID of the ticket to find SLA tracking for
     * @return Optional containing the SLA tracking record if found, or empty if not found
     * @since 2.3
     */
    Optional<SlaTracking> findByTicketId(UUID ticketId);

    /**
     * Finds all SLA tracking records that are due before a specified time and not yet breached.
     *
     * <p>This query method is designed for future breach detection background jobs that
     * periodically scan for SLAs approaching or past their {@code dueAt} deadline.
     * Uses an indexed lookup on {@code due_at} column for efficient range queries.</p>
     *
     * <p><strong>Not implemented in Story 2.3 MVP.</strong> Included for future use.</p>
     *
     * @param now the reference timestamp to compare against (typically {@code Instant.now()})
     * @return list of SLA records that are due before {@code now} and have {@code breached = false}
     * @since 2.3
     */
    List<SlaTracking> findByDueAtBeforeAndBreachedFalse(Instant now);
}
