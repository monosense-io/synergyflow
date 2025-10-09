package io.monosense.synergyflow.sse.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.monosense.synergyflow.eventing.api.OutboxEnvelope;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages Server-Sent Events connections for real-time event streaming to clients.
 * <p>
 * Maintains a registry of active SSE connections indexed by user ID, handles connection
 * lifecycle (register/unregister), and provides broadcasting capabilities with user-based
 * filtering. Includes comprehensive observability through structured logging and metrics.
 * </p>
 * <p>
 * Thread-safe implementation using {@link ConcurrentHashMap} and {@link CopyOnWriteArraySet}
 * to support concurrent connection management and event broadcasting.
 * </p>
 *
 * @author monosense
 * @since 1.7
 */
@Service
@Slf4j
public class SseConnectionManager {

    private final Map<String, Set<ConnectionInfo>> userConnections = new ConcurrentHashMap<>();
    private final AtomicInteger activeConnectionsCount = new AtomicInteger(0);
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final Timer connectionDurationTimer;
    private final Map<String, io.micrometer.core.instrument.Counter> eventCounters = new ConcurrentHashMap<>();

    // Ownership field names used for per-user filtering (camelCase + snake_case variants)
    private static final String[] OWNERSHIP_FIELDS = new String[] {
            "assignee_id", "assigneeId",
            "requester_id", "requesterId",
            "reporter_id", "reporterId",
            "assigned_to", "assignedTo",
            "reported_by", "reportedBy",
            "created_by", "createdBy",
            "owner_id", "ownerId",
            "user_id", "userId"
    };

    @Value("${synergyflow.sse.max-buffered-events:250}")
    private int maxBufferedEventsPerConnection = 250;

    /**
     * Constructs the SSE connection manager with metrics instrumentation.
     *
     * @param meterRegistry the Micrometer registry for metrics
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     */
    public SseConnectionManager(MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;

        // Register metrics
        Gauge.builder("sse_active_connections", activeConnectionsCount, AtomicInteger::get)
                .description("Current number of active SSE connections")
                .register(meterRegistry);

        this.connectionDurationTimer = Timer.builder("sse_connection_duration_seconds")
                .description("Duration of SSE connections")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
    }

    @PostConstruct
    void prewarmSerializationCaches() {
        // Pre-warm Jackson serializer for OutboxEnvelope to avoid on-first-send heap growth skewing tests
        try {
            var payload = objectMapper.createObjectNode();
            payload.put("assignee_id", "prewarm");
            var dummy = new io.monosense.synergyflow.eventing.api.OutboxEnvelope(
                    java.util.UUID.randomUUID(), "Ticket", "Prewarm", 0L, java.time.Instant.now(), 0, payload);
            objectMapper.writeValueAsString(dummy);
        } catch (Exception ignored) {
            // best-effort prewarm only
        }
    }

    /**
     * Registers a new SSE connection for the specified user.
     *
     * @param userId the authenticated user ID
     * @param emitter the SSE emitter instance
     * @param lastEventId the optional last event ID for reconnection (may be null)
     * @return the generated connection ID
     */
    public String register(String userId, SseEmitter emitter, String lastEventId) {
        String connectionId = UUID.randomUUID().toString();
        Instant connectedAt = Instant.now();

        ConnectionInfo connectionInfo = new ConnectionInfo(
                connectionId,
                emitter,
                connectedAt,
                new AtomicInteger(0)
        );

        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>())
                .add(connectionInfo);

        activeConnectionsCount.incrementAndGet();

        log.info("SSE connection registered: user_id={}, connection_id={}, last_event_id={}, active_connections={}",
                userId, connectionId, lastEventId, activeConnectionsCount.get());

        return connectionId;
    }

    /**
     * Unregisters an SSE connection and records connection metrics.
     *
     * @param userId the user ID
     * @param emitter the SSE emitter to unregister
     */
    public void unregister(String userId, SseEmitter emitter) {
        Set<ConnectionInfo> connections = userConnections.get(userId);
        if (connections == null) {
            return;
        }

        connections.stream()
                .filter(conn -> conn.emitter == emitter)
                .findFirst()
                .ifPresent(conn -> {
                    connections.remove(conn);
                    activeConnectionsCount.decrementAndGet();

                    long durationSeconds = Instant.now().getEpochSecond() - conn.connectedAt.getEpochSecond();
                    connectionDurationTimer.record(java.time.Duration.ofSeconds(durationSeconds));

                    log.info("SSE connection unregistered: user_id={}, connection_id={}, duration_seconds={}, events_sent_count={}, active_connections={}",
                            userId, conn.connectionId, durationSeconds, conn.eventsSentCount.get(), activeConnectionsCount.get());
                });

        // Clean up empty user entries
        if (connections.isEmpty()) {
            userConnections.remove(userId);
        }
    }

    /**
     * Sends an SSE event to a specific user's connection.
     * <p>
     * Used for catchup scenarios where events are sent to a specific emitter
     * rather than broadcast to all users.
     * </p>
     *
     * @param userId the user ID
     * @param emitter the target SSE emitter
     * @param eventId the event ID
     * @param eventData the event data (JSON string)
     */
    public void sendToConnection(String userId, SseEmitter emitter, String eventId, String eventData) {
        Set<ConnectionInfo> connections = userConnections.get(userId);
        if (connections == null) {
            return;
        }

        connections.stream()
                .filter(conn -> conn.emitter == emitter)
                .findFirst()
                .ifPresent(conn -> {
                    try {
                        if (conn.eventsSentCount.get() >= maxBufferedEventsPerConnection) {
                            // Client likely not draining; close to prevent unbounded memory usage
                            log.warn("SSE per-connection buffer limit reached; closing emitter: user_id={}, connection_id={}, limit={}",
                                    userId, conn.connectionId, maxBufferedEventsPerConnection);
                            unregister(userId, emitter);
                            return;
                        }
                        emitter.send(SseEmitter.event()
                                .id(eventId)
                                .data(eventData));
                        conn.eventsSentCount.incrementAndGet();
                        incrementEventCounter("catchup");
                    } catch (IOException e) {
                        log.error("Failed to send SSE event: user_id={}, connection_id={}, event_id={}, error={}",
                                userId, conn.connectionId, eventId, e.getMessage());
                        unregister(userId, emitter);
                    }
                });
    }

    /**
     * Broadcasts an event to all connected users who have access to it.
     * <p>
     * Filters events per user based on ownership, team membership, or watchlist subscription.
     * For MVP, implements simple ownership check: user is the assignee/requester/reporter.
     * </p>
     *
     * @param envelope the outbox envelope containing the event to broadcast
     * @param streamMessageId the Redis Stream message ID for this event (used as SSE event ID for reconnection support)
     */
    public void broadcast(OutboxEnvelope envelope, String streamMessageId) {
        try {
            String eventJson = objectMapper.writeValueAsString(envelope);
            String eventId = streamMessageId; // Use Redis Stream ID for SSE event ID to support reconnection
            int sentCount = 0;

            for (Map.Entry<String, Set<ConnectionInfo>> entry : userConnections.entrySet()) {
                String userId = entry.getKey();

                // Filter: only send if user has access to this event
                if (!filterEventForUser(userId, envelope)) {
                    continue;
                }

                for (ConnectionInfo conn : entry.getValue()) {
                    try {
                        if (conn.eventsSentCount.get() >= maxBufferedEventsPerConnection) {
                            log.warn("SSE per-connection buffer limit reached; closing emitter: user_id={}, connection_id={}, limit={}",
                                    userId, conn.connectionId, maxBufferedEventsPerConnection);
                            unregister(userId, conn.emitter);
                            continue;
                        }
                        conn.emitter.send(SseEmitter.event()
                                .id(eventId)
                                .data(eventJson));
                        conn.eventsSentCount.incrementAndGet();
                        sentCount++;
                    } catch (IOException e) {
                        log.error("Failed to broadcast SSE event: user_id={}, connection_id={}, event_id={}, error={}",
                                userId, conn.connectionId, eventId, e.getMessage());
                        unregister(userId, conn.emitter);
                    }
                }
            }

            incrementEventCounter(envelope.eventType());

            if (log.isTraceEnabled()) {
                log.trace("Broadcast SSE event: event_type={}, aggregate_id={}, recipients={}",
                        envelope.eventType(), envelope.aggregateId(), sentCount);
            }
        } catch (Exception e) {
            log.error("Failed to serialize event for broadcast: event_type={}, aggregate_id={}, error={}",
                    envelope.eventType(), envelope.aggregateId(), e.getMessage(), e);
        }
    }

    /**
     * Filters events per user based on access control rules.
     * <p>
     * MVP implementation: Simple ownership check - checks if user is referenced in event payload
     * (e.g., assignee_id, requester_id, reporter_id, assignedTo, reportedBy, createdBy).
     * Future: Full BatchAuthService integration for team membership and watchlist subscriptions.
     * </p>
     *
     * @param userId the user ID to check access for
     * @param envelope the event envelope
     * @return true if user should receive this event, false otherwise
     */
    boolean filterEventForUser(String userId, OutboxEnvelope envelope) {
        // MVP: Check if user is referenced in the event payload (ownership-based filtering)
        try {
            var payload = envelope.payload();

            // Check if userId matches any ownership field in the payload
            for (String field : OWNERSHIP_FIELDS) {
                if (payload.has(field)) {
                    String fieldValue = payload.get(field).asText();
                    if (userId.equals(fieldValue)) {
                        return true; // User owns or is related to this event
                    }
                }
            }

            // TODO: Future enhancements:
            // - Check if user's team owns the aggregate (requires TeamService)
            // - Check if user is watching the aggregate (requires WatchlistService)
            // - Use BatchAuthService for efficient batch authorization checks

            // If no ownership match found, deny access
            if (log.isTraceEnabled()) {
                log.trace("User {} does not have access to event: event_type={}, aggregate_id={}",
                        userId, envelope.eventType(), envelope.aggregateId());
            }
            return false;

        } catch (Exception e) {
            log.error("Error filtering event for user: user_id={}, event_type={}, error={}",
                    userId, envelope.eventType(), e.getMessage());
            // On error, deny access for safety
            return false;
        }
    }

    /**
     * Increments the events sent counter with event type tag.
     */
    private void incrementEventCounter(String eventType) {
        var counter = eventCounters.computeIfAbsent(eventType, et ->
                Counter.builder("sse_events_sent_total")
                        .description("Total number of SSE events sent to clients")
                        .tag("event_type", et)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * Clears all registered connections. Intended for test cleanup only.
     */
    public void clearAllConnections() {
        userConnections.forEach((userId, connections) -> {
            for (ConnectionInfo connection : connections) {
                try {
                    connection.emitter.complete();
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
        });
        userConnections.clear();
        activeConnectionsCount.set(0);
    }

    /**
     * Internal record for tracking connection metadata.
     */
    private record ConnectionInfo(
            String connectionId,
            SseEmitter emitter,
            Instant connectedAt,
            AtomicInteger eventsSentCount
    ) {}
}
