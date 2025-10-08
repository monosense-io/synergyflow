package io.monosense.synergyflow.eventing.internal.worker;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled worker that drains the transactional outbox and orchestrates event publication.
 *
 * @since 1.6
 */
@Slf4j
@Service
@Profile("worker")
public class OutboxPoller {
    private final WorkerOutboxRepository outboxRepository;
    private final RedisStreamPublisher redisStreamPublisher;
    private final ReadModelUpdater readModelUpdater;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    private final AtomicLong lagGauge;
    private final Timer processingTimer;

    @Value("${synergyflow.outbox.polling.batch-size:100}")
    private int batchSize;

    public OutboxPoller(WorkerOutboxRepository outboxRepository,
                        RedisStreamPublisher redisStreamPublisher,
                        ReadModelUpdater readModelUpdater,
                        MeterRegistry meterRegistry,
                        PlatformTransactionManager transactionManager) {
        this.outboxRepository = outboxRepository;
        this.redisStreamPublisher = redisStreamPublisher;
        this.readModelUpdater = readModelUpdater;
        this.meterRegistry = meterRegistry;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.lagGauge = meterRegistry.gauge("outbox_lag_rows", new AtomicLong(0));
        this.processingTimer = Timer.builder("outbox_processing_time_ms")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${synergyflow.outbox.polling.fixed-delay-ms:100}")
    public void poll() {
        Timer.Sample sample = Timer.start(meterRegistry);
        int processed = 0;
        int failures = 0;

        for (int i = 0; i < batchSize; i++) {
            EventProcessingResult result = transactionTemplate.execute(processSingleEvent());
            if (result == null || result == EventProcessingResult.EMPTY) {
                break;
            }
            if (result == EventProcessingResult.PROCESSED) {
                processed++;
            } else if (result == EventProcessingResult.FAILED) {
                failures++;
                break; // stop early on failure to avoid tight retry loop
            }
        }

        long lag = outboxRepository.countUnprocessed();
        lagGauge.set(lag);
        long nanos = sample.stop(processingTimer);
        long processingTimeMs = nanos / 1_000_000L;

        log.info("outbox_batch_complete batch_size={} processed={} failures={} lag_rows={} processing_time_ms={}",
                batchSize, processed, failures, lag, processingTimeMs);
    }

    private TransactionCallback<EventProcessingResult> processSingleEvent() {
        return status -> {
            List<OutboxMessage> events = outboxRepository.findNextBatch(1);
            if (events.isEmpty()) {
                return EventProcessingResult.EMPTY;
            }
            OutboxMessage event = events.get(0);
            try {
                redisStreamPublisher.publish(event);
            } catch (RedisPublishBackoffException backoff) {
                log.debug("Redis publish backoff active for event_id={} next_attempt_at={}", backoff.eventId(), backoff.nextAttemptAt());
                status.setRollbackOnly();
                return EventProcessingResult.FAILED;
            } catch (Exception ex) {
                log.error("redis_publish_failed event_id={} event_type={} error={}", event.id(), event.eventType(), ex.getMessage(), ex);
                status.setRollbackOnly();
                return EventProcessingResult.FAILED;
            }

            readModelUpdater.update(event);
            int updated = outboxRepository.markProcessed(event.id(), Instant.now());
            if (updated == 0) {
                log.warn("markProcessed affected 0 rows for event id={}", event.id());
            }
            return EventProcessingResult.PROCESSED;
        };
    }

    private enum EventProcessingResult {
        PROCESSED,
        EMPTY,
        FAILED
    }
}
