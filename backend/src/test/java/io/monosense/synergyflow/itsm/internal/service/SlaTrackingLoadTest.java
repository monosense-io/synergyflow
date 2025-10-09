package io.monosense.synergyflow.itsm.internal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import io.monosense.synergyflow.itsm.internal.repository.SlaTrackingRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;

/**
 * Load tests for SLA tracking functionality.
 * 
 * <p>Validates that SLA tracking does not degrade ticket operations performance
 * under concurrent load. Tests use realistic PostgreSQL environment via Testcontainers.</p>
 * 
 * <p>Performance targets (from Story 2.3 AC-11):</p>
 * <ul>
 *   <li>LT-SLA-1: 1000 incidents/minute with SLA tracking, p95 < 400ms</li>
 *   <li>LT-SLA-2: 500 priority updates/minute with recalculation, p95 < 400ms</li>
 *   <li>Retry exhaustion rate < 10% for concurrent operations</li>
 * </ul>
 * 
 * @author monosense
 * @since 2025-10-09
 */
@SpringBootTest
@Testcontainers
@Tag("load")
public class SlaTrackingLoadTest {

    private static final Logger log = LoggerFactory.getLogger(SlaTrackingLoadTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("logging.level.io.monosense.synergyflow.itsm", () -> "WARN");
    }

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SlaTrackingRepository slaTrackingRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test to ensure isolation
        slaTrackingRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    /**
     * LT-SLA-1: Concurrent incident creation with SLA tracking.
     * 
     * <p>Performance target: 1000 incidents/minute with SLA tracking, p95 < 400ms</p>
     * <p>Test duration: 2 minutes (2000 incidents total)</p>
     * <p>Success criteria: p95 < 400ms, p99 < 800ms, zero failures, all SLA records created</p>
     */
    @Test
    @DisplayName("LT-SLA-1: 1000 concurrent incidents with SLA tracking")
    void testConcurrentIncidentCreationWithSlaTracking() throws Exception {
        // Test configuration
        final int TOTAL_INCIDENTS = 2000; // 1000 incidents/minute for 2 minutes
        final int THREAD_COUNT = 20; // Concurrent threads
        final int INCIDENTS_PER_THREAD = TOTAL_INCIDENTS / THREAD_COUNT;
        final Duration TEST_DURATION = Duration.ofMinutes(2);
        final Duration TARGET_P95 = Duration.ofMillis(400);
        final Duration TARGET_P99 = Duration.ofMillis(800);

        log.info("Starting LT-SLA-1: {} incidents across {} threads", TOTAL_INCIDENTS, THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Long> latencies = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Instant testStart = Instant.now();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < INCIDENTS_PER_THREAD; j++) {
                    try {
                        Instant operationStart = Instant.now();
                        
                        // Create incident with CRITICAL priority (max SLA tracking overhead)
                        UUID currentUserId = UUID.randomUUID();
                        var ticket = ticketService.createTicket(new CreateTicketCommand(
                            "Load Test Incident " + threadIndex + "-" + j,
                            "Load test description for incident creation performance",
                            TicketType.INCIDENT,
                            Priority.CRITICAL,
                            "Load Test",
                            currentUserId
                        ), currentUserId);

                        Instant operationEnd = Instant.now();
                        long latencyMs = Duration.between(operationStart, operationEnd).toMillis();
                        
                        synchronized (latencies) {
                            latencies.add(latencyMs);
                        }
                        
                        successCount.incrementAndGet();
                        
                        // Verify SLA record was created
                        assertNotNull(ticket, "Ticket should not be null");
                        assertNotNull(ticket.getId(), "Ticket ID should not be null");
                        
                    } catch (Exception e) {
                        log.error("Failed to create incident in thread {} iteration {}: {}", 
                                threadIndex, j, e.getMessage());
                        failureCount.incrementAndGet();
                    }
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(TEST_DURATION.toSeconds() + 30, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        Instant testEnd = Instant.now();
        Duration actualDuration = Duration.between(testStart, testEnd);

        // Calculate performance metrics
        latencies.sort(Long::compare);
        double p95Ms = latencies.get((int) (latencies.size() * 0.95));
        double p99Ms = latencies.get((int) (latencies.size() * 0.99));
        double avgMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double incidentsPerMinute = (double) successCount.get() / actualDuration.toMinutes();

        // Verify all SLA records were created
        long slaRecordCount = slaTrackingRepository.count();
        long ticketRecordCount = ticketRepository.count();

        log.info("LT-SLA-1 Results:");
        log.info("  Total incidents created: {}/{}", successCount.get(), TOTAL_INCIDENTS);
        log.info("  Failed incidents: {}", failureCount.get());
        log.info("  Average latency: {:.2f}ms", avgMs);
        log.info("  P95 latency: {:.2f}ms (target: <{}ms)", p95Ms, TARGET_P95.toMillis());
        log.info("  P99 latency: {:.2f}ms (target: <{}ms)", p99Ms, TARGET_P99.toMillis());
        log.info("  Throughput: {:.2f} incidents/minute", incidentsPerMinute);
        log.info("  Test duration: {} (target: {})", actualDuration, TEST_DURATION);
        log.info("  Tickets in database: {}", ticketRecordCount);
        log.info("  SLA records in database: {}", slaRecordCount);

        // Assertions
        assertEquals(0, failureCount.get(), "No incident creation failures should occur");
        assertEquals(TOTAL_INCIDENTS, successCount.get(), "All incidents should be created successfully");
        assertEquals(TOTAL_INCIDENTS, ticketRecordCount, "All tickets should persist in database");
        assertEquals(TOTAL_INCIDENTS, slaRecordCount, "All incidents should have SLA tracking records");
        assertTrue(p95Ms < TARGET_P95.toMillis(), 
                String.format("P95 latency %.2fms exceeds target %dms", p95Ms, TARGET_P95.toMillis()));
        assertTrue(p99Ms < TARGET_P99.toMillis(), 
                String.format("P99 latency %.2fms exceeds target %dms", p99Ms, TARGET_P99.toMillis()));
        assertTrue(incidentsPerMinute >= 1000, 
                String.format("Throughput %.2f incidents/minute below target 1000", incidentsPerMinute));
    }

    /**
     * LT-SLA-2: Concurrent priority updates with SLA recalculation.
     * 
     * <p>Performance target: 500 priority updates/minute with SLA recalculation, p95 < 400ms</p>
     * <p>Test duration: 1 minute (500 updates across 100 tickets)</p>
     * <p>Success criteria: p95 < 400ms, retry exhaustion rate < 10%, all SLA records updated correctly</p>
     */
    @Test
    @DisplayName("LT-SLA-2: 500 concurrent priority updates with SLA recalculation")
    void testConcurrentPriorityUpdatesWithSlaRecalculation() throws Exception {
        // Test configuration
        final int TOTAL_TICKETS = 100; // Base tickets to create
        final int TOTAL_UPDATES = 500; // 500 priority updates in 1 minute
        final int THREAD_COUNT = 10; // Concurrent threads
        final int UPDATES_PER_THREAD = TOTAL_UPDATES / THREAD_COUNT;
        final Duration TEST_DURATION = Duration.ofMinutes(1);
        final Duration TARGET_P95 = Duration.ofMillis(400);
        final double MAX_RETRY_EXHAUSTION_RATE = 0.10; // 10%

        log.info("Starting LT-SLA-2: {} priority updates across {} threads", TOTAL_UPDATES, THREAD_COUNT);

        // Create base tickets first
        List<UUID> ticketIds = new ArrayList<>();
        for (int i = 0; i < TOTAL_TICKETS; i++) {
            UUID currentUserId = UUID.randomUUID();
            var ticket = ticketService.createTicket(new CreateTicketCommand(
                "Load Test Base Ticket " + i,
                "Base ticket for priority update load test",
                TicketType.INCIDENT,
                Priority.LOW, // Start with LOW priority
                "Load Test",
                currentUserId
            ), currentUserId);
            ticketIds.add(ticket.getId());
        }

        // Verify all SLA records created
        assertEquals(TOTAL_TICKETS, slaTrackingRepository.count(), 
                "All base tickets should have SLA records");

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Long> latencies = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger retryExhaustionCount = new AtomicInteger(0);

        Instant testStart = Instant.now();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < UPDATES_PER_THREAD; j++) {
                    try {
                        Instant operationStart = Instant.now();
                        
                        // Select random ticket and update priority to CRITICAL (max SLA recalculation overhead)
                        UUID ticketId = ticketIds.get((threadIndex * UPDATES_PER_THREAD + j) % TOTAL_TICKETS);
                        
                        ticketService.updatePriority(ticketId, Priority.CRITICAL, UUID.randomUUID());

                        Instant operationEnd = Instant.now();
                        long latencyMs = Duration.between(operationStart, operationEnd).toMillis();
                        
                        synchronized (latencies) {
                            latencies.add(latencyMs);
                        }
                        
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        log.error("Failed to update priority in thread {} iteration {}: {}", 
                                threadIndex, j, e.getMessage());
                        failureCount.incrementAndGet();
                        
                        // Check if this is a retry exhaustion (could indicate concurrent update conflicts)
                        if (e.getMessage() != null && 
                            (e.getMessage().contains("retry") || 
                             e.getMessage().contains("optimistic") ||
                             e.getMessage().contains("lock"))) {
                            retryExhaustionCount.incrementAndGet();
                        }
                    }
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(TEST_DURATION.toSeconds() + 30, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        Instant testEnd = Instant.now();
        Duration actualDuration = Duration.between(testStart, testEnd);

        // Calculate performance metrics
        latencies.sort(Long::compare);
        double p95Ms = latencies.get((int) (latencies.size() * 0.95));
        double p99Ms = latencies.get((int) (latencies.size() * 0.99));
        double avgMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double updatesPerMinute = (double) successCount.get() / actualDuration.toMinutes();
        double retryExhaustionRate = (double) retryExhaustionCount.get() / TOTAL_UPDATES;

        // Verify SLA records were updated correctly
        long slaRecordCount = slaTrackingRepository.count();
        long criticalSlaCount = slaTrackingRepository.findAll().stream()
                .filter(sla -> sla.getPriority() == Priority.CRITICAL)
                .count();

        log.info("LT-SLA-2 Results:");
        log.info("  Total priority updates: {}/{}", successCount.get(), TOTAL_UPDATES);
        log.info("  Failed updates: {}", failureCount.get());
        log.info("  Retry exhaustion count: {}", retryExhaustionCount.get());
        log.info("  Retry exhaustion rate: {:.2f}% (target: <{:.2f}%)", 
                retryExhaustionRate * 100, MAX_RETRY_EXHAUSTION_RATE * 100);
        log.info("  Average latency: {:.2f}ms", avgMs);
        log.info("  P95 latency: {:.2f}ms (target: <{}ms)", p95Ms, TARGET_P95.toMillis());
        log.info("  P99 latency: {:.2f}ms", p99Ms);
        log.info("  Throughput: {:.2f} updates/minute", updatesPerMinute);
        log.info("  Test duration: {} (target: {})", actualDuration, TEST_DURATION);
        log.info("  SLA records in database: {}", slaRecordCount);
        log.info("  CRITICAL priority SLA records: {}", criticalSlaCount);

        // Assertions
        assertTrue(failureCount.get() <= TOTAL_UPDATES * 0.05, 
                "Failure rate should be <= 5%: " + failureCount.get());
        assertTrue(p95Ms < TARGET_P95.toMillis(), 
                String.format("P95 latency %.2fms exceeds target %dms", p95Ms, TARGET_P95.toMillis()));
        assertTrue(retryExhaustionRate < MAX_RETRY_EXHAUSTION_RATE, 
                String.format("Retry exhaustion rate %.2f%% exceeds target %.2f%%", 
                        retryExhaustionRate * 100, MAX_RETRY_EXHAUSTION_RATE * 100));
        assertTrue(updatesPerMinute >= 500, 
                String.format("Throughput %.2f updates/minute below target 500", updatesPerMinute));
        assertEquals(TOTAL_TICKETS, slaRecordCount, "SLA record count should remain unchanged");
        assertTrue(criticalSlaCount > 0, "Some SLA records should have CRITICAL priority");
    }

    /**
     * Performance baseline test without SLA tracking for comparison.
     * 
     * <p>This test helps establish the performance impact of SLA tracking
     * by measuring baseline ticket creation performance.</p>
     */
    @Test
    @DisplayName("Baseline: Concurrent incident creation without SLA tracking")
    void testBaselineIncidentCreationWithoutSlaTracking() throws Exception {
        // Test configuration - smaller scale for baseline
        final int TOTAL_INCIDENTS = 500;
        final int THREAD_COUNT = 10;
        final int INCIDENTS_PER_THREAD = TOTAL_INCIDENTS / THREAD_COUNT;

        log.info("Starting baseline test: {} incidents across {} threads", TOTAL_INCIDENTS, THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Long> latencies = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < INCIDENTS_PER_THREAD; j++) {
                    try {
                        Instant operationStart = Instant.now();
                        
                        // Create SERVICE REQUEST (no SLA tracking) for baseline
                        UUID currentUserId = UUID.randomUUID();
                        var ticket = ticketService.createTicket(new CreateTicketCommand(
                            "Baseline Test " + threadIndex + "-" + j,
                            "Baseline test without SLA tracking",
                            TicketType.SERVICE_REQUEST, // No SLA tracking
                            Priority.MEDIUM,
                            "Baseline Test",
                            currentUserId
                        ), currentUserId);

                        Instant operationEnd = Instant.now();
                        long latencyMs = Duration.between(operationStart, operationEnd).toMillis();
                        
                        synchronized (latencies) {
                            latencies.add(latencyMs);
                        }
                        
                        successCount.incrementAndGet();
                        assertNotNull(ticket, "Ticket should not be null");
                        assertNotNull(ticket.getId(), "Ticket ID should not be null");
                        
                    } catch (Exception e) {
                        log.error("Failed to create baseline ticket in thread {} iteration {}: {}", 
                                threadIndex, j, e.getMessage());
                    }
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Calculate baseline metrics
        latencies.sort(Long::compare);
        double p95Ms = latencies.get((int) (latencies.size() * 0.95));
        double avgMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);

        log.info("Baseline Results (without SLA tracking):");
        log.info("  Total tickets created: {}/{}", successCount.get(), TOTAL_INCIDENTS);
        log.info("  Average latency: {:.2f}ms", avgMs);
        log.info("  P95 latency: {:.2f}ms", p95Ms);
        log.info("  SLA records in database: {}", slaTrackingRepository.count());

        // Verify no SLA records were created for service requests
        assertEquals(0, slaTrackingRepository.count(), 
                "No SLA records should be created for service requests");
    }
}
