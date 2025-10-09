package io.monosense.synergyflow.itsm.internal;

import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import org.springframework.stereotype.Service;

/**
 * Abstraction for validating ticket state transitions within the ITSM module.
 *
 * <p>This interface is intentionally public to allow usage across internal subpackages
 * while keeping concrete implementations package-private. It is annotated with
 * {@code @Service} to satisfy architecture rules that allow public internal
 * types only when marked as services.</p>
 */
@Service
public interface StateTransitionValidator {
    void validateTransition(TicketStatus from, TicketStatus to);
    boolean isValidTransition(TicketStatus from, TicketStatus to);
}

