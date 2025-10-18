package io.monosense.synergyflow.time.application.consumers;

import io.monosense.synergyflow.common.idempotency.ProcessedEventRepository;
import io.monosense.synergyflow.time.api.TimeEntryCreatedEvent;
import io.monosense.synergyflow.time.observability.TimeEntryMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Event consumer that mirrors time entries to incident worklogs.
 *
 * <p>Listens for TimeEntryCreatedEvent and creates corresponding worklog entries
 * for incident target entities with idempotency guarantees.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@Component
public class IncidentWorklogConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncidentWorklogConsumer.class);
    private final JdbcTemplate jdbcTemplate;
    private final ProcessedEventRepository processedEventRepository;
    private final TimeEntryMetrics metrics;

    public IncidentWorklogConsumer(JdbcTemplate jdbcTemplate, ProcessedEventRepository processedEventRepository, TimeEntryMetrics metrics) {
        this.jdbcTemplate = jdbcTemplate;
        this.processedEventRepository = processedEventRepository;
        this.metrics = metrics;
    }

    /**
     * Handles TimeEntryCreatedEvent by mirroring to incident worklogs.
     *
     * @param event the time entry created event
     */
    @EventListener
    public void handleTimeEntryCreated(TimeEntryCreatedEvent event) {
        String idempotencyKey = ProcessedEventRepository.generateKey(event.timeEntryId(), this.getClass());

        if (processedEventRepository.isProcessed(idempotencyKey)) {
            log.debug("Time entry {} already processed by IncidentWorklogConsumer", event.timeEntryId());
            return;
        }

        log.info("Mirroring time entry {} to incident worklogs", event.timeEntryId());

        // Filter for incident target entities
        var incidentTargets = event.targetEntities().stream()
                .filter(target -> target.type() == TimeEntryCreatedEvent.TargetEntity.EntityType.INCIDENT)
                .toList();

        if (incidentTargets.isEmpty()) {
            log.debug("No incident targets for time entry {}", event.timeEntryId());
            processedEventRepository.markAsProcessed(
                    idempotencyKey,
                    TimeEntryCreatedEvent.class.getName(),
                    event.correlationId(),
                    event.causationId()
            );
            return;
        }

        // Mirror to each incident target
        for (var target : incidentTargets) {
            Instant startTime = Instant.now();
            try {
                mirrorToIncident(event, target);
                Duration latency = Duration.between(startTime, Instant.now());
                metrics.recordTimeEntryMirrored(latency);
                log.info("Successfully mirrored time entry {} to incident {} in {}ms",
                        event.timeEntryId(), target.entityId(), latency.toMillis());
            } catch (Exception e) {
                metrics.recordTimeEntryFailed();
                log.error("Failed to mirror time entry {} to incident {}: {}",
                        event.timeEntryId(), target.entityId(), e.getMessage(), e);
                // Continue processing other incidents even if one fails
            }
        }

        // Mark as processed
        processedEventRepository.markAsProcessed(
                idempotencyKey,
                TimeEntryCreatedEvent.class.getName(),
                event.correlationId(),
                event.causationId()
        );
    }

    /**
     * Mirrors a time entry to a specific incident's worklog.
     *
     * @param event the time entry event
     * @param target the incident target entity
     */
    private void mirrorToIncident(TimeEntryCreatedEvent event, TimeEntryCreatedEvent.TargetEntity target) {
        String sql = """
            INSERT INTO incident_worklogs (
                id, incident_id, time_entry_id, user_id, duration_minutes,
                description, occurred_at, created_at, correlation_id, causation_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (time_entry_id, incident_id) DO UPDATE SET
                duration_minutes = EXCLUDED.duration_minutes,
                description = EXCLUDED.description,
                occurred_at = EXCLUDED.occurred_at,
                updated_at = NOW()
            """;

        String worklogId = java.util.UUID.randomUUID().toString();

        jdbcTemplate.update(sql,
                worklogId,
                target.entityId(),
                event.timeEntryId(),
                event.userId(),
                event.duration().toMinutes(),
                event.description(),
                Timestamp.from(event.occurredAt()),
                Timestamp.from(Instant.now()),
                event.correlationId(),
                event.causationId()
        );

        // Update incident's aggregate time
        updateIncidentAggregateTime(target.entityId());
    }

    /**
     * Updates the aggregate time for an incident.
     *
     * @param incidentId the incident ID
     */
    private void updateIncidentAggregateTime(String incidentPublicId) {
        String sql = """
            UPDATE incidents
            SET total_time_minutes = (
                SELECT COALESCE(SUM(duration_minutes), 0)
                FROM incident_worklogs
                WHERE incident_id = ?
            ),
            updated_at = NOW()
            WHERE incident_id = ?
            """;

        jdbcTemplate.update(sql, incidentPublicId, incidentPublicId);
    }
}
