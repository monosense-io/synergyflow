package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.ConditionType;
import io.monosense.synergyflow.itsm.internal.domain.RoutingRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RoutingRuleRepository custom query methods.
 *
 * <p>Uses Testcontainers with real PostgreSQL container per ADR-009.
 * Tests the filtering and ordering of routing rules by enabled status and priority.</p>
 *
 * @author monosense
 * @since 2.1
 */
@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
@Testcontainers
@Transactional
@ActiveProfiles("test")
class RoutingRuleRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RoutingRuleRepository routingRuleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertTeam(UUID teamId, String teamName) {
        jdbcTemplate.update(
                "INSERT INTO teams (id, name, description, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, now(), now())",
                teamId, teamName, "Test team for routing rules", true
        );
    }

    @Test
    void findByEnabledTrueOrderByPriorityAsc_shouldReturnEnabledRulesInPriorityOrder() {
        // Given: Create routing rules with different priorities and enabled states
        UUID teamId = UUID.randomUUID();
        insertTeam(teamId, "Routing Team A");

        RoutingRule rule1 = new RoutingRule("Rule 1", ConditionType.CATEGORY, "Network",
                teamId, null, 10, true);
        RoutingRule rule2 = new RoutingRule("Rule 2", ConditionType.PRIORITY, "CRITICAL",
                teamId, null, 5, true);
        RoutingRule rule3 = new RoutingRule("Rule 3", ConditionType.SUBCATEGORY, "Hardware",
                teamId, null, 15, false); // Disabled
        RoutingRule rule4 = new RoutingRule("Rule 4", ConditionType.CATEGORY, "Software",
                teamId, null, 1, true);

        routingRuleRepository.save(rule1);
        routingRuleRepository.save(rule2);
        routingRuleRepository.save(rule3);
        routingRuleRepository.save(rule4);

        // When: Query enabled rules
        List<RoutingRule> enabledRules = routingRuleRepository.findByEnabledTrueOrderByPriorityAsc();

        // Then: Should return only enabled rules in priority order (ascending)
        assertThat(enabledRules).hasSize(3);
        assertThat(enabledRules).extracting("ruleName")
                .containsExactly("Rule 4", "Rule 2", "Rule 1");
        assertThat(enabledRules).extracting("priority")
                .containsExactly(1, 5, 10);
        assertThat(enabledRules).allMatch(RoutingRule::isEnabled);
    }

    @Test
    void findByEnabledTrueOrderByPriorityAsc_shouldReturnEmptyListWhenNoEnabledRules() {
        // Given: Only disabled rules
        UUID teamId = UUID.randomUUID();
        insertTeam(teamId, "Routing Team B");

        RoutingRule disabledRule = new RoutingRule("Disabled Rule", ConditionType.CATEGORY,
                "Network", teamId, null, 5, false);
        routingRuleRepository.save(disabledRule);

        // When: Query enabled rules
        List<RoutingRule> enabledRules = routingRuleRepository.findByEnabledTrueOrderByPriorityAsc();

        // Then: Should return empty list
        assertThat(enabledRules).isEmpty();
    }

    @Test
    void findByEnabledTrueOrderByPriorityAsc_shouldExcludeDisabledRules() {
        // Given: Mix of enabled and disabled rules
        UUID teamId = UUID.randomUUID();
        insertTeam(teamId, "Routing Team C");

        RoutingRule enabled1 = new RoutingRule("Enabled 1", ConditionType.CATEGORY, "Network",
                teamId, null, 10, true);
        RoutingRule disabled1 = new RoutingRule("Disabled 1", ConditionType.PRIORITY, "HIGH",
                teamId, null, 5, false);
        RoutingRule enabled2 = new RoutingRule("Enabled 2", ConditionType.SUBCATEGORY, "Hardware",
                teamId, null, 15, true);

        routingRuleRepository.save(enabled1);
        routingRuleRepository.save(disabled1);
        routingRuleRepository.save(enabled2);

        // When: Query enabled rules
        List<RoutingRule> enabledRules = routingRuleRepository.findByEnabledTrueOrderByPriorityAsc();

        // Then: Should return only enabled rules
        assertThat(enabledRules).hasSize(2);
        assertThat(enabledRules).extracting("ruleName")
                .containsExactly("Enabled 1", "Enabled 2");
        assertThat(enabledRules).noneMatch(rule -> rule.getRuleName().equals("Disabled 1"));
    }
}
