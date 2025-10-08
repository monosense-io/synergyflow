package io.monosense.synergyflow.eventing.internal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStreamPublisherTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RStream<String, String> stream;

    private ObjectMapper objectMapper;
    private SimpleMeterRegistry meterRegistry;
    private MutableClock clock;
    private RedisStreamPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        meterRegistry = new SimpleMeterRegistry();
        clock = new MutableClock(Instant.parse("2025-10-07T10:00:00Z"));
        publisher = new RedisStreamPublisher(redissonClient, objectMapper, meterRegistry, clock);
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "streamName", "domain-events");
        when(redissonClient.<String, String>getStream(anyString())).thenReturn(stream);
    }

    @Test
    void schedulesBackoffWithoutBlockingAndRetriesAfterDelay() {
        UUID aggregateId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("title", "Test Ticket");
        OutboxMessage message = new OutboxMessage(1L, aggregateId, "Ticket", "TicketCreated", 1L, java.time.Instant.now(), payload);

        when(stream.add(any()))
                .thenThrow(new RuntimeException("redis down"))
                .thenReturn(new org.redisson.api.StreamMessageId(0, 1));

        Throwable first = org.assertj.core.api.Assertions.catchThrowable(() -> publisher.publish(message));
        assertThat(first).isNotNull();
        assertThat(first)
                .isInstanceOf(RedisPublishBackoffException.class)
                .hasMessageContaining("until");

        Throwable second = org.assertj.core.api.Assertions.catchThrowable(() -> publisher.publish(message));
        assertThat(second).isInstanceOf(RedisPublishBackoffException.class);

        clock.advance(Duration.ofMillis(1000));

        publisher.publish(message);

        verify(stream, times(2)).add(org.mockito.ArgumentMatchers.<StreamAddArgs<String, String>>any());
        Counter success = meterRegistry.find("outbox_events_published_total").counter();
        assertThat(success).isNotNull();
        assertThat(success.count()).isEqualTo(1.0);
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
