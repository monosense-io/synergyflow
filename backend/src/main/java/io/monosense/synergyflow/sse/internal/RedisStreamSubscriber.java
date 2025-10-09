package io.monosense.synergyflow.sse.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.eventing.api.OutboxEnvelope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Subscribes to Redis Stream and broadcasts domain events to connected SSE clients.
 * <p>
 * Consumes events from the Redis Stream (topic: domain-events) using a Redisson consumer group
 * (sse-gateways) with a unique consumer name per instance (gateway-{hostname}). This enables
 * fault-tolerant event distribution across multiple SSE Gateway replicas while ensuring
 * at-least-once delivery semantics.
 * </p>
 * <p>
 * The subscriber runs in a dedicated thread with a blocking read loop (1-second block timeout)
 * to efficiently consume events while minimizing CPU usage. Events are deserialized as
 * {@link OutboxEnvelope} and broadcast to connected clients via {@link SseConnectionManager}.
 * </p>
 * <p>
 * Graceful shutdown is supported - the subscriber will complete processing in-flight events
 * before terminating, ensuring no messages are lost during pod restarts.
 * </p>
 *
 * @author monosense
 * @since 1.7
 */
@Service
@Profile("!worker")
@Slf4j
public class RedisStreamSubscriber {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final SseConnectionManager connectionManager;
    private final MeterRegistry meterRegistry;
    private final Supplier<ExecutorService> executorServiceSupplier;

    @Value("${synergyflow.outbox.redis.stream-name:domain-events}")
    private String streamName;

    @Value("${synergyflow.sse.consumer-group:sse-gateways}")
    private String consumerGroup;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;
    private Counter eventsConsumedCounter;
    private Counter deserializationErrorsCounter;
    private String consumerName;

    @Autowired
    public RedisStreamSubscriber(RedissonClient redissonClient,
                                 ObjectMapper objectMapper,
                                 SseConnectionManager connectionManager,
                                 MeterRegistry meterRegistry) {
        this(redissonClient, objectMapper, connectionManager, meterRegistry, RedisStreamSubscriber::createExecutor);
    }

    RedisStreamSubscriber(RedissonClient redissonClient,
                          ObjectMapper objectMapper,
                          SseConnectionManager connectionManager,
                          MeterRegistry meterRegistry,
                          Supplier<ExecutorService> executorServiceSupplier) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.connectionManager = Objects.requireNonNull(connectionManager, "connectionManager");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.executorServiceSupplier = Objects.requireNonNull(executorServiceSupplier, "executorServiceSupplier");
    }

    /**
     * Initializes the Redis Stream consumer and starts the subscription loop.
     */
    @PostConstruct
    public void start() {
        try {
            this.consumerName = "gateway-" + InetAddress.getLocalHost().getHostName();

            // Register metrics
            this.eventsConsumedCounter = Counter.builder("sse_events_consumed_total")
                    .description("Total number of events consumed from Redis Stream")
                    .register(meterRegistry);

            this.deserializationErrorsCounter = Counter.builder("sse_deserialization_errors_total")
                    .description("Total number of event deserialization errors")
                    .register(meterRegistry);

            // Create consumer group if not exists
            RStream<String, String> stream = redissonClient.getStream(streamName);
            try {
                stream.createGroup(StreamCreateGroupArgs.name(consumerGroup).id(StreamMessageId.ALL).makeStream());
                log.info("Created Redis Stream consumer group: stream={}, group={}", streamName, consumerGroup);
            } catch (Exception e) {
                // Group already exists, ignore
                log.debug("Consumer group already exists: stream={}, group={}, error={}",
                        streamName, consumerGroup, e.getMessage());
            }

            // Start subscription loop in dedicated thread
            this.executorService = executorServiceSupplier.get();
            if (this.executorService == null) {
                throw new IllegalStateException("ExecutorService supplier returned null");
            }

            running.set(true);
            executorService.submit(this::subscriptionLoop);

            log.info("SSE Redis Stream subscriber started: stream={}, consumer_group={}, consumer_name={}",
                    streamName, consumerGroup, consumerName);

        } catch (Exception e) {
            log.error("Failed to start SSE Redis Stream subscriber: error={}", e.getMessage(), e);
            throw new IllegalStateException("Failed to start SSE subscriber", e);
        }
    }

    /**
     * Subscription loop that continuously reads from Redis Stream and broadcasts events.
     */
    private void subscriptionLoop() {
        log.info("SSE subscription loop started: consumer_name={}", consumerName);

        RStream<String, String> stream = redissonClient.getStream(streamName);

        while (running.get()) {
            try {
                // Read from Redis Stream with consumer group (blocking for 1 second)
                // readGroup returns StreamMessageId -> Map<K,V>
                Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
                        consumerGroup,
                        consumerName,
                        StreamReadGroupArgs.greaterThan(StreamMessageId.NEVER_DELIVERED).timeout(java.time.Duration.ofSeconds(1))
                );

                if (messages == null || messages.isEmpty()) {
                    continue;
                }

                if (log.isTraceEnabled()) {
                    log.trace("Consumed {} events from Redis Stream: consumer_name={}", messages.size(), consumerName);
                }

                for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
                    StreamMessageId messageId = entry.getKey();
                    Map<String, String> data = entry.getValue();

                    try {
                        String eventJson = data.get("event");
                        if (eventJson == null) {
                            log.warn("Missing 'event' field in Redis Stream message: stream_id={}", messageId);
                            stream.ack(consumerGroup, messageId); // ACK to skip this message
                            continue;
                        }

                        OutboxEnvelope envelope = objectMapper.readValue(eventJson, OutboxEnvelope.class);
                        connectionManager.broadcast(envelope, messageId.toString());
                        eventsConsumedCounter.increment();

                        // ACK message after successful processing
                        stream.ack(consumerGroup, messageId);

                    } catch (Exception e) {
                        log.error("Failed to process SSE event: stream_id={}, error={}", messageId, e.getMessage(), e);
                        deserializationErrorsCounter.increment();
                        // ACK to skip problematic message (don't block stream)
                        stream.ack(consumerGroup, messageId);
                    }
                }

            } catch (Exception e) {
                if (running.get()) {
                    log.error("Error in SSE subscription loop: error={}", e.getMessage(), e);
                    // Sleep briefly to avoid tight error loop
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.info("SSE subscription loop terminated: consumer_name={}", consumerName);
    }

    /**
     * Gracefully shuts down the Redis Stream subscriber.
     * <p>
     * Stops the subscription loop and waits for in-flight events to complete processing
     * before terminating. Ensures no messages are lost during shutdown.
     * </p>
     */
    @PreDestroy
    public void stop() {
        log.info("Stopping SSE Redis Stream subscriber: consumer_name={}", consumerName);

        running.set(false);

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("SSE subscriber did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for SSE subscriber shutdown");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("SSE Redis Stream subscriber stopped: consumer_name={}", consumerName);
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "sse-redis-subscriber");
            thread.setDaemon(false); // Not daemon - allow graceful shutdown
            return thread;
        });
    }
}
