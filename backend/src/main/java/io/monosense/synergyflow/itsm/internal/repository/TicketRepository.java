package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for Ticket aggregate persistence.
 *
 * <p>Provides database access operations for Ticket entities using UUIDv7 as the primary key.
 * This repository extends JpaRepository to inherit standard CRUD operations and JpaSpecificationExecutor
 * for dynamic query support, and includes custom query methods for common ticket lookup patterns.</p>
 *
 * <p>Custom query methods support filtering by:
 * <ul>
 *   <li>Status (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)</li>
 *   <li>Requester ID (user who created the ticket)</li>
 *   <li>Assignee ID (user assigned to work on the ticket)</li>
 * </ul>
 *
 * <p>Dynamic query support via JpaSpecificationExecutor enables complex criteria-based searches
 * for the TicketQueryService SPI implementation.</p>
 *
 * <p>Public visibility required for Spring dependency injection across internal packages.</p>
 *
 * @author monosense
 * @since 2.1
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    /**
     * Finds all tickets with the specified status.
     *
     * @param status the ticket status to filter by (e.g., NEW, IN_PROGRESS, RESOLVED)
     * @return list of tickets matching the status, or empty list if none found
     * @since 2.1
     */
    List<Ticket> findByStatus(TicketStatus status);

    /**
     * Finds all tickets requested by a specific user.
     *
     * @param requesterId the UUID of the user who created the tickets
     * @return list of tickets created by the requester, or empty list if none found
     * @since 2.1
     */
    List<Ticket> findByRequesterId(UUID requesterId);

    /**
     * Finds all tickets assigned to a specific user.
     *
     * @param assigneeId the UUID of the user assigned to work on the tickets
     * @return list of tickets assigned to the user, or empty list if none found
     * @since 2.1
     */
    List<Ticket> findByAssigneeId(UUID assigneeId);
}
