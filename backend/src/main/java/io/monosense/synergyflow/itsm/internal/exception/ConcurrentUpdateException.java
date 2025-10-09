package io.monosense.synergyflow.itsm.internal.exception;

import java.util.UUID;

/**
 * Exception thrown when retry exhaustion occurs due to concurrent updates on a ticket.
 *
 * <p>This exception is thrown by @Recover methods in TicketService after all retry attempts
 * (typically 4 attempts) have been exhausted due to OptimisticLockingFailureException.
 * It indicates that multiple users are attempting to update the same ticket simultaneously,
 * and the user should try their operation again.</p>
 *
 * <p>Public visibility within internal package to allow use across internal.* subpackages
 * (service, repository, etc.), but still maintains module boundary - only used within ITSM module.</p>
 *
 * @since 2.2
 */
public class ConcurrentUpdateException extends RuntimeException {

    private final UUID ticketId;

    /**
     * Constructs a new ConcurrentUpdateException with the specified message and ticket ID.
     *
     * @param message  the detail message
     * @param ticketId the ID of the ticket that experienced concurrent updates (nullable)
     */
    public ConcurrentUpdateException(String message, UUID ticketId) {
        super(message);
        this.ticketId = ticketId;
    }

    /**
     * Constructs a new ConcurrentUpdateException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
        this.ticketId = null;
    }

    /**
     * Returns the ID of the ticket that experienced concurrent updates.
     *
     * @return the ticket ID, or null if not available
     */
    public UUID getTicketId() {
        return ticketId;
    }
}
