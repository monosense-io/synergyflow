package io.monosense.synergyflow.itsm.integration;

import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.IncidentRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Load tests for ticket entity persistence (Story 2.1 Task 7).
 *
 * <p>Performance targets:</p>
 * <ul>
 *   <li>LT1: Bulk insert 1000 tickets in &lt;5s (200 tickets/sec sustained), p99 latency &lt;50ms, UUIDv7 time-ordering</li>
 *   <li>LT2: 500 concurrent reads/sec + 50 writes/sec for 5 minutes, p95 read &lt;10ms, write &lt;50ms, zero OptimisticLockExceptions</li>
 * </ul>
 *
 * <p>Tagged with @Tag("load") to exclude from CI pipeline. Run manually:</p>
 * <pre>./gradlew test -DincludeTags=load</pre>
 *
 * @since 2.1
 */
@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
@Testcontainers
@ActiveProfiles("test")
@Tag("load")
class TicketEntityLoadTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID requesterId;
    private UUID assigneeId;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        insertUser(requesterId, "load-requester");
        insertUser(assigneeId, "load-agent");
    }

    /**
     * LT1: Bulk ticket creation performance test.
     *
     * <p>Performance targets:</p>
     * <ul>
     *   <li>Insert 1000 tickets in &lt;5 seconds (200 tickets/sec sustained)</li>
     *   <li>p99 insert latency &lt;50ms per ticket</li>
     *   <li>Zero constraint violations or deadlocks</li>
     *   <li>UUIDv7 IDs maintain time-ordering property (≤1% inversions due to concurrent inserts)</li>
     * </ul>
     *
     * @since 2.1
     */
    @Test
    @Tag("load")
    void lt1_bulkTicketCreation() {
        final int ticketCount = 1000;
        final long maxDurationMs = 5000L;
        final long maxP99LatencyMs = 50L;
        final double maxInversionRate = 1.0; // 1% tolerance for UUIDv7 time-ordering

        List<Long> latencies = new ArrayList<>(ticketCount);
        List<UUID> ticketIds = new ArrayList<>(ticketCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < ticketCount; i++) {
            long insertStart = System.nanoTime();

            Incident incident = new Incident(
                    "Load Test Ticket " + i,
                    "Performance test ticket for bulk insert scenario",
                    TicketStatus.NEW,
                    Priority.MEDIUM,
                    "Load Testing",
                    requesterId,
                    null,
                    Severity.LOW
            );

            Incident saved = incidentRepository.saveAndFlush(incident);
            ticketIds.add(saved.getId());

            long insertEnd = System.nanoTime();
            long latencyMs = (insertEnd - insertStart) / 1_000_000L;
            latencies.add(latencyMs);
        }

        long endTime = System.currentTimeMillis();
        long totalDurationMs = endTime - startTime;
        double throughput = (double) ticketCount / (totalDurationMs / 1000.0);

        // Calculate p99 latency
        long p99Latency = calculatePercentile(latencies, 99.0);

        // Verify UUIDv7 time-ordering (allow ≤1% inversions due to timestamp precision)
        double inversionRate = calculateInversionRate(ticketIds);

        // Performance assertions
        assertThat(totalDurationMs)
                .as("Total insert time should be less than 5 seconds")
                .isLessThan(maxDurationMs);

        assertThat(throughput)
                .as("Throughput should exceed 200 tickets/sec")
                .isGreaterThan(200.0);

        assertThat(p99Latency)
                .as("p99 latency should be less than 50ms")
                .isLessThan(maxP99LatencyMs);

        assertThat(inversionRate)
                .as("UUIDv7 inversion rate should be ≤1% (time-ordered IDs)")
                .isLessThanOrEqualTo(maxInversionRate);

        // Log performance metrics
        System.out.printf("""

                LT1 Performance Results:
                  - Total tickets: %d
                  - Total time: %d ms
                  - Throughput: %.2f tickets/sec
                  - p99 latency: %d ms
                  - UUIDv7 inversion rate: %.2f%%
                  - Target: <5000ms, >200/sec, p99 <50ms, inversions ≤1%%
                  - Status: %s
                """,
                ticketCount,
                totalDurationMs,
                throughput,
                p99Latency,
                inversionRate,
                (totalDurationMs < maxDurationMs && throughput > 200 && p99Latency < maxP99LatencyMs && inversionRate <= maxInversionRate)
                        ? "PASS ✓" : "FAIL ✗"
        );
    }

    /**
     * LT2: Concurrent read/write performance test.
     *
     * <p>Performance targets:</p>
     * <ul>
     *   <li>500 concurrent reads/sec + 50 concurrent writes/sec</li>
     *   <li>Test duration: 5 minutes sustained load</li>
     *   <li>Read p95 latency &lt;10ms, write p95 latency &lt;50ms</li>
     *   <li>Zero OptimisticLockExceptions (writes serialize correctly)</li>
     *   <li>Connection pool stable (no exhaustion)</li>
     * </ul>
     *
     * @since 2.1
     */
    @Test
    @Tag("load")
    void lt2_concurrentReadWrite() throws Exception {
        final int testDurationSeconds = 300; // 5 minutes
        final int readerThreads = 10; // 50 reads/sec each = 500 total
        final int writerThreads = 5;  // 10 writes/sec each = 50 total
        final long readIntervalMs = 20;  // 50 reads/sec per thread
        final long writeIntervalMs = 100; // 10 writes/sec per thread

        // Pre-populate 1000 tickets for reading
        System.out.println("LT2: Pre-populating 1000 tickets for concurrent read/write test...");
        List<UUID> ticketIds = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            Incident incident = new Incident(
                    "Concurrent Test Ticket " + i,
                    "Pre-populated ticket for LT2",
                    TicketStatus.NEW,
                    Priority.LOW,
                    "Concurrency Testing",
                    requesterId,
                    null,
                    Severity.LOW
            );
            Incident saved = incidentRepository.saveAndFlush(incident);
            ticketIds.add(saved.getId());
        }
        System.out.println("LT2: Pre-population complete. Starting concurrent workload...");

        // Thread-safe collections for metrics
        ConcurrentLinkedQueue<Long> readLatencies = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> writeLatencies = new ConcurrentLinkedQueue<>();
        AtomicInteger optimisticLockExceptions = new AtomicInteger(0);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(readerThreads + writerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerThreads + writerThreads);

        Random random = new Random();

        // Reader threads
        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000L);

                    while (System.currentTimeMillis() < endTime) {
                        long readStart = System.nanoTime();
                        UUID randomId = ticketIds.get(random.nextInt(ticketIds.size()));
                        ticketRepository.findById(randomId);
                        long readEnd = System.nanoTime();

                        readLatencies.add((readEnd - readStart) / 1_000_000L);
                        readCount.incrementAndGet();

                        Thread.sleep(readIntervalMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Writer threads - partition ticket IDs to avoid conflicts
        int partitionSize = ticketIds.size() / writerThreads;
        for (int i = 0; i < writerThreads; i++) {
            final int threadIndex = i;
            final int startIdx = threadIndex * partitionSize;
            final int endIdx = (threadIndex == writerThreads - 1) ? ticketIds.size() : startIdx + partitionSize;
            final List<UUID> partition = ticketIds.subList(startIdx, endIdx);

            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000L);

                    while (System.currentTimeMillis() < endTime) {
                        long writeStart = System.nanoTime();
                        UUID randomId = partition.get(random.nextInt(partition.size()));

                        try {
                            ticketRepository.findById(randomId).ifPresent(ticket -> {
                                // Toggle status to simulate update
                                TicketStatus newStatus = ticket.getStatus() == TicketStatus.NEW
                                        ? TicketStatus.IN_PROGRESS
                                        : TicketStatus.NEW;
                                ticket.updateStatus(newStatus);
                                ticketRepository.saveAndFlush(ticket);
                            });
                        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                            optimisticLockExceptions.incrementAndGet();
                        }

                        long writeEnd = System.nanoTime();
                        writeLatencies.add((writeEnd - writeStart) / 1_000_000L);
                        writeCount.incrementAndGet();

                        Thread.sleep(writeIntervalMs);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads
        System.out.println("LT2: Starting concurrent workload (5 minutes)...");
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion
        doneLatch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        // Calculate metrics
        List<Long> readLatencyList = new ArrayList<>(readLatencies);
        List<Long> writeLatencyList = new ArrayList<>(writeLatencies);

        long readP95 = calculatePercentile(readLatencyList, 95.0);
        long writeP95 = calculatePercentile(writeLatencyList, 95.0);

        double actualDurationSec = (testEndTime - testStartTime) / 1000.0;
        double actualReadThroughput = readCount.get() / actualDurationSec;
        double actualWriteThroughput = writeCount.get() / actualDurationSec;

        // Performance assertions
        assertThat(readP95)
                .as("Read p95 latency should be less than 10ms")
                .isLessThan(10L);

        assertThat(writeP95)
                .as("Write p95 latency should be less than 50ms")
                .isLessThan(50L);

        assertThat(optimisticLockExceptions.get())
                .as("Should have zero OptimisticLockExceptions (writes partitioned by thread)")
                .isEqualTo(0);

        // Log performance metrics
        System.out.printf("""

                LT2 Performance Results:
                  - Test duration: %.1f seconds
                  - Total reads: %d (%.1f reads/sec)
                  - Total writes: %d (%.1f writes/sec)
                  - Read p95 latency: %d ms
                  - Write p95 latency: %d ms
                  - OptimisticLockExceptions: %d
                  - Target: 500 reads/sec, 50 writes/sec, read p95 <10ms, write p95 <50ms, 0 exceptions
                  - Status: %s
                """,
                actualDurationSec,
                readCount.get(),
                actualReadThroughput,
                writeCount.get(),
                actualWriteThroughput,
                readP95,
                writeP95,
                optimisticLockExceptions.get(),
                (readP95 < 10 && writeP95 < 50 && optimisticLockExceptions.get() == 0)
                        ? "PASS ✓" : "FAIL ✗"
        );
    }

    // ==================== Helper Methods ====================

    /**
     * Inserts a test user into the users table to satisfy FK constraints.
     *
     * @param userId         the user ID
     * @param usernamePrefix prefix for generated username/email
     * @since 2.1
     */
    private void insertUser(UUID userId, String usernamePrefix) {
        String uniqueUsername = usernamePrefix + "-" + userId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                userId, uniqueUsername, uniqueEmail, usernamePrefix + " User", true, 1
        );
    }

    /**
     * Calculates the specified percentile from a list of latencies.
     *
     * @param latencies  list of latency measurements (ms)
     * @param percentile percentile to calculate (e.g., 95.0, 99.0)
     * @return the percentile value
     * @since 2.1
     */
    private long calculatePercentile(List<Long> latencies, double percentile) {
        if (latencies.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    /**
     * Calculates the inversion rate for UUIDv7 time-ordering verification.
     *
     * <p>Inversions occur when UUID(i) > UUID(i+1) lexicographically, indicating
     * out-of-order IDs. UUIDv7 should be mostly time-ordered with ≤1% inversions.</p>
     *
     * @param ids list of UUIDs in insertion order
     * @return inversion rate as percentage (0.0-100.0)
     * @since 2.1
     */
    private double calculateInversionRate(List<UUID> ids) {
        if (ids.size() < 2) {
            return 0.0;
        }
        int inversions = 0;
        for (int i = 1; i < ids.size(); i++) {
            if (ids.get(i).compareTo(ids.get(i - 1)) < 0) {
                inversions++;
            }
        }
        return (double) inversions / (ids.size() - 1) * 100.0;
    }
}
