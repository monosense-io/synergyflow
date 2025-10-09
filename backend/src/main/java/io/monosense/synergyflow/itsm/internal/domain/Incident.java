package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Incident ticket representing unplanned service disruptions or issues.
 *
 * <p>An incident is a ticket type used to track and resolve unplanned interruptions
 * or quality degradations to IT services. Incidents have additional fields beyond
 * the base ticket to capture technical severity, resolution details, and completion
 * timestamps.</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Automatically sets {@code ticketType = INCIDENT}</li>
 *   <li>Tracks technical severity independent of business priority</li>
 *   <li>Records resolution notes and timestamp when closed</li>
 *   <li>Part of SINGLE_TABLE inheritance strategy for query performance</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
@Entity
@DiscriminatorValue("INCIDENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident extends Ticket {

    /**
     * Technical severity level of the incident.
     *
     * <p>Indicates the scope and impact of the technical issue. Severity differs
     * from priority in that severity reflects technical impact while priority
     * reflects business urgency.</p>
     *
     * <p>Nullable to support incidents where severity hasn't been assessed yet.</p>
     */
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    /**
     * Detailed notes describing how the incident was resolved.
     *
     * <p>Stored as TEXT column for comprehensive resolution documentation.
     * Used for knowledge base articles and future incident prevention.</p>
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * Timestamp when the incident was resolved.
     *
     * <p>Set when the incident status changes to RESOLVED or CLOSED.
     * Nullable until the incident is actually resolved.</p>
     */
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /**
     * Creates a new incident ticket with the specified details.
     *
     * <p>Initializes an incident ticket and automatically sets the ticket type
     * to INCIDENT. The severity can be provided during creation or set later
     * as the incident is triaged.</p>
     *
     * @param title the descriptive title of the incident (required)
     * @param description detailed description of the issue
     * @param status initial status (typically NEW)
     * @param priority business priority level
     * @param category category classification for routing
     * @param requesterId the UUID of the user reporting the incident (required)
     * @param assigneeId the UUID of the assigned agent (nullable)
     * @param severity technical severity level (nullable, can be set during triage)
     * @since 2.1
     */
    public Incident(String title, String description, TicketStatus status,
                    Priority priority, String category, UUID requesterId,
                    UUID assigneeId, Severity severity) {
        super(title, description, TicketType.INCIDENT, status, priority,
                category, requesterId, assigneeId);
        this.severity = severity;
    }

    /**
     * Records the resolution of this incident.
     *
     * <p>Captures resolution notes and timestamp. Typically called when
     * the incident status is updated to RESOLVED or CLOSED.</p>
     *
     * @param resolutionNotes detailed notes on how the incident was resolved (required)
     * @since 2.1
     */
    public void resolve(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = Instant.now();
    }

    /**
     * Updates the severity level of this incident.
     *
     * <p>Used during triage or escalation to adjust the technical severity
     * classification as more information becomes available.</p>
     *
     * @param severity the new severity level (required)
     * @since 2.1
     */
    public void updateSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Clears resolution fields when incident is reopened.
     *
     * <p>Sets resolutionNotes and resolvedAt to null, effectively reverting
     * the incident to an unresolved state.</p>
     *
     * @since 2.2
     */
    public void clearResolution() {
        this.resolutionNotes = null;
        this.resolvedAt = null;
    }
}
