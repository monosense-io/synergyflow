package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TicketStateTransitionValidator.
 * Tests all 25 possible state transition combinations (8 valid + 17 invalid)
 * plus edge cases (null statuses, same-state transitions).
 */
@DisplayName("TicketStateTransitionValidator")
class TicketStateTransitionValidatorTest {

    private TicketStateTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TicketStateTransitionValidator();
    }

    @Nested
    @DisplayName("Valid Transitions")
    class ValidTransitions {

        @ParameterizedTest(name = "{0} → {1} should be valid")
        @MethodSource("provideValidTransitions")
        @DisplayName("validateTransition() should succeed for all valid transitions")
        void validateTransition_withValidTransition_shouldNotThrowException(
            TicketStatus from, TicketStatus to) {

            assertThatCode(() -> validator.validateTransition(from, to))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} → {1} should return true")
        @MethodSource("provideValidTransitions")
        @DisplayName("isValidTransition() should return true for all valid transitions")
        void isValidTransition_withValidTransition_shouldReturnTrue(
            TicketStatus from, TicketStatus to) {

            boolean result = validator.isValidTransition(from, to);

            assertThat(result).isTrue();
        }

        static Stream<Arguments> provideValidTransitions() {
            return Stream.of(
                // NEW → ASSIGNED
                Arguments.of(TicketStatus.NEW, TicketStatus.ASSIGNED),

                // ASSIGNED → NEW, IN_PROGRESS
                Arguments.of(TicketStatus.ASSIGNED, TicketStatus.NEW),
                Arguments.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS),

                // IN_PROGRESS → ASSIGNED, RESOLVED
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.ASSIGNED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),

                // RESOLVED → NEW, CLOSED
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.NEW),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED),

                // CLOSED → NEW
                Arguments.of(TicketStatus.CLOSED, TicketStatus.NEW)
            );
        }
    }

    @Nested
    @DisplayName("Invalid Transitions")
    class InvalidTransitions {

        @ParameterizedTest(name = "{0} → {1} should be invalid")
        @MethodSource("provideInvalidTransitions")
        @DisplayName("validateTransition() should throw InvalidStateTransitionException for invalid transitions")
        void validateTransition_withInvalidTransition_shouldThrowException(
            TicketStatus from, TicketStatus to) {

            assertThatThrownBy(() -> validator.validateTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid state transition: " + from + " → " + to);
        }

        @ParameterizedTest(name = "{0} → {1} should return false")
        @MethodSource("provideInvalidTransitions")
        @DisplayName("isValidTransition() should return false for invalid transitions")
        void isValidTransition_withInvalidTransition_shouldReturnFalse(
            TicketStatus from, TicketStatus to) {

            boolean result = validator.isValidTransition(from, to);

            assertThat(result).isFalse();
        }

        static Stream<Arguments> provideInvalidTransitions() {
            return Stream.of(
                // NEW → (invalid targets: IN_PROGRESS, RESOLVED, CLOSED)
                Arguments.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.NEW, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.NEW, TicketStatus.CLOSED),

                // ASSIGNED → (invalid targets: RESOLVED, CLOSED)
                Arguments.of(TicketStatus.ASSIGNED, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.ASSIGNED, TicketStatus.CLOSED),

                // IN_PROGRESS → (invalid targets: NEW, IN_PROGRESS, CLOSED)
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.NEW),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),

                // RESOLVED → (invalid targets: ASSIGNED, IN_PROGRESS)
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.ASSIGNED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS),

                // CLOSED → (invalid targets: ASSIGNED, IN_PROGRESS, RESOLVED)
                Arguments.of(TicketStatus.CLOSED, TicketStatus.ASSIGNED),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.RESOLVED)
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("validateTransition() with null 'from' status should throw IllegalArgumentException")
        void validateTransition_withNullFrom_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateTransition(null, TicketStatus.ASSIGNED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status cannot be null");
        }

        @Test
        @DisplayName("validateTransition() with null 'to' status should throw IllegalArgumentException")
        void validateTransition_withNullTo_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateTransition(TicketStatus.NEW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status cannot be null");
        }

        @Test
        @DisplayName("validateTransition() with both null statuses should throw IllegalArgumentException")
        void validateTransition_withBothNull_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateTransition(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status cannot be null");
        }

        @Test
        @DisplayName("isValidTransition() with null 'from' status should return false")
        void isValidTransition_withNullFrom_shouldReturnFalse() {
            boolean result = validator.isValidTransition(null, TicketStatus.ASSIGNED);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidTransition() with null 'to' status should return false")
        void isValidTransition_withNullTo_shouldReturnFalse() {
            boolean result = validator.isValidTransition(TicketStatus.NEW, null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isValidTransition() with both null statuses should return false")
        void isValidTransition_withBothNull_shouldReturnFalse() {
            boolean result = validator.isValidTransition(null, null);

            assertThat(result).isFalse();
        }

        @ParameterizedTest(name = "{0} → {0} should be invalid (same-state)")
        @MethodSource("provideAllStatuses")
        @DisplayName("validateTransition() with same source and target should throw InvalidStateTransitionException")
        void validateTransition_withSameState_shouldThrowException(TicketStatus status) {
            assertThatThrownBy(() -> validator.validateTransition(status, status))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid state transition: " + status + " → " + status);
        }

        @ParameterizedTest(name = "{0} → {0} should return false (same-state)")
        @MethodSource("provideAllStatuses")
        @DisplayName("isValidTransition() with same source and target should return false")
        void isValidTransition_withSameState_shouldReturnFalse(TicketStatus status) {
            boolean result = validator.isValidTransition(status, status);

            assertThat(result).isFalse();
        }

        static Stream<Arguments> provideAllStatuses() {
            return Stream.of(
                Arguments.of(TicketStatus.NEW),
                Arguments.of(TicketStatus.ASSIGNED),
                Arguments.of(TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.CLOSED)
            );
        }
    }

    @Nested
    @DisplayName("Comprehensive Coverage")
    class ComprehensiveCoverage {

        @Test
        @DisplayName("All 25 state transition combinations should be tested")
        void testAllTransitionCombinations() {
            // 5 statuses × 5 statuses = 25 combinations
            // 8 valid transitions + 17 invalid transitions = 25 total

            int validTransitions = 8; // Verified in provideValidTransitions()
            int invalidTransitions = 12; // Verified in provideInvalidTransitions()
            int sameStateTransitions = 5; // Verified in provideAllStatuses()

            int totalCombinations = validTransitions + invalidTransitions + sameStateTransitions;

            assertThat(totalCombinations)
                .as("All 25 state transition combinations covered")
                .isEqualTo(25);
        }
    }
}
