package io.monosense.synergyflow.eventing.internal.worker;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight representation of an outbox row used by the worker pipeline.
 * <p>
 * Contains all fields necessary to construct a complete {@link io.monosense.synergyflow.eventing.api.OutboxEnvelope}
 * for publishing to Redis Stream and downstream consumers.
 * </p>
 *
 * @since 1.6
 */
record OutboxMessage(Long id,
                     UUID aggregateId,
                     String aggregateType,
                     String eventType,
                     Long version,
                     Instant occurredAt,
                     JsonNode payload) {
}
