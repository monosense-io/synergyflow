package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.eventing.readmodel.TicketCardRepository;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.RoutingRuleRepository;
import io.monosense.synergyflow.itsm.internal.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoutingEngine Unit Tests")
class RoutingEngineTest {

    @Mock RoutingRuleRepository ruleRepository;
    @Mock TeamRepository teamRepository;
    @Mock TicketCardRepository ticketCardRepository;

    private RoutingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RoutingEngine(ruleRepository, teamRepository, ticketCardRepository);
    }

    private Incident incident(String category, Priority priority) {
        return new Incident(
                "Network down",
                "WAN is unreachable",
                TicketStatus.NEW,
                priority,
                category,
                UUID.randomUUID(),
                null,
                null
        );
    }

    @Test
    @DisplayName("UT-ROUTING-1: CATEGORY match returns true when equal")
    void categoryMatch() {
        RoutingRule rule = new RoutingRule("Category Network", ConditionType.CATEGORY, "Network", UUID.randomUUID(), null, 1, true);
        Ticket ticket = incident("Network", Priority.HIGH);
        assertThat(engine.matches(rule, ticket)).isTrue();
    }

    @Test
    @DisplayName("UT-ROUTING-2: PRIORITY match returns true when equal")
    void priorityMatch() {
        RoutingRule rule = new RoutingRule("Critical incidents", ConditionType.PRIORITY, "CRITICAL", null, UUID.randomUUID(), 1, true);
        Ticket ticket = incident("Network", Priority.CRITICAL);
        assertThat(engine.matches(rule, ticket)).isTrue();
    }

    @Test
    @DisplayName("UT-ROUTING-3: ROUND_ROBIN always matches")
    void roundRobinAlwaysMatches() {
        RoutingRule rule = new RoutingRule("Fallback", ConditionType.ROUND_ROBIN, null, UUID.randomUUID(), null, 999, true);
        Ticket ticket = incident(null, Priority.MEDIUM);
        assertThat(engine.matches(rule, ticket)).isTrue();
    }

    @Test
    @DisplayName("UT-ROUTING-5: CATEGORY no match returns false")
    void categoryNoMatch() {
        RoutingRule rule = new RoutingRule("Category Hardware", ConditionType.CATEGORY, "Hardware", UUID.randomUUID(), null, 1, true);
        Ticket ticket = incident("Software", Priority.LOW);
        assertThat(engine.matches(rule, ticket)).isFalse();
    }

    @Test
    @DisplayName("UT-ROUTING-6: applyRules assigns to direct agent and updates fields")
    void applyRulesDirectAgent() {
        UUID agentId = UUID.randomUUID();
        RoutingRule rule = new RoutingRule("Direct to agent", ConditionType.PRIORITY, "HIGH", null, agentId, 1, true);
        when(ruleRepository.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(rule));

        Ticket ticket = incident("Network", Priority.HIGH);
        engine.applyRules(ticket);
        assertThat(ticket.getAssigneeId()).isEqualTo(agentId);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    }

    @Test
    @DisplayName("UT-ROUTING-6b: applyRules team rule uses round-robin and assigns in-place")
    void applyRulesTeamRoundRobin() {
        UUID teamId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        RoutingRule rule = new RoutingRule("Network team", ConditionType.CATEGORY, "Network", teamId, null, 1, true);
        when(ruleRepository.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(rule));

        // Mock Team entity
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(teamId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        when(ticketCardRepository.findAgentWithFewestTickets(teamId)).thenReturn(Optional.of(agentId));

        Ticket ticket = incident("Network", Priority.MEDIUM);
        engine.applyRules(ticket);
        assertThat(ticket.getAssigneeId()).isEqualTo(agentId);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    }

    @Test
    @DisplayName("UT-ROUTING-7: already-assigned ticket is not modified")
    void alreadyAssignedIsNotModified() {
        Incident ticket = incident("Network", Priority.MEDIUM);
        UUID preAssigned = UUID.randomUUID();
        ticket.assignTo(preAssigned);
        ticket.updateStatus(TicketStatus.ASSIGNED);

        engine.applyRules(ticket);

        assertThat(ticket.getAssigneeId()).isEqualTo(preAssigned);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    }
}
