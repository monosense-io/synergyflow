package io.monosense.synergyflow.time.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Metrics collector for Time Entry functionality.
 *
 * <p>Tracks key performance indicators for time entry creation,
 * mirroring latency, and error rates to meet SLO requirements.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@Component
public class TimeEntryMetrics {

    private final Counter timeEntryCreatedCounter;
    private final Counter timeEntryMirroredCounter;
    private final Counter timeEntryFailedCounter;
    private final Timer mirroringLatencyTimer;
    private final Timer timeEntryCreationTimer;

    public TimeEntryMetrics(MeterRegistry meterRegistry) {
        this.timeEntryCreatedCounter = Counter.builder("time_entries_created_total")
                .description("Total number of time entries created")
                .register(meterRegistry);

        this.timeEntryMirroredCounter = Counter.builder("time_entries_mirrored_total")
                .description("Total number of time entries successfully mirrored")
                .register(meterRegistry);

        this.timeEntryFailedCounter = Counter.builder("time_entries_failed_total")
                .description("Total number of time entries that failed to mirror")
                .register(meterRegistry);

        this.mirroringLatencyTimer = Timer.builder("time_entry_mirroring_latency_seconds")
                .description("Time taken for time entry mirroring to complete")
                .register(meterRegistry);

        this.timeEntryCreationTimer = Timer.builder("time_entry_creation_duration_seconds")
                .description("Time taken to create a time entry")
                .register(meterRegistry);
    }

    /**
     * Records the creation of a time entry.
     */
    public void recordTimeEntryCreated() {
        timeEntryCreatedCounter.increment();
    }

    /**
     * Records the successful mirroring of a time entry.
     *
     * @param latency the time taken to complete mirroring
     */
    public void recordTimeEntryMirrored(Duration latency) {
        timeEntryMirroredCounter.increment();
        mirroringLatencyTimer.record(latency);
    }

    /**
     * Records a failed time entry mirroring.
     */
    public void recordTimeEntryFailed() {
        timeEntryFailedCounter.increment();
    }

    /**
     * Records the duration of time entry creation.
     *
     * @param duration the creation duration
     */
    public void recordCreationDuration(Duration duration) {
        timeEntryCreationTimer.record(duration);
    }

    /**
     * Records a time entry creation with timing.
     * Convenience method that combines creation and timing.
     */
    public void recordTimeEntryCreation(Runnable creationTask) {
        Timer.Sample sample = Timer.start();
        try {
            creationTask.run();
            recordTimeEntryCreated();
        } finally {
            sample.stop(timeEntryCreationTimer);
        }
    }

    /**
     * Records mirroring with timing.
     * Convenience method that combines mirroring and timing.
     */
    public void recordMirroring(Runnable mirroringTask) {
        Timer.Sample sample = Timer.start();
        try {
            mirroringTask.run();
            Duration latency = Duration.ofNanos((long) (sample.stop(mirroringLatencyTimer) * 1_000_000_000));
            recordTimeEntryMirrored(latency);
        } catch (Exception e) {
            recordTimeEntryFailed();
            throw e;
        }
    }
}