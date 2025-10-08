package io.monosense.synergyflow.pm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a Project Management issue is created.
 *
 * <p>This event captures the complete state of a newly created issue and is published
 * to notify other parts of the system about issue creation. It contains all essential
 * issue information needed for downstream processing and read model updates. The event
 * includes the initial state (TODO) and default priority (MEDIUM) assigned to new issues.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * IssueCreated event = new IssueCreated(
 *     issue.getId(),
 *     issue.getIssueKey(),
 *     issue.getTitle(),
 *     issue.getIssueType(),
 *     issue.getStatus(),
 *     issue.getPriority(),
 *     issue.getReporterId(),
 *     issue.getCreatedAt(),
 *     issue.getUpdatedAt()
 * );
 *
 * eventPublisher.publish(
 *     event.issueId(),
 *     "ISSUE",
 *     "IssueCreated",
 *     1L, // initial version
 *     event.createdAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 *
 * @param issueId the unique identifier of the created issue
 * @param issueKey the human-readable issue key
 * @param title the descriptive title of the issue
 * @param issueType the type of issue (e.g., "BUG", "FEATURE", "TASK", "EPIC")
 * @param status the initial status of the issue (typically "TODO")
 * @param priority the priority level of the issue (default "MEDIUM")
 * @param reporterId the unique identifier of the user who created the issue
 * @param createdAt the timestamp when the issue was created
 * @param updatedAt the timestamp when the issue was last updated (same as createdAt for new issues)
 */
public record IssueCreated(
        UUID issueId,
        String issueKey,
        String title,
        String issueType,
        String status,
        String priority,
        UUID reporterId,
        Instant createdAt,
        Instant updatedAt
) {
}
