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
 * Denormalized queue row read model entity for work queue management and assignment.
 *
 * <p>This JPA entity implements the read model pattern for IT service management work queues,
 * providing an optimized view of tickets organized by queue and priority for efficient
 * assignment and monitoring. It stores queue-specific information in a denormalized format
 * to support real-time queue operations, SLA monitoring, and workload distribution.
 *
 * <p><strong>Queue Management Features:</strong>
 * <ul>
 *   <li><strong>Priority-based Ordering:</strong> Tickets sorted by priority and queue position</li>
 *   <li><strong>SLA Monitoring:</strong> Real-time SLA breach detection and alerting</li>
 *   <li><strong>Auto-routing Support:</strong> Scoring for intelligent ticket assignment</li>
 *   <li><strong>Team Assignment:</strong> Team-based queue organization and workload balancing</li>
 *   <li><strong>Wait Time Tracking:</strong> Continuous monitoring of ticket aging</li>
 * </ul>
 *
 * <p><strong>Data Synchronization:</strong>
 * <p>This entity is updated asynchronously through event-driven architecture:
 * <ol>
 *   <li>Domain events are published when tickets are created, assigned, or status changes</li>
 *   <li>Event processors update queue positions and assignments</li>
 *   <li>Queue recalculations happen on ticket priority changes</li>
 *   <li>Real-time updates reflect in agent dashboards</li>
 * </ol>
 *
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Agent work queue dashboards and real-time updates</li>
 *   <li>Queue monitoring and SLA compliance tracking</li>
 *   <li>Automatic ticket routing and assignment algorithms</li>
 *   <li>Team workload balancing and capacity planning</li>
 *   <li>Queue performance metrics and reporting</li>
 *   <li>Escalation and breach alerting systems</li>
 * </ul>
 *
 * <p><strong>Field Details:</strong>
 * <ul>
 *   <li>{@code id}: Primary key for the read model (UUID)</li>
 *   <li>{@code ticketId}: Foreign key reference to the domain ticket</li>
 *   <li>{@code ticketNumber}: Human-readable ticket identifier for display</li>
 *   <li>{@code title}: Ticket title for quick identification</li>
 *   <li>{@code priority}: Priority level for queue ordering (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")</li>
 *   <li>{@code status}: Current ticket status affecting queue eligibility</li>
 *   <li>{@code queueName}: Target queue name (e.g., "IT_SUPPORT", "HR_HELPDESK", "FACILITIES")</li>
 *   <li>{@code queuePosition}: Position within the queue for ordering</li>
 *   <li>{@code assignedTeamName}: Team currently handling the ticket</li>
 *   <li>{@code waitTimeMinutes}: Time spent waiting in queue for SLA monitoring</li>
 *   <li>{@code slaBreached}: Flag indicating SLA violation status</li>
 *   <li>{@code autoRouteScore}: Algorithmic score for automatic assignment decisions</li>
 *   <li>{@code createdAt}: Original ticket creation timestamp</li>
 *   <li>{@code version}: Optimistic locking for concurrent queue updates</li>
 * </ul>
 *
 * <p><strong>Queue Operations:</strong>
 * <p>Typical queue management operations supported by this entity:
 * <ul>
 *   <li><strong>Enqueue:</strong> Add new tickets to appropriate queues based on category</li>
 *   <li><strong>Reorder:</strong> Adjust queue positions based on priority changes</li>
 *   <li><strong>Assign:</strong> Assign tickets to agents and update queue positions</li>
 *   <li><strong>Transfer:</strong> Move tickets between queues when necessary</li>
 *   <li><strong>Escalate:</strong> Handle SLA breaches and automatic escalations</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class QueueService {
 *     @Autowired
 *     private QueueRowRepository repository;
 *
 *     public List<QueueRowEntity> getAgentQueue(String queueName) {
 *         return repository.findByQueueNameOrderByQueuePositionAsc(queueName);
 *     }
 *
 *     public List<QueueRowEntity> getSLABreachedTickets() {
 *         return repository.findBySlaBreachedTrueOrderByWaitTimeMinutesDesc();
 *     }
 *
 *     public void assignNextTicket(String agentId, String queueName) {
 *         QueueRowEntity nextTicket = repository.findFirstByQueueNameOrderByQueuePositionAsc(queueName);
 *         // Assignment logic here
 *     }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see QueueRowRepository
 * @see QueueRowMapper
 * @see io.monosense.synergyflow.itsm.events.TicketCreated
 * @see io.monosense.synergyflow.itsm.events.TicketAssigned
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "queue_row")
public class QueueRowEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private UUID ticketId;

    @Column(name = "ticket_number", nullable = false, length = 50)
    private String ticketNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "queue_name", nullable = false, length = 100)
    private String queueName;

    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;

    @Column(name = "assigned_team_name", length = 200)
    private String assignedTeamName;

    @Column(name = "wait_time_minutes", nullable = false)
    private Integer waitTimeMinutes;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    @Column(name = "auto_route_score")
    private Integer autoRouteScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "version", nullable = false)
    private Long version;
}
