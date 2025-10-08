package io.monosense.synergyflow.eventing.internal.worker;

import java.time.Instant;

/**
 * Signal indicating that a Redis publish attempt is postponed until the configured backoff elapses.
 */
class RedisPublishBackoffException extends RuntimeException {

    private final long eventId;
    private final Instant nextAttemptAt;

    RedisPublishBackoffException(long eventId, Instant nextAttemptAt, Throwable cause) {
        super("Redis publish backoff active for event " + eventId + " until " + nextAttemptAt, cause);
        this.eventId = eventId;
        this.nextAttemptAt = nextAttemptAt;
    }

    long eventId() {
        return eventId;
    }

    Instant nextAttemptAt() {
        return nextAttemptAt;
    }
}

