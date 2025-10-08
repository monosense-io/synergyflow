package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Service request ticket representing planned service fulfillment requests.
 *
 * <p>A service request is a ticket type used to track formal requests for service
 * delivery, such as software installations, access provisioning, or hardware requests.
 * Service requests typically follow an approval workflow before fulfillment.</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Automatically sets {@code ticketType = SERVICE_REQUEST}</li>
 *   <li>Supports approval workflow with PENDING/APPROVED/REJECTED states</li>
 *   <li>Tracks fulfiller and fulfillment timestamp separately from assignee</li>
 *   <li>Part of SINGLE_TABLE inheritance strategy for query performance</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
@Entity
@DiscriminatorValue("SERVICE_REQUEST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceRequest extends Ticket {

    /**
     * Type of service being requested.
     *
     * <p>Examples: "Software Installation", "Access Request", "Hardware Provisioning",
     * "Account Creation", "Password Reset", etc.</p>
     *
     * <p>This field helps categorize and route service requests to appropriate
     * fulfillment teams.</p>
     */
    @Column(name = "request_type", length = 100)
    private String requestType;

    /**
     * Current approval status of the service request.
     *
     * <p>Tracks the approval workflow state:
     * <ul>
     *   <li>PENDING - Awaiting approval decision</li>
     *   <li>APPROVED - Request approved and can proceed to fulfillment</li>
     *   <li>REJECTED - Request denied</li>
     * </ul>
     *
     * <p>Nullable to support requests that don't require approval.</p>
     */
    @Column(name = "approval_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    /**
     * UUID of the agent who fulfilled this service request.
     *
     * <p>Tracks the actual fulfiller, which may differ from the assignee.
     * For example, the assignee might be a team lead who delegates fulfillment
     * to a team member.</p>
     *
     * <p>Nullable until the request is actually fulfilled.</p>
     */
    @Column(name = "fulfiller_id")
    private UUID fulfillerId;

    /**
     * Timestamp when the service request was fulfilled.
     *
     * <p>Set when the request is completed and the requested service has been
     * delivered. Nullable until fulfillment is complete.</p>
     */
    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    /**
     * Creates a new service request ticket with the specified details.
     *
     * <p>Initializes a service request and automatically sets the ticket type
     * to SERVICE_REQUEST. The request type should be specified to enable proper
     * routing and fulfillment workflows.</p>
     *
     * @param title the descriptive title of the service request (required)
     * @param description detailed description of what is being requested
     * @param status initial status (typically NEW)
     * @param priority business priority level
     * @param category category classification for routing
     * @param requesterId the UUID of the user making the request (required)
     * @param assigneeId the UUID of the assigned agent (nullable)
     * @param requestType the type of service being requested (e.g., "Software Installation")
     * @since 2.1
     */
    public ServiceRequest(String title, String description, TicketStatus status,
                          Priority priority, String category, UUID requesterId,
                          UUID assigneeId, String requestType) {
        super(title, description, TicketType.SERVICE_REQUEST, status, priority,
                category, requesterId, assigneeId);
        this.requestType = requestType;
    }

    /**
     * Approves this service request.
     *
     * <p>Updates the approval status to APPROVED, allowing the request to
     * proceed to fulfillment.</p>
     *
     * @since 2.1
     */
    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    /**
     * Rejects this service request.
     *
     * <p>Updates the approval status to REJECTED, preventing fulfillment.</p>
     *
     * @since 2.1
     */
    public void reject() {
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    /**
     * Records the fulfillment of this service request.
     *
     * <p>Captures the fulfiller ID and timestamp when the requested service
     * has been delivered. Typically called when the request status is updated
     * to RESOLVED or CLOSED.</p>
     *
     * @param fulfillerId the UUID of the agent who fulfilled the request (required)
     * @since 2.1
     */
    public void fulfill(UUID fulfillerId) {
        this.fulfillerId = fulfillerId;
        this.fulfilledAt = Instant.now();
    }

    /**
     * Updates the request type classification.
     *
     * <p>Used to reclassify the service request if the initial categorization
     * was incorrect or if the request scope changes.</p>
     *
     * @param requestType the new request type (required)
     * @since 2.1
     */
    public void updateRequestType(String requestType) {
        this.requestType = requestType;
    }
}
