package io.monosense.synergyflow.eventing.internal.worker;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Periodically checks the outbox stream for gaps and raises operational telemetry.
 *
 * @since 1.6
 */
@Slf4j
@Service
@Profile("worker")
@RequiredArgsConstructor
public class GapDetector {

    private final WorkerOutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 60000)
    public void detectGaps() {
        Long maxProcessedId = outboxRepository.findMaxProcessedId();
        if (maxProcessedId == null) {
            return;
        }

        List<Long> gapIds = outboxRepository.findGapIdsUpTo(maxProcessedId);
        if (gapIds.isEmpty()) {
            return;
        }

        long gapStart = Collections.min(gapIds);
        long gapEnd = Collections.max(gapIds);
        int gapSize = gapIds.size();

        log.error("outbox_gap_detected gap_start_id={} gap_end_id={} gap_size={}", gapStart, gapEnd, gapSize);
        Counter.builder("outbox_gaps_detected_total")
                .register(meterRegistry)
                .increment();
    }
}
