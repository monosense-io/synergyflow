package io.monosense.synergyflow.itsm.internal.domain;

/**
 * Enumeration of incident severity levels.
 *
 * <p>Defines the technical severity classification for incident tickets,
 * indicating the scope and impact of the technical issue:</p>
 * <ul>
 *   <li>{@link #CRITICAL} - Complete service outage or critical functionality lost</li>
 *   <li>{@link #HIGH} - Major functionality degraded, significant impact</li>
 *   <li>{@link #MEDIUM} - Moderate impact, workaround available</li>
 *   <li>{@link #LOW} - Minor issue with minimal impact</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Severity differs from {@link Priority} in that severity
 * reflects the technical impact of an incident, while priority reflects business
 * urgency. A low-severity incident could have high business priority if it affects
 * a critical stakeholder.</p>
 *
 * @since 2.1
 */
public enum Severity {
    /**
     * Critical severity - complete service outage or critical functionality lost.
     */
    CRITICAL,

    /**
     * High severity - major functionality degraded with significant impact.
     */
    HIGH,

    /**
     * Medium severity - moderate impact with available workaround.
     */
    MEDIUM,

    /**
     * Low severity - minor issue with minimal impact.
     */
    LOW
}
