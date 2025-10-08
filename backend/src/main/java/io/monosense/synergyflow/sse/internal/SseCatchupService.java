package io.monosense.synergyflow.sse.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.eventing.api.OutboxEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Handles SSE catchup logic for reconnecting clients.
 * <p>
 * When clients reconnect after disconnection, they provide the {@code Last-Event-ID} header
 * indicating the last event they successfully received. This service reads all missed events
 * from the Redis Stream since that ID and sends them to the client before resuming live streaming.
 * </p>
 * <p>
 * Catchup completes in &lt;1s p95 for ≤100 missed events (AC2).
 * </p>
 *
 * @author monosense
 * @since 1.7
 */
@Service
@Slf4j
public class SseCatchupService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final SseConnectionManager connectionManager;
    private final Counter catchupEventsCounter;

    @Value("${synergyflow.outbox.redis.stream-name:domain-events}")
    private String streamName;

    @Value("${synergyflow.sse.catchup.max-events:100}")
    private int maxCatchupEvents;

    public SseCatchupService(RedissonClient redissonClient,
                             ObjectMapper objectMapper,
                             SseConnectionManager connectionManager,
                             MeterRegistry meterRegistry) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
        this.catchupEventsCounter = Counter.builder("sse_catchup_events_sent_total")
                .description("Total number of SSE catchup events sent during reconnection")
                .register(meterRegistry);
    }

    /**
     * Reads missed events from Redis Stream and sends them to the SSE emitter.
     *
     * @param userId the authenticated user ID
     * @param emitter the SSE emitter to send events to
     * @param lastEventId the last event ID received by the client (Redis Stream ID format)
     * @return the number of catchup events sent
     */
    public int sendMissedEvents(String userId, SseEmitter emitter, String lastEventId) {
        try {
            long startTime = System.currentTimeMillis();
            Map<String, OutboxEnvelope> missedEvents = readMissedEventsWithIds(lastEventId, maxCatchupEvents);

            log.debug("Sending {} catchup events to user_id={}, last_event_id={}",
                    missedEvents.size(), userId, lastEventId);

            for (Map.Entry<String, OutboxEnvelope> entry : missedEvents.entrySet()) {
                String streamId = entry.getKey();
                OutboxEnvelope envelope = entry.getValue();
                String eventJson = objectMapper.writeValueAsString(envelope);
                // Use the actual Redis Stream ID as SSE event ID for reconnection support
                connectionManager.sendToConnection(userId, emitter, streamId, eventJson);
                catchupEventsCounter.increment();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Catchup complete: user_id={}, events_sent={}, duration_ms={}, last_event_id={}",
                    userId, missedEvents.size(), duration, lastEventId);

            return missedEvents.size();
        } catch (Exception e) {
            log.error("Catchup failed: user_id={}, last_event_id={}, error={}",
                    userId, lastEventId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Reads missed events from Redis Stream starting after the given event ID.
     * Package-private for testing.
     *
     * @param lastEventId the last event ID received (Redis Stream ID format: timestamp-sequence)
     * @param maxEvents the maximum number of events to fetch
     * @return list of missed events in order
     */
    List<OutboxEnvelope> readMissedEvents(String lastEventId, int maxEvents) {
        return new java.util.ArrayList<>(readMissedEventsWithIds(lastEventId, maxEvents).values());
    }

    /**
     * Reads missed events from Redis Stream starting after the given event ID, with Stream IDs.
     *
     * @param lastEventId the last event ID received (Redis Stream ID format: timestamp-sequence)
     * @param maxEvents the maximum number of events to fetch
     * @return map of stream ID to missed events (in order)
     */
    private Map<String, OutboxEnvelope> readMissedEventsWithIds(String lastEventId, int maxEvents) {
        Map<String, OutboxEnvelope> events = new java.util.LinkedHashMap<>();

        try {
            RStream<String, String> stream = redissonClient.getStream(streamName);

            // Parse lastEventId string (format: "timestamp-sequence" e.g., "1234567890-0")
            StreamMessageId startId = parseStreamMessageId(lastEventId);

            // Read from (lastEventId, +) exclusive to get events after the last received ID
            // Redisson RStream.range(count, startId, endId) - note startId is exclusive when using range
            Map<StreamMessageId, Map<String, String>> entries = stream.range(
                    maxEvents,
                    startId,
                    StreamMessageId.MAX
            );

            for (Map.Entry<StreamMessageId, Map<String, String>> entry : entries.entrySet()) {
                try {
                    String eventJson = entry.getValue().get("event");
                    if (eventJson != null) {
                        OutboxEnvelope envelope = objectMapper.readValue(eventJson, OutboxEnvelope.class);
                        events.put(entry.getKey().toString(), envelope);
                    }
                } catch (Exception e) {
                    log.error("Failed to deserialize catchup event: stream_id={}, error={}",
                            entry.getKey(), e.getMessage());
                }
            }

            log.debug("Read {} missed events from Redis Stream: last_event_id={}, stream={}",
                    events.size(), lastEventId, streamName);

        } catch (Exception e) {
            log.error("Failed to read missed events from Redis Stream: last_event_id={}, stream={}, error={}",
                    lastEventId, streamName, e.getMessage(), e);
        }

        return events;
    }

    /**
     * Parses a Redis Stream message ID from string format.
     *
     * @param streamId the stream ID in format "timestamp-sequence" (e.g., "1234567890-0")
     * @return the parsed StreamMessageId
     */
    private StreamMessageId parseStreamMessageId(String streamId) {
        try {
            String[] parts = streamId.split("-");
            long timestamp = Long.parseLong(parts[0]);
            long sequence = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
            return new StreamMessageId(timestamp, sequence);
        } catch (Exception e) {
            log.warn("Failed to parse stream message ID '{}', using MIN: {}", streamId, e.getMessage());
            return StreamMessageId.MIN;
        }
    }
}
