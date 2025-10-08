package io.monosense.synergyflow.itsm.internal.domain;

/**
 * Enumeration of ticket lifecycle states in the ITSM system.
 *
 * <p>Represents the standard ITIL-aligned ticket workflow states:</p>
 * <ul>
 *   <li>{@link #NEW} - Ticket created, awaiting assignment</li>
 *   <li>{@link #ASSIGNED} - Ticket assigned to agent, awaiting work</li>
 *   <li>{@link #IN_PROGRESS} - Active work in progress</li>
 *   <li>{@link #RESOLVED} - Issue resolved, awaiting closure</li>
 *   <li>{@link #CLOSED} - Ticket closed, no further action</li>
 * </ul>
 *
 * @since 2.1
 */
public enum TicketStatus {
    /**
     * Ticket has been created and is awaiting assignment.
     */
    NEW,

    /**
     * Ticket has been assigned to an agent.
     */
    ASSIGNED,

    /**
     * Ticket is actively being worked on.
     */
    IN_PROGRESS,

    /**
     * Ticket has been resolved and is awaiting closure.
     */
    RESOLVED,

    /**
     * Ticket is closed and requires no further action.
     */
    CLOSED
}
