package io.monosense.synergyflow.itsm.internal.domain;

/**
 * Enumeration of ticket priority levels in the ITSM system.
 *
 * <p>Defines the urgency and impact level for ticket handling:</p>
 * <ul>
 *   <li>{@link #CRITICAL} - Highest priority, immediate attention required</li>
 *   <li>{@link #HIGH} - High priority, expedited handling</li>
 *   <li>{@link #MEDIUM} - Normal priority, standard handling</li>
 *   <li>{@link #LOW} - Low priority, deferred handling acceptable</li>
 * </ul>
 *
 * @since 2.1
 */
public enum Priority {
    /**
     * Critical priority requiring immediate attention.
     */
    CRITICAL,

    /**
     * High priority requiring expedited handling.
     */
    HIGH,

    /**
     * Medium priority with standard handling.
     */
    MEDIUM,

    /**
     * Low priority where deferral is acceptable.
     */
    LOW
}
