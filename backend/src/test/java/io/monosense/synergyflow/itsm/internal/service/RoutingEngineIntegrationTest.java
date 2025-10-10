package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RoutingEngineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired JdbcTemplate jdbc;
    @Autowired TicketService ticketService;
    @Autowired TicketRepository ticketRepository;

    static final UUID requester = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID netTeam   = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID genTeam   = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    static final UUID softTeam  = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
    static final UUID net1      = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    static final UUID net2      = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    static final UUID net3      = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    static final UUID senior    = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    @BeforeEach
    void setup() {
        // Align JDBC session with Flyway/Hibernate default schema used in tests
        jdbc.execute("SET search_path TO it_test");

        // Clean mutable tables
        jdbc.update("TRUNCATE TABLE it_test.routing_rules RESTART IDENTITY CASCADE");
        jdbc.update("TRUNCATE TABLE it_test.tickets RESTART IDENTITY CASCADE");

        // Ensure core users exist
        insertUser(requester, "requester", "requester@example.com", "Requester");
        insertUser(net1, "net1", "net1@example.com", "Net One");
        insertUser(net2, "net2", "net2@example.com", "Net Two");
        insertUser(net3, "net3", "net3@example.com", "Net Three");
        insertUser(senior, "senior", "senior@example.com", "Senior Agent");

        // Teams
        insertTeam(netTeam, "IT-ROUTING-Network");
        insertTeam(genTeam, "IT-ROUTING-General");
        insertTeam(softTeam, "IT-ROUTING-Software");

        // Team memberships: all three agents belong to Network and General teams
        addMember("IT-ROUTING-Network", net1);
        addMember("IT-ROUTING-Network", net2);
        addMember("IT-ROUTING-Network", net3);
        addMember("IT-ROUTING-General", net1);
        addMember("IT-ROUTING-General", net2);
        addMember("IT-ROUTING-General", net3);
    }

    // IT-ROUTING-1: Auto-assign by category → Network Team agent
    @Test
    @DisplayName("IT-ROUTING-1 category rule assigns to least-loaded agent")
    void categoryAssignsToNetworkTeamAgent() {
        // Seed workload so net2 is least loaded
        seedAssigned(net1, 2); // net1 has 2
        seedAssigned(net2, 1); // net2 has 1
        seedAssigned(net3, 1); // net3 has 1 so net2 is least-loaded

        insertCategoryTeamRuleByName("Network incidents", "Network", "IT-ROUTING-Network", 1, true);

        Ticket t = createTicket("Router down", TicketType.INCIDENT, Priority.HIGH, "Network");

        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isEqualTo(net2);
    }

    // IT-ROUTING-2: Auto-assign by priority → Senior Agent
    @Test
    @DisplayName("IT-ROUTING-2 priority rule assigns to direct agent")
    void priorityAssignsToDirectAgent() {
        insertPriorityAgentRule("Critical to Senior", "CRITICAL", senior, 1, true);

        Ticket t = createTicket("Prod outage", TicketType.INCIDENT, Priority.CRITICAL, "Network");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isEqualTo(senior);
    }

    // IT-ROUTING-3: First rule wins
    @Test
    @DisplayName("IT-ROUTING-3 first matching rule wins by priority asc")
    void firstRuleWins() {
        insertCategoryTeamRuleByName("Rule A low prio", "Network", "IT-ROUTING-Network", 2, true);
        insertPriorityAgentRule("Rule B higher prio", "HIGH", senior, 1, true);

        Ticket t = createTicket("Switch flap", TicketType.INCIDENT, Priority.HIGH, "Network");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isEqualTo(senior); // priority=1 rule wins
    }

    // IT-ROUTING-4: No matching rule → remains unassigned
    @Test
    @DisplayName("IT-ROUTING-4 no matching rule leaves ticket unassigned")
    void noMatchingRuleLeavesUnassigned() {
        // No rules inserted
        Ticket t = createTicket("Unknown issue", TicketType.INCIDENT, Priority.LOW, "Other");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isNull();
    }

    // IT-ROUTING-5: Round-robin load balancing (global least-loaded heuristic)
    @Test
    @DisplayName("IT-ROUTING-5 round-robin distributes across 3 agents")
    void roundRobinDistributes() {
        insertRoundRobinTeamRuleByName("Fallback RR to General", "IT-ROUTING-General", 10, true);

        // Ensure baseline presence so least-loaded query has candidates
        seedAssigned(net1, 1);
        seedAssigned(net2, 1);
        seedAssigned(net3, 1);

        UUID a1 = reloadAssignee(createTicket("t1", TicketType.INCIDENT, Priority.MEDIUM, null).getId());
        UUID a2 = reloadAssignee(createTicket("t2", TicketType.INCIDENT, Priority.MEDIUM, null).getId());
        UUID a3 = reloadAssignee(createTicket("t3", TicketType.INCIDENT, Priority.MEDIUM, null).getId());

        // Since selection is global least-loaded, we still expect distribution
        Set<UUID> set = new HashSet<>(Arrays.asList(a1, a2, a3));
        assertThat(set).containsExactlyInAnyOrder(net1, net2, net3);
    }

    // IT-ROUTING-6: Direct agent assignment skips round-robin
    @Test
    @DisplayName("IT-ROUTING-6 direct agent rule skips RR")
    void directAgentSkipsRoundRobin() {
        insertPriorityAgentRule("Critical direct", "CRITICAL", net1, 1, true);
        // Seed workload making net1 seem busiest to prove direct rule ignores RR
        seedAssigned(net1, 5);
        seedAssigned(net2, 0);

        Ticket t = createTicket("Major outage", TicketType.INCIDENT, Priority.CRITICAL, "Network");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isEqualTo(net1);
    }

    // IT-ROUTING-7: Disabled rule skipped
    @Test
    @DisplayName("IT-ROUTING-7 disabled rule is ignored")
    void disabledRuleSkipped() {
        insertCategoryTeamRuleByName("Disabled", "Network", "IT-ROUTING-Network", 1, false);
        insertRoundRobinTeamRuleByName("Enabled RR", "IT-ROUTING-General", 2, true);

        // Baseline assignments so RR has candidates
        seedAssigned(net1, 1);
        seedAssigned(net2, 1);
        seedAssigned(net3, 1);

        Ticket t = createTicket("VPN help", TicketType.SERVICE_REQUEST, Priority.LOW, "Network");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isIn(net1, net2, net3);
    }

    // IT-ROUTING-8: Round-robin fallback catches unmapped tickets
    @Test
    @DisplayName("IT-ROUTING-8 RR fallback assigns when no other rule matches")
    void roundRobinFallback() {
        insertRoundRobinTeamRuleByName("Fallback RR", "IT-ROUTING-General", 999, true);
        // Baseline assignments so query returns a candidate
        seedAssigned(net1, 1);
        seedAssigned(net2, 1);
        seedAssigned(net3, 1);
        Ticket t = createTicket("Generic", TicketType.INCIDENT, Priority.MEDIUM, "Unmapped");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isIn(net1, net2, net3);
    }

    // IT-ROUTING-9: Team with no members leaves ticket unassigned
    @Test
    @DisplayName("IT-ROUTING-9 empty team leaves ticket unassigned")
    void emptyTeamLeavesUnassigned() {
        // Create a new team with no members
        UUID emptyTeam = UUID.fromString("00000000-0000-0000-0000-0000000000ee");
        insertTeam(emptyTeam, "IT-ROUTING-Empty");

        // Route by category to empty team
        insertCategoryTeamRuleByName("Empty team route", "Network", "IT-ROUTING-Empty", 1, true);

        Ticket t = createTicket("No agent available", TicketType.INCIDENT, Priority.LOW, "Network");
        UUID assignee = reloadAssignee(t.getId());
        assertThat(assignee).isNull();
    }

    // Helpers
    private void insertUser(UUID id, String username, String email, String fullName) {
        jdbc.update("INSERT INTO it_test.users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, now(), now(), 1) " +
                        "ON CONFLICT (username) DO NOTHING",
                id, username, email, fullName);
    }

    private void insertTeam(UUID id, String name) {
        jdbc.update("INSERT INTO it_test.teams (id, name, description, enabled, created_at, updated_at) VALUES (?, ?, ?, true, now(), now()) ON CONFLICT (name) DO NOTHING",
                id, name, name + " desc");
    }

    private void addMember(String teamName, UUID userId) {
        UUID teamId = teamIdByName(teamName);
        jdbc.update("INSERT INTO it_test.team_members (id, team_id, user_id, added_at, version) VALUES (gen_random_uuid(), ?, ?, now(), 1) ON CONFLICT (team_id, user_id) DO NOTHING",
                teamId, userId);
    }

    private void insertCategoryTeamRuleByName(String ruleName, String category, String teamName, int priority, boolean enabled) {
        UUID teamId = teamIdByName(teamName);
        jdbc.update("INSERT INTO it_test.routing_rules (id, rule_name, condition_type, condition_value, target_team_id, priority, enabled, created_at, updated_at) VALUES (?, ?, 'CATEGORY', ?, ?, ?, ?, now(), now())",
                UUID.randomUUID(), ruleName, category, teamId, priority, enabled);
    }

    private void insertPriorityAgentRule(String ruleName, String priorityValue, UUID agentId, int priority, boolean enabled) {
        jdbc.update("INSERT INTO it_test.routing_rules (id, rule_name, condition_type, condition_value, target_agent_id, priority, enabled, created_at, updated_at) VALUES (?, ?, 'PRIORITY', ?, ?, ?, ?, now(), now())",
                UUID.randomUUID(), ruleName, priorityValue, agentId, priority, enabled);
    }

    private void insertRoundRobinTeamRuleByName(String ruleName, String teamName, int priority, boolean enabled) {
        UUID teamId = teamIdByName(teamName);
        jdbc.update("INSERT INTO it_test.routing_rules (id, rule_name, condition_type, condition_value, target_team_id, priority, enabled, created_at, updated_at) VALUES (?, ?, 'ROUND_ROBIN', NULL, ?, ?, ?, now(), now())",
                UUID.randomUUID(), ruleName, teamId, priority, enabled);
    }

    private void seedAssigned(UUID assigneeId, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO it_test.tickets (id, title, description, status, priority, category, requester_id, assignee_id, ticket_type, created_at, updated_at, version) VALUES (?, 'seed', 'seed', 'ASSIGNED', 'MEDIUM', 'Seed', ?, ?, 'INCIDENT', now(), now(), 1)",
                    UUID.randomUUID(), requester, assigneeId);
        }
    }

    private UUID teamIdByName(String name) {
        return jdbc.queryForObject("SELECT id FROM it_test.teams WHERE name = ?", UUID.class, name);
    }

    private Ticket createTicket(String title, TicketType type, Priority priority, String category) {
        CreateTicketCommand cmd = new CreateTicketCommand(
                title,
                "desc",
                type,
                priority,
                category,
                requester
        );
        return ticketService.createTicket(cmd, requester);
    }

    private UUID reloadAssignee(UUID ticketId) {
        return ticketRepository.findById(ticketId).orElseThrow().getAssigneeId();
    }
}
