package io.monosense.synergyflow.time.application.consumers;

import io.monosense.synergyflow.common.idempotency.ProcessedEventRepository;
import io.monosense.synergyflow.time.api.TimeEntryCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Event consumer that mirrors time entries to task worklogs.
 *
 * <p>Listens for TimeEntryCreatedEvent and creates corresponding worklog entries
 * for task target entities with idempotency guarantees.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@Component
public class TaskWorklogConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskWorklogConsumer.class);
    private final JdbcTemplate jdbcTemplate;
    private final ProcessedEventRepository processedEventRepository;

    public TaskWorklogConsumer(JdbcTemplate jdbcTemplate, ProcessedEventRepository processedEventRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Handles TimeEntryCreatedEvent by mirroring to task worklogs.
     *
     * @param event the time entry created event
     */
    @EventListener
    public void handleTimeEntryCreated(TimeEntryCreatedEvent event) {
        String idempotencyKey = ProcessedEventRepository.generateKey(event.timeEntryId(), this.getClass());

        if (processedEventRepository.isProcessed(idempotencyKey)) {
            log.debug("Time entry {} already processed by TaskWorklogConsumer", event.timeEntryId());
            return;
        }

        log.info("Mirroring time entry {} to task worklogs", event.timeEntryId());

        // Filter for task target entities
        var taskTargets = event.targetEntities().stream()
                .filter(target -> target.type() == TimeEntryCreatedEvent.TargetEntity.EntityType.TASK)
                .toList();

        if (taskTargets.isEmpty()) {
            log.debug("No task targets for time entry {}", event.timeEntryId());
            processedEventRepository.markAsProcessed(
                    idempotencyKey,
                    TimeEntryCreatedEvent.class.getName(),
                    event.correlationId(),
                    event.causationId()
            );
            return;
        }

        // Mirror to each task target
        for (var target : taskTargets) {
            try {
                mirrorToTask(event, target);
                log.info("Successfully mirrored time entry {} to task {}", event.timeEntryId(), target.entityId());
            } catch (Exception e) {
                log.error("Failed to mirror time entry {} to task {}: {}",
                        event.timeEntryId(), target.entityId(), e.getMessage(), e);
                // Continue processing other tasks even if one fails
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
     * Mirrors a time entry to a specific task's worklog.
     *
     * @param event the time entry event
     * @param target the task target entity
     */
    private void mirrorToTask(TimeEntryCreatedEvent event, TimeEntryCreatedEvent.TargetEntity target) {
        String sql = """
            INSERT INTO task_worklogs (
                id, task_id, time_entry_id, user_id, duration_minutes,
                description, occurred_at, created_at, correlation_id, causation_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (time_entry_id, task_id) DO UPDATE SET
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

        // Update task's aggregate time
        updateTaskAggregateTime(target.entityId());
    }

    /**
     * Updates the aggregate time for a task.
     *
     * @param taskId the task ID
     */
    private void updateTaskAggregateTime(String taskId) {
        String sql = """
            UPDATE tasks
            SET total_time_minutes = (
                SELECT COALESCE(SUM(duration_minutes), 0)
                FROM task_worklogs
                WHERE task_id = ?
            ),
            updated_at = NOW()
            WHERE id = ?
            """;

        jdbcTemplate.update(sql, taskId, taskId);
    }
}