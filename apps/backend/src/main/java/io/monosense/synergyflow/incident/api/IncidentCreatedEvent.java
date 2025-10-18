package io.monosense.synergyflow.incident.api;

import java.time.Instant;

/**
 * Domain event published when a new Incident is created.
 */
public record IncidentCreatedEvent(
        String incidentId,
        String title,
        String description,
        Priority priority,
        String reportedBy,
        String correlationId,
        String causationId,
        Instant occurredAt
) {
    public enum Priority {
        P1_CRITICAL,
        P2_HIGH,
        P3_MEDIUM,
        P4_LOW
    }
}
