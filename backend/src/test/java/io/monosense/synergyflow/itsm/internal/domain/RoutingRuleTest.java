package io.monosense.synergyflow.itsm.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RoutingRule} entity.
 *
 * <p>Tests verify entity construction, field initialization, lifecycle callback behavior,
 * and domain methods for the RoutingRule entity without requiring database persistence.</p>
 *
 * @author monosense
 * @since 2.1
 */
@DisplayName("RoutingRule Entity Tests")
class RoutingRuleTest {

    @Test
    @DisplayName("Constructor should initialize all fields correctly")
    void testConstructorInitializesFields() {
        // Arrange
        String ruleName = "Critical Incidents to Network Team";
        ConditionType conditionType = ConditionType.PRIORITY;
        String conditionValue = "CRITICAL";
        UUID targetTeamId = UUID.randomUUID();
        UUID targetAgentId = null;
        int priority = 1;
        boolean enabled = true;

        // Act
        RoutingRule rule = new RoutingRule(ruleName, conditionType, conditionValue,
                targetTeamId, targetAgentId, priority, enabled);

        // Assert
        assertThat(rule.getRuleName()).isEqualTo(ruleName);
        assertThat(rule.getConditionType()).isEqualTo(conditionType);
        assertThat(rule.getConditionValue()).isEqualTo(conditionValue);
        assertThat(rule.getTargetTeamId()).isEqualTo(targetTeamId);
        assertThat(rule.getTargetAgentId()).isNull();
        assertThat(rule.getPriority()).isEqualTo(1);
        assertThat(rule.isEnabled()).isTrue();
        assertThat(rule.getId()).isNull(); // ID not set until persistence
        assertThat(rule.getVersion()).isEqualTo(1L); // Default version
        assertThat(rule.getCreatedAt()).isNull(); // Timestamp not set until @PrePersist
        assertThat(rule.getUpdatedAt()).isNull(); // Timestamp not set until @PrePersist
    }

    @Test
    @DisplayName("Constructor should handle agent-specific routing (no targetTeamId)")
    void testConstructorAgentRouting() {
        // Arrange & Act
        RoutingRule rule = new RoutingRule(
                "High Priority to Agent John",
                ConditionType.PRIORITY,
                "HIGH",
                null,
                UUID.randomUUID(),
                2,
                true
        );

        // Assert
        assertThat(rule.getTargetTeamId()).isNull();
        assertThat(rule.getTargetAgentId()).isNotNull();
    }

    @Test
    @DisplayName("Constructor should handle ROUND_ROBIN with null conditionValue")
    void testConstructorRoundRobin() {
        // Arrange & Act
        RoutingRule rule = new RoutingRule(
                "Round Robin to L1 Support",
                ConditionType.ROUND_ROBIN,
                null,
                UUID.randomUUID(),
                null,
                10,
                true
        );

        // Assert
        assertThat(rule.getConditionType()).isEqualTo(ConditionType.ROUND_ROBIN);
        assertThat(rule.getConditionValue()).isNull();
    }

    @Test
    @DisplayName("Constructor should create disabled rule when enabled=false")
    void testConstructorDisabledRule() {
        // Arrange & Act
        RoutingRule rule = new RoutingRule(
                "Temporarily Disabled Rule",
                ConditionType.CATEGORY,
                "Network",
                UUID.randomUUID(),
                null,
                5,
                false
        );

        // Assert
        assertThat(rule.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("@PrePersist should set createdAt and updatedAt timestamps")
    void testPrePersistSetsTimestamps() throws Exception {
        // Arrange
        RoutingRule rule = new RoutingRule(
                "Test Rule",
                ConditionType.CATEGORY,
                "Hardware",
                UUID.randomUUID(),
                null,
                1,
                true
        );
        Instant beforeCreate = Instant.now();

        // Act - manually invoke the @PrePersist callback
        Method onCreate = RoutingRule.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(rule);

        Instant afterCreate = Instant.now();

        // Assert
        assertThat(rule.getCreatedAt()).isNotNull();
        assertThat(rule.getUpdatedAt()).isNotNull();
        assertThat(rule.getCreatedAt()).isBetween(beforeCreate, afterCreate);
        assertThat(rule.getUpdatedAt()).isBetween(beforeCreate, afterCreate);
        assertThat(rule.getCreatedAt()).isEqualTo(rule.getUpdatedAt()); // Both set to same instant on create
    }

    @Test
    @DisplayName("@PreUpdate should update only updatedAt timestamp")
    void testPreUpdateUpdatesTimestamp() throws Exception {
        // Arrange
        RoutingRule rule = new RoutingRule(
                "Test Rule",
                ConditionType.PRIORITY,
                "MEDIUM",
                UUID.randomUUID(),
                null,
                3,
                true
        );

        // Invoke @PrePersist first
        Method onCreate = RoutingRule.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(rule);

        Instant originalCreatedAt = rule.getCreatedAt();
        Instant originalUpdatedAt = rule.getUpdatedAt();

        Thread.sleep(10); // Ensure time difference

        // Act - manually invoke the @PreUpdate callback
        Method onUpdate = RoutingRule.class.getDeclaredMethod("onUpdate");
        onUpdate.setAccessible(true);
        onUpdate.invoke(rule);

        // Assert
        assertThat(rule.getCreatedAt()).isEqualTo(originalCreatedAt); // createdAt unchanged
        assertThat(rule.getUpdatedAt()).isAfter(originalUpdatedAt); // updatedAt incremented
    }

    @Test
    @DisplayName("enable() method should set enabled to true")
    void testEnableMethod() {
        // Arrange
        RoutingRule rule = new RoutingRule(
                "Test Rule",
                ConditionType.CATEGORY,
                "Software",
                UUID.randomUUID(),
                null,
                1,
                false // Start disabled
        );

        // Act
        rule.enable();

        // Assert
        assertThat(rule.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("disable() method should set enabled to false")
    void testDisableMethod() {
        // Arrange
        RoutingRule rule = new RoutingRule(
                "Test Rule",
                ConditionType.SUBCATEGORY,
                "Database",
                UUID.randomUUID(),
                null,
                2,
                true // Start enabled
        );

        // Act
        rule.disable();

        // Assert
        assertThat(rule.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("updatePriority() method should change priority value")
    void testUpdatePriorityMethod() {
        // Arrange
        RoutingRule rule = new RoutingRule(
                "Test Rule",
                ConditionType.PRIORITY,
                "LOW",
                UUID.randomUUID(),
                null,
                10,
                true
        );

        // Act
        rule.updatePriority(1);

        // Assert
        assertThat(rule.getPriority()).isEqualTo(1);
    }

    @Test
    @DisplayName("Constructor should handle zero priority")
    void testConstructorZeroPriority() {
        // Arrange & Act
        RoutingRule rule = new RoutingRule(
                "Highest Priority Rule",
                ConditionType.CATEGORY,
                "Critical Infrastructure",
                UUID.randomUUID(),
                null,
                0, // Lowest number = highest priority
                true
        );

        // Assert
        assertThat(rule.getPriority()).isEqualTo(0);
    }

    @Test
    @DisplayName("Constructor should handle negative priority")
    void testConstructorNegativePriority() {
        // Arrange & Act
        RoutingRule rule = new RoutingRule(
                "Negative Priority Rule",
                ConditionType.PRIORITY,
                "CRITICAL",
                UUID.randomUUID(),
                null,
                -1,
                true
        );

        // Assert
        assertThat(rule.getPriority()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Constructor should handle all ConditionType enum values")
    void testConstructorAllConditionTypes() {
        // Test CATEGORY
        RoutingRule categoryRule = new RoutingRule("Category Rule", ConditionType.CATEGORY,
                "Network", UUID.randomUUID(), null, 1, true);
        assertThat(categoryRule.getConditionType()).isEqualTo(ConditionType.CATEGORY);

        // Test PRIORITY
        RoutingRule priorityRule = new RoutingRule("Priority Rule", ConditionType.PRIORITY,
                "HIGH", UUID.randomUUID(), null, 2, true);
        assertThat(priorityRule.getConditionType()).isEqualTo(ConditionType.PRIORITY);

        // Test SUBCATEGORY
        RoutingRule subcategoryRule = new RoutingRule("Subcategory Rule", ConditionType.SUBCATEGORY,
                "VPN Access", UUID.randomUUID(), null, 3, true);
        assertThat(subcategoryRule.getConditionType()).isEqualTo(ConditionType.SUBCATEGORY);

        // Test ROUND_ROBIN
        RoutingRule roundRobinRule = new RoutingRule("Round Robin Rule", ConditionType.ROUND_ROBIN,
                null, UUID.randomUUID(), null, 4, true);
        assertThat(roundRobinRule.getConditionType()).isEqualTo(ConditionType.ROUND_ROBIN);
    }
}
