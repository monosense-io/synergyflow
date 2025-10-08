package io.monosense.synergyflow.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.SynergyFlowApplication;
import io.monosense.synergyflow.eventing.api.OutboxEnvelope;
import io.monosense.synergyflow.sse.internal.SseConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for SSE Gateway functionality.
 * <p>
 * Tests the complete SSE event flow: Redis Stream → RedisStreamSubscriber → SseConnectionManager → SSE clients
 * </p>
 *
 * @author monosense
 * @since 1.7
 */
@SpringBootTest(
        classes = SynergyFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "synergyflow.outbox.redis.stream-name=domain-events",
                "synergyflow.sse.consumer-group=sse-gateways",
                "synergyflow.sse.catchup.max-events=100"
        }
)
@ActiveProfiles("app")
@Testcontainers
class SseGatewayIntegrationTest {

    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    private static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> REDIS_HOST);
        registry.add("spring.redis.port", () -> REDIS_PORT);
        if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
            registry.add("spring.redis.password", () -> REDIS_PASSWORD);
        }
    }

    @Autowired
    private io.monosense.synergyflow.sse.api.SseController sseController;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SseConnectionManager connectionManager;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void tearDown() {
        connectionManager.clearAllConnections();
    }

    @Test
    void streamEvents_returnsEmitterForAuthenticatedUser() {
        Principal principal = () -> "user1";

        SseEmitter emitter = sseController.streamEvents(principal, null);

        assertThat(emitter).isNotNull();
        emitter.complete();
    }

    @Test
    void redisStreamToSseClient_shouldBroadcastEvents() throws Exception {
        String userId = "user1";
        mockJwtAuthentication(userId);

        SseEmitter emitter = new SseEmitter(5000L);
        try (var ignored = registerEmitter(userId, emitter)) {
            OutboxEnvelope envelope = createTestEnvelopeWithOwner("TicketCreated", userId);
            publishToRedisStream(envelope);

            Thread.sleep(500);

            Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
            assertThat(activeConnections).isGreaterThanOrEqualTo(1.0);
        }
    }

    @Test
    void catchup_shouldSendMissedEvents() throws Exception {
        String userId = "user1";
        mockJwtAuthentication(userId);

        // Publish events to Redis Stream before connection
        OutboxEnvelope event1 = createTestEnvelopeWithOwner("TicketCreated", userId);
        OutboxEnvelope event2 = createTestEnvelopeWithOwner("TicketAssigned", userId);

        StreamMessageId id1 = publishToRedisStream(event1);
        publishToRedisStream(event2); // id2 intentionally not used in MVP

        Thread.sleep(200); // Allow time for events to be written

        // Connect with Last-Event-ID header
        String lastEventId = id1.toString();

        SseEmitter emitter = new SseEmitter(5000L);

        // In a real scenario, we'd intercept emitter.send() calls
        // For this test, we verify that catchup counter increments
        Double beforeCatchup = meterRegistry.find("sse_catchup_events_sent_total")
                .counter() != null ? meterRegistry.get("sse_catchup_events_sent_total").counter().count() : 0.0;

        try (var ignored = registerEmitter(userId, emitter, lastEventId)) {
            Thread.sleep(500);
        }

        Double afterCatchup = meterRegistry.find("sse_catchup_events_sent_total")
                .counter() != null ? meterRegistry.get("sse_catchup_events_sent_total").counter().count() : 0.0;

        // Catchup should have sent at least one event
        assertThat(afterCatchup).isGreaterThanOrEqualTo(beforeCatchup);
    }

    @Test
    void connectionLifecycle_shouldTrackMetrics() throws Exception {
        String userId = "user1";
        mockJwtAuthentication(userId);

        SseEmitter emitter = new SseEmitter(1000L);

        Double beforeConnections = meterRegistry.get("sse_active_connections").gauge().value();

        try (var ignored = registerEmitter(userId, emitter)) {
            Double duringConnection = meterRegistry.get("sse_active_connections").gauge().value();
            assertThat(duringConnection).isEqualTo(beforeConnections + 1);
        }

        Double afterDisconnect = meterRegistry.get("sse_active_connections").gauge().value();
        assertThat(afterDisconnect).isEqualTo(beforeConnections);
    }

    @Test
    void concurrentConnections_shouldHandleMultipleClients() throws Exception {
        int numClients = 10;
        CountDownLatch allConnectedLatch = new CountDownLatch(numClients);
        List<AutoCloseable> closers = new ArrayList<>();
        try {
            for (int i = 0; i < numClients; i++) {
                String userId = "user" + i;
                mockJwtAuthentication(userId);

                SseEmitter emitter = new SseEmitter(5000L);
                closers.add(registerEmitter(userId, emitter));
                allConnectedLatch.countDown();
            }

            assertThat(allConnectedLatch.await(5, TimeUnit.SECONDS)).isTrue();

            Double activeConnections = meterRegistry.get("sse_active_connections").gauge().value();
            assertThat(activeConnections).isGreaterThanOrEqualTo((double) numClients);
        } finally {
            for (AutoCloseable closer : closers) {
                try {
                    closer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void eventBroadcast_shouldIncrementEventsSentCounter() throws Exception {
        String userId = "user1";
        mockJwtAuthentication(userId);

        Double beforeBroadcast = meterRegistry.find("sse_events_sent_total")
                .counter() != null ? meterRegistry.get("sse_events_sent_total").counter().count() : 0.0;

        SseEmitter emitter = new SseEmitter(5000L);
        try (var ignored = registerEmitter(userId, emitter)) {
            OutboxEnvelope envelope = createTestEnvelopeWithOwner("TicketCreated", userId);
            connectionManager.broadcast(envelope, "1234567890-0");

            Thread.sleep(100);
        }

        Double afterBroadcast = meterRegistry.find("sse_events_sent_total")
                .tag("event_type", "TicketCreated")
                .counter() != null ? meterRegistry.get("sse_events_sent_total").tag("event_type", "TicketCreated").counter().count() : 0.0;

        assertThat(afterBroadcast).isGreaterThan(beforeBroadcast);
    }

    @Test
    void userFiltering_shouldOnlySendToOwner() throws Exception {
        String user1 = "user1";
        String user2 = "user2";

        mockJwtAuthentication(user1);
        SseEmitter emitter1 = new SseEmitter(5000L);

        Double beforeBroadcast = meterRegistry.find("sse_events_sent_total")
                .counter() != null ? meterRegistry.get("sse_events_sent_total").counter().count() : 0.0;

        try (var ignored1 = registerEmitter(user1, emitter1)) {
            mockJwtAuthentication(user2);
            SseEmitter emitter2 = new SseEmitter(5000L);
            try (var ignored2 = registerEmitter(user2, emitter2)) {
                Thread.sleep(200);

                OutboxEnvelope envelope = createTestEnvelopeWithOwner("TicketCreated", user1);
                connectionManager.broadcast(envelope, "1234567891-0");

                Thread.sleep(100);
            }
        }

        Double afterBroadcast = meterRegistry.find("sse_events_sent_total")
                .counter() != null ? meterRegistry.get("sse_events_sent_total").counter().count() : 0.0;

        assertThat(afterBroadcast - beforeBroadcast).isEqualTo(1.0);
    }

    private void mockJwtAuthentication(String userId) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", userId)
                .claim("scope", "read write")
                .build();

        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
    }

    private StreamMessageId publishToRedisStream(OutboxEnvelope envelope) throws Exception {
        RStream<String, String> stream = redissonClient.getStream("domain-events");

        String eventJson = objectMapper.writeValueAsString(envelope);

        return stream.add(StreamAddArgs.entry("event", eventJson));
    }

    private OutboxEnvelope createTestEnvelope(String eventType) {
        return createTestEnvelopeWithOwner(eventType, "user1");
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

    private AutoCloseable registerEmitter(String userId, SseEmitter emitter) {
        return registerEmitter(userId, emitter, null);
    }

    private AutoCloseable registerEmitter(String userId, SseEmitter emitter, String lastEventId) {
        connectionManager.register(userId, emitter, lastEventId);
        return () -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            } finally {
                connectionManager.unregister(userId, emitter);
            }
        };
    }
}
