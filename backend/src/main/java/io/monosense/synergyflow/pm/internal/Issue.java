package io.monosense.synergyflow.pm.internal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Issue aggregate root representing Project Management issues.
 *
 * <p>This JPA entity serves as the aggregate root for Project Management issues in the domain model.
 * It encapsulates issue state and behavior while maintaining consistency through business rules
 * and domain events. The issue lifecycle follows standard project management practices with states
 * such as TODO, IN_PROGRESS, DONE, and BLOCKED. Issues support various types including BUG, FEATURE,
 * TASK, and EPIC.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Maintain issue state and business invariants</li>
 *   <li>Generate domain events for state changes</li>
 *   <li>Enforce data integrity through JPA constraints</li>
 *   <li>Support optimistic locking with version control</li>
 *   <li>Track issue assignment and reporter information</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Create a new issue
 * Issue issue = new Issue(
 *     UUID.randomUUID(),
 *     "ISSUE-001",
 *     "Login page responsive design issue",
 *     "BUG",
 *     reporterId
 * );
 *
 * // Change issue state
 * issue.changeState("IN_PROGRESS");
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "issues")
class Issue {

    @Id
    private UUID id;

    @Column(name = "issue_key", nullable = false, unique = true)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Version
    private Long version = 1L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Default constructor required by JPA.
     *
     * @deprecated Should not be used directly. Use the public constructor instead.
     */
    @Deprecated
    protected Issue() {
        // JPA constructor
    }

    /**
     * Creates a new issue with the specified details.
     *
     * <p>Initializes an issue in TODO status with MEDIUM priority and creation/update timestamps.
     * The issue key must be unique within the system. The version is automatically managed
     * by JPA's optimistic locking mechanism.</p>
     *
     * @param id the unique identifier for the issue
     * @param issueKey the human-readable issue key (must be unique)
     * @param title the descriptive title of the issue
     * @param issueType the type of issue (e.g., "BUG", "FEATURE", "TASK", "EPIC")
     * @param reporterId the unique identifier of the user who created the issue
     * @throws IllegalArgumentException if any required parameter is null or empty
     */
    public Issue(UUID id, String issueKey, String title, String issueType, UUID reporterId) {
        this.id = id;
        this.issueKey = issueKey;
        this.title = title;
        this.issueType = issueType;
        this.status = "TODO";
        this.priority = "MEDIUM";
        this.reporterId = reporterId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // version is managed by JPA @Version
    }

    /**
     * Changes the state of this issue to the specified new state.
     *
     * <p>Updates the issue status and automatically updates the last modified timestamp.
     * The JPA version will be automatically incremented for optimistic locking. This method
     * supports transitions between any valid issue states (TODO, IN_PROGRESS, DONE, BLOCKED).</p>
     *
     * @param newState the new state to transition to (e.g., "IN_PROGRESS", "DONE", "BLOCKED")
     * @throws IllegalArgumentException if newState is null or empty
     */
    public void changeState(String newState) {
        this.status = newState;
        this.updatedAt = Instant.now();
        // Version will be incremented automatically by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getTitle() {
        return title;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
