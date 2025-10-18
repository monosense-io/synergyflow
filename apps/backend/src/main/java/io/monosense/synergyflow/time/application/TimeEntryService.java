package io.monosense.synergyflow.time.application;

import io.monosense.synergyflow.time.api.TimeEntryController;
import io.monosense.synergyflow.time.api.TimeEntryCreatedEvent;
import io.monosense.synergyflow.time.domain.TimeEntry;
import io.monosense.synergyflow.time.infrastructure.TimeEntryRepository;
import io.monosense.synergyflow.time.observability.TimeEntryMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing time entries and their mirroring process.
 *
 * <p>Handles the core business logic for creating time entries,
 * persisting them with audit trail, and publishing events for mirroring.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@Service
@Transactional
public class TimeEntryService {

    private static final Logger log = LoggerFactory.getLogger(TimeEntryService.class);
    private final TimeEntryRepository timeEntryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeEntryMetrics metrics;

    public TimeEntryService(TimeEntryRepository timeEntryRepository, ApplicationEventPublisher eventPublisher, TimeEntryMetrics metrics) {
        this.timeEntryRepository = timeEntryRepository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    /**
     * Creates a single time entry and initiates the mirroring process.
     *
     * @param userId the user creating the time entry
     * @param duration the duration of work performed
     * @param description the description of work
     * @param occurredAt when the work occurred
     * @param targetEntities the entities to mirror the time entry to
     * @param correlationId the correlation ID for tracing
     * @return the created time entry
     */
    public TimeEntry createTimeEntry(
            String userId,
            Duration duration,
            String description,
            Instant occurredAt,
            List<TimeEntryController.TargetEntityDto> targetEntities,
            String correlationId) {

        log.debug("Creating time entry for user: {}, duration: {} minutes", userId, duration.toMinutes());

        Instant startTime = Instant.now();

        TimeEntry timeEntry = TimeEntry.create(userId, duration, description, occurredAt);
        TimeEntry savedEntry = timeEntryRepository.save(timeEntry);

        // Record creation duration
        metrics.recordCreationDuration(Duration.between(startTime, Instant.now()));
        metrics.recordTimeEntryCreated();

        // Convert target entities
        List<TimeEntryCreatedEvent.TargetEntity> targets = targetEntities.stream()
                .map(dto -> new TimeEntryCreatedEvent.TargetEntity(
                        dto.type(),
                        dto.entityId(),
                        dto.entityTitle()
                ))
                .collect(Collectors.toList());

        // Publish event for mirroring
        TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
                savedEntry.id(),
                userId,
                duration,
                description,
                occurredAt,
                targets,
                correlationId,
                correlationId, // For user-initiated events, causation equals correlation
                Instant.now()
        );

        eventPublisher.publishEvent(event);
        log.info("Published TimeEntryCreatedEvent for time entry: {}", savedEntry.id());

        return savedEntry;
    }

    /**
     * Creates multiple time entries in a single transaction.
     *
     * @param userId the user creating the time entries
     * @param entries the list of time entry requests
     * @param correlationId the correlation ID for tracing
     * @return list of tracking IDs
     */
    public List<String> createBulkTimeEntries(
            String userId,
            List<TimeEntryController.CreateTimeEntryRequest> entries,
            String correlationId) {

        log.debug("Creating {} bulk time entries for user: {}", entries.size(), userId);

        return entries.stream()
                .map(request -> createTimeEntry(
                        userId,
                        Duration.ofMinutes(request.durationMinutes()),
                        request.description(),
                        request.occurredAt() != null ? request.occurredAt() : Instant.now(),
                        request.targetEntities(),
                        correlationId
                ))
                .map(TimeEntry::id)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a time entry by its ID.
     *
     * @param id the time entry ID
     * @return the time entry if found
     */
    @Transactional(readOnly = true)
    public java.util.Optional<TimeEntry> getTimeEntry(String id) {
        return timeEntryRepository.findById(id);
    }

    /**
     * Retrieves all time entries for a specific user.
     *
     * @param userId the user ID
     * @return list of time entries
     */
    @Transactional(readOnly = true)
    public List<TimeEntry> getTimeEntriesByUser(String userId) {
        return timeEntryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}