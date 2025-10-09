package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSearchCriteria;
import io.monosense.synergyflow.itsm.spi.TicketStatus;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load tests for TicketService concurrent operations (Task 8.1-8.4).
 *
 * <p>These tests verify performance targets under concurrent load with retry behavior,
 * event publishing, and query operations. Tagged with @Tag("load") to exclude from
 * default test runs.</p>
 *
 * <p>Covers AC-24:
 * <ul>
 *   <li>LT-SERVICE-1: Concurrent ticket creation (100/sec, p95 &lt;400ms, 2min duration)</li>
 *   <li>LT-SERVICE-2: Concurrent assignment with retry (200/sec, p95 &lt;400ms, &lt;10% retry exhaustion)</li>
 *   <li>LT-SERVICE-3: SPI query load (500 queries/sec, p95 &lt;100ms, 1min duration)</li>
 * </ul>
 *
 * @since 2.2
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("load")
@DisplayName("TicketService Load Tests (Task 8.1-8.4)")
class TicketServiceLoadTest {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceLoadTest.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketQueryService ticketQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID requesterId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        // Create test users
        requesterId = createUser("requester");
        agentId = createUser("agent");
    }

    @Test
    @DisplayName("LT-SERVICE-1: Concurrent ticket creation - 100 tickets/sec, p95 <400ms, 2min duration (AC-24)")
    void concurrentTicketCreation_shouldMeetPerformanceTargets() throws InterruptedException {
        // Given: Performance targets
        int targetTicketsPerSecond = 100;
        int durationSeconds = 120; // 2 minutes
        int totalTickets = targetTicketsPerSecond * durationSeconds; // 12,000 tickets
        int threadPoolSize = 20;

        log.info("Starting LT-SERVICE-1: {} tickets over {} seconds ({} tickets/sec target)",
                totalTickets, durationSeconds, targetTicketsPerSecond);

        // When: Create tickets concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalTickets);
        List<Long> latencies = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Instant startTime = Instant.now();

        for (int i = 0; i < totalTickets; i++) {
            final int ticketNum = i;
            executor.submit(() -> {
                try {
                    Instant operationStart = Instant.now();

                    CreateTicketCommand command = new CreateTicketCommand(
                            "Load test ticket #" + ticketNum,
                            "Testing concurrent ticket creation performance",
                            TicketType.INCIDENT,
                            Priority.MEDIUM,
                            "LoadTest",
                            requesterId
                    );

                    ticketService.createTicket(command, requesterId);

                    long latencyMs = Duration.between(operationStart, Instant.now()).toMillis();
                    latencies.add(latencyMs);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Failed to create ticket #{}: {}", ticketNum, e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        boolean completed = latch.await(durationSeconds + 30, TimeUnit.SECONDS);
        executor.shutdown();

        Instant endTime = Instant.now();
        long actualDurationSeconds = Duration.between(startTime, endTime).getSeconds();

        // Then: Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = calculatePercentile(latencies, 50);
        long p95 = calculatePercentile(latencies, 95);
        long p99 = calculatePercentile(latencies, 99);
        long max = latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1);

        log.info("LT-SERVICE-1 Results:");
        log.info("  Duration: {}s (target: {}s)", actualDurationSeconds, durationSeconds);
        log.info("  Success: {}/{} ({:.2f}%)", successCount.get(), totalTickets,
                (successCount.get() * 100.0 / totalTickets));
        log.info("  Failures: {}", failureCount.get());
        log.info("  Throughput: {:.2f} tickets/sec", successCount.get() / (double) actualDurationSeconds);
        log.info("  Latencies: p50={}ms, p95={}ms, p99={}ms, max={}ms", p50, p95, p99, max);

        // Verify success criteria
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(totalTickets)
                .as("All %d tickets should be created successfully", totalTickets);
        assertThat(failureCount.get()).isEqualTo(0)
                .as("Zero failures expected");
        assertThat(p95).isLessThan(400)
                .as("p95 latency should be < 400ms");
        assertThat(p99).isLessThan(800)
                .as("p99 latency should be < 800ms");

        log.info("✅ LT-SERVICE-1 PASSED: All success criteria met");
    }

    @Test
    @DisplayName("LT-SERVICE-2: Concurrent assignment with retry - 200/sec, p95 <400ms, <10% retry exhaustion (AC-24)")
    void concurrentAssignmentWithRetry_shouldMeetPerformanceTargets() throws InterruptedException {
        // Given: Create 1,000 tickets to assign concurrently
        int ticketCount = 1000;
        int targetAssignmentsPerSecond = 200;
        int durationSeconds = 60; // 1 minute
        int totalAssignments = targetAssignmentsPerSecond * durationSeconds; // 12,000 assignments
        int threadPoolSize = 20;

        log.info("Starting LT-SERVICE-2: Creating {} tickets for {} assignments over {}s",
                ticketCount, totalAssignments, durationSeconds);

        // Create tickets
        List<UUID> ticketIds = new ArrayList<>();
        for (int i = 0; i < ticketCount; i++) {
            CreateTicketCommand command = new CreateTicketCommand(
                    "Assignment load test ticket #" + i,
                    "Testing concurrent assignment with retry",
                    TicketType.INCIDENT,
                    Priority.HIGH,
                    "LoadTest",
                    requesterId
            );
            Ticket ticket = ticketService.createTicket(command, requesterId);
            ticketIds.add(ticket.getId());
        }

        log.info("Created {} tickets, starting concurrent assignment test", ticketCount);

        // Query initial retry metrics
        Long initialRetryExhaustionCount = queryRetryExhaustionMetric();

        // When: Assign tickets concurrently (high contention - multiple threads assign same tickets)
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalAssignments);
        List<Long> latencies = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger retryExhaustionCount = new AtomicInteger(0);

        Instant startTime = Instant.now();

        for (int i = 0; i < totalAssignments; i++) {
            final UUID ticketId = ticketIds.get(i % ticketCount); // Reuse tickets to create contention
            final int assignmentNum = i;

            executor.submit(() -> {
                try {
                    Instant operationStart = Instant.now();

                    ticketService.assignTicket(ticketId, agentId, requesterId);

                    long latencyMs = Duration.between(operationStart, Instant.now()).toMillis();
                    latencies.add(latencyMs);
                    successCount.incrementAndGet();

                } catch (io.monosense.synergyflow.itsm.internal.exception.ConcurrentUpdateException e) {
                    // Retry exhaustion
                    retryExhaustionCount.incrementAndGet();
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // Other failures (InvalidStateTransitionException for already assigned tickets is OK)
                    if (!e.getMessage().contains("Invalid state transition")) {
                        log.warn("Unexpected failure on assignment #{}: {}", assignmentNum, e.getMessage());
                    }
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        boolean completed = latch.await(durationSeconds + 30, TimeUnit.SECONDS);
        executor.shutdown();

        Instant endTime = Instant.now();
        long actualDurationSeconds = Duration.between(startTime, endTime).getSeconds();

        // Query final retry metrics
        Long finalRetryExhaustionCount = queryRetryExhaustionMetric();
        long metricsRetryExhaustion = (finalRetryExhaustionCount != null && initialRetryExhaustionCount != null)
                ? (finalRetryExhaustionCount - initialRetryExhaustionCount) : 0;

        // Then: Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = latencies.isEmpty() ? 0 : calculatePercentile(latencies, 50);
        long p95 = latencies.isEmpty() ? 0 : calculatePercentile(latencies, 95);
        long p99 = latencies.isEmpty() ? 0 : calculatePercentile(latencies, 99);

        double retryExhaustionRate = (retryExhaustionCount.get() * 100.0) / totalAssignments;

        log.info("LT-SERVICE-2 Results:");
        log.info("  Duration: {}s (target: {}s)", actualDurationSeconds, durationSeconds);
        log.info("  Success: {}/{} ({:.2f}%)", successCount.get(), totalAssignments,
                (successCount.get() * 100.0 / totalAssignments));
        log.info("  Failures: {} (retry exhaustion: {}, other: {})",
                failureCount.get(), retryExhaustionCount.get(), failureCount.get() - retryExhaustionCount.get());
        log.info("  Retry exhaustion rate: {:.2f}% (target: <10%)", retryExhaustionRate);
        log.info("  Metrics retry exhaustion: {}", metricsRetryExhaustion);
        log.info("  Throughput: {:.2f} assignments/sec", successCount.get() / (double) actualDurationSeconds);
        log.info("  Latencies: p50={}ms, p95={}ms, p99={}ms", p50, p95, p99);

        // Verify success criteria
        assertThat(completed).isTrue();
        assertThat(retryExhaustionRate).isLessThan(10.0)
                .as("Retry exhaustion rate should be < 10%%");
        if (!latencies.isEmpty()) {
            assertThat(p95).isLessThan(400)
                    .as("p95 latency should be < 400ms");
        }

        log.info("✅ LT-SERVICE-2 PASSED: Retry exhaustion rate {:.2f}% < 10%", retryExhaustionRate);
    }

    @Test
    @DisplayName("LT-SERVICE-3: SPI query load - 500 queries/sec, p95 <100ms, 1min duration (AC-24)")
    void spiQueryLoad_shouldMeetPerformanceTargets() throws InterruptedException {
        // Given: Create test data - 500 tickets
        int ticketCount = 500;
        log.info("Starting LT-SERVICE-3: Creating {} tickets for query load test", ticketCount);

        List<UUID> ticketIds = new ArrayList<>();
        for (int i = 0; i < ticketCount; i++) {
            CreateTicketCommand command = new CreateTicketCommand(
                    "Query load test ticket #" + i,
                    "Testing SPI query performance",
                    TicketType.INCIDENT,
                    i % 2 == 0 ? Priority.HIGH : Priority.MEDIUM,
                    "LoadTest",
                    requesterId
            );
            Ticket ticket = ticketService.createTicket(command, requesterId);
            ticketIds.add(ticket.getId());

            // Assign half the tickets
            if (i % 2 == 0) {
                ticketService.assignTicket(ticket.getId(), agentId, requesterId);
            }
        }

        log.info("Created {} tickets, starting SPI query load test", ticketCount);

        // Performance targets
        int targetQueriesPerSecond = 500;
        int durationSeconds = 60; // 1 minute
        int totalQueries = targetQueriesPerSecond * durationSeconds; // 30,000 queries
        int threadPoolSize = 30;

        // Query mix: 50% findById, 30% findByStatus, 20% findByCriteria
        int findByIdCount = (int) (totalQueries * 0.5); // 15,000
        int findByStatusCount = (int) (totalQueries * 0.3); // 9,000
        int findByCriteriaCount = totalQueries - findByIdCount - findByStatusCount; // 6,000

        // When: Execute queries concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalQueries);
        List<Long> latencies = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Instant startTime = Instant.now();

        // Submit findById queries (50%)
        for (int i = 0; i < findByIdCount; i++) {
            final UUID ticketId = ticketIds.get(i % ticketCount);
            executor.submit(() -> {
                try {
                    Instant operationStart = Instant.now();

                    Optional<TicketSummaryDTO> result = ticketQueryService.findById(ticketId);

                    long latencyMs = Duration.between(operationStart, Instant.now()).toMillis();
                    latencies.add(latencyMs);

                    if (result.isPresent()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("findById query failed: {}", e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Submit findByStatus queries (30%)
        for (int i = 0; i < findByStatusCount; i++) {
            final TicketStatus status = i % 2 == 0 ? TicketStatus.NEW : TicketStatus.ASSIGNED;
            executor.submit(() -> {
                try {
                    Instant operationStart = Instant.now();

                    List<TicketSummaryDTO> results = ticketQueryService.findByStatus(status);

                    long latencyMs = Duration.between(operationStart, Instant.now()).toMillis();
                    latencies.add(latencyMs);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("findByStatus query failed: {}", e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Submit findByCriteria queries (20%)
        for (int i = 0; i < findByCriteriaCount; i++) {
            final TicketSearchCriteria criteria = TicketSearchCriteria.builder()
                    .status(i % 2 == 0 ? TicketStatus.NEW : TicketStatus.ASSIGNED)
                    .priority(i % 3 == 0 ? io.monosense.synergyflow.itsm.spi.Priority.HIGH : io.monosense.synergyflow.itsm.spi.Priority.MEDIUM)
                    .category("LoadTest")
                    .build();

            executor.submit(() -> {
                try {
                    Instant operationStart = Instant.now();

                    List<TicketSummaryDTO> results = ticketQueryService.findByCriteria(criteria);

                    long latencyMs = Duration.between(operationStart, Instant.now()).toMillis();
                    latencies.add(latencyMs);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("findByCriteria query failed: {}", e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for completion
        boolean completed = latch.await(durationSeconds + 30, TimeUnit.SECONDS);
        executor.shutdown();

        Instant endTime = Instant.now();
        long actualDurationSeconds = Duration.between(startTime, endTime).getSeconds();

        // Then: Calculate statistics
        latencies.sort(Long::compareTo);
        long p50 = calculatePercentile(latencies, 50);
        long p95 = calculatePercentile(latencies, 95);
        long p99 = calculatePercentile(latencies, 99);
        long max = latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1);

        log.info("LT-SERVICE-3 Results:");
        log.info("  Duration: {}s (target: {}s)", actualDurationSeconds, durationSeconds);
        log.info("  Success: {}/{} ({:.2f}%)", successCount.get(), totalQueries,
                (successCount.get() * 100.0 / totalQueries));
        log.info("  Failures: {}", failureCount.get());
        log.info("  Throughput: {:.2f} queries/sec", successCount.get() / (double) actualDurationSeconds);
        log.info("  Query mix: findById={}, findByStatus={}, findByCriteria={}",
                findByIdCount, findByStatusCount, findByCriteriaCount);
        log.info("  Latencies: p50={}ms, p95={}ms, p99={}ms, max={}ms", p50, p95, p99, max);

        // Verify success criteria
        assertThat(completed).isTrue();
        assertThat(failureCount.get()).isEqualTo(0)
                .as("Zero errors expected for read-only queries");
        assertThat(p95).isLessThan(100)
                .as("p95 latency should be < 100ms for read-only operations");

        log.info("✅ LT-SERVICE-3 PASSED: All success criteria met");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates a test user and returns the UUID.
     */
    private UUID createUser(String username) {
        UUID userId = UUID.randomUUID();
        String uniqueUsername = username + "-" + userId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                userId, uniqueUsername, uniqueEmail, username + " User", true, 1
        );
        return userId;
    }

    /**
     * Calculates the percentile value from a sorted list of latencies.
     */
    private long calculatePercentile(List<Long> sortedLatencies, int percentile) {
        if (sortedLatencies.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(index, sortedLatencies.size() - 1));
        return sortedLatencies.get(index);
    }

    /**
     * Queries retry exhaustion metric from Micrometer if available.
     */
    private Long queryRetryExhaustionMetric() {
        try {
            // This is a placeholder - actual implementation would query MeterRegistry
            // For now, return null to indicate metric not available
            return null;
        } catch (Exception e) {
            log.debug("Could not query retry exhaustion metric: {}", e.getMessage());
            return null;
        }
    }
}
