package io.monosense.synergyflow.eventing.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.monosense.synergyflow.eventing.api.DuplicateEventException;
import io.monosense.synergyflow.eventing.api.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventPublisherImpl.
 */
class EventPublisherImplTest {

    private OutboxRepository repository;
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private EventPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxRepository.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        meterRegistry = new SimpleMeterRegistry();
        publisher = new EventPublisherImpl(repository, objectMapper, meterRegistry);
    }

    @Test
    void publishSuccessfully() {
        // Given
        UUID aggregateId = UUID.randomUUID();
        String aggregateType = "TICKET";
        String eventType = "TicketCreated";
        Long version = 1L;
        Instant occurredAt = Instant.now();
        JsonNode payload = objectMapper.createObjectNode().put("title", "Test");

        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        publisher.publish(aggregateId, aggregateType, eventType, version, occurredAt, payload);

        // Then
        verify(repository, times(1)).save(any(OutboxEvent.class));

        // Verify metrics were recorded
        assertThat(meterRegistry.counter("outbox_published_total",
                "aggregate_type", aggregateType,
                "event_type", eventType,
                "result", "success").count()).isEqualTo(1.0);
    }

    @Test
    void handlesDuplicateEventGracefully() {
        // Given
        UUID aggregateId = UUID.randomUUID();
        DataIntegrityViolationException duplicateException = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"idx_outbox_idempotency\""
        );

        when(repository.save(any(OutboxEvent.class))).thenThrow(duplicateException);

        // When/Then
        assertThatThrownBy(() -> publisher.publish(
                aggregateId,
                "TICKET",
                "TicketCreated",
                1L,
                Instant.now(),
                objectMapper.createObjectNode()
        ))
                .isInstanceOf(DuplicateEventException.class)
                .hasMessageContaining("Event already exists for aggregate");

        // Verify error metric was recorded
        assertThat(meterRegistry.counter("outbox_publish_errors_total",
                "aggregate_type", "TICKET",
                "event_type", "TicketCreated",
                "result", "duplicate").count()).isEqualTo(1.0);
    }

    @Test
    void propagatesOtherDataIntegrityViolations() {
        // Given
        DataIntegrityViolationException otherException = new DataIntegrityViolationException(
                "some other constraint violation"
        );

        when(repository.save(any(OutboxEvent.class))).thenThrow(otherException);

        // When/Then
        assertThatThrownBy(() -> publisher.publish(
                UUID.randomUUID(),
                "TICKET",
                "TicketCreated",
                1L,
                Instant.now(),
                objectMapper.createObjectNode()
        ))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Verify error metric was recorded
        assertThat(meterRegistry.counter("outbox_publish_errors_total",
                "aggregate_type", "TICKET",
                "event_type", "TicketCreated",
                "result", "data_integrity").count()).isEqualTo(1.0);
    }

    @Test
    void detectsDuplicateViaSqlState23505() {
        // Given
        UUID aggregateId = UUID.randomUUID();
        SQLException sqlException = new SQLException("unique violation", "23505");
        DataIntegrityViolationException duplicate = new DataIntegrityViolationException("duplicate", sqlException);

        when(repository.save(any(OutboxEvent.class))).thenThrow(duplicate);

        // When/Then
        assertThatThrownBy(() -> publisher.publish(
                aggregateId,
                "TICKET",
                "TicketCreated",
                1L,
                Instant.now(),
                objectMapper.createObjectNode()
        ))
                .isInstanceOf(DuplicateEventException.class);
    }

    @Test
    void retriesOnTransientSerializationFailureThenSucceeds() {
        // Given
        SQLException sqlException = new SQLException("serialization failure", "40001");
        DataIntegrityViolationException transientEx = new DataIntegrityViolationException("transient", sqlException);

        when(repository.save(any(OutboxEvent.class)))
                .thenThrow(transientEx) // first attempt fails
                .thenAnswer(invocation -> invocation.getArgument(0)); // second attempt succeeds

        // When
        publisher.publish(
                UUID.randomUUID(),
                "TICKET",
                "TicketCreated",
                1L,
                Instant.now(),
                objectMapper.createObjectNode()
        );

        // Then - verify two attempts
        verify(repository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void recordsLatencyMetrics() throws Exception {
        // Given
        UUID aggregateId = UUID.randomUUID();
        when(repository.save(any(OutboxEvent.class))).thenAnswer(invocation -> {
            // Simulate some processing time
            Thread.sleep(10);
            return invocation.getArgument(0);
        });

        // When
        publisher.publish(
                aggregateId,
                "TICKET",
                "TicketCreated",
                1L,
                Instant.now(),
                objectMapper.createObjectNode()
        );

        // Then
        assertThat(meterRegistry.timer("outbox_publish_latency_ms",
                "aggregate_type", "TICKET",
                "event_type", "TicketCreated").count()).isEqualTo(1L);
    }
}
