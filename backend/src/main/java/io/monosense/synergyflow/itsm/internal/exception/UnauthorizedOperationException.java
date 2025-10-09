package io.monosense.synergyflow.itsm.internal.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts an operation they are not authorized to perform.
 *
 * <p>This exception is thrown by TicketService methods when authorization checks fail.
 * Examples include:
 * <ul>
 *   <li>A user who is not the assignee attempts to start work on a ticket</li>
 *   <li>A user who is not the assignee attempts to resolve a ticket</li>
 *   <li>A non-manager attempts to reopen a closed ticket</li>
 * </ul>
 *
 * <p>Public visibility within internal package to allow use across internal.* subpackages
 * (service, repository, etc.), but still maintains module boundary - only used within ITSM module.</p>
 *
 * @since 2.2
 */
public class UnauthorizedOperationException extends RuntimeException {

    private final String operation;
    private final UUID userId;

    /**
     * Constructs a new UnauthorizedOperationException with the specified operation and user ID.
     *
     * @param operation the operation that was attempted
     * @param userId    the ID of the user who attempted the operation
     */
    public UnauthorizedOperationException(String operation, UUID userId) {
        super("User " + userId + " not authorized for operation: " + operation);
        this.operation = operation;
        this.userId = userId;
    }

    /**
     * Returns the operation that was attempted.
     *
     * @return the operation name
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns the ID of the user who attempted the operation.
     *
     * @return the user ID
     */
    public UUID getUserId() {
        return userId;
    }
}
