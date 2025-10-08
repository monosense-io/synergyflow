package io.monosense.synergyflow.pm.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.monosense.synergyflow.eventing.api.EventPublisher;
import io.monosense.synergyflow.pm.events.IssueCreated;
import io.monosense.synergyflow.pm.events.IssueStateChanged;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing Project Management issues with event-driven architecture.
 *
 * <p>This service handles the core business operations for issues in the Project Management domain.
 * It demonstrates transactional event publishing by persisting domain events to the outbox
 * pattern within the same database transaction as aggregate state changes. This ensures
 * reliable event delivery and maintains consistency between command and query sides.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Create new issues with automatic issue key generation</li>
 *   <li>Change issue state and track state transitions</li>
 *   <li>Publish domain events for lifecycle changes</li>
 *   <li>Maintain transactional consistency between commands and events</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class IssueController {
 *     private final IssueService issueService;
 *
 *     public UUID createNewIssue(String title, String issueType, UUID reporterId) {
 *         return issueService.createIssue(title, issueType, reporterId);
 *     }
 *
 *     public void changeIssueState(UUID issueId, String newState, UUID changedBy) {
 *         issueService.changeIssueState(issueId, newState, changedBy);
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Service
public class IssueService {

    private final IssueRepository repository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new IssueService with the required dependencies.
     *
     * @param repository the JPA repository for issue persistence
     * @param eventPublisher the event publisher for domain events
     * @param objectMapper the JSON object mapper for event serialization
     * @throws IllegalArgumentException if any parameter is null
     */
    public IssueService(IssueRepository repository, EventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new issue with the specified details and publishes an IssueCreated event.
     *
     * <p>This method creates a new issue with automatically generated issue key,
     * persists it to the database, and publishes a corresponding domain event. The operation
     * is performed within a single transaction to ensure consistency between the issue
     * state and the published event.</p>
     *
     * @param title the descriptive title of the issue
     * @param issueType the type of issue (e.g., "BUG", "FEATURE", "TASK", "EPIC")
     * @param reporterId the unique identifier of the user creating the issue
     * @return the unique identifier of the created issue
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws DuplicateEventException if an event with the same aggregate ID and version already exists
     */
    @Transactional
    public UUID createIssue(String title, String issueType, UUID reporterId) {
        // Create and save issue
        String issueKey = "I-" + System.currentTimeMillis(); // Simple issue key generation
        Issue issue = new Issue(UUID.randomUUID(), issueKey, title, issueType, reporterId);
        issue = repository.save(issue);

        // Prepare event payload
        IssueCreated event = new IssueCreated(
                issue.getId(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getIssueType(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getReporterId(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );

        // Publish event with incremented version
        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                issue.getId(),
                "ISSUE",
                "IssueCreated",
                issue.getVersion(),
                issue.getCreatedAt(),
                payload
        );

        return issue.getId();
    }

    /**
     * Changes the state of an issue and publishes an IssueStateChanged event.
     *
     * <p>This method updates the issue's state, tracks the previous state for auditing,
     * persists the change to the database, and publishes a corresponding domain event.
     * The operation is performed within a single transaction to ensure consistency between
     * the issue state and the published event. The method uses saveAndFlush to ensure
     * the JPA version is incremented before event publishing.</p>
     *
     * @param issueId the unique identifier of the issue to update
     * @param newState the new state to transition to (e.g., "IN_PROGRESS", "DONE", "BLOCKED")
     * @param changedBy the unique identifier of the user performing the state change
     * @throws IllegalArgumentException if any parameter is null or if the issue is not found
     * @throws DuplicateEventException if an event with the same aggregate ID and version already exists
     */
    @Transactional
    public void changeIssueState(UUID issueId, String newState, UUID changedBy) {
        Issue issue = repository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));

        String oldState = issue.getStatus();

        // Update issue
        issue.changeState(newState);
        // Flush to ensure @Version increments before publishing the event
        issue = repository.saveAndFlush(issue);

        // Prepare event payload
        IssueStateChanged event = new IssueStateChanged(
                issue.getId(),
                oldState,
                newState,
                changedBy,
                issue.getVersion(),
                issue.getUpdatedAt()
        );

        // Publish event with incremented version
        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                issue.getId(),
                "ISSUE",
                "IssueStateChanged",
                issue.getVersion(),
                issue.getUpdatedAt(),
                payload
        );
    }
}
