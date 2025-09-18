package io.monosense.synergyflow.user;

import java.time.Instant;

/**
 * Event published when a user is created.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public record UserCreatedEvent(
    String userId,
    String username,
    Instant createdAt
) {
}