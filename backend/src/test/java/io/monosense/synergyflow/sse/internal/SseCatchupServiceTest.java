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
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SseCatchupService}.
 *
 * @author monosense
 * @since 1.7
 */
@ExtendWith(MockitoExtension.class)
class SseCatchupServiceTest {

    private SseCatchupService catchupService;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private SseConnectionManager connectionManager;

    @Mock
    @SuppressWarnings("rawtypes")
    private RStream mockStream;

    @Mock
    private SseEmitter mockEmitter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for Instant serialization
        catchupService = new SseCatchupService(redissonClient, objectMapper, connectionManager, meterRegistry);

        // Set properties via reflection (simulating Spring's @Value injection)
        ReflectionTestUtils.setField(catchupService, "streamName", "domain-events");
        ReflectionTestUtils.setField(catchupService, "maxCatchupEvents", 100);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMissedEvents_shouldReadFromRedisStreamAndSendToEmitter() throws Exception {
        String userId = "user1";
        String lastEventId = "1234567890-0";

        OutboxEnvelope envelope1 = createTestEnvelope("TicketCreated", 1L);
        OutboxEnvelope envelope2 = createTestEnvelope("TicketAssigned", 2L);

        Map<StreamMessageId, Map<String, String>> streamData = new HashMap<>();
        streamData.put(new StreamMessageId(1234567891L, 0), Map.of("event", objectMapper.writeValueAsString(envelope1)));
        streamData.put(new StreamMessageId(1234567892L, 0), Map.of("event", objectMapper.writeValueAsString(envelope2)));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(streamData);

        int eventsSent = catchupService.sendMissedEvents(userId, mockEmitter, lastEventId);

        assertThat(eventsSent).isEqualTo(2);
        verify(connectionManager, times(2)).sendToConnection(eq(userId), eq(mockEmitter), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMissedEvents_shouldIncrementCatchupCounter() throws Exception {
        String userId = "user1";
        String lastEventId = "1234567890-0";

        OutboxEnvelope envelope = createTestEnvelope("TicketCreated", 1L);

        Map<StreamMessageId, Map<String, String>> streamData = new HashMap<>();
        streamData.put(new StreamMessageId(1234567891L, 0), Map.of("event", objectMapper.writeValueAsString(envelope)));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(streamData);

        catchupService.sendMissedEvents(userId, mockEmitter, lastEventId);

        Double catchupCount = meterRegistry.get("sse_catchup_events_sent_total").counter().count();
        assertThat(catchupCount).isEqualTo(1.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMissedEvents_withNoMissedEvents_shouldReturnZero() {
        String userId = "user1";
        String lastEventId = "1234567890-0";

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(Map.of());

        int eventsSent = catchupService.sendMissedEvents(userId, mockEmitter, lastEventId);

        assertThat(eventsSent).isEqualTo(0);
        verify(connectionManager, never()).sendToConnection(anyString(), any(), anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMissedEvents_whenRedisStreamFails_shouldReturnZeroAndLogError() {
        String userId = "user1";
        String lastEventId = "1234567890-0";

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenThrow(new RuntimeException("Redis connection failed"));

        int eventsSent = catchupService.sendMissedEvents(userId, mockEmitter, lastEventId);

        assertThat(eventsSent).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMissedEvents_whenDeserializationFails_shouldSkipMalformedEvent() {
        String userId = "user1";
        String lastEventId = "1234567890-0";

        Map<StreamMessageId, Map<String, String>> streamData = new HashMap<>();
        // Invalid JSON
        streamData.put(new StreamMessageId(1234567891L, 0), Map.of("event", "{invalid-json}"));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(streamData);

        int eventsSent = catchupService.sendMissedEvents(userId, mockEmitter, lastEventId);

        // Should return 0 as the malformed event is skipped
        assertThat(eventsSent).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void readMissedEvents_shouldParseStreamMessageIdCorrectly() throws Exception {
        String lastEventId = "1234567890-5";

        OutboxEnvelope envelope = createTestEnvelope("TicketCreated", 1L);

        Map<StreamMessageId, Map<String, String>> streamData = new HashMap<>();
        streamData.put(new StreamMessageId(1234567891L, 0), Map.of("event", objectMapper.writeValueAsString(envelope)));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(streamData);

        List<OutboxEnvelope> events = catchupService.readMissedEvents(lastEventId, 100);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).eventType()).isEqualTo("TicketCreated");
    }

    @Test
    @SuppressWarnings("unchecked")
    void readMissedEvents_withInvalidStreamId_shouldUseMinIdAsFallback() {
        String invalidStreamId = "invalid-format";

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(Map.of());

        List<OutboxEnvelope> events = catchupService.readMissedEvents(invalidStreamId, 100);

        // Should not throw, returns empty list
        assertThat(events).isEmpty();
        verify(mockStream).range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX));
    }

    @Test
    @SuppressWarnings("unchecked")
    void readMissedEvents_shouldRespectMaxEventsLimit() throws Exception {
        String lastEventId = "1234567890-0";
        int maxEvents = 10;

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(maxEvents), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(Map.of());

        catchupService.readMissedEvents(lastEventId, maxEvents);

        verify(mockStream).range(eq(maxEvents), any(StreamMessageId.class), eq(StreamMessageId.MAX));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendMissedEvents_shouldLogDurationMetrics() throws Exception {
        String userId = "user1";
        String lastEventId = "1234567890-0";

        OutboxEnvelope envelope = createTestEnvelope("TicketCreated", 1L);

        Map<StreamMessageId, Map<String, String>> streamData = new HashMap<>();
        streamData.put(new StreamMessageId(1234567891L, 0), Map.of("event", objectMapper.writeValueAsString(envelope)));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.range(eq(100), any(StreamMessageId.class), eq(StreamMessageId.MAX)))
                .thenReturn(streamData);

        long startTime = System.currentTimeMillis();
        catchupService.sendMissedEvents(userId, mockEmitter, lastEventId);
        long duration = System.currentTimeMillis() - startTime;

        // Catchup should complete quickly (< 1 second for small batches)
        assertThat(duration).isLessThan(1000);
    }

    private OutboxEnvelope createTestEnvelope(String eventType, Long version) {
        return new OutboxEnvelope(
                UUID.randomUUID(),
                "Ticket",
                eventType,
                version,
                Instant.now(),
                1,
                objectMapper.createObjectNode()
        );
    }
}
