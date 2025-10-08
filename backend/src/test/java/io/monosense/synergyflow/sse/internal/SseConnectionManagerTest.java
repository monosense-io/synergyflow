package io.monosense.synergyflow.sse.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.monosense.synergyflow.eventing.api.OutboxEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SseConnectionManager}.
 *
 * @author monosense
 * @since 1.7
 */
@ExtendWith(MockitoExtension.class)
class SseConnectionManagerTest {

    private SseConnectionManager connectionManager;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;

    @Mock
    private SseEmitter mockEmitter1;

    @Mock
    private SseEmitter mockEmitter2;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for Instant serialization
        connectionManager = new SseConnectionManager(meterRegistry, objectMapper);
    }

    @Test
    void register_shouldIncrementActiveConnectionsGauge() {
        String userId = "user1";

        connectionManager.register(userId, mockEmitter1, null);

        Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(activeConnections).isEqualTo(1.0);
    }

    @Test
    void register_shouldReturnUniqueConnectionId() {
        String userId = "user1";

        String connectionId1 = connectionManager.register(userId, mockEmitter1, null);
        String connectionId2 = connectionManager.register(userId, mockEmitter2, null);

        assertThat(connectionId1).isNotEqualTo(connectionId2);
    }

    @Test
    void register_shouldSupportMultipleConnectionsPerUser() {
        String userId = "user1";

        connectionManager.register(userId, mockEmitter1, null);
        connectionManager.register(userId, mockEmitter2, null);

        Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(activeConnections).isEqualTo(2.0);
    }

    @Test
    void unregister_shouldDecrementActiveConnectionsGauge() {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        connectionManager.unregister(userId, mockEmitter1);

        Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(activeConnections).isEqualTo(0.0);
    }

    @Test
    void unregister_shouldRecordConnectionDuration() {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        // Small delay to ensure non-zero duration
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        connectionManager.unregister(userId, mockEmitter1);

        assertThat(meterRegistry.get("sse_connection_duration_seconds").timer().count()).isEqualTo(1);
    }

    @Test
    void unregister_nonExistentConnection_shouldNotThrowException() {
        String userId = "user1";

        // Should not throw
        connectionManager.unregister(userId, mockEmitter1);
    }

    @Test
    void broadcast_shouldSendEventToAllConnectedUsers() throws IOException {
        String user1 = "user1";
        String user2 = "user2";
        connectionManager.register(user1, mockEmitter1, null);
        connectionManager.register(user2, mockEmitter2, null);

        // Create envelope with both users as owners
        var payload = objectMapper.createObjectNode();
        payload.put("assignee_id", user1);
        payload.put("requester_id", user2);
        OutboxEnvelope envelope = new OutboxEnvelope(
                UUID.randomUUID(),
                "Ticket",
                "TicketCreated",
                1L,
                Instant.now(),
                1,
                payload
        );

        connectionManager.broadcast(envelope, "1234567890-0");

        verify(mockEmitter1).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter2).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void broadcast_shouldIncrementEventsSentCounter() throws IOException {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        OutboxEnvelope envelope = createTestEnvelope("TicketCreated");

        connectionManager.broadcast(envelope, "1234567891-0");

        Double eventsSent = meterRegistry.get("sse_events_sent_total")
                .tag("event_type", "TicketCreated")
                .counter()
                .count();
        assertThat(eventsSent).isEqualTo(1.0);
    }

    @Test
    void broadcast_whenSendFails_shouldUnregisterConnection() throws IOException {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        doThrow(new IOException("Connection broken")).when(mockEmitter1).send(any(SseEmitter.SseEventBuilder.class));

        OutboxEnvelope envelope = createTestEnvelopeWithOwner("TicketCreated", userId);
        connectionManager.broadcast(envelope, "1234567892-0");

        // Connection should be auto-unregistered after failure
        Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(activeConnections).isEqualTo(0.0);
    }

    @Test
    void sendToConnection_shouldSendEventToSpecificEmitter() throws IOException {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        String eventId = "event-123";
        String eventData = "{\"test\":\"data\"}";

        connectionManager.sendToConnection(userId, mockEmitter1, eventId, eventData);

        verify(mockEmitter1).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void sendToConnection_shouldIncrementCatchupCounter() throws IOException {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        String eventId = "event-123";
        String eventData = "{\"test\":\"data\"}";

        connectionManager.sendToConnection(userId, mockEmitter1, eventId, eventData);

        Double catchupEventsSent = meterRegistry.get("sse_events_sent_total")
                .tag("event_type", "catchup")
                .counter()
                .count();
        assertThat(catchupEventsSent).isEqualTo(1.0);
    }

    @Test
    void sendToConnection_whenSendFails_shouldUnregisterConnection() {
        String userId = "user1";
        connectionManager.register(userId, mockEmitter1, null);

        try {
            doThrow(new IOException("Connection broken")).when(mockEmitter1).send(any(SseEmitter.SseEventBuilder.class));
        } catch (IOException e) {
            // This won't happen in mock setup
        }

        String eventId = "event-123";
        String eventData = "{\"test\":\"data\"}";

        connectionManager.sendToConnection(userId, mockEmitter1, eventId, eventData);

        // Connection should be auto-unregistered after failure
        Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(activeConnections).isEqualTo(0.0);
    }

    @Test
    void concurrentRegistration_shouldBeThreadSafe() throws InterruptedException {
        int numThreads = 10;
        int connectionsPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String userId = "user" + i;
            new Thread(() -> {
                try {
                    startLatch.await(); // All threads wait here
                    for (int j = 0; j < connectionsPerThread; j++) {
                        SseEmitter emitter = mock(SseEmitter.class);
                        connectionManager.register(userId, emitter, null);
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // Release all threads
        assertThat(doneLatch.await(5, TimeUnit.SECONDS)).isTrue();

        Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(activeConnections).isEqualTo((double) (numThreads * connectionsPerThread));
        assertThat(successCount.get()).isEqualTo(numThreads * connectionsPerThread);
    }

    @Test
    void filterEventForUser_shouldAllowOwner() {
        String userId = "user123";
        OutboxEnvelope envelope = createTestEnvelopeWithOwner("TicketCreated", userId);

        boolean allowed = connectionManager.filterEventForUser(userId, envelope);

        assertThat(allowed).isTrue();
    }

    @Test
    void filterEventForUser_shouldDenyNonOwner() {
        String ownerId = "user123";
        String otherUserId = "user456";
        OutboxEnvelope envelope = createTestEnvelopeWithOwner("TicketCreated", ownerId);

        boolean allowed = connectionManager.filterEventForUser(otherUserId, envelope);

        assertThat(allowed).isFalse();
    }

    @Test
    void filterEventForUser_withEmptyPayload_shouldDenyAccess() {
        OutboxEnvelope envelope = createTestEnvelope("TicketCreated");

        boolean allowed = connectionManager.filterEventForUser("anyUser", envelope);

        assertThat(allowed).isFalse();
    }

    private OutboxEnvelope createTestEnvelope(String eventType) {
        return new OutboxEnvelope(
                UUID.randomUUID(),
                "Ticket",
                eventType,
                1L,
                Instant.now(),
                1,
                objectMapper.createObjectNode()
        );
    }

    private OutboxEnvelope createTestEnvelopeWithOwner(String eventType, String userId) {
        var payload = objectMapper.createObjectNode();
        payload.put("assignee_id", userId);
        return new OutboxEnvelope(
                UUID.randomUUID(),
                "Ticket",
                eventType,
                1L,
                Instant.now(),
                1,
                payload
        );
    }
}
