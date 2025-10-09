package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for TicketComment entity persistence.
 *
 * <p>Provides database access operations for TicketComment entities using UUIDv7 as the primary key.
 * This repository extends JpaRepository to inherit standard CRUD operations and includes
 * custom query methods for retrieving comments in chronological order.</p>
 *
 * <p>Custom query methods support:
 * <ul>
 *   <li>Chronological retrieval of all comments for a specific ticket (oldest first)</li>
 * </ul>
 *
 * <p>Public visibility required for Spring dependency injection across internal packages.</p>
 *
 * @author monosense
 * @since 2.1
 */
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    /**
     * Finds all comments for a specific ticket, ordered by creation time (oldest first).
     *
     * <p>This method returns comments in chronological order, which is the natural reading order
     * for ticket history and conversation threads. The ordering is based on the createdAt timestamp
     * field, which is immutable and set once at comment creation.</p>
     *
     * @param ticketId the UUID of the ticket to retrieve comments for
     * @return list of comments in chronological order (oldest to newest), or empty list if no comments exist
     * @since 2.1
     */
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
