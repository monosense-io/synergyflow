package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.internal.domain.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SlaCalculator}.
 *
 * <p>Tests the priority-based SLA deadline calculation logic including:
 * <ul>
 *   <li>All four priority levels (CRITICAL, HIGH, MEDIUM, LOW)</li>
 *   <li>Null priority validation</li>
 *   <li>Time zone independence (UTC-based Instant behavior)</li>
 * </ul>
 *
 * @author monosense
 * @since 2.3
 */
class SlaCalculatorTest {

    private SlaCalculator slaCalculator;

    @BeforeEach
    void setUp() {
        slaCalculator = new SlaCalculator();
    }

    /**
     * UT-SLA-1: CRITICAL priority → startTime + 2 hours
     */
    @Test
    void calculateDueAt_criticalPriority_returns2Hours() {
        // Given
        Instant startTime = Instant.parse("2025-10-09T10:00:00Z");
        Priority priority = Priority.CRITICAL;

        // When
        Instant dueAt = slaCalculator.calculateDueAt(priority, startTime);

        // Then
        Instant expected = startTime.plus(Duration.ofHours(2));
        assertThat(dueAt).isEqualTo(expected);
        assertThat(dueAt).isEqualTo(Instant.parse("2025-10-09T12:00:00Z"));
    }

    /**
     * UT-SLA-2: HIGH priority → startTime + 4 hours
     */
    @Test
    void calculateDueAt_highPriority_returns4Hours() {
        // Given
        Instant startTime = Instant.parse("2025-10-09T10:00:00Z");
        Priority priority = Priority.HIGH;

        // When
        Instant dueAt = slaCalculator.calculateDueAt(priority, startTime);

        // Then
        Instant expected = startTime.plus(Duration.ofHours(4));
        assertThat(dueAt).isEqualTo(expected);
        assertThat(dueAt).isEqualTo(Instant.parse("2025-10-09T14:00:00Z"));
    }

    /**
     * UT-SLA-3: MEDIUM priority → startTime + 8 hours
     */
    @Test
    void calculateDueAt_mediumPriority_returns8Hours() {
        // Given
        Instant startTime = Instant.parse("2025-10-09T10:00:00Z");
        Priority priority = Priority.MEDIUM;

        // When
        Instant dueAt = slaCalculator.calculateDueAt(priority, startTime);

        // Then
        Instant expected = startTime.plus(Duration.ofHours(8));
        assertThat(dueAt).isEqualTo(expected);
        assertThat(dueAt).isEqualTo(Instant.parse("2025-10-09T18:00:00Z"));
    }

    /**
     * UT-SLA-4: LOW priority → startTime + 24 hours
     */
    @Test
    void calculateDueAt_lowPriority_returns24Hours() {
        // Given
        Instant startTime = Instant.parse("2025-10-09T10:00:00Z");
        Priority priority = Priority.LOW;

        // When
        Instant dueAt = slaCalculator.calculateDueAt(priority, startTime);

        // Then
        Instant expected = startTime.plus(Duration.ofHours(24));
        assertThat(dueAt).isEqualTo(expected);
        assertThat(dueAt).isEqualTo(Instant.parse("2025-10-10T10:00:00Z"));
    }

    /**
     * UT-SLA-5: Null priority → throws IllegalArgumentException
     */
    @Test
    void calculateDueAt_nullPriority_throwsException() {
        // Given
        Instant startTime = Instant.now();
        Priority priority = null;

        // When/Then
        assertThatThrownBy(() -> slaCalculator.calculateDueAt(priority, startTime))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Priority cannot be null");
    }

    /**
     * UT-SLA-6: Time zone independence → calculation works correctly with different Instant values
     *
     * <p>Tests that SLA calculation is time zone agnostic by using Instant (UTC-based).
     * Verifies that the same duration is added regardless of what time zone the original
     * timestamp was created in.</p>
     */
    @Test
    void calculateDueAt_timeZoneIndependence_worksCorrectly() {
        // Given: Two Instants representing the same moment in different time zones
        // 10:00 AM in New York (UTC-4) = 2:00 PM UTC
        ZonedDateTime newYorkTime = ZonedDateTime.of(2025, 10, 9, 10, 0, 0, 0, ZoneId.of("America/New_York"));
        Instant startTime1 = newYorkTime.toInstant();

        // 10:00 AM in Tokyo (UTC+9) = 1:00 AM UTC
        ZonedDateTime tokyoTime = ZonedDateTime.of(2025, 10, 9, 10, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        Instant startTime2 = tokyoTime.toInstant();

        Priority priority = Priority.CRITICAL;

        // When: Calculate SLA deadline for both instants
        Instant dueAt1 = slaCalculator.calculateDueAt(priority, startTime1);
        Instant dueAt2 = slaCalculator.calculateDueAt(priority, startTime2);

        // Then: Both should have exactly 2 hours added to their UTC representation
        assertThat(Duration.between(startTime1, dueAt1)).isEqualTo(Duration.ofHours(2));
        assertThat(Duration.between(startTime2, dueAt2)).isEqualTo(Duration.ofHours(2));

        // Verify the specific UTC times
        assertThat(dueAt1).isEqualTo(Instant.parse("2025-10-09T16:00:00Z")); // 2 PM + 2h = 4 PM UTC
        assertThat(dueAt2).isEqualTo(Instant.parse("2025-10-09T03:00:00Z")); // 1 AM + 2h = 3 AM UTC
    }

    /**
     * Additional edge case: Past start time still calculates correctly
     *
     * <p>Verifies that the calculator doesn't perform validation on whether the start time
     * is in the past, as the system should support analyzing historical tickets.</p>
     */
    @Test
    void calculateDueAt_pastStartTime_calculatesCorrectly() {
        // Given: A start time from 1 year ago
        Instant pastStartTime = Instant.now().minus(Duration.ofDays(365));
        Priority priority = Priority.HIGH;

        // When
        Instant dueAt = slaCalculator.calculateDueAt(priority, pastStartTime);

        // Then: Deadline is calculated correctly (even though it's also in the past)
        assertThat(Duration.between(pastStartTime, dueAt)).isEqualTo(Duration.ofHours(4));
    }

    /**
     * Additional edge case: Future start time still calculates correctly
     *
     * <p>Verifies that the calculator supports scheduling future tickets with SLAs.</p>
     */
    @Test
    void calculateDueAt_futureStartTime_calculatesCorrectly() {
        // Given: A start time 1 week in the future
        Instant futureStartTime = Instant.now().plus(Duration.ofDays(7));
        Priority priority = Priority.MEDIUM;

        // When
        Instant dueAt = slaCalculator.calculateDueAt(priority, futureStartTime);

        // Then: Deadline is calculated correctly
        assertThat(Duration.between(futureStartTime, dueAt)).isEqualTo(Duration.ofHours(8));
    }
}
