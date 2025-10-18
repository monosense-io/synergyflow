package io.monosense.synergyflow.time.api;

import io.monosense.synergyflow.common.events.EventBase;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Domain event published when a new Time Entry is created.
 *
 * <p>This event triggers the mirroring process to copy the time entry
 * to related Incident and Task worklogs with full audit trail.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
public record TimeEntryCreatedEvent(
        String timeEntryId,
        String userId,
        Duration duration,
        String description,
        Instant occurredAt,
        List<TargetEntity> targetEntities,
        String correlationId,
        String causationId,
        Instant publishedAt
) implements EventBase {

    /**
     * Represents a target entity where the time entry should be mirrored.
     */
    public record TargetEntity(
            EntityType type,
            String entityId,
            String entityTitle
    ) {
        public enum EntityType {
            INCIDENT,
            TASK,
            PROJECT
        }
    }
}