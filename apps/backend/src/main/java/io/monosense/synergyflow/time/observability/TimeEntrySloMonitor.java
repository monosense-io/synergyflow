package io.monosense.synergyflow.time.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Service Level Objective (SLO) monitor for Time Entry functionality.
 *
 * <p>Monitors compliance with defined SLOs:
 * - p95 mirroring latency ≤200ms for two destinations under nominal load
 * - Error rate < 1% for time entry operations
 * - Availability > 99.9% for time entry API
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@Component
public class TimeEntrySloMonitor {

    private static final Logger log = LoggerFactory.getLogger(TimeEntrySloMonitor.class);

    // SLO Constants
    private static final Duration P95_LATENCY_THRESHOLD = Duration.ofMillis(200);
    private static final double ERROR_RATE_THRESHOLD = 0.01; // 1%
    private static final double AVAILABILITY_THRESHOLD = 0.999; // 99.9%

    private final MeterRegistry meterRegistry;
    private final TimeEntryMetrics metrics;

    public TimeEntrySloMonitor(MeterRegistry meterRegistry, TimeEntryMetrics metrics) {
        this.meterRegistry = meterRegistry;
        this.metrics = metrics;
    }

    /**
     * Monitors SLO compliance every minute.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorSloCompliance() {
        checkLatencySlo();
        checkErrorRateSlo();
        logSloStatus();
    }

    /**
     * Checks if the p95 latency meets the SLO requirement.
     */
    private void checkLatencySlo() {
        try {
            // Get p95 latency from the timer
            var snapshot = meterRegistry.get("time_entry_mirroring_latency_seconds")
                    .timer()
                    .takeSnapshot();

            double p95Latency = Double.MAX_VALUE;
            for (var percentile : snapshot.percentileValues()) {
                if (percentile.percentile() == 95.0) {
                    p95Latency = (double) Duration.ofNanos((long) (percentile.value() * 1_000_000_000)).toMillis();
                    break;
                }
            }

            boolean meetsLatencySlo = p95Latency <= P95_LATENCY_THRESHOLD.toMillis();

            // Record SLO compliance
            meterRegistry.gauge("time_entry_slo_latency_compliance", meetsLatencySlo ? 1 : 0);

            if (!meetsLatencySlo) {
                log.warn("SLO Violation: p95 mirroring latency {}ms exceeds threshold {}ms",
                        p95Latency, P95_LATENCY_THRESHOLD.toMillis());
            }

        } catch (Exception e) {
            log.error("Failed to check latency SLO", e);
            meterRegistry.gauge("time_entry_slo_latency_compliance", 0);
        }
    }

    /**
     * Checks if the error rate meets the SLO requirement.
     */
    private void checkErrorRateSlo() {
        try {
            double totalCreated = meterRegistry.get("time_entries_created_total")
                    .counter()
                    .count();

            double totalFailed = meterRegistry.get("time_entries_failed_total")
                    .counter()
                    .count();

            double errorRate = totalCreated > 0 ? totalFailed / totalCreated : 0.0;
            boolean meetsErrorRateSlo = errorRate <= ERROR_RATE_THRESHOLD;

            // Record SLO compliance
            meterRegistry.gauge("time_entry_slo_error_rate_compliance", meetsErrorRateSlo ? 1 : 0);
            meterRegistry.gauge("time_entry_error_rate", errorRate);

            if (!meetsErrorRateSlo) {
                log.warn("SLO Violation: error rate {:.2%} exceeds threshold {:.2%}",
                        errorRate, ERROR_RATE_THRESHOLD);
            }

        } catch (Exception e) {
            log.error("Failed to check error rate SLO", e);
            meterRegistry.gauge("time_entry_slo_error_rate_compliance", 0);
        }
    }

    /**
     * Logs current SLO status for monitoring and alerting.
     */
    private void logSloStatus() {
        try {
            Double latencyCompliance = meterRegistry.get("time_entry_slo_latency_compliance")
                    .gauge()
                    .value();

            Double errorRateCompliance = meterRegistry.get("time_entry_slo_error_rate_compliance")
                    .gauge()
                    .value();

            Double errorRate = meterRegistry.get("time_entry_error_rate")
                    .gauge()
                    .value();

            boolean overallCompliance = latencyCompliance == 1.0 && errorRateCompliance == 1.0;

            if (overallCompliance) {
                log.info("✅ Time Entry SLOs: All objectives met (Latency: OK, Error Rate: {:.2%})", errorRate);
            } else {
                log.warn("⚠️ Time Entry SLOs: Objectives not met (Latency: {}, Error Rate: {:.2%})",
                        latencyCompliance == 1.0 ? "OK" : "VIOLATION", errorRate);
            }

            // Record overall compliance
            meterRegistry.gauge("time_entry_slo_overall_compliance", overallCompliance ? 1 : 0);

        } catch (Exception e) {
            log.error("Failed to log SLO status", e);
            meterRegistry.gauge("time_entry_slo_overall_compliance", 0);
        }
    }

    /**
     * Records a freshness badge status change for monitoring.
     *
     * @param entityId the entity ID
     * @param entityType the entity type
     * @param status the freshness status
     */
    public void recordFreshnessBadgeStatus(String entityId, String entityType, String status) {
        meterRegistry.counter("time_entry_freshness_badge_status_changes",
                "entity_type", entityType,
                "status", status)
                .increment();
    }

    /**
     * Records mirroring performance by entity type.
     *
     * @param entityType the target entity type
     * @param duration the mirroring duration
     * @param success whether mirroring succeeded
     */
    public void recordMirroringPerformance(String entityType, Duration duration, boolean success) {
        meterRegistry.timer("time_entry_mirroring_by_entity_duration_seconds",
                "entity_type", entityType,
                "success", String.valueOf(success))
                .record(duration);

        if (!success) {
            meterRegistry.counter("time_entry_mirroring_failures",
                    "entity_type", entityType)
                    .increment();
        }
    }
}