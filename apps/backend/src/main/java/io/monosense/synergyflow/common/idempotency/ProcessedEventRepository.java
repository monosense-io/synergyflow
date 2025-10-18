package io.monosense.synergyflow.common.idempotency;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for tracking processed events (idempotency).
 *
 * <p>Consumers check this table before processing events to prevent duplicate side-effects.
 * Uses JDBC directly for lightweight idempotency checks.
 */
@Repository
public class ProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Check if an event has already been processed by a specific consumer.
     *
     * @param idempotencyKey Unique key: eventId-listenerClassName
     * @return true if already processed, false otherwise
     */
    public boolean isProcessed(String idempotencyKey) {
        String sql = "SELECT COUNT(*) FROM processed_events WHERE idempotency_key = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, idempotencyKey);
        return count != null && count > 0;
    }

    /**
     * Mark an event as processed by a specific consumer.
     *
     * @param idempotencyKey Unique key: eventId-listenerClassName
     * @param eventType Fully qualified event class name
     * @param correlationId Correlation ID from event
     * @param causationId Causation ID from event
     */
    public void markAsProcessed(String idempotencyKey, String eventType, String correlationId, String causationId) {
        String dbProduct = "";
        try {
            var con = DataSourceUtils.getConnection(jdbcTemplate.getDataSource());
            dbProduct = con.getMetaData().getDatabaseProductName();
        } catch (Exception ignored) {}

        String sql;
        var nowTs = java.sql.Timestamp.from(Instant.now());
        if (dbProduct.toLowerCase().contains("h2")) {
            sql = "MERGE INTO processed_events KEY(idempotency_key) VALUES (?,?,?,?, ?)";
            jdbcTemplate.update(sql, idempotencyKey, eventType, correlationId, causationId, nowTs);
        } else {
            sql = "INSERT INTO processed_events (idempotency_key, event_type, correlation_id, causation_id, processed_at) VALUES (?, ?, ?, ?, ?) ON CONFLICT (idempotency_key) DO NOTHING";
            jdbcTemplate.update(sql, idempotencyKey, eventType, correlationId, causationId, nowTs);
        }
    }

    /**
     * Generate idempotency key from event ID and listener class name.
     *
     * @param eventId Event identifier
     * @param listenerClass Consumer class
     * @return Idempotency key
     */
    public static String generateKey(String eventId, Class<?> listenerClass) {
        return eventId + "-" + listenerClass.getSimpleName();
    }
}
