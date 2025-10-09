package io.monosense.synergyflow.itsm.spi;

/**
 * SPI representation of ticket types.
 *
 * <p>This enum is duplicated from the internal package to maintain a stable SPI contract
 * and avoid coupling cross-module consumers to internal implementation details.</p>
 *
 * <p>Distinguishes between different categories of tickets to support
 * type-specific workflows and business rules:</p>
 * <ul>
 *   <li>{@link #INCIDENT} - Unplanned interruptions or reductions in service quality</li>
 *   <li>{@link #SERVICE_REQUEST} - Requests for service, information, or access</li>
 * </ul>
 *
 * <p><strong>SPI Stability Contract:</strong> This enum is part of the public SPI and follows
 * semantic versioning. New values may be added in minor versions. Existing values will not be
 * removed or renamed without a major version bump and deprecation cycle.</p>
 *
 * @since 2.2
 */
public enum TicketType {
    /**
     * Incident ticket type for unplanned service disruptions.
     */
    INCIDENT,

    /**
     * Service request ticket type for planned service fulfillment.
     */
    SERVICE_REQUEST
}
