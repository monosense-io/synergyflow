package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.internal.domain.Priority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for calculating SLA (Service Level Agreement) deadlines based on ticket priority.
 *
 * <p>This stateless service provides pure calculation logic for determining SLA {@code dueAt}
 * timestamps without any database access. It implements the priority-based SLA policy defined
 * in Epic 2 Tech Spec:</p>
 *
 * <ul>
 *   <li>CRITICAL: 2 hours</li>
 *   <li>HIGH: 4 hours</li>
 *   <li>MEDIUM: 8 hours</li>
 *   <li>LOW: 24 hours</li>
 * </ul>
 *
 * <p><strong>Design Rationale:</strong></p>
 * <ul>
 *   <li>Separation of concerns: calculator handles math, TicketService handles persistence</li>
 *   <li>Testability: pure function logic enables comprehensive unit testing without database</li>
 *   <li>Clock-time arithmetic: MVP uses simple duration addition (no business calendar awareness)</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * Instant ticketCreatedAt = Instant.now();
 * Priority priority = Priority.CRITICAL;
 * Instant dueAt = slaCalculator.calculateDueAt(priority, ticketCreatedAt);
 * // dueAt = ticketCreatedAt + 2 hours
 * }</pre>
 *
 * <p><strong>Future Enhancements:</strong> Business calendar support (exclude weekends,
 * holidays, non-business hours) will require injecting a calendar service and more complex
 * logic. The current implementation provides a foundation for that evolution.</p>
 *
 * @author monosense
 * @since 2.3
 */
@Service
public class SlaCalculator {

    /**
     * Calculates the SLA deadline for a ticket based on its priority.
     *
     * <p>Adds a priority-based duration to the start time using clock-time arithmetic:</p>
     * <ul>
     *   <li>CRITICAL: {@code startTime + 2 hours}</li>
     *   <li>HIGH: {@code startTime + 4 hours}</li>
     *   <li>MEDIUM: {@code startTime + 8 hours}</li>
     *   <li>LOW: {@code startTime + 24 hours}</li>
     * </ul>
     *
     * <p><strong>Time Zone Independence:</strong> Both input and output are {@link Instant}
     * (UTC-based), ensuring consistent behavior regardless of server or client time zones.</p>
     *
     * <p><strong>Edge Cases:</strong></p>
     * <ul>
     *   <li>Null priority: throws {@code IllegalArgumentException}</li>
     *   <li>Past startTime: still calculates deadline (may already be breached)</li>
     *   <li>Far future startTime: no validation (system allows scheduling future tickets)</li>
     * </ul>
     *
     * @param priority the priority level determining SLA duration (CRITICAL, HIGH, MEDIUM, or LOW)
     * @param startTime the reference timestamp to add duration to (typically ticket creation time)
     * @return the calculated SLA deadline as {@code startTime + priority-based duration}
     * @throws IllegalArgumentException if {@code priority} is null
     * @since 2.3
     */
    public Instant calculateDueAt(Priority priority, Instant startTime) {
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null for SLA calculation");
        }

        Duration slaDuration = switch (priority) {
            case CRITICAL -> Duration.ofHours(2);
            case HIGH -> Duration.ofHours(4);
            case MEDIUM -> Duration.ofHours(8);
            case LOW -> Duration.ofHours(24);
        };

        return startTime.plus(slaDuration);
    }
}
