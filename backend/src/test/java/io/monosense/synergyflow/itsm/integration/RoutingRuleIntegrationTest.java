package io.monosense.synergyflow.itsm.integration;

import io.monosense.synergyflow.itsm.internal.domain.ConditionType;
import io.monosense.synergyflow.itsm.internal.domain.RoutingRule;
import io.monosense.synergyflow.itsm.internal.repository.RoutingRuleRepository;
import org.junit.jupiter.api.Tag;
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
 * Integration tests for RoutingRule entity persistence and queries (Story 2.1 Task 6).
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>IT6: RoutingRule query by enabled status and priority order (findByEnabledTrueOrderByPriorityAsc)</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
@Testcontainers
@Transactional
@ActiveProfiles("test")
@Tag("integration")
class RoutingRuleIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RoutingRuleRepository routingRuleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertTeam(UUID teamId, String teamName) {
        jdbcTemplate.update(
                "INSERT INTO teams (id, team_name, description, is_active, version) VALUES (?, ?, ?, ?, ?)",
                teamId, teamName, "Test team for routing rules", true, 1
        );
    }

    @Test
    void it6_routingRuleQueryByEnabledAndPriorityOrder() {
        UUID networkTeamId = UUID.randomUUID();
        UUID infrastructureTeamId = UUID.randomUUID();
        UUID supportTeamId = UUID.randomUUID();
        UUID archivedTeamId = UUID.randomUUID();

        // Insert teams to satisfy foreign key constraints
        insertTeam(networkTeamId, "Network Team");
        insertTeam(infrastructureTeamId, "Infrastructure Team");
        insertTeam(supportTeamId, "Support Team");
        insertTeam(archivedTeamId, "Archived Team");

        // Create 3 enabled routing rules with different priorities
        RoutingRule rule1 = new RoutingRule(
                "Critical Incidents to Network Team",
                ConditionType.PRIORITY,
                "CRITICAL",
                networkTeamId,
                null,
                1, // Highest priority (evaluated first)
                true
        );
        routingRuleRepository.saveAndFlush(rule1);

        RoutingRule rule2 = new RoutingRule(
                "Infrastructure Category to Infrastructure Team",
                ConditionType.CATEGORY,
                "Infrastructure",
                infrastructureTeamId,
                null,
                2, // Medium priority
                true
        );
        routingRuleRepository.saveAndFlush(rule2);

        RoutingRule rule3 = new RoutingRule(
                "Default Round Robin to Support",
                ConditionType.ROUND_ROBIN,
                null,
                supportTeamId,
                null,
                3, // Lowest priority (fallback rule)
                true
        );
        routingRuleRepository.saveAndFlush(rule3);

        // Create 1 disabled rule (should not be returned)
        RoutingRule disabledRule = new RoutingRule(
                "Archived Rule - Do Not Use",
                ConditionType.CATEGORY,
                "Legacy",
                archivedTeamId,
                null,
                0, // Would be highest priority if enabled
                false // DISABLED
        );
        routingRuleRepository.saveAndFlush(disabledRule);

        // Query enabled rules in priority order
        List<RoutingRule> enabledRules = routingRuleRepository.findByEnabledTrueOrderByPriorityAsc();

        // Verify only 3 enabled rules returned (disabled rule excluded)
        assertThat(enabledRules).hasSize(3);

        // Verify rules returned in priority order (ascending: 1, 2, 3)
        assertThat(enabledRules.get(0).getPriority()).isEqualTo(1);
        assertThat(enabledRules.get(0).getRuleName()).isEqualTo("Critical Incidents to Network Team");
        assertThat(enabledRules.get(0).getConditionType()).isEqualTo(ConditionType.PRIORITY);
        assertThat(enabledRules.get(0).getTargetTeamId()).isEqualTo(networkTeamId);

        assertThat(enabledRules.get(1).getPriority()).isEqualTo(2);
        assertThat(enabledRules.get(1).getRuleName()).isEqualTo("Infrastructure Category to Infrastructure Team");
        assertThat(enabledRules.get(1).getConditionType()).isEqualTo(ConditionType.CATEGORY);
        assertThat(enabledRules.get(1).getTargetTeamId()).isEqualTo(infrastructureTeamId);

        assertThat(enabledRules.get(2).getPriority()).isEqualTo(3);
        assertThat(enabledRules.get(2).getRuleName()).isEqualTo("Default Round Robin to Support");
        assertThat(enabledRules.get(2).getConditionType()).isEqualTo(ConditionType.ROUND_ROBIN);
        assertThat(enabledRules.get(2).getTargetTeamId()).isEqualTo(supportTeamId);

        // Verify all returned rules are enabled
        assertThat(enabledRules).allMatch(RoutingRule::isEnabled);

        // Verify disabled rule is not in the result set
        assertThat(enabledRules).noneMatch(r -> r.getRuleName().equals("Archived Rule - Do Not Use"));
    }
}
