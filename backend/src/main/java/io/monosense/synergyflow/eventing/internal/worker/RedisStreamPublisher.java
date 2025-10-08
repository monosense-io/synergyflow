package io.monosense.synergyflow.eventing.internal.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes outbox envelopes to the Redis stream with resilience and instrumentation.
 *
 * @since 1.6
 */
@Slf4j
@Service
@Profile("worker")
public class RedisStreamPublisher {

    private static final long[] BACKOFF_DELAYS_MS = {1000L, 2000L, 4000L};

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final Map<Long, RetryState> retryStates = new ConcurrentHashMap<>();

    @Value("${synergyflow.outbox.redis.stream-name:domain-events}")
    private String streamName;

    @Autowired
    public RedisStreamPublisher(RedissonClient redissonClient,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry,
                                ObjectProvider<Clock> clockProvider) {
        this(redissonClient, objectMapper, meterRegistry, clockProvider.getIfAvailable(Clock::systemUTC));
    }

    RedisStreamPublisher(RedissonClient redissonClient,
                         ObjectMapper objectMapper,
                         MeterRegistry meterRegistry,
                         Clock clock) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    public void publish(OutboxMessage message) {
        RStream<String, String> stream = redissonClient.getStream(streamName);

        // Construct full OutboxEnvelope for Redis Stream
        io.monosense.synergyflow.eventing.api.OutboxEnvelope envelope =
                new io.monosense.synergyflow.eventing.api.OutboxEnvelope(
                        message.aggregateId(),
                        message.aggregateType(),
                        message.eventType(),
                        message.version(),
                        message.occurredAt(),
                        1, // schema_version
                        message.payload()
                );

        String envelopeJson = toJsonString(envelope);
        StreamAddArgs<String, String> args = StreamAddArgs.entries(
                Map.of("event", envelopeJson)
        );

        Instant now = Instant.now(clock);
        RetryState state = retryStates.get(message.id());
        if (state != null && !state.canAttempt(now)) {
            Duration remaining = Duration.between(now, state.nextAttemptAt());
            log.debug("Redis publish backoff active for event_id={} remaining_ms={}", message.id(), Math.max(remaining.toMillis(), 0));
            throw new RedisPublishBackoffException(message.id(), state.nextAttemptAt(), null);
        }

        try {
            stream.add(args);
            retryStates.remove(message.id());
            incrementSuccess(message.eventType());
            if (log.isDebugEnabled()) {
                log.debug("Published event to Redis stream: id={}, event_type={}, version={}", message.id(), message.eventType(), message.version());
            }
        } catch (Exception ex) {
            RetryState updated = retryStates.compute(message.id(), (id, existing) -> {
                RetryState next = existing != null ? existing : new RetryState();
                next.registerFailure(now);
                return next;
            });

            int failureCount = updated.failureCount();
            if (failureCount > BACKOFF_DELAYS_MS.length) {
                retryStates.remove(message.id());
                recordPublishError(message.eventType(), ex);
                throw ex;
            }

            long delayMs = BACKOFF_DELAYS_MS[failureCount - 1];
            Instant nextAttemptAt = updated.nextAttemptAt();
            log.warn("Redis publish failed, scheduling retry: event_id={} event_type={} attempt={} delay_ms={} next_attempt_at={} error={}",
                    message.id(), message.eventType(), failureCount, delayMs, nextAttemptAt, ex.getMessage());
            throw new RedisPublishBackoffException(message.id(), nextAttemptAt, ex);
        }
    }

    private void incrementSuccess(String eventType) {
        Counter.builder("outbox_events_published_total")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();
    }

    private void recordPublishError(String eventType, Throwable ex) {
        Counter.builder("outbox_redis_publish_errors_total")
                .tag("event_type", eventType)
                .tag("error", ex.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
        log.error("Failed to publish event to Redis after retries: event_type={}, error={}", eventType, ex.getMessage(), ex);
    }

    private String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }

    private static final class RetryState {
        private int failureCount;
        private Instant nextAttemptAt;

        boolean canAttempt(Instant now) {
            return nextAttemptAt == null || !now.isBefore(nextAttemptAt);
        }

        void registerFailure(Instant now) {
            failureCount++;
            if (failureCount <= BACKOFF_DELAYS_MS.length) {
                nextAttemptAt = now.plusMillis(BACKOFF_DELAYS_MS[failureCount - 1]);
            } else {
                nextAttemptAt = null;
            }
        }

        int failureCount() {
            return failureCount;
        }

        Instant nextAttemptAt() {
            return nextAttemptAt;
        }
    }
}
