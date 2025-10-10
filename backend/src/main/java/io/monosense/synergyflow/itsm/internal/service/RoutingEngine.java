package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.eventing.readmodel.TicketCardRepository;
import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.RoutingRuleRepository;
import io.monosense.synergyflow.itsm.internal.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Routing engine that evaluates routing rules and auto-assigns tickets.
 */
@Service
public class RoutingEngine {

    private static final Logger log = LoggerFactory.getLogger(RoutingEngine.class);

    private final RoutingRuleRepository ruleRepository;
    private final TeamRepository teamRepository;
    private final TicketCardRepository ticketCardRepository;

    RoutingEngine(RoutingRuleRepository ruleRepository,
                  TeamRepository teamRepository,
                  TicketCardRepository ticketCardRepository) {
        this.ruleRepository = ruleRepository;
        this.teamRepository = teamRepository;
        this.ticketCardRepository = ticketCardRepository;
    }

    /**
     * Applies routing rules to a newly created ticket, assigning it when a rule matches.
     */
    public void applyRules(Ticket ticket) {
        if (ticket.getAssigneeId() != null) {
            return; // already assigned
        }

        List<RoutingRule> rules = ruleRepository.findByEnabledTrueOrderByPriorityAsc();
        for (RoutingRule rule : rules) {
            if (matches(rule, ticket)) {
                assignByRule(rule, ticket);
                break; // first match wins
            }
        }
    }

    /**
     * Returns true if the given rule matches the provided ticket.
     */
    boolean matches(RoutingRule rule, Ticket ticket) {
        ConditionType type = rule.getConditionType();
        String value = rule.getConditionValue();

        return switch (type) {
            case CATEGORY -> Objects.equals(value, ticket.getCategory());
            case PRIORITY -> ticket.getPriority() != null && Objects.equals(value, ticket.getPriority().name());
            case SUBCATEGORY -> false; // No subcategory field in current model
            case ROUND_ROBIN -> true;  // fallback always matches
        };
    }

    /**
     * Assigns the ticket per the rule target (agent or team round-robin).
     */
    void assignByRule(RoutingRule rule, Ticket ticket) {
        UUID ticketId = ticket.getId();

        if (rule.getTargetAgentId() != null) {
            // Direct agent assignment
            ticket.assignTo(rule.getTargetAgentId());
            ticket.updateStatus(TicketStatus.ASSIGNED);
            log.debug("Ticket {} auto-assigned to agent {} by rule '{}' (in-create)", ticketId, rule.getTargetAgentId(), rule.getRuleName());
            return;
        }

        if (rule.getTargetTeamId() != null) {
            // Team-based assignment via round-robin
            Optional<Team> teamOpt = teamRepository.findById(rule.getTargetTeamId());
            if (teamOpt.isEmpty()) {
                log.warn("Routing rule '{}' references missing team {}", rule.getRuleName(), rule.getTargetTeamId());
                return;
            }

            UUID agentId = selectAgentRoundRobin(teamOpt.get());
            if (agentId != null) {
                ticket.assignTo(agentId);
                ticket.updateStatus(TicketStatus.ASSIGNED);
                log.debug("Ticket {} auto-assigned to agent {} via team {} by rule '{}' (in-create)", ticketId, agentId, teamOpt.get().getId(), rule.getRuleName());
            } else {
                log.warn("No eligible agent found for team {} when routing ticket {} via rule '{}'", teamOpt.get().getId(), ticketId, rule.getRuleName());
            }
        }
    }

    /**
     * Selects the agent with the fewest open tickets within the given team.
     */
    UUID selectAgentRoundRobin(Team team) {
        return ticketCardRepository.findAgentWithFewestTickets(team.getId()).orElse(null);
    }
}
