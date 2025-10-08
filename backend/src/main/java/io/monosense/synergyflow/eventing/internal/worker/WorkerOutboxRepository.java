package io.monosense.synergyflow.eventing.internal.worker;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Worker-specific repository that executes locking queries against the transactional outbox.
 *
 * @since 1.6
 */
@Repository
@Profile("worker")
class WorkerOutboxRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    WorkerOutboxRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<OutboxMessage> findNextBatch(int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT id, aggregate_id, aggregate_type, event_type, version, occurred_at, event_payload FROM outbox " +
                                "WHERE processed_at IS NULL ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED")
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(this::mapRow)
                .collect(Collectors.toList());
    }

    long countUnprocessed() {
        Number result = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM outbox WHERE processed_at IS NULL")
            .getSingleResult();
        return result.longValue();
    }

    int markProcessed(Long id, Instant processedAt) {
        return entityManager.createNativeQuery("UPDATE outbox SET processed_at = :processedAt WHERE id = :id")
                .setParameter("processedAt", processedAt)
                .setParameter("id", id)
                .executeUpdate();
    }

    Long findMaxProcessedId() {
        Number result = (Number) entityManager.createNativeQuery(
                "SELECT MAX(id) FROM outbox WHERE processed_at IS NOT NULL")
            .getSingleResult();
        return result != null ? result.longValue() : null;
    }

    Long findCurrentMaxId() {
        Number result = (Number) entityManager.createNativeQuery(
                "SELECT MAX(id) FROM outbox")
            .getSingleResult();
        return result != null ? result.longValue() : null;
    }

    List<Long> findGapIdsUpTo(Long maxId) {
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery(
                        "SELECT id FROM outbox WHERE id <= :maxId AND processed_at IS NULL ORDER BY id")
                .setParameter("maxId", maxId)
                .getResultList();
        return rows.stream().map(Number::longValue).toList();
    }

    private OutboxMessage mapRow(Object[] row) {
        Long id = ((Number) row[0]).longValue();
        java.util.UUID aggregateId = (java.util.UUID) row[1];
        String aggregateType = (String) row[2];
        String eventType = (String) row[3];
        Long version = row[4] != null ? ((Number) row[4]).longValue() : null;
        java.sql.Timestamp occurredAtTimestamp = (java.sql.Timestamp) row[5];
        Instant occurredAt = occurredAtTimestamp != null ? occurredAtTimestamp.toInstant() : Instant.now();
        JsonNode payload = parseJson(row[6]);
        return new OutboxMessage(id, aggregateId, aggregateType, eventType, version, occurredAt, payload);
    }

    private JsonNode parseJson(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse outbox payload", e);
        }
    }
}
