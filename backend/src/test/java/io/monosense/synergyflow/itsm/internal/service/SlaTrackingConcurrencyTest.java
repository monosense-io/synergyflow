package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.SlaTrackingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Concurrency integration tests for SLA tracking (Story 2.3 AC-10).
 *
 * <p>Tests verify:
 * <ul>
 *   <li>IT-SLA-7: Concurrent priority changes with optimistic locking retry</li>
 *   <li>IT-SLA-8: Unique constraint enforcement on ticket_id</li>
 * </ul>
 *
 * @author monosense
 * @since 2.3
 */
@SpringBootTest
@Testcontainers
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SLA Tracking Concurrency Tests (Story 2.3)")
class SlaTrackingConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SlaTrackingRepository slaTrackingRepository;

    @Autowired
    private io.monosense.synergyflow.itsm.internal.repository.TicketRepository ticketRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UUID testUserId;
    private UUID testAgentId;

    @org.junit.jupiter.api.BeforeAll
    void setupTestUsers() {
        // Create test users to satisfy foreign key constraints
        testUserId = UUID.randomUUID();
        testAgentId = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, NOW(), NOW(), 1)",
            testUserId, "test-requester-concurrency", "requester-concurrency@test.com", "Test Requester Concurrency"
        );
        jdbcTemplate.update(
            "INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, NOW(), NOW(), 1)",
            testAgentId, "test-agent-concurrency", "agent-concurrency@test.com", "Test Agent Concurrency"
        );
    }

    @AfterEach
    void cleanup() {
        // Clean up test data to prevent constraint violations between tests
        slaTrackingRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    /**
     * IT-SLA-7: Concurrent priority changes with retry
     *
     * <p>Verifies that @Retryable on TicketService.updatePriorityInternal handles
     * OptimisticLockException and both concurrent updates succeed.</p>
     */
    @Test
    @DisplayName("IT-SLA-7: Concurrent priority changes succeed with retry")
    void concurrentPriorityUpdates_shouldSucceedWithRetry() throws Exception {
        // Given: Create incident with MEDIUM priority
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Concurrent Updates",
                "Testing optimistic locking",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                "Test",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testAgentId);
        UUID ticketId = ticket.getId();

        // When: Two threads concurrently update priority
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable updateTask1 = () -> {
            try {
                startLatch.await();
                ticketService.updatePriority(ticketId, Priority.HIGH, testAgentId);
                successCount.incrementAndGet();
                System.out.println("Thread 1 succeeded: HIGH");
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.err.println("Thread 1 failed: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                completionLatch.countDown();
            }
        };

        Runnable updateTask2 = () -> {
            try {
                startLatch.await();
                ticketService.updatePriority(ticketId, Priority.CRITICAL, testAgentId);
                successCount.incrementAndGet();
                System.out.println("Thread 2 succeeded: CRITICAL");
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.err.println("Thread 2 failed: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                completionLatch.countDown();
            }
        };

        executor.submit(updateTask1);
        executor.submit(updateTask2);

        // Start both threads simultaneously
        startLatch.countDown();
        completionLatch.await();
        executor.shutdown();

        // Then: Both updates should succeed (one via retry)
        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);

        // Verify final SLA state matches one of the priorities
        SlaTracking finalSla = slaTrackingRepository.findByTicketId(ticketId).orElseThrow();
        assertThat(finalSla.getPriority()).isIn(Priority.HIGH, Priority.CRITICAL);
    }

    /**
     * IT-SLA-8: Unique constraint enforcement on ticket_id
     *
     * <p>Verifies that database unique constraint prevents duplicate SLA records for same ticket.</p>
     */
    @Test
    @DisplayName("IT-SLA-8: Unique constraint prevents duplicate SLA records")
    void duplicateSlaRecord_shouldViolateUniqueConstraint() {
        // Given: Create incident with SLA tracking
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Unique Constraint",
                "Testing database constraint",
                TicketType.INCIDENT,
                Priority.LOW,
                "Test",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // When/Then: Attempt to insert duplicate SLA record manually
        SlaTracking duplicate = new SlaTracking(ticket.getId(), Priority.HIGH, ticket.getCreatedAt().plusSeconds(7200));
        assertThatThrownBy(() -> slaTrackingRepository.saveAndFlush(duplicate))
                .isInstanceOfAny(DataIntegrityViolationException.class, org.springframework.dao.DuplicateKeyException.class);
    }
}
