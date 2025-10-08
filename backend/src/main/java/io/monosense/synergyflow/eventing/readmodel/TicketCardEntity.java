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
 * Denormalized ticket read model entity for optimized query performance.
 *
 * <p>This JPA entity implements the read model pattern by providing a denormalized
 * view of ticket data optimized for fast read operations. It stores frequently accessed
 * ticket information in a single table, eliminating the need for complex joins and
 * improving query performance for UI components and reporting.
 *
 * <p><strong>Read Model Benefits:</strong>
 * <ul>
 *   <li><strong>Performance:</strong> Optimized for fast reads with minimal database queries</li>
 *   <li><strong>Simplicity:</strong> Single-table access for common ticket display needs</li>
 *   <li><strong>Scalability:</strong> Supports high-volume read operations without impacting write performance</li>
 *   <li><strong>UI Optimization:</strong> Pre-aggregated data perfect for dashboard and list views</li>
 * </ul>
 *
 * <p><strong>Data Synchronization:</strong>
 * <p>This entity is updated asynchronously through event-driven architecture:
 * <ol>
 *   <li>Domain events are published when tickets are created or modified</li>
 *   <li>Event processors update this read model based on the events</li>
 *   <li>Updates are eventually consistent with the domain model</li>
 *   <li>Eventual consistency delay is typically sub-second</li>
 * </ol>
 *
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Ticket list views and dashboards</li>
 *   <li>Ticket search and filtering operations</li>
 *   <li>SLA monitoring and overdue ticket tracking</li>
 *   <li>Assignment and workload management</li>
 *   <li>Reporting and analytics queries</li>
 * </ul>
 *
 * <p><strong>Field Details:</strong>
 * <ul>
 *   <li>{@code id}: Primary key for the read model (UUID)</li>
 *   <li>{@code ticketId}: Foreign key reference to the domain ticket</li>
 *   <li>{@code ticketNumber}: Human-readable ticket identifier</li>
 *   <li>{@code title}: Ticket title/description</li>
 *   <li>{@code status}: Current ticket status (e.g., "OPEN", "IN_PROGRESS", "RESOLVED")</li>
 *   <li>{@code priority}: Ticket priority level (e.g., "LOW", "MEDIUM", "HIGH", "CRITICAL")</li>
 *   <li>{@code category}: Ticket categorization for organization and filtering</li>
 *   <li>{@code requesterName/Email}: Requester contact information</li>
 *   <li>{@code assigneeName/Email}: Assigned agent contact information</li>
 *   <li>{@code slaDueAt/slaStatus/overdue}: SLA tracking fields</li>
 *   <li>{@code createdAt/updatedAt}: Timestamps for tracking</li>
 *   <li>{@code version}: Optimistic locking version</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class TicketQueryService {
 *     @Autowired
 *     private TicketCardRepository repository;
 *
 *     public List<TicketCardEntity> findOverdueTickets() {
 *         return repository.findByOverdueTrueOrderBySlaDueAtAsc();
 *     }
 *
 *     public List<TicketCardEntity> findByAssignee(String email) {
 *         return repository.findByAssigneeEmailOrderByCreatedAtDesc(email);
 *     }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see TicketCardRepository
 * @see TicketCardMapper
 * @see io.monosense.synergyflow.itsm.events.TicketCreated
 * @see io.monosense.synergyflow.itsm.events.TicketAssigned
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_card")
public class TicketCardEntity {

    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private UUID ticketId;

    @Column(name = "ticket_number", nullable = false, length = 50)
    private String ticketNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "requester_name", nullable = false, length = 200)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 255)
    private String requesterEmail;

    @Column(name = "assignee_name", length = 200)
    private String assigneeName;

    @Column(name = "assignee_email", length = 255)
    private String assigneeEmail;

    @Column(name = "sla_due_at")
    private Instant slaDueAt;

    @Column(name = "sla_status", length = 20)
    private String slaStatus;

    @Column(name = "is_overdue", nullable = false)
    private boolean overdue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version", nullable = false)
    private Long version;
}
