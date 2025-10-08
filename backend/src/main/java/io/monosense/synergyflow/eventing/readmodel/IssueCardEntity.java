package io.monosense.synergyflow.eventing.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Denormalized issue read model entity for agile project management and tracking.
 *
 * <p>This JPA entity implements the read model pattern for project management issues
 * (stories, tasks, bugs, etc.) by providing a denormalized view optimized for
 * fast read operations in agile development workflows. It stores frequently accessed
 * issue information in a single table, eliminating complex joins and improving query
 * performance for dashboards, kanban boards, and reporting.
 *
 * <p><strong>Read Model Benefits:</strong>
 * <ul>
 *   <li><strong>Kanban Performance:</strong> Optimized for rapid board column updates and drag-drop operations</li>
 *   <li><strong>Sprint Tracking:</strong> Pre-aggregated sprint data for velocity and burndown charts</li>
 *   <li><strong>Assignment Management:</strong> Quick access to workloads and availability</li>
 *   <li><strong>Dependency Tracking:</strong> Parent-child relationships and blocker information readily available</li>
 * </ul>
 *
 * <p><strong>Data Synchronization:</strong>
 * <p>This entity is updated asynchronously through event-driven architecture:
 * <ol>
 *   <li>Domain events are published when issues are created, updated, or moved</li>
 *   <li>Event processors update this read model based on the events</li>
 *   <li>Updates are eventually consistent with the domain model</li>
 *   <li>Board operations reflect changes with minimal latency</li>
 * </ol>
 *
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Kanban board display and interactions</li>
 *   <li>Sprint planning and progress tracking</li>
 *   <li>Team workload and capacity management</li>
 *   <li>Blocker and dependency visualization</li>
 *   <li>Velocity and performance metrics</li>
 *   <li>Issue search and filtering</li>
 * </ul>
 *
 * <p><strong>Field Details:</strong>
 * <ul>
 *   <li>{@code id}: Primary key for the read model (UUID)</li>
 *   <li>{@code issueId}: Foreign key reference to the domain issue</li>
 *   <li>{@code issueKey}: Human-readable issue identifier (e.g., "PROJ-123")</li>
 *   <li>{@code title}: Issue title and description</li>
 *   <li>{@code issueType}: Type classification (e.g., "Story", "Task", "Bug", "Epic")</li>
 *   <li>{@code status}: Current workflow status (e.g., "TODO", "IN_PROGRESS", "DONE")</li>
 *   <li>{@code priority}: Priority level (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")</li>
 *   <li>{@code storyPoints}: Estimated effort for agile planning</li>
 *   <li>{@code assigneeName/Email}: Assigned team member information</li>
 *   <li>{@code sprintName}: Current sprint assignment</li>
 *   <li>{@code boardColumn}: Visual board column position</li>
 *   <li>{@code blocked/blockerCount}: Dependency and blocking information</li>
 *   <li>{@code parentIssueKey}: Parent issue for hierarchical relationships</li>
 *   <li>{@code createdAt/updatedAt}: Timestamps for tracking and auditing</li>
 *   <li>{@code version}: Optimistic locking for concurrent updates</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class IssueQueryService {
 *     @Autowired
 *     private IssueCardRepository repository;
 *
 *     public List<IssueCardEntity> findSprintBacklog(String sprintName) {
 *         return repository.findBySprintNameOrderByPriorityDesc(sprintName);
 *     }
 *
 *     public List<IssueCardEntity> findBlockedIssues() {
 *         return repository.findByBlockedTrueOrderByBlockerCountDesc();
 *     }
 *
 *     public List<IssueCardEntity> findByAssignee(String email) {
 *         return repository.findByAssigneeEmailOrderByStatusAsc(email);
 *     }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see IssueCardRepository
 * @see IssueCardMapper
 * @see io.monosense.synergyflow.pm.events.IssueCreated
 * @see io.monosense.synergyflow.pm.events.IssueStateChanged
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "issue_card")
public class IssueCardEntity {

    @Id
    private UUID id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private UUID issueId;

    @Column(name = "issue_key", nullable = false, length = 50)
    private String issueKey;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "assignee_name", length = 200)
    private String assigneeName;

    @Column(name = "assignee_email", length = 255)
    private String assigneeEmail;

    @Column(name = "sprint_name", length = 200)
    private String sprintName;

    @Column(name = "board_column", length = 100)
    private String boardColumn;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @Column(name = "blocker_count", nullable = false)
    private Integer blockerCount;

    @Column(name = "parent_issue_key", length = 50)
    private String parentIssueKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version", nullable = false)
    private Long version;
}
