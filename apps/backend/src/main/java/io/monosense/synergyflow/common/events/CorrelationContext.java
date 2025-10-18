package io.monosense.synergyflow.common.events;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Manages correlation and causation IDs in MDC (Mapped Diagnostic Context).
 *
 * <p>These IDs are propagated through:
 * <ul>
 *   <li>Request headers (X-Correlation-ID)</li>
 *   <li>SLF4J MDC for logging</li>
 *   <li>OpenTelemetry span attributes</li>
 *   <li>Event publication (event_publication table)</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * MDC is thread-local, safe for async event processing with @Async.
 */
public final class CorrelationContext {

    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String CAUSATION_ID_KEY = "causationId";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private CorrelationContext() {
        // Utility class
    }

    /**
     * Generate a new correlation ID (UUID v4).
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get current correlation ID from MDC, or generate new one if missing.
     */
    public static String getOrCreateCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
        return correlationId;
    }

    /**
     * Get current correlation ID from MDC (may be null).
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Set correlation ID in MDC.
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }

    /**
     * Get current causation ID from MDC (may be null).
     */
    public static String getCausationId() {
        return MDC.get(CAUSATION_ID_KEY);
    }

    /**
     * Set causation ID in MDC (usually set when consuming events).
     */
    public static void setCausationId(String causationId) {
        if (causationId != null && !causationId.isBlank()) {
            MDC.put(CAUSATION_ID_KEY, causationId);
        }
    }

    /**
     * Clear all correlation context from MDC.
     * Call this in finally blocks to prevent leakage.
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_KEY);
        MDC.remove(CAUSATION_ID_KEY);
    }
}
