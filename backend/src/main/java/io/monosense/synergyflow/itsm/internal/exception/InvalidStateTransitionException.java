package io.monosense.synergyflow.itsm.internal.exception;

import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;

/**
 * Exception thrown when an invalid state transition is attempted on a ticket.
 *
 * <p>This exception is thrown by TicketStateTransitionValidator when a state transition
 * violates the defined state machine rules. Only specific transitions are allowed based
 * on the current ticket status.</p>
 *
 * <p>Public visibility within internal package to allow use across internal.* subpackages
 * (service, repository, etc.), but still maintains module boundary - only used within ITSM module.</p>
 *
 * @since 2.2
 */
public class InvalidStateTransitionException extends RuntimeException {

    private final TicketStatus fromStatus;
    private final TicketStatus toStatus;

    /**
     * Constructs a new InvalidStateTransitionException with the specified state transition.
     *
     * @param from the current status of the ticket
     * @param to   the target status that was attempted
     */
    public InvalidStateTransitionException(TicketStatus from, TicketStatus to) {
        super("Invalid state transition: " + from + " → " + to);
        this.fromStatus = from;
        this.toStatus = to;
    }

    /**
     * Constructs a new InvalidStateTransitionException with a custom message.
     *
     * @param message the custom error message
     * @param currentStatus the current status of the ticket
     */
    public InvalidStateTransitionException(String message, TicketStatus currentStatus) {
        super(message);
        this.fromStatus = currentStatus;
        this.toStatus = currentStatus;
    }

    /**
     * Returns the current status of the ticket.
     *
     * @return the from status
     */
    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    /**
     * Returns the target status that was attempted.
     *
     * @return the to status
     */
    public TicketStatus getToStatus() {
        return toStatus;
    }
}
