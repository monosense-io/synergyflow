package io.monosense.synergyflow.itsm.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServiceRequest} entity.
 *
 * <p>Tests service request specific behavior including constructor initialization,
 * ticketType discriminator, and domain methods for approval and fulfillment workflows.</p>
 *
 * @since 2.1
 */
@DisplayName("ServiceRequest Entity Unit Tests")
class ServiceRequestTest {

    @Test
    @DisplayName("Constructor should set ticketType to SERVICE_REQUEST")
    void testConstructorSetsTicketType() {
        // Given: ServiceRequest parameters
        ServiceRequest serviceRequest = createTestServiceRequest();

        // Then: ticketType should be set to SERVICE_REQUEST
        assertThat(serviceRequest.getTicketType()).isEqualTo(TicketType.SERVICE_REQUEST);
    }

    @Test
    @DisplayName("Constructor should initialize all fields including requestType")
    void testConstructorInitialization() {
        // Given: ServiceRequest parameters with all fields
        String title = "New laptop request";
        String description = "Request for MacBook Pro for new developer";
        TicketStatus status = TicketStatus.NEW;
        Priority priority = Priority.HIGH;
        String category = "Hardware";
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        String requestType = "Hardware Provisioning";

        // When: ServiceRequest is created
        ServiceRequest serviceRequest = new ServiceRequest(title, description, status,
                priority, category, requesterId, assigneeId, requestType);

        // Then: All fields should be initialized correctly
        assertThat(serviceRequest.getTitle()).isEqualTo(title);
        assertThat(serviceRequest.getDescription()).isEqualTo(description);
        assertThat(serviceRequest.getTicketType()).isEqualTo(TicketType.SERVICE_REQUEST);
        assertThat(serviceRequest.getStatus()).isEqualTo(status);
        assertThat(serviceRequest.getPriority()).isEqualTo(priority);
        assertThat(serviceRequest.getCategory()).isEqualTo(category);
        assertThat(serviceRequest.getRequesterId()).isEqualTo(requesterId);
        assertThat(serviceRequest.getAssigneeId()).isEqualTo(assigneeId);
        assertThat(serviceRequest.getRequestType()).isEqualTo(requestType);
        assertThat(serviceRequest.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Constructor should accept null approvalStatus")
    void testConstructorWithNullApprovalStatus() {
        // Given: ServiceRequest with default null approval status
        ServiceRequest serviceRequest = createTestServiceRequest();

        // Then: ApprovalStatus should be null (not yet set)
        assertThat(serviceRequest.getApprovalStatus()).isNull();
        assertThat(serviceRequest.getTicketType()).isEqualTo(TicketType.SERVICE_REQUEST);
    }

    @Test
    @DisplayName("approve() should set approvalStatus to APPROVED")
    void testApprove() {
        // Given: A service request pending approval
        ServiceRequest serviceRequest = createTestServiceRequest();
        assertThat(serviceRequest.getApprovalStatus()).isNull();

        // When: Request is approved
        serviceRequest.approve();

        // Then: Approval status should be APPROVED
        assertThat(serviceRequest.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("reject() should set approvalStatus to REJECTED")
    void testReject() {
        // Given: A service request pending approval
        ServiceRequest serviceRequest = createTestServiceRequest();
        assertThat(serviceRequest.getApprovalStatus()).isNull();

        // When: Request is rejected
        serviceRequest.reject();

        // Then: Approval status should be REJECTED
        assertThat(serviceRequest.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
    }

    @Test
    @DisplayName("fulfill() should set fulfillerId and timestamp")
    void testFulfill() {
        // Given: An unfulfilled service request
        ServiceRequest serviceRequest = createTestServiceRequest();
        assertThat(serviceRequest.getFulfillerId()).isNull();
        assertThat(serviceRequest.getFulfilledAt()).isNull();

        Instant beforeFulfill = Instant.now();

        // When: Request is fulfilled
        UUID fulfillerId = UUID.randomUUID();
        serviceRequest.fulfill(fulfillerId);

        // Then: Fulfiller ID and timestamp should be set
        assertThat(serviceRequest.getFulfillerId()).isEqualTo(fulfillerId);
        assertThat(serviceRequest.getFulfilledAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeFulfill)
                .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("updateRequestType() should change request type classification")
    void testUpdateRequestType() {
        // Given: A service request with initial request type
        ServiceRequest serviceRequest = createTestServiceRequest();
        assertThat(serviceRequest.getRequestType()).isEqualTo("Software Installation");

        // When: Request type is updated
        String newRequestType = "Access Request";
        serviceRequest.updateRequestType(newRequestType);

        // Then: Request type should be updated
        assertThat(serviceRequest.getRequestType()).isEqualTo(newRequestType);
    }

    @Test
    @DisplayName("ServiceRequest should inherit lifecycle callbacks from Ticket")
    void testInheritedLifecycleCallbacks() {
        // Given: A new service request without timestamps
        ServiceRequest serviceRequest = createTestServiceRequest();
        assertThat(serviceRequest.getCreatedAt()).isNull();
        assertThat(serviceRequest.getUpdatedAt()).isNull();

        // When: onCreate() lifecycle callback is invoked
        serviceRequest.onCreate();

        // Then: Timestamps should be set
        assertThat(serviceRequest.getCreatedAt()).isNotNull();
        assertThat(serviceRequest.getUpdatedAt()).isNotNull();
        assertThat(serviceRequest.getCreatedAt()).isEqualTo(serviceRequest.getUpdatedAt());
    }

    @Test
    @DisplayName("ServiceRequest should inherit domain methods from Ticket")
    void testInheritedDomainMethods() {
        // Given: An unassigned service request
        ServiceRequest serviceRequest = createTestServiceRequest();
        assertThat(serviceRequest.getAssigneeId()).isNull();

        // When: Request is assigned to an agent
        UUID agentId = UUID.randomUUID();
        serviceRequest.assignTo(agentId);

        // Then: Assignee should be set
        assertThat(serviceRequest.getAssigneeId()).isEqualTo(agentId);

        // When: Status is updated
        serviceRequest.updateStatus(TicketStatus.IN_PROGRESS);

        // Then: Status should be updated
        assertThat(serviceRequest.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Full approval and fulfillment workflow")
    void testApprovalFulfillmentWorkflow() {
        // Given: A new service request
        ServiceRequest serviceRequest = createTestServiceRequest();

        // When: Request goes through approval workflow
        serviceRequest.approve();
        assertThat(serviceRequest.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        serviceRequest.updateStatus(TicketStatus.IN_PROGRESS);
        assertThat(serviceRequest.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        UUID fulfillerId = UUID.randomUUID();
        serviceRequest.fulfill(fulfillerId);

        // Then: Request should be fully processed
        assertThat(serviceRequest.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(serviceRequest.getFulfillerId()).isEqualTo(fulfillerId);
        assertThat(serviceRequest.getFulfilledAt()).isNotNull();
    }

    /**
     * Helper method to create a test service request with minimal required fields.
     */
    private ServiceRequest createTestServiceRequest() {
        return new ServiceRequest(
                "Test Service Request",
                "Test Description",
                TicketStatus.NEW,
                Priority.MEDIUM,
                "Test Category",
                UUID.randomUUID(),
                null,
                "Software Installation"
        );
    }
}
