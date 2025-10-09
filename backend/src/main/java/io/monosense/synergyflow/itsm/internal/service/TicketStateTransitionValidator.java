package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException;
import io.monosense.synergyflow.itsm.internal.StateTransitionValidator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Validator component for enforcing ticket state machine transition rules.
 *
 * <p>This component maintains a static transition matrix defining all valid state transitions
 * in the ticket lifecycle. It is used by TicketService to validate that state changes
 * follow the defined business rules and prevent invalid transitions.</p>
 *
 * <p>Valid transitions:</p>
 * <ul>
 *   <li>NEW → ASSIGNED (ticket is assigned to an agent)</li>
 *   <li>ASSIGNED → NEW (ticket is unassigned)</li>
 *   <li>ASSIGNED → IN_PROGRESS (agent starts work)</li>
 *   <li>IN_PROGRESS → ASSIGNED (agent pauses work)</li>
 *   <li>IN_PROGRESS → RESOLVED (agent resolves ticket)</li>
 *   <li>RESOLVED → CLOSED (requester confirms resolution)</li>
 *   <li>RESOLVED → NEW (ticket is reopened)</li>
 *   <li>CLOSED → NEW (closed ticket is reopened)</li>
 * </ul>
 *
 * <p>Public visibility within internal package to allow use across internal.* subpackages
 * (service, repository, etc.), but still maintains module boundary - only used within ITSM module.</p>
 *
 * @since 2.2
 */
@Component
class TicketStateTransitionValidator implements StateTransitionValidator {

    /**
     * Static transition matrix defining all valid state transitions.
     * The map key is the source status, and the value is the set of valid target statuses.
     */
    private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.of(
        TicketStatus.NEW, Set.of(TicketStatus.ASSIGNED),
        TicketStatus.ASSIGNED, Set.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
        TicketStatus.IN_PROGRESS, Set.of(TicketStatus.ASSIGNED, TicketStatus.RESOLVED),
        TicketStatus.RESOLVED, Set.of(TicketStatus.NEW, TicketStatus.CLOSED),
        TicketStatus.CLOSED, Set.of(TicketStatus.NEW)
    );

    /**
     * Validates that a state transition is allowed according to the state machine rules.
     *
     * @param from the current status of the ticket
     * @param to   the target status to transition to
     * @throws InvalidStateTransitionException if the transition is not allowed
     * @throws IllegalArgumentException        if either status is null
     */
    @Override
    public void validateTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        if (!isValidTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }

    /**
     * Checks whether a state transition is valid without throwing an exception.
     *
     * @param from the current status of the ticket
     * @param to   the target status to transition to
     * @return true if the transition is valid, false otherwise
     */
    @Override
    public boolean isValidTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }

        Set<TicketStatus> allowedTargets = VALID_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }
}
