package io.monosense.synergyflow.itsm.internal.exception;

import java.util.UUID;

/**
 * Exception thrown when a ticket cannot be found by its ID.
 *
 * <p>This exception is typically thrown by TicketService methods when attempting to
 * perform operations on a non-existent ticket.</p>
 *
 * <p>Public visibility within internal package to allow use across internal.* subpackages
 * (service, repository, etc.), but still maintains module boundary - only used within ITSM module.</p>
 *
 * @since 2.2
 */
public class TicketNotFoundException extends RuntimeException {

    private final UUID ticketId;

    /**
     * Constructs a new TicketNotFoundException with the specified ticket ID.
     *
     * @param ticketId the ID of the ticket that was not found
     */
    public TicketNotFoundException(UUID ticketId) {
        super("Ticket not found: " + ticketId);
        this.ticketId = ticketId;
    }

    /**
     * Returns the ID of the ticket that was not found.
     *
     * @return the ticket ID
     */
    public UUID getTicketId() {
        return ticketId;
    }
}
