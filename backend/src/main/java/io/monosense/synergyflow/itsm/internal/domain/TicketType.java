package io.monosense.synergyflow.itsm.internal.domain;

/**
 * Enumeration of ticket types in the ITSM system.
 *
 * <p>Distinguishes between different categories of tickets to support
 * type-specific workflows and business rules:</p>
 * <ul>
 *   <li>{@link #INCIDENT} - Unplanned interruptions or reductions in service quality</li>
 *   <li>{@link #SERVICE_REQUEST} - Requests for service, information, or access</li>
 * </ul>
 *
 * @since 2.1
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
