package io.monosense.synergyflow.itsm.spi;

/**
 * SPI representation of ticket lifecycle states.
 *
 * <p>This enum is duplicated from the internal package to maintain a stable SPI contract
 * and avoid coupling cross-module consumers to internal implementation details.</p>
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
 * <p><strong>SPI Stability Contract:</strong> This enum is part of the public SPI and follows
 * semantic versioning. New values may be added in minor versions. Existing values will not be
 * removed or renamed without a major version bump and deprecation cycle.</p>
 *
 * @since 2.2
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
