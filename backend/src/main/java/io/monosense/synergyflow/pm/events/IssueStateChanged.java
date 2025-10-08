package io.monosense.synergyflow.pm.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when a Project Management issue state changes.
 *
 * <p>This event captures state transitions for issues, including the previous state,
 * new state, and who performed the change. This event is used to notify other parts
 * of the system about workflow progressions, update read models, and trigger any
 * follow-up actions such as notifications or automated workflows.</p>
 *
 * <p>The event includes the aggregate version to support event ordering and conflict
 * resolution in distributed systems. The changedAt timestamp represents when the
 * state change occurred, which may differ from the issue's updatedAt timestamp if
 * multiple operations occurred in the same transaction.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Publishing the event
 * IssueStateChanged event = new IssueStateChanged(
 *     issue.getId(),
 *     oldState,
 *     newState,
 *     changedBy,
 *     issue.getVersion(),
 *     Instant.now()
 * );
 *
 * eventPublisher.publish(
 *     event.issueId(),
 *     "ISSUE",
 *     "IssueStateChanged",
 *     event.version(),
 *     event.changedAt(),
 *     objectMapper.valueToTree(event)
 * );
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 *
 * @param issueId the unique identifier of the issue whose state changed
 * @param fromState the previous state before the change (e.g., "TODO")
 * @param toState the new state after the change (e.g., "IN_PROGRESS")
 * @param changedBy the unique identifier of the user who changed the state
 * @param version the version number of the issue aggregate after this state change
 * @param changedAt the timestamp when the state change occurred
 */
public record IssueStateChanged(
        UUID issueId,
        String fromState,
        String toState,
        UUID changedBy,
        Long version,
        Instant changedAt
) {
}
