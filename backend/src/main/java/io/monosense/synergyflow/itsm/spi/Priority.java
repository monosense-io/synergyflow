package io.monosense.synergyflow.itsm.spi;

/**
 * SPI representation of ticket priority levels.
 *
 * <p>This enum is duplicated from the internal package to maintain a stable SPI contract
 * and avoid coupling cross-module consumers to internal implementation details.</p>
 *
 * <p>Defines the urgency and impact level for ticket handling:</p>
 * <ul>
 *   <li>{@link #CRITICAL} - Highest priority, immediate attention required</li>
 *   <li>{@link #HIGH} - High priority, expedited handling</li>
 *   <li>{@link #MEDIUM} - Normal priority, standard handling</li>
 *   <li>{@link #LOW} - Low priority, deferred handling acceptable</li>
 * </ul>
 *
 * <p><strong>SPI Stability Contract:</strong> This enum is part of the public SPI and follows
 * semantic versioning. New values may be added in minor versions. Existing values will not be
 * removed or renamed without a major version bump and deprecation cycle.</p>
 *
 * @since 2.2
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
