package io.monosense.synergyflow.eventing.readmodel;

import java.time.Instant;
import java.util.UUID;

/**
 * Custom repository contract providing database-level version guards for ticket read models.
 */
public interface TicketCardRepositoryCustom {

    /**
     * Inserts or updates a ticket card row ensuring {@code version < ?} semantics in the database.
     *
     * @param entity hydrated entity to persist
     * @return {@code true} when a row was inserted or updated, {@code false} when skipped because an equal-or-newer version already exists
     */
    boolean upsertWithVersionGuard(TicketCardEntity entity);

    /**
     * Applies assignment changes guarded by {@code version < ?} directly in the database.
     *
     * @param ticketId     aggregate identifier
     * @param assigneeName resolved assignee display name
     * @param assigneeEmail resolved assignee email
     * @param updatedAt    timestamp for the projection update
     * @param version      event version being applied
     * @return {@code true} if the update affected a row, {@code false} when skipped due to stale version or missing record
     */
    boolean updateAssignmentWithVersionGuard(UUID ticketId,
                                             String assigneeName,
                                             String assigneeEmail,
                                             Instant updatedAt,
                                             long version);
}

