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
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RedisStreamSubscriber}.
 *
 * @author monosense
 * @since 1.7
 */
@ExtendWith(MockitoExtension.class)
class RedisStreamSubscriberTest {

    private RedisStreamSubscriber subscriber;
    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private SseConnectionManager connectionManager;

    @Mock
    @SuppressWarnings("rawtypes")
    private RStream mockStream;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register JavaTimeModule for Instant serialization
        subscriber = new RedisStreamSubscriber(redissonClient, objectMapper, connectionManager, meterRegistry);

        // Set properties via reflection
        ReflectionTestUtils.setField(subscriber, "streamName", "domain-events");
        ReflectionTestUtils.setField(subscriber, "consumerGroup", "sse-gateways");
    }

    @Test
    @SuppressWarnings("unchecked")
    void start_shouldCreateConsumerGroup() {
        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        doNothing().when(mockStream).createGroup(any(StreamCreateGroupArgs.class));

        subscriber.start();

        verify(mockStream).createGroup(any(StreamCreateGroupArgs.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void start_whenConsumerGroupExists_shouldNotThrow() {
        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        doThrow(new RuntimeException("BUSYGROUP Consumer Group name already exists"))
                .when(mockStream).createGroup(any(StreamCreateGroupArgs.class));

        assertThatCode(() -> subscriber.start()).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void start_shouldRegisterMetrics() {
        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        doNothing().when(mockStream).createGroup(any(StreamCreateGroupArgs.class));

        subscriber.start();

        assertThat(meterRegistry.find("sse_events_consumed_total").counter()).isNotNull();
        assertThat(meterRegistry.find("sse_deserialization_errors_total").counter()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionLoop_shouldConsumeEventsAndBroadcast() throws Exception {
        OutboxEnvelope envelope = createTestEnvelope("TicketCreated");
        String eventJson = objectMapper.writeValueAsString(envelope);

        Map<StreamMessageId, Map<String, String>> messages = new HashMap<>();
        messages.put(new StreamMessageId(1L, 0), Map.of("event", eventJson));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(messages)
                .thenReturn(null); // Stop loop

        subscriber.start();

        // Give subscription loop time to process
        Thread.sleep(200);

        verify(connectionManager, timeout(1000)).broadcast(any(OutboxEnvelope.class), anyString());
        verify(mockStream, timeout(1000)).ack(eq("sse-gateways"), any(StreamMessageId.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionLoop_shouldIncrementConsumedCounter() throws Exception {
        OutboxEnvelope envelope = createTestEnvelope("TicketCreated");
        String eventJson = objectMapper.writeValueAsString(envelope);

        Map<StreamMessageId, Map<String, String>> messages = new HashMap<>();
        messages.put(new StreamMessageId(1L, 0), Map.of("event", eventJson));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(messages)
                .thenReturn(null); // Stop loop

        subscriber.start();

        // Give subscription loop time to process
        Thread.sleep(200);

        Double consumed = meterRegistry.get("sse_events_consumed_total").counter().count();
        assertThat(consumed).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionLoop_whenDeserializationFails_shouldIncrementErrorCounter() throws Exception {
        Map<StreamMessageId, Map<String, String>> messages = new HashMap<>();
        messages.put(new StreamMessageId(1L, 0), Map.of("event", "{invalid-json}"));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(messages)
                .thenReturn(null); // Stop loop

        subscriber.start();

        // Give subscription loop time to process
        Thread.sleep(200);

        Double errors = meterRegistry.get("sse_deserialization_errors_total").counter().count();
        assertThat(errors).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionLoop_whenDeserializationFails_shouldAckMessage() throws Exception {
        Map<StreamMessageId, Map<String, String>> messages = new HashMap<>();
        StreamMessageId messageId = new StreamMessageId(1L, 0);
        messages.put(messageId, Map.of("event", "{invalid-json}"));

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(messages)
                .thenReturn(null); // Stop loop

        subscriber.start();

        // Give subscription loop time to process
        Thread.sleep(200);

        // Message should be ACKed even if deserialization fails (to not block stream)
        verify(mockStream, timeout(1000)).ack(eq("sse-gateways"), eq(messageId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionLoop_whenEventFieldMissing_shouldAckAndSkip() throws Exception {
        Map<StreamMessageId, Map<String, String>> messages = new HashMap<>();
        StreamMessageId messageId = new StreamMessageId(1L, 0);
        messages.put(messageId, Map.of("other_field", "value")); // Missing 'event' field

        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(messages)
                .thenReturn(null); // Stop loop

        subscriber.start();

        // Give subscription loop time to process
        Thread.sleep(200);

        verify(mockStream, timeout(1000)).ack(eq("sse-gateways"), eq(messageId));
        verify(connectionManager, never()).broadcast(any(OutboxEnvelope.class), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void stop_shouldGracefullyShutdownSubscriber() throws InterruptedException {
        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        // Use lenient stubbing to avoid UnnecessaryStubbingException if read is not invoked
        lenient().when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(null); // Empty read

        subscriber.start();
        awaitSubscriberRunning();

        assertThatCode(() -> subscriber.stop()).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void stop_shouldTerminateWithinTimeout() throws InterruptedException {
        when(redissonClient.getStream("domain-events")).thenReturn(mockStream);
        // Use lenient stubbing to avoid UnnecessaryStubbingException if read is not invoked
        lenient().when(mockStream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(null); // Empty read

        subscriber.start();
        awaitSubscriberRunning();

        long startTime = System.currentTimeMillis();
        subscriber.stop();
        long duration = System.currentTimeMillis() - startTime;

        // Should terminate within 10 seconds (the awaitTermination timeout)
        assertThat(duration).isLessThan(10000);
    }

    private void awaitSubscriberRunning() throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            AtomicBoolean running = (AtomicBoolean) ReflectionTestUtils.getField(subscriber, "running");
            ExecutorService executor = (ExecutorService) ReflectionTestUtils.getField(subscriber, "executorService");
            if (running != null && running.get() && executor != null && !executor.isShutdown()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Subscriber did not start within expected time");
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
}
