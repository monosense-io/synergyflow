package io.monosense.synergyflow.time.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a time entry in SynergyFlow.
 *
 * <p>Time entries can be created once and mirrored to multiple target entities
 * (Incident, Task, etc.) through the event-driven mirroring system.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
public record TimeEntry(
        String id,
        String userId,
        Duration duration,
        String description,
        Instant occurredAt,
        TimeEntryStatus status,
        Instant createdAt
) {

    /**
     * Creates a new time entry with generated ID and timestamps.
     *
     * @param userId the user who created the entry
     * @param duration the duration of work performed
     * @param description the description of work performed
     * @param occurredAt when the work occurred (business time)
     * @return a new TimeEntry in DRAFT status
     */
    public static TimeEntry create(String userId, Duration duration, String description, Instant occurredAt) {
        return new TimeEntry(
                UUID.randomUUID().toString(),
                userId,
                duration,
                description,
                occurredAt,
                TimeEntryStatus.DRAFT,
                Instant.now()
        );
    }

    /**
     * Marks this time entry as confirmed and ready for mirroring.
     *
     * @return a new TimeEntry with CONFIRMED status
     */
    public TimeEntry confirm() {
        return new TimeEntry(
                id,
                userId,
                duration,
                description,
                occurredAt,
                TimeEntryStatus.CONFIRMED,
                createdAt
        );
    }

    /**
     * Marks this time entry as failed due to mirroring issues.
     *
     * @return a new TimeEntry with FAILED status
     */
    public TimeEntry fail() {
        return new TimeEntry(
                id,
                userId,
                duration,
                description,
                occurredAt,
                TimeEntryStatus.FAILED,
                createdAt
        );
    }

    /**
     * Enumeration of time entry statuses.
     */
    public enum TimeEntryStatus {
        DRAFT,
        CONFIRMED,
        MIRRORING,
        COMPLETED,
        FAILED
    }
}